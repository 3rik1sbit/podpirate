package com.podpirate.controller

import com.podpirate.config.PodPirateProperties
import com.podpirate.model.EpisodeStatus
import com.podpirate.repository.AdSegmentRepository
import com.podpirate.repository.EpisodeRepository
import com.podpirate.repository.PodcastRepository
import com.podpirate.repository.TranscriptionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*
import java.nio.file.Files
import java.nio.file.Path

data class EpisodeSummary(
    val id: Long,
    val title: String,
    val podcastTitle: String,
    val durationSeconds: Long?,
)

data class PodcastStorage(
    val podcastId: Long,
    val podcastTitle: String,
    val audioBytes: Long,
    val processedBytes: Long,
    val episodeCount: Int,
)

data class StatsResponse(
    val pipeline: Map<String, Long>,
    val totalEpisodes: Long,
    val remainingEpisodes: Long,
    val etaSeconds: Double?,
    val totalPodcasts: Long,
    val totalAudioSeconds: Long,
    val avgDurationSeconds: Double,
    val totalAdSeconds: Double,
    val adSourceCounts: Map<String, Long>,
    val transcriptionSegments: Long,
    val longestEpisode: EpisodeSummary?,
    val shortestEpisode: EpisodeSummary?,
    val mostAdHeavyEpisode: EpisodeSummary?,
    val storage: List<PodcastStorage>,
)

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = ["*"])
class StatsController(
    private val episodeRepository: EpisodeRepository,
    private val adSegmentRepository: AdSegmentRepository,
    private val podcastRepository: PodcastRepository,
    private val transcriptionRepository: TranscriptionRepository,
    private val properties: PodPirateProperties,
) {

    @GetMapping("/stats")
    fun getStats(): StatsResponse {
        val pipeline = episodeRepository.countByStatusGrouped()
            .associate { row -> (row[0] as EpisodeStatus).name to (row[1] as Long) }

        val totalEpisodes = pipeline.values.sum()
        val remaining = episodeRepository.countRemaining()
        val avgReady = episodeRepository.avgDurationOfReady()
        // Rough ETA: remaining episodes * average duration of ready episodes (whisper ~ realtime)
        val etaSeconds = if (remaining > 0 && avgReady > 0) remaining * avgReady else null

        val topOne = PageRequest.of(0, 1)

        val longest = episodeRepository.findTopByDurationIsNotNullOrderByDurationDesc(topOne)
            .content.firstOrNull()?.let {
                EpisodeSummary(it.id, it.title, it.podcast.title, it.duration)
            }

        val shortest = episodeRepository.findTopByDurationIsNotNullOrderByDurationAsc(topOne)
            .content.firstOrNull()?.let {
                EpisodeSummary(it.id, it.title, it.podcast.title, it.duration)
            }

        val adSourceCounts = adSegmentRepository.countBySourceGrouped()
            .associate { row -> (row[0]).toString() to (row[1] as Long) }

        val mostAdHeavy = adSegmentRepository.findMostAdHeavyEpisodeId(topOne)
            .firstOrNull()?.let { row ->
                val epId = row[0] as Long
                episodeRepository.findById(epId).orElse(null)?.let { ep ->
                    EpisodeSummary(ep.id, ep.title, ep.podcast.title, ep.duration)
                }
            }

        val segments = try {
            transcriptionRepository.countTotalSegments()
        } catch (_: Exception) {
            0L
        }

        val storage = calculateStorage()

        return StatsResponse(
            pipeline = pipeline,
            totalEpisodes = totalEpisodes,
            remainingEpisodes = remaining,
            etaSeconds = etaSeconds,
            totalPodcasts = podcastRepository.count(),
            totalAudioSeconds = episodeRepository.sumDuration(),
            avgDurationSeconds = episodeRepository.avgDuration(),
            totalAdSeconds = adSegmentRepository.sumAdDuration(),
            adSourceCounts = adSourceCounts,
            transcriptionSegments = segments,
            longestEpisode = longest,
            shortestEpisode = shortest,
            mostAdHeavyEpisode = mostAdHeavy,
            storage = storage,
        )
    }

    private fun calculateStorage(): List<PodcastStorage> {
        // Build episode ID → podcast lookup
        val episodePodcastMap = episodeRepository.findAll()
            .associate { it.id to it.podcast }

        // Scan audio and processed directories for episode files
        val audioSizes = scanDir(Path.of(properties.audioDir))
        val processedSizes = scanDir(Path.of(properties.processedDir))

        // Group by podcast
        val podcastAudio = mutableMapOf<Long, Long>()
        val podcastProcessed = mutableMapOf<Long, Long>()
        val podcastEpisodes = mutableMapOf<Long, MutableSet<Long>>()

        for ((episodeId, bytes) in audioSizes) {
            val podcast = episodePodcastMap[episodeId] ?: continue
            podcastAudio.merge(podcast.id, bytes) { a, b -> a + b }
            podcastEpisodes.getOrPut(podcast.id) { mutableSetOf() }.add(episodeId)
        }
        for ((episodeId, bytes) in processedSizes) {
            val podcast = episodePodcastMap[episodeId] ?: continue
            podcastProcessed.merge(podcast.id, bytes) { a, b -> a + b }
            podcastEpisodes.getOrPut(podcast.id) { mutableSetOf() }.add(episodeId)
        }

        val allPodcastIds = podcastAudio.keys + podcastProcessed.keys
        return allPodcastIds.map { podcastId ->
            val podcast = episodePodcastMap.values.first { it.id == podcastId }
            PodcastStorage(
                podcastId = podcastId,
                podcastTitle = podcast.title,
                audioBytes = podcastAudio[podcastId] ?: 0,
                processedBytes = podcastProcessed[podcastId] ?: 0,
                episodeCount = podcastEpisodes[podcastId]?.size ?: 0,
            )
        }.sortedByDescending { it.audioBytes + it.processedBytes }
    }

    private fun scanDir(dir: Path): Map<Long, Long> {
        if (!Files.isDirectory(dir)) return emptyMap()
        val sizes = mutableMapOf<Long, Long>()
        Files.list(dir).use { stream ->
            stream.forEach { file ->
                // Files are named episode_{id}.ext
                val name = file.fileName.toString()
                val id = name.removePrefix("episode_").substringBefore(".").toLongOrNull()
                if (id != null) {
                    sizes[id] = Files.size(file)
                }
            }
        }
        return sizes
    }
}
