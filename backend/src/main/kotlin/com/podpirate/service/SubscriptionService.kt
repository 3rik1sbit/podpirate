package com.podpirate.service

import com.podpirate.model.*
import com.podpirate.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

data class SubscribeRequest(
    val feedUrl: String,
    val itunesId: Long? = null,
    val title: String? = null,
    val author: String? = null,
    val artworkUrl: String? = null,
)

@Service
class SubscriptionService(
    private val podcastRepository: PodcastRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val episodeRepository: EpisodeRepository,
    private val rssFeedService: RssFeedService,
    private val episodeDownloadService: EpisodeDownloadService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun subscribe(request: SubscribeRequest): Subscription {
        // Find or create podcast
        val podcast = podcastRepository.findByFeedUrl(request.feedUrl).orElseGet {
            val metadata = rssFeedService.fetchPodcastMetadata(request.feedUrl)
            podcastRepository.save(
                Podcast(
                    title = request.title ?: metadata?.first ?: "Unknown",
                    author = request.author,
                    description = metadata?.second,
                    artworkUrl = request.artworkUrl,
                    feedUrl = request.feedUrl,
                    itunesId = request.itunesId,
                )
            )
        }

        // Check if already subscribed
        subscriptionRepository.findByPodcastId(podcast.id).ifPresent {
            throw IllegalStateException("Already subscribed to this podcast")
        }

        val subscription = subscriptionRepository.save(Subscription(podcast = podcast))

        // Fetch and save episodes
        syncEpisodes(podcast)

        return subscription
    }

    @Transactional
    fun unsubscribe(subscriptionId: Long) {
        subscriptionRepository.deleteById(subscriptionId)
    }

    fun listSubscriptions(): List<Subscription> {
        return subscriptionRepository.findAll()
    }

    @Transactional
    fun syncEpisodes(podcast: Podcast) {
        val feedEpisodes = rssFeedService.fetchEpisodes(podcast.feedUrl)
        val savedEpisodeIds = mutableListOf<Long>()

        for (feedEp in feedEpisodes) {
            val exists = feedEp.guid?.let { episodeRepository.findByGuid(it) } != null
            if (exists) continue

            val episode = episodeRepository.save(
                Episode(
                    podcast = podcast,
                    title = feedEp.title,
                    description = feedEp.description,
                    publishedAt = feedEp.publishedAt,
                    audioUrl = feedEp.audioUrl,
                    guid = feedEp.guid,
                    duration = feedEp.duration,
                    status = EpisodeStatus.PENDING,
                )
            )

            savedEpisodeIds.add(episode.id)
        }

        log.info("Synced ${feedEpisodes.size} episodes for ${podcast.title}")

        // Trigger downloads after the transaction commits so async threads can find the episodes
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                savedEpisodeIds.forEach { episodeDownloadService.downloadAsync(it) }
            }
        })
    }
}
