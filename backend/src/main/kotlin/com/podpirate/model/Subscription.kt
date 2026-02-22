package com.podpirate.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "subscriptions")
data class Subscription(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "podcast_id", nullable = false)
    @JsonIgnoreProperties("episodes", "hibernateLazyInitializer")
    val podcast: Podcast = Podcast(),

    val subscribedAt: Instant = Instant.now(),
)
