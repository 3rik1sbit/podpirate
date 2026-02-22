package com.podpirate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.podpirate.data.local.entity.QueueItem
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueDao {
    @Query("SELECT * FROM QueueItem ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<QueueItem>>

    @Insert
    suspend fun insert(item: QueueItem)

    @Query("DELETE FROM QueueItem WHERE episodeId = :episodeId")
    suspend fun deleteByEpisodeId(episodeId: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM QueueItem")
    suspend fun maxSortOrder(): Int

    @Query("DELETE FROM QueueItem")
    suspend fun deleteAll()
}
