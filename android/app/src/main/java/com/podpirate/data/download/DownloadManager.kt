package com.podpirate.data.download

import android.content.Context
import androidx.work.*
import com.podpirate.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DownloadManager {

    fun enqueueDownload(
        context: Context,
        episodeId: Long,
        episodeTitle: String,
        podcastTitle: String,
        audioUrl: String,
        artworkUrl: String? = null,
    ) {
        val data = workDataOf(
            "episodeId" to episodeId,
            "episodeTitle" to episodeTitle,
            "podcastTitle" to podcastTitle,
            "audioUrl" to audioUrl,
            "artworkUrl" to artworkUrl,
        )

        val request = OneTimeWorkRequestBuilder<EpisodeDownloadWorker>()
            .setInputData(data)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("download_$episodeId")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "download_$episodeId",
                ExistingWorkPolicy.KEEP,
                request,
            )
    }

    fun cancelDownload(context: Context, episodeId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("download_$episodeId")
    }

    suspend fun deleteDownload(context: Context, episodeId: Long) {
        withContext(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context).downloadDao()
            val download = dao.get(episodeId)
            download?.let {
                File(it.filePath).delete()
            }
            dao.delete(episodeId)
        }
    }
}
