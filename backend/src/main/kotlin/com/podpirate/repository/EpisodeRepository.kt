package com.podpirate.repository

import com.podpirate.model.Episode
import com.podpirate.model.EpisodeStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface EpisodeRepository : JpaRepository<Episode, Long> {
    fun findByPodcastIdOrderByPublishedAtDesc(podcastId: Long, pageable: Pageable): Page<Episode>
    fun findByPodcastId(podcastId: Long): List<Episode>
    fun findByGuid(guid: String): Episode?
    fun findByStatus(status: EpisodeStatus): List<Episode>

    @Query("""
        SELECT e FROM Episode e
        WHERE e.podcast.id IN (SELECT s.podcast.id FROM Subscription s)
        ORDER BY e.publishedAt DESC
    """)
    fun findFeedEpisodes(pageable: Pageable): Page<Episode>
}
