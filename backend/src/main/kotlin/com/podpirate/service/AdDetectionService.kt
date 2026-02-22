package com.podpirate.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.podpirate.config.PodPirateProperties
import com.podpirate.model.AdSegment
import com.podpirate.model.AdSegmentSource
import com.podpirate.model.EpisodeStatus
import com.podpirate.repository.AdSegmentRepository
import com.podpirate.repository.EpisodeRepository
import com.podpirate.repository.TranscriptionRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient

@Service
class AdDetectionService(
    private val episodeRepository: EpisodeRepository,
    private val transcriptionRepository: TranscriptionRepository,
    private val adSegmentRepository: AdSegmentRepository,
    private val audioProcessingService: AudioProcessingService,
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val properties: PodPirateProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    fun detectAdsAsync(episodeId: Long) {
        try {
            detectAds(episodeId)
        } catch (e: Exception) {
            log.error("Failed to detect ads for episode $episodeId", e)
            val episode = episodeRepository.findById(episodeId).orElseThrow()
            episodeRepository.save(episode.copy(status = EpisodeStatus.ERROR))
        }
    }

    @Transactional
    fun detectAds(episodeId: Long) {
        val episode = episodeRepository.findById(episodeId).orElseThrow()
        val transcription = transcriptionRepository.findByEpisodeId(episodeId)
            .orElseThrow { IllegalStateException("No transcription found") }

        episodeRepository.save(episode.copy(status = EpisodeStatus.DETECTING_ADS))

        val segments = objectMapper.readTree(transcription.segments)
        val transcript = buildTranscriptText(segments)

        val prompt = """Analyze this podcast transcript and identify advertisement segments.
For each ad segment, provide the start and end timestamps in seconds.

Ads typically include:
- Sponsor mentions ("this episode is brought to you by", "sponsored by")
- Promo codes and discount offers
- Product pitches unrelated to the podcast topic
- Mid-roll ad reads

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
            .block()

        val responseText = response?.get("response")?.asText() ?: "[]"

        // Parse ad segments from response
        val detectedAds = try {
            val parsed = objectMapper.readTree(responseText)
            val adArray = if (parsed.isArray) parsed else parsed["ads"] ?: parsed.fields().next()?.value ?: objectMapper.createArrayNode()
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

    private fun buildTranscriptText(segments: JsonNode): String {
        return segments.joinToString("\n") { seg ->
            val start = seg["start"]?.asDouble() ?: 0.0
            val end = seg["end"]?.asDouble() ?: 0.0
            val text = seg["text"]?.asText() ?: ""
            "[%.1f-%.1f] %s".format(start, end, text)
        }
    }
}
