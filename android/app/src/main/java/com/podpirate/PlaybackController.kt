package com.podpirate

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.podpirate.data.local.entity.QueueItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object PlaybackController {

    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller = _controller.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private val _currentEpisodeId = MutableStateFlow<Long?>(null)
    val currentEpisodeId = _currentEpisodeId.asStateFlow()

    private var positionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun connect(context: Context) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val mc = future.get()
            _controller.value = mc
            mc.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                    if (playing) startPositionPolling() else stopPositionPolling()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    _currentEpisodeId.value = mediaItem?.mediaId?.toLongOrNull()
                }
            })
        }, MoreExecutors.directExecutor())
    }

    fun playEpisode(episodeId: Long, uri: String, seekToMs: Long = 0, localFilePath: String? = null) {
        val mc = _controller.value ?: return
        val actualUri = localFilePath ?: uri
        val item = MediaItem.Builder()
            .setMediaId(episodeId.toString())
            .setUri(actualUri)
            .build()
        mc.setMediaItem(item)
        mc.prepare()
        if (seekToMs > 0) mc.seekTo(seekToMs)
        mc.play()
        _currentEpisodeId.value = episodeId
    }

    fun playQueue(items: List<QueueItem>) {
        val mc = _controller.value ?: return
        val mediaItems = items.map { item ->
            MediaItem.Builder()
                .setMediaId(item.episodeId.toString())
                .setUri(item.audioUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.episodeTitle)
                        .setArtist(item.podcastTitle)
                        .build()
                )
                .build()
        }
        mc.setMediaItems(mediaItems)
        mc.prepare()
        mc.play()
        if (items.isNotEmpty()) {
            _currentEpisodeId.value = items.first().episodeId
        }
    }

    private fun startPositionPolling() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                _controller.value?.let { mc ->
                    _currentPosition.value = mc.currentPosition
                    _duration.value = mc.duration.coerceAtLeast(0)
                }
                delay(500)
            }
        }
    }

    private fun stopPositionPolling() {
        positionJob?.cancel()
        _controller.value?.let { mc ->
            _currentPosition.value = mc.currentPosition
            _duration.value = mc.duration.coerceAtLeast(0)
        }
    }
}
