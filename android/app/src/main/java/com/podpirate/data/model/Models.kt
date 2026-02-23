package com.podpirate.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PodcastSearchResult(
    val itunesId: Long,
    val title: String,
    val author: String? = null,
    val description: String? = null,
    val artworkUrl: String? = null,
    val feedUrl: String? = null,
)

@Serializable
data class Podcast(
    val id: Long,
    val title: String,
    val author: String? = null,
    val description: String? = null,
    val artworkUrl: String? = null,
    val feedUrl: String,
    val itunesId: Long? = null,
)

@Serializable
data class Episode(
    val id: Long,
    val title: String,
    val description: String? = null,
    val publishedAt: String? = null,
    val audioUrl: String,
    val duration: Long? = null,
    val imageUrl: String? = null,
    val status: String = "PENDING",
    val podcast: Podcast? = null,
)

@Serializable
data class Subscription(
    val id: Long,
    val podcast: Podcast,
    val subscribedAt: String,
)

@Serializable
data class Transcription(
    val id: Long,
    val segments: String, // JSON string
)

@Serializable
data class TranscriptionSegment(
    val start: Double,
    val end: Double,
    val text: String,
)

@Serializable
data class AdSegment(
    val id: Long? = null,
    val startTime: Double,
    val endTime: Double,
    val source: String = "AUTO",
    val confirmed: Boolean = false,
)

@Serializable
data class Page<T>(
    val content: List<T>,
    val totalPages: Int,
    val totalElements: Long,
    val number: Int,
    val size: Int,
)

@Serializable
data class SubscribeRequest(
    val feedUrl: String,
    val itunesId: Long? = null,
    val title: String? = null,
    val author: String? = null,
    val artworkUrl: String? = null,
)

@Serializable
data class MessageResponse(
    val message: String,
)
