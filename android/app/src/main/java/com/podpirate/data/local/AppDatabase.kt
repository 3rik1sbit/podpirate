package com.podpirate.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.podpirate.data.local.entity.DownloadedEpisode
import com.podpirate.data.local.entity.PlaybackPosition
import com.podpirate.data.local.entity.QueueItem

@Database(
    entities = [PlaybackPosition::class, QueueItem::class, DownloadedEpisode::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playbackPositionDao(): PlaybackPositionDao
    abstract fun queueDao(): QueueDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "podpirate.db",
                ).build().also { instance = it }
            }
        }
    }
}
