package com.podpirate.repository

import com.podpirate.model.Subscription
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface SubscriptionRepository : JpaRepository<Subscription, Long> {
    fun findByPodcastId(podcastId: Long): Optional<Subscription>
    fun existsByPodcastId(podcastId: Long): Boolean
}
