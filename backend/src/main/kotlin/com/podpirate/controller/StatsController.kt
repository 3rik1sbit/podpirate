package com.podpirate.controller

import com.podpirate.model.EpisodeStatus
import com.podpirate.repository.AdSegmentRepository
import com.podpirate.repository.EpisodeRepository
import com.podpirate.repository.PodcastRepository
import com.podpirate.repository.TranscriptionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*

data class EpisodeSummary(
    val id: Long,
    val title: String,
    val podcastTitle: String,
    val durationSeconds: Long?,
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
)

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = ["*"])
class StatsController(
    private val episodeRepository: EpisodeRepository,
    private val adSegmentRepository: AdSegmentRepository,
    private val podcastRepository: PodcastRepository,
    private val transcriptionRepository: TranscriptionRepository,
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
        )
    }
}
