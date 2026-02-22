package com.podpirate.repository

import com.podpirate.model.Podcast
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PodcastRepository : JpaRepository<Podcast, Long> {
    fun findByFeedUrl(feedUrl: String): Optional<Podcast>
    fun findByItunesId(itunesId: Long): Optional<Podcast>
}
