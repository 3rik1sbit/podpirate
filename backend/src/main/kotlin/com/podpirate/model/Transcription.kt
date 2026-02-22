package com.podpirate.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

@Entity
@Table(name = "transcriptions")
data class Transcription(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false, unique = true)
    val episode: Episode = Episode(),

    @Column(columnDefinition = "JSONB")
    val segments: String = "[]", // JSON array of {start, end, text}
)
