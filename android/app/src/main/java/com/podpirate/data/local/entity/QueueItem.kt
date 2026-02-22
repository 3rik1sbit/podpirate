package com.podpirate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class QueueItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val episodeId: Long,
    val episodeTitle: String,
    val podcastTitle: String,
    val audioUrl: String,
    val artworkUrl: String? = null,
    val sortOrder: Int = 0,
)
