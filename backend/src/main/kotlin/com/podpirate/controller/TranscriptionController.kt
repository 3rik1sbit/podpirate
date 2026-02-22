package com.podpirate.controller

import com.podpirate.model.AdSegment
import com.podpirate.model.AdSegmentSource
import com.podpirate.model.EpisodeStatus
import com.podpirate.repository.AdSegmentRepository
import com.podpirate.repository.EpisodeRepository
import com.podpirate.service.AdDetectionService
import com.podpirate.service.AudioProcessingService
import com.podpirate.service.TranscriptionService
import com.podpirate.repository.PodcastRepository
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = ["*"])
class TranscriptionController(
    private val transcriptionService: TranscriptionService,
    private val adSegmentRepository: AdSegmentRepository,
    private val episodeRepository: EpisodeRepository,
    private val podcastRepository: PodcastRepository,
    private val audioProcessingService: AudioProcessingService,
    private val adDetectionService: AdDetectionService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/episodes/{id}/transcription")
    fun getTranscription(@PathVariable id: Long): ResponseEntity<Any> {
        val transcription = transcriptionService.getTranscription(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(transcription)
    }

    @GetMapping("/episodes/{id}/ad-segments")
    fun getAdSegments(@PathVariable id: Long): List<AdSegment> {
        return adSegmentRepository.findByEpisodeIdOrderByStartTime(id)
    }

    data class AdSegmentDto(
        val startTime: Double,
        val endTime: Double,
        val source: String = "MANUAL",
        val confirmed: Boolean = true,
    )

    @PutMapping("/episodes/{id}/ad-segments")
    @Transactional
    fun updateAdSegments(@PathVariable id: Long, @RequestBody segments: List<AdSegmentDto>): ResponseEntity<List<AdSegment>> {
        val episode = episodeRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()

        adSegmentRepository.deleteByEpisodeId(id)

        val saved = adSegmentRepository.saveAll(segments.map {
            AdSegment(
                episode = episode,
                startTime = it.startTime,
                endTime = it.endTime,
                source = AdSegmentSource.valueOf(it.source),
                confirmed = it.confirmed,
            )
        })

        return ResponseEntity.ok(saved)
    }

    @PostMapping("/episodes/{id}/reprocess")
    fun reprocess(@PathVariable id: Long): ResponseEntity<Any> {
        val episode = episodeRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()

        episodeRepository.save(episode.copy(status = EpisodeStatus.PROCESSING))
        audioProcessingService.processAsync(id)

        return ResponseEntity.accepted().body(mapOf("message" to "Reprocessing started"))
    }

    @PostMapping("/podcasts/{id}/redetect-ads")
    @Transactional
    fun redetectPodcastAds(@PathVariable id: Long): ResponseEntity<Any> {
        if (!podcastRepository.existsById(id)) {
            return ResponseEntity.notFound().build()
        }

        val episodes = episodeRepository.findByPodcastId(id)
            .filter { it.status == EpisodeStatus.READY }

        var queued = 0
        for (episode in episodes) {
            episodeRepository.save(episode.copy(status = EpisodeStatus.DETECTING_ADS))
            adDetectionService.detectAdsAsync(episode.id)
            queued++
        }

        log.info("Re-detecting ads for podcast {} — queued {} episodes", id, queued)
        return ResponseEntity.accepted().body(mapOf("message" to "Re-detecting ads for $queued episodes"))
    }
}
