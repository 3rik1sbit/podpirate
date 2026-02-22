package com.podpirate.data.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.podpirate.data.local.AppDatabase
import com.podpirate.data.local.entity.DownloadedEpisode
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class EpisodeDownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val episodeId = inputData.getLong("episodeId", -1)
        val episodeTitle = inputData.getString("episodeTitle") ?: ""
        val podcastTitle = inputData.getString("podcastTitle") ?: ""
        val audioUrl = inputData.getString("audioUrl") ?: return Result.failure()
        val artworkUrl = inputData.getString("artworkUrl")

        if (episodeId < 0) return Result.failure()

        val db = AppDatabase.getInstance(applicationContext)
        val dao = db.downloadDao()
        val dir = applicationContext.getExternalFilesDir("episodes") ?: return Result.failure()
        dir.mkdirs()
        val file = File(dir, "$episodeId.mp3")

        try {
            // Create initial record
            dao.upsert(
                DownloadedEpisode(
                    episodeId = episodeId,
                    episodeTitle = episodeTitle,
                    podcastTitle = podcastTitle,
                    audioUrl = audioUrl,
                    artworkUrl = artworkUrl,
                    filePath = file.absolutePath,
                    downloadProgress = 0,
                )
            )

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.MINUTES)
                .build()

            val request = Request.Builder().url(audioUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                cleanup(file, dao, episodeId)
                return Result.failure()
            }

            val body = response.body ?: run {
                cleanup(file, dao, episodeId)
                return Result.failure()
            }

            val contentLength = body.contentLength()
            var bytesRead = 0L

            file.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesRead += read

                        if (contentLength > 0) {
                            val progress = ((bytesRead * 100) / contentLength).toInt().coerceIn(0, 99)
                            dao.upsert(
                                DownloadedEpisode(
                                    episodeId = episodeId,
                                    episodeTitle = episodeTitle,
                                    podcastTitle = podcastTitle,
                                    audioUrl = audioUrl,
                                    artworkUrl = artworkUrl,
                                    filePath = file.absolutePath,
                                    fileSizeBytes = bytesRead,
                                    downloadProgress = progress,
                                )
                            )
                        }
                    }
                }
            }

            // Mark complete
            dao.upsert(
                DownloadedEpisode(
                    episodeId = episodeId,
                    episodeTitle = episodeTitle,
                    podcastTitle = podcastTitle,
                    audioUrl = audioUrl,
                    artworkUrl = artworkUrl,
                    filePath = file.absolutePath,
                    fileSizeBytes = file.length(),
                    downloadProgress = 100,
                )
            )

            return Result.success()
        } catch (e: Exception) {
            cleanup(file, dao, episodeId)
            return Result.failure()
        }
    }

    private suspend fun cleanup(file: File, dao: com.podpirate.data.local.DownloadDao, episodeId: Long) {
        file.delete()
        dao.delete(episodeId)
    }
}
