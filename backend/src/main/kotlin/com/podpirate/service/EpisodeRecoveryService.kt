package com.podpirate.service

import com.podpirate.model.EpisodeStatus
import com.podpirate.repository.EpisodeRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EpisodeRecoveryService(
    private val episodeRepository: EpisodeRepository,
    private val episodeDownloadService: EpisodeDownloadService,
    private val transcriptionService: TranscriptionService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun recoverOnStartup() {
        // Reset stuck DOWNLOADING/TRANSCRIBING/DETECTING_ADS back to their previous state
        val downloading = episodeRepository.findByStatus(EpisodeStatus.DOWNLOADING)
        val transcribing = episodeRepository.findByStatus(EpisodeStatus.TRANSCRIBING)
        val detectingAds = episodeRepository.findByStatus(EpisodeStatus.DETECTING_ADS)

        for (ep in downloading) {
            episodeRepository.save(ep.copy(status = EpisodeStatus.PENDING))
        }
        for (ep in transcribing) {
            episodeRepository.save(ep.copy(status = EpisodeStatus.DOWNLOADED))
        }
        for (ep in detectingAds) {
            episodeRepository.save(ep.copy(status = EpisodeStatus.DOWNLOADED))
        }

        if (downloading.isNotEmpty() || transcribing.isNotEmpty() || detectingAds.isNotEmpty()) {
            log.info("Reset ${downloading.size} downloading, ${transcribing.size} transcribing, ${detectingAds.size} detecting-ads episodes")
        }

        // Now resume: download pending, transcribe downloaded (ordered by priority then newest first)
        val pending = episodeRepository.findByStatusOrdered(EpisodeStatus.PENDING)
        val downloaded = episodeRepository.findByStatusOrdered(EpisodeStatus.DOWNLOADED)

        if (pending.isNotEmpty()) {
            log.info("Resuming downloads for ${pending.size} pending episodes")
            pending.forEach { episodeDownloadService.downloadAsync(it.id) }
        }

        if (downloaded.isNotEmpty()) {
            log.info("Resuming transcription for ${downloaded.size} downloaded episodes")
            downloaded.forEach { transcriptionService.transcribeAsync(it.id) }
        }
    }
}
