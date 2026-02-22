package com.podpirate.repository

import com.podpirate.model.AdSegment
import com.podpirate.model.AdSegmentSource
import org.springframework.data.jpa.repository.JpaRepository

interface AdSegmentRepository : JpaRepository<AdSegment, Long> {
    fun findByEpisodeIdOrderByStartTime(episodeId: Long): List<AdSegment>
    fun deleteByEpisodeId(episodeId: Long)
    fun findByEpisodePodcastIdAndSource(podcastId: Long, source: AdSegmentSource): List<AdSegment>
}
