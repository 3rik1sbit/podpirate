package com.podpirate.controller

import com.podpirate.model.Episode
import com.podpirate.repository.EpisodeRepository
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.nio.file.Path

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = ["*"])
class EpisodeController(
    private val episodeRepository: EpisodeRepository,
) {

    @GetMapping("/feed")
    fun getFeed(@RequestParam(defaultValue = "0") page: Int,
                @RequestParam(defaultValue = "20") size: Int): Page<Episode> {
        return episodeRepository.findFeedEpisodes(PageRequest.of(page, size))
    }

    @GetMapping("/podcasts/{podcastId}/episodes")
    fun getEpisodes(@PathVariable podcastId: Long,
                    @RequestParam(defaultValue = "0") page: Int,
                    @RequestParam(defaultValue = "50") size: Int): Page<Episode> {
        return episodeRepository.findByPodcastIdOrderByPublishedAtDesc(podcastId, PageRequest.of(page, size))
    }

    @GetMapping("/episodes/{id}")
    fun getEpisode(@PathVariable id: Long): ResponseEntity<Episode> {
        return episodeRepository.findById(id)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())
    }

    @GetMapping("/episodes/{id}/audio")
    fun streamAudio(@PathVariable id: Long,
                    @RequestParam(defaultValue = "true") processed: Boolean): ResponseEntity<Resource> {
        val episode = episodeRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()

        val audioPath = if (processed) {
            episode.processedAudioPath ?: episode.localAudioPath
        } else {
            episode.localAudioPath
        } ?: return ResponseEntity.notFound().build()

        val file = Path.of(audioPath).toFile()
        if (!file.exists()) return ResponseEntity.notFound().build()

        val resource = FileSystemResource(file)
        val contentType = when {
            audioPath.endsWith(".mp3") -> "audio/mpeg"
            audioPath.endsWith(".m4a") -> "audio/mp4"
            audioPath.endsWith(".ogg") -> "audio/ogg"
            else -> "application/octet-stream"
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
            .contentLength(file.length())
            .body(resource)
    }
}
