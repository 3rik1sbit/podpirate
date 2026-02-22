package com.podpirate.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class EpisodeDownloadEventListener(
    private val episodeDownloadService: EpisodeDownloadService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener
    fun onEpisodesReady(event: EpisodesReadyEvent) {
        log.info("Starting downloads for ${event.episodeIds.size} episodes")
        event.episodeIds.forEach { episodeDownloadService.downloadAsync(it) }
    }
}
