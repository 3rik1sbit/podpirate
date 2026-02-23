package com.podpirate.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.*
import java.time.Instant

enum class EpisodeStatus {
    PENDING, DOWNLOADING, DOWNLOADED, TRANSCRIBING, DETECTING_ADS, PROCESSING, READY, ERROR
}

@Entity
@Table(name = "episodes")
data class Episode(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "podcast_id", nullable = false)
    @JsonIgnoreProperties("episodes", "hibernateLazyInitializer")
    val podcast: Podcast = Podcast(),

    @Column(nullable = false)
    val title: String = "",

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    val publishedAt: Instant? = null,

    @Column(nullable = false)
    val audioUrl: String = "",

    @JsonIgnore
    val localAudioPath: String? = null,

    @JsonIgnore
    val processedAudioPath: String? = null,

    val duration: Long? = null,

    val imageUrl: String? = null,

    @Column(unique = true)
    val guid: String? = null,

    @Column(nullable = false, columnDefinition = "integer default 0")
    val priority: Int = 0,

    @Enumerated(EnumType.STRING)
    val status: EpisodeStatus = EpisodeStatus.PENDING,

    val createdAt: Instant = Instant.now(),

    @JsonIgnore
    @OneToOne(mappedBy = "episode", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val transcription: Transcription? = null,

    @JsonIgnore
    @OneToMany(mappedBy = "episode", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val adSegments: MutableList<AdSegment> = mutableListOf(),
)
