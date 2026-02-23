package com.podpirate.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "podcasts")
data class Podcast(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val title: String = "",

    val author: String? = null,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    val artworkUrl: String? = null,

    @Column(nullable = false, unique = true)
    val feedUrl: String = "",

    @Column(unique = true)
    val itunesId: Long? = null,

    val createdAt: Instant = Instant.now(),

    @Column(columnDefinition = "TEXT")
    val adDetectionHints: String? = null,

    @JsonIgnore
    @OneToMany(mappedBy = "podcast", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val episodes: MutableList<Episode> = mutableListOf(),
)
