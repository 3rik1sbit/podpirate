package com.podpirate

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.podpirate.data.local.AppDatabase
import com.podpirate.data.local.entity.PlaybackPosition
import kotlinx.coroutines.*

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()

        val dao = AppDatabase.getInstance(this).playbackPositionDao()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) savePosition(player, dao)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Save position of previous item when transitioning
                if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                    savePosition(player, dao)
                }
            }
        })
    }

    private fun savePosition(player: Player, dao: com.podpirate.data.local.PlaybackPositionDao) {
        val episodeId = player.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        val position = player.currentPosition
        val duration = player.duration.coerceAtLeast(0)
        if (position <= 0) return

        scope.launch {
            dao.upsert(
                PlaybackPosition(
                    episodeId = episodeId,
                    positionMs = position,
                    durationMs = duration,
                )
            )
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        scope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
