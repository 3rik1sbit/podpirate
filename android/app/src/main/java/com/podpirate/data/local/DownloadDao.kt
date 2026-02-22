package com.podpirate.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.podpirate.data.local.entity.DownloadedEpisode
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM DownloadedEpisode ORDER BY downloadedAt DESC")
    fun getAll(): Flow<List<DownloadedEpisode>>

    @Query("SELECT * FROM DownloadedEpisode WHERE episodeId = :episodeId")
    suspend fun get(episodeId: Long): DownloadedEpisode?

    @Upsert
    suspend fun upsert(episode: DownloadedEpisode)

    @Query("DELETE FROM DownloadedEpisode WHERE episodeId = :episodeId")
    suspend fun delete(episodeId: Long)
}
