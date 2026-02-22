package com.podpirate.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.podpirate.data.local.entity.PlaybackPosition

@Dao
interface PlaybackPositionDao {
    @Query("SELECT * FROM PlaybackPosition WHERE episodeId = :episodeId")
    suspend fun get(episodeId: Long): PlaybackPosition?

    @Upsert
    suspend fun upsert(position: PlaybackPosition)
}
