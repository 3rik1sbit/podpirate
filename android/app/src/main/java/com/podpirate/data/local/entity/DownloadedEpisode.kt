package com.podpirate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DownloadedEpisode(
    @PrimaryKey val episodeId: Long,
    val episodeTitle: String,
    val podcastTitle: String,
    val audioUrl: String,
    val artworkUrl: String? = null,
    val filePath: String,
    val fileSizeBytes: Long = 0,
    val downloadedAt: Long = System.currentTimeMillis(),
    val downloadProgress: Int = 0, // 0-100
)
