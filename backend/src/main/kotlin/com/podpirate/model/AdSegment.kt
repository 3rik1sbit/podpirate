package com.podpirate.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

enum class AdSegmentSource {
    AUTO, MANUAL
}

@Entity
@Table(name = "ad_segments")
data class AdSegment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    val episode: Episode = Episode(),

    @Column(nullable = false)
    val startTime: Double = 0.0, // seconds

    @Column(nullable = false)
    val endTime: Double = 0.0, // seconds

    @Enumerated(EnumType.STRING)
    val source: AdSegmentSource = AdSegmentSource.AUTO,

    val confirmed: Boolean = false,
)
