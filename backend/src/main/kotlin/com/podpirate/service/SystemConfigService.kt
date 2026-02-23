package com.podpirate.service

import com.podpirate.config.PodPirateProperties
import com.podpirate.model.EpisodeStatus
import com.podpirate.model.SystemConfig
import com.podpirate.repository.EpisodeRepository
import com.podpirate.repository.SystemConfigRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

@Service
class SystemConfigService(
    private val systemConfigRepository: SystemConfigRepository,
    private val episodeRepository: EpisodeRepository,
    private val applicationContext: ApplicationContext,
    private val properties: PodPirateProperties,
    private val webClient: WebClient,
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
        if (!paused) {
            checkAiServicesAvailable()
        }

        aiPaused.set(paused)
        systemConfigRepository.save(SystemConfig(key = "ai_paused", value = paused.toString()))
        log.info("AI paused set to $paused")

        if (!paused) {
            resumeStuckEpisodes()
        }
    }

    private fun checkAiServicesAvailable() {
        val timeout = Duration.ofSeconds(3)
        val errors = mutableListOf<String>()

        try {
            webClient.get()
                .uri("${properties.whisperUrl}/health")
                .retrieve()
                .toBodilessEntity()
                .block(timeout)
        } catch (e: Exception) {
            log.warn("Whisper service health check failed: ${e.message}")
            errors.add("Whisper service unreachable at ${properties.whisperUrl}")
        }

        try {
            webClient.get()
                .uri("${properties.ollamaUrl}/api/tags")
                .retrieve()
                .toBodilessEntity()
                .block(timeout)
        } catch (e: Exception) {
            log.warn("Ollama service health check failed: ${e.message}")
            errors.add("Ollama service unreachable at ${properties.ollamaUrl}")
        }

        if (errors.isNotEmpty()) {
            throw RuntimeException("AI services unavailable: ${errors.joinToString("; ")}")
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
