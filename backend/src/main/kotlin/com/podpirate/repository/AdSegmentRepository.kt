package com.podpirate.repository

import com.podpirate.model.AdSegment
import com.podpirate.model.AdSegmentSource
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AdSegmentRepository : JpaRepository<AdSegment, Long> {
    fun findByEpisodeIdOrderByStartTime(episodeId: Long): List<AdSegment>
    fun deleteByEpisodeId(episodeId: Long)
    fun findByEpisodePodcastIdAndSource(podcastId: Long, source: AdSegmentSource): List<AdSegment>

    @Query("SELECT COALESCE(SUM(a.endTime - a.startTime), 0) FROM AdSegment a")
    fun sumAdDuration(): Double

    @Query("SELECT a.source, COUNT(a) FROM AdSegment a GROUP BY a.source")
    fun countBySourceGrouped(): List<Array<Any>>

    @Query("SELECT a.episode.id, SUM(a.endTime - a.startTime) as total FROM AdSegment a GROUP BY a.episode.id ORDER BY total DESC")
    fun findMostAdHeavyEpisodeId(pageable: Pageable): List<Array<Any>>
}
