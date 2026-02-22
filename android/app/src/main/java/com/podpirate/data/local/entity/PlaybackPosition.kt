package com.podpirate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PlaybackPosition(
    @PrimaryKey val episodeId: Long,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long = System.currentTimeMillis(),
)
