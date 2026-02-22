package com.podpirate.repository

import com.podpirate.model.Transcription
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface TranscriptionRepository : JpaRepository<Transcription, Long> {
    fun findByEpisodeId(episodeId: Long): Optional<Transcription>
}
