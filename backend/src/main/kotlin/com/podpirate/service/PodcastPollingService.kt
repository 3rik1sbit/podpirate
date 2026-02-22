package com.podpirate.service

import com.podpirate.repository.PodcastRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class PodcastPollingService(
    private val podcastRepository: PodcastRepository,
    private val subscriptionService: SubscriptionService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRate = 600_000) // 10 minutes
    fun pollAllPodcasts() {
        log.info("Starting scheduled podcast sync")
        for (podcast in podcastRepository.findAll()) {
            try {
                subscriptionService.syncEpisodes(podcast)
            } catch (e: Exception) {
                log.error("Failed to sync ${podcast.title}", e)
            }
        }
        log.info("Finished scheduled podcast sync")
    }
}
