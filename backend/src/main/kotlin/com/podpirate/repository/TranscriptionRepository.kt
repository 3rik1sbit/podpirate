package com.podpirate.repository

import com.podpirate.model.Transcription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface TranscriptionRepository : JpaRepository<Transcription, Long> {
    fun findByEpisodeId(episodeId: Long): Optional<Transcription>

    @Query("SELECT COALESCE(SUM(jsonb_array_length(t.segments)), 0) FROM transcriptions t", nativeQuery = true)
    fun countTotalSegments(): Long
}
