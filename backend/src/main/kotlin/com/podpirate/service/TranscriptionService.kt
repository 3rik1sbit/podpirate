package com.podpirate.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.podpirate.config.PodPirateProperties
import com.podpirate.model.EpisodeStatus
import com.podpirate.model.Transcription
import com.podpirate.repository.EpisodeRepository
import com.podpirate.repository.TranscriptionRepository
import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import java.nio.file.Path

@Service
class TranscriptionService(
    private val episodeRepository: EpisodeRepository,
    private val transcriptionRepository: TranscriptionRepository,
    private val adDetectionService: AdDetectionService,
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val properties: PodPirateProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async("aiExecutor")
    fun transcribeAsync(episodeId: Long) {
        try {
            transcribe(episodeId)
        } catch (e: Exception) {
            log.error("Failed to transcribe episode $episodeId", e)
            val episode = episodeRepository.findById(episodeId).orElseThrow()
            episodeRepository.save(episode.copy(status = EpisodeStatus.ERROR))
        }
    }

    @Transactional
    fun transcribe(episodeId: Long) {
        val episode = episodeRepository.findById(episodeId).orElseThrow()
        val audioPath = episode.localAudioPath
        if (audioPath == null || !java.nio.file.Path.of(audioPath).toFile().exists()) {
            log.warn("Episode $episodeId has no audio file, resetting to PENDING for re-download")
            episodeRepository.save(episode.copy(status = EpisodeStatus.PENDING, localAudioPath = null))
            return
        }

        episodeRepository.save(episode.copy(status = EpisodeStatus.TRANSCRIBING))

        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("file", FileSystemResource(Path.of(audioPath).toFile()))

        val response = webClient.post()
            .uri("${properties.whisperUrl}/transcribe")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .block() ?: throw RuntimeException("Empty response from whisper service")

        val segments = response["segments"] ?: throw RuntimeException("No segments in response")
        val segmentsJson = objectMapper.writeValueAsString(segments)

        val transcription = transcriptionRepository.findByEpisodeId(episodeId)
            .map { it.copy(segments = segmentsJson) }
            .orElse(Transcription(episode = episode, segments = segmentsJson))

        transcriptionRepository.save(transcription)
        episodeRepository.save(episode.copy(status = EpisodeStatus.DETECTING_ADS))

        log.info("Transcribed episode ${episode.id}: ${episode.title}")

        // Trigger ad detection
        adDetectionService.detectAdsAsync(episodeId)
    }

    fun getTranscription(episodeId: Long): Transcription? {
        return transcriptionRepository.findByEpisodeId(episodeId).orElse(null)
    }
}
