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

    @Query("SELECT e.status, COUNT(e) FROM Episode e GROUP BY e.status")
    fun countByStatusGrouped(): List<Array<Any>>

    @Query("SELECT COALESCE(SUM(e.duration), 0) FROM Episode e WHERE e.duration IS NOT NULL")
    fun sumDuration(): Long

    @Query("SELECT COALESCE(AVG(e.duration), 0) FROM Episode e WHERE e.duration IS NOT NULL")
    fun avgDuration(): Double

    fun findTopByDurationIsNotNullOrderByDurationDesc(pageable: Pageable): Page<Episode>
    fun findTopByDurationIsNotNullOrderByDurationAsc(pageable: Pageable): Page<Episode>

    @Query("SELECT COALESCE(AVG(e.duration), 0) FROM Episode e WHERE e.status = 'READY'")
    fun avgDurationOfReady(): Double

    @Query("SELECT COUNT(e) FROM Episode e WHERE e.status NOT IN ('READY', 'ERROR')")
    fun countRemaining(): Long
}
