package com.podpirate.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.podpirate.config.PodPirateProperties
import com.podpirate.model.AdSegment
import com.podpirate.model.AdSegmentSource
import com.podpirate.model.EpisodeStatus
import com.podpirate.repository.AdSegmentRepository
import com.podpirate.repository.EpisodeRepository
import com.podpirate.repository.PodcastRepository
import com.podpirate.repository.TranscriptionRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

@Service
class AdDetectionService(
    private val episodeRepository: EpisodeRepository,
    private val transcriptionRepository: TranscriptionRepository,
    private val adSegmentRepository: AdSegmentRepository,
    private val podcastRepository: PodcastRepository,
    private val audioProcessingService: AudioProcessingService,
    private val systemConfigService: SystemConfigService,
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val properties: PodPirateProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async("adDetectionExecutor")
    fun detectAdsAsync(episodeId: Long) {
        if (systemConfigService.isAiPaused()) {
            log.info("AI is paused, skipping ad detection for episode $episodeId")
            return
        }
        try {
            detectAds(episodeId)
        } catch (e: Exception) {
            log.error("Failed to detect ads for episode $episodeId, skipping ad detection", e)
            // Don't block the pipeline — skip ad detection and proceed to audio processing
            adSegmentRepository.deleteByEpisodeId(episodeId)
            log.info("Skipped ad detection for episode $episodeId, proceeding to audio processing")
            audioProcessingService.processAsync(episodeId)
        }
    }

    fun detectAds(episodeId: Long) {
        val episode = episodeRepository.findById(episodeId).orElseThrow()
        val transcription = transcriptionRepository.findByEpisodeId(episodeId)
            .orElseThrow { IllegalStateException("No transcription found") }

        episodeRepository.save(episode.copy(status = EpisodeStatus.DETECTING_ADS))

        val segments = objectMapper.readTree(transcription.segments)
        val podcastId = episode.podcast.id

        val hintsSection = buildHintsSection(podcastId)
        val transcript = buildTranscriptWithMarkers(segments, podcastId)
        val fewShotSection = buildFewShotExamples(podcastId, episodeId)

        val prompt = """Analyze this podcast transcript and identify advertisement segments.
For each ad segment, provide the start and end timestamps in seconds.

Ads typically include:
- Sponsor mentions ("this episode is brought to you by", "sponsored by")
- Promo codes and discount offers
- Product pitches unrelated to the podcast topic
- Mid-roll ad reads
$hintsSection$fewShotSection
Return ONLY a JSON array of ad segments, no other text. Example format:
[{"start": 120.5, "end": 180.3}, {"start": 450.0, "end": 510.2}]

If no ads are found, return an empty array: []

Transcript:
$transcript"""

        val ollamaRequest = mapOf(
            "model" to properties.ollamaModel,
            "prompt" to prompt,
            "stream" to false,
            "format" to "json",
        )

        val response = webClient.post()
            .uri("${properties.ollamaUrl}/api/generate")
            .bodyValue(ollamaRequest)
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .block(Duration.ofMinutes(10))

        val responseText = response?.get("response")?.asText() ?: "[]"

        // Parse ad segments from response
        val detectedAds = try {
            val parsed = objectMapper.readTree(responseText)
            val adArray = if (parsed.isArray) {
                parsed
            } else {
                parsed["ads"]
                    ?: parsed.fields().let { if (it.hasNext()) it.next().value else null }
                    ?: objectMapper.createArrayNode()
            }
            adArray.mapNotNull { node ->
                val start = node["start"]?.asDouble() ?: return@mapNotNull null
                val end = node["end"]?.asDouble() ?: return@mapNotNull null
                if (end > start) AdSegment(
                    episode = episode,
                    startTime = start,
                    endTime = end,
                    source = AdSegmentSource.AUTO,
                ) else null
            }
        } catch (e: Exception) {
            log.warn("Failed to parse ad detection response for episode $episodeId: $responseText", e)
            emptyList()
        }

        // Clear old auto-detected segments and save new ones
        adSegmentRepository.deleteByEpisodeId(episodeId)
        adSegmentRepository.saveAll(detectedAds)

        log.info("Detected ${detectedAds.size} ad segments for episode ${episode.id}")

        // Trigger audio processing
        audioProcessingService.processAsync(episodeId)
    }

    @Async("aiExecutor")
    fun extractAdHintsAsync(podcastId: Long) {
        if (systemConfigService.isAiPaused()) {
            log.info("AI is paused, skipping ad hint extraction for podcast $podcastId")
            return
        }
        try {
            extractAndSaveAdHints(podcastId)
        } catch (e: Exception) {
            log.error("Failed to extract ad hints for podcast $podcastId", e)
        }
    }

    fun extractAndSaveAdHints(podcastId: Long) {
        val manualSegments = adSegmentRepository.findByEpisodePodcastIdAndSource(podcastId, AdSegmentSource.MANUAL)
        if (manualSegments.isEmpty()) {
            log.info("No manual segments for podcast $podcastId, skipping hint extraction")
            return
        }

        // Collect transcript text overlapping with manual ad segments, grouped by episode
        val byEpisode = manualSegments.groupBy { it.episode.id }
        val adTranscriptTexts = mutableListOf<String>()

        for ((epId, adSegs) in byEpisode) {
            val trans = transcriptionRepository.findByEpisodeId(epId).orElse(null) ?: continue
            val segments = objectMapper.readTree(trans.segments)

            val overlapping = segments.filter { seg ->
                val start = seg["start"]?.asDouble() ?: 0.0
                adSegs.any { ad -> start >= ad.startTime && start <= ad.endTime }
            }.joinToString(" ") { it["text"]?.asText() ?: "" }

            if (overlapping.isNotBlank()) {
                adTranscriptTexts.add(overlapping.trim())
            }
        }

        if (adTranscriptTexts.isEmpty()) {
            log.info("No transcript text found for manual segments in podcast $podcastId")
            return
        }

        val combinedText = adTranscriptTexts.joinToString("\n---\n")

        val prompt = """Below are transcript excerpts from manually-tagged advertisement segments in a podcast.
Analyze them and extract:
1. Brand/advertiser names mentioned
2. Recurring ad patterns (e.g. "use code X for", "dot com slash")
3. Short sample phrases the hosts use to introduce or deliver ads

Return ONLY a JSON object with this format, no other text:
{"brands": ["Brand1", "Brand2"], "patterns": ["pattern1", "pattern2"], "samplePhrases": ["phrase1", "phrase2"]}

Ad transcript excerpts:
$combinedText"""

        val ollamaRequest = mapOf(
            "model" to properties.ollamaModel,
            "prompt" to prompt,
            "stream" to false,
            "format" to "json",
        )

        val response = webClient.post()
            .uri("${properties.ollamaUrl}/api/generate")
            .bodyValue(ollamaRequest)
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .block(Duration.ofMinutes(5))

        val responseText = response?.get("response")?.asText() ?: return

        // Validate it's parseable JSON with expected structure
        try {
            val parsed = objectMapper.readTree(responseText)
            if (parsed.has("brands") || parsed.has("patterns") || parsed.has("samplePhrases")) {
                val podcast = podcastRepository.findById(podcastId).orElseThrow()
                podcastRepository.save(podcast.copy(adDetectionHints = responseText))
                log.info("Saved ad detection hints for podcast $podcastId: $responseText")
            } else {
                log.warn("Ad hint response missing expected fields for podcast $podcastId: $responseText")
            }
        } catch (e: Exception) {
            log.warn("Failed to parse ad hint response for podcast $podcastId: $responseText", e)
        }
    }

    private fun buildHintsSection(podcastId: Long): String {
        val podcast = podcastRepository.findById(podcastId).orElse(null) ?: return ""
        val hintsJson = podcast.adDetectionHints ?: return ""

        return try {
            val hints = objectMapper.readTree(hintsJson)
            val parts = mutableListOf<String>()

            val brands = hints["brands"]?.mapNotNull { it.asText().takeIf { t -> t.isNotBlank() } }
            if (!brands.isNullOrEmpty()) {
                parts.add("Known advertisers on this podcast: ${brands.joinToString(", ")}")
            }

            val patterns = hints["patterns"]?.mapNotNull { it.asText().takeIf { t -> t.isNotBlank() } }
            if (!patterns.isNullOrEmpty()) {
                parts.add("Ad patterns to look for: ${patterns.joinToString("; ")}")
            }

            val phrases = hints["samplePhrases"]?.mapNotNull { it.asText().takeIf { t -> t.isNotBlank() } }
            if (!phrases.isNullOrEmpty()) {
                parts.add("The hosts typically introduce ads with: ${phrases.joinToString("; ")}")
            }

            if (parts.isEmpty()) return ""
            "\n${parts.joinToString("\n")}\n\nNote: Lines marked [POSSIBLE AD] contain known advertiser names.\n"
        } catch (e: Exception) {
            log.warn("Failed to parse ad detection hints for podcast $podcastId", e)
            ""
        }
    }

    private fun buildTranscriptWithMarkers(segments: JsonNode, podcastId: Long): String {
        val brandNames = try {
            val podcast = podcastRepository.findById(podcastId).orElse(null)
            val hintsJson = podcast?.adDetectionHints ?: return buildTranscriptText(segments)
            val hints = objectMapper.readTree(hintsJson)
            hints["brands"]?.mapNotNull { it.asText().takeIf { t -> t.isNotBlank() } }
                ?.map { it.lowercase() }
                ?: return buildTranscriptText(segments)
        } catch (e: Exception) {
            return buildTranscriptText(segments)
        }

        if (brandNames.isEmpty()) return buildTranscriptText(segments)

        return segments.joinToString("\n") { seg ->
            val start = seg["start"]?.asDouble() ?: 0.0
            val end = seg["end"]?.asDouble() ?: 0.0
            val text = seg["text"]?.asText() ?: ""
            val textLower = text.lowercase()
            val hasMatch = brandNames.any { brand -> textLower.contains(brand) }
            val marker = if (hasMatch) "[POSSIBLE AD] " else ""
            "[%.1f-%.1f] %s%s".format(start, end, marker, text)
        }
    }

    private fun buildFewShotExamples(podcastId: Long, currentEpisodeId: Long): String {
        val manualSegments = adSegmentRepository.findByEpisodePodcastIdAndSource(podcastId, AdSegmentSource.MANUAL)
            .filter { it.episode.id != currentEpisodeId }

        // Group by episode, take up to 3 episodes
        val byEpisode = manualSegments.groupBy { it.episode.id }.entries.take(3)
        if (byEpisode.isEmpty()) return ""

        val examples = byEpisode.mapNotNull { (epId, adSegs) ->
            val trans = transcriptionRepository.findByEpisodeId(epId).orElse(null) ?: return@mapNotNull null
            val segments = objectMapper.readTree(trans.segments)

            // Find transcript lines that overlap with manual ad segments
            val adLines = segments.filter { seg ->
                val start = seg["start"]?.asDouble() ?: 0.0
                adSegs.any { ad -> start >= ad.startTime && start <= ad.endTime }
            }.joinToString("\n") { seg ->
                val start = seg["start"]?.asDouble() ?: 0.0
                val end = seg["end"]?.asDouble() ?: 0.0
                val text = seg["text"]?.asText() ?: ""
                "[%.1f-%.1f] %s".format(start, end, text)
            }

            if (adLines.isBlank()) null
            else "Example ad from this podcast:\n$adLines"
        }

        if (examples.isEmpty()) return ""
        return "\nHere are examples of ads previously identified in this podcast:\n${examples.joinToString("\n\n")}\n"
    }

    private fun buildTranscriptText(segments: JsonNode): String {
        return segments.joinToString("\n") { seg ->
            val start = seg["start"]?.asDouble() ?: 0.0
            val end = seg["end"]?.asDouble() ?: 0.0
            val text = seg["text"]?.asText() ?: ""
            "[%.1f-%.1f] %s".format(start, end, text)
        }
    }
}
