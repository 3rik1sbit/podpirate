package com.podpirate.service

import com.podpirate.config.PodPirateProperties
import com.podpirate.model.EpisodeStatus
import com.podpirate.repository.EpisodeRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.FileOutputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

@Service
class EpisodeDownloadService(
    private val episodeRepository: EpisodeRepository,
    private val transcriptionService: TranscriptionService,
    private val properties: PodPirateProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async("downloadExecutor")
    fun downloadAsync(episodeId: Long) {
        try {
            download(episodeId)
        } catch (e: Exception) {
            log.error("Failed to download episode $episodeId", e)
            updateStatus(episodeId, EpisodeStatus.ERROR)
        }
    }

    fun download(episodeId: Long) {
        val episode = episodeRepository.findById(episodeId).orElseThrow()

        updateStatus(episodeId, EpisodeStatus.DOWNLOADING)

        val audioDir = Path.of(properties.audioDir)
        Files.createDirectories(audioDir)

        val extension = episode.audioUrl.substringAfterLast(".").take(4).ifBlank { "mp3" }
        val filename = "episode_${episode.id}.$extension"
        val filePath = audioDir.resolve(filename)

        // Download the file
        URI(episode.audioUrl).toURL().openStream().use { input ->
            FileOutputStream(filePath.toFile()).use { output ->
                input.copyTo(output)
            }
        }

        val updated = episode.copy(
            localAudioPath = filePath.toString(),
            status = EpisodeStatus.DOWNLOADED,
        )
        episodeRepository.save(updated)

        log.info("Downloaded episode ${episode.id}: ${episode.title}")

        // Trigger transcription (save above must be committed before the async thread reads it)
        transcriptionService.transcribeAsync(episode.id)
    }

    private fun updateStatus(episodeId: Long, status: EpisodeStatus) {
        val episode = episodeRepository.findById(episodeId).orElseThrow()
        episodeRepository.save(episode.copy(status = status))
    }
}
