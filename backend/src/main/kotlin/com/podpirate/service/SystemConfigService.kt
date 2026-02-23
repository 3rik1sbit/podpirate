package com.podpirate.service

import com.podpirate.model.EpisodeStatus
import com.podpirate.model.SystemConfig
import com.podpirate.repository.EpisodeRepository
import com.podpirate.repository.SystemConfigRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean

@Service
class SystemConfigService(
    private val systemConfigRepository: SystemConfigRepository,
    private val episodeRepository: EpisodeRepository,
    private val applicationContext: ApplicationContext,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val aiPaused = AtomicBoolean(false)

    @PostConstruct
    fun init() {
        val saved = systemConfigRepository.findById("ai_paused").orElse(null)
        if (saved != null) {
            aiPaused.set(saved.value == "true")
        }
        log.info("AI paused state loaded: ${aiPaused.get()}")
    }

    fun isAiPaused(): Boolean = aiPaused.get()

    fun setAiPaused(paused: Boolean) {
        aiPaused.set(paused)
        systemConfigRepository.save(SystemConfig(key = "ai_paused", value = paused.toString()))
        log.info("AI paused set to $paused")

        if (!paused) {
            resumeStuckEpisodes()
        }
    }

    private fun resumeStuckEpisodes() {
        // Use ApplicationContext to break circular dependency
        val transcriptionService = applicationContext.getBean(TranscriptionService::class.java)
        val adDetectionService = applicationContext.getBean(AdDetectionService::class.java)

        val downloaded = episodeRepository.findByStatus(EpisodeStatus.DOWNLOADED)
        for (episode in downloaded) {
            log.info("Resuming transcription for episode ${episode.id}: ${episode.title}")
            transcriptionService.transcribeAsync(episode.id)
        }

        val detectingAds = episodeRepository.findByStatus(EpisodeStatus.DETECTING_ADS)
        for (episode in detectingAds) {
            log.info("Resuming ad detection for episode ${episode.id}: ${episode.title}")
            adDetectionService.detectAdsAsync(episode.id)
        }

        if (downloaded.isNotEmpty() || detectingAds.isNotEmpty()) {
            log.info("Resumed ${downloaded.size} transcriptions and ${detectingAds.size} ad detections")
        }
    }
}
