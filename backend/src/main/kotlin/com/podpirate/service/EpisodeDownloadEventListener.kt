package com.podpirate.service

import com.podpirate.model.Episode
import com.podpirate.repository.EpisodeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class EpisodeDownloadEventListener(
    private val episodeDownloadService: EpisodeDownloadService,
    private val episodeRepository: EpisodeRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener
    fun onEpisodesReady(event: EpisodesReadyEvent) {
        log.info("Starting downloads for ${event.episodeIds.size} episodes")
        val episodes = episodeRepository.findAllById(event.episodeIds)
            .sortedWith(compareByDescending<Episode> { it.priority }
                .thenByDescending { it.publishedAt })
        episodes.forEach { episodeDownloadService.downloadAsync(it.id) }
    }
}
