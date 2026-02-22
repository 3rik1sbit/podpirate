package com.podpirate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.podpirate.data.api.ApiClient
import com.podpirate.data.model.TranscriptionSegment
import com.podpirate.ui.screens.viewmodels.EpisodePlayerViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodePlayerScreen(
    episodeId: Long,
    onBack: () -> Unit,
    viewModel: EpisodePlayerViewModel = viewModel(),
) {
    val episode by viewModel.episode.collectAsState()
    val segments by viewModel.segments.collectAsState()
    val adSegments by viewModel.adSegments.collectAsState()
    val loading by viewModel.loading.collectAsState()

    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var useProcessed by remember { mutableStateOf(true) }

    LaunchedEffect(episodeId) { viewModel.load(episodeId) }

    // Set media source when episode loads or toggle changes
    LaunchedEffect(episode, useProcessed) {
        episode?.let {
            val url = ApiClient.audioUrl(it.id, useProcessed)
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
        }
    }

    // Track playback position
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = player.currentPosition
            delay(500)
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        IconButton(onClick = onBack, modifier = Modifier.padding(4.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
            return
        }

        episode?.let { ep ->
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(ep.title, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Status: ${ep.status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ep.status == "READY") Color(0xFF4ADE80) else Color(0xFFFACC15),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Player controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledIconButton(
                    onClick = {
                        if (isPlaying) player.pause() else player.play()
                        isPlaying = !isPlaying
                    },
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                    )
                }

                Text(
                    formatMs(currentPosition),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { player.seekTo(it.toLong()) },
                    valueRange = 0f..(player.duration.coerceAtLeast(1L).toFloat()),
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = useProcessed, onCheckedChange = { useProcessed = it })
                Text("Ad-free version", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transcript
            if (segments.isNotEmpty()) {
                Text(
                    "Transcript",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))

                val currentTimeSec = currentPosition / 1000.0
                val listState = rememberLazyListState()

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(segments) { seg ->
                        val isActive = currentTimeSec >= seg.start && currentTimeSec < seg.end
                        val isAd = adSegments.any { ad -> seg.start >= ad.startTime && seg.start <= ad.endTime }

                        val bgColor = when {
                            isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            isAd -> Color(0xFF7F1D1D).copy(alpha = 0.3f)
                            else -> Color.Transparent
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bgColor, RoundedCornerShape(6.dp))
                                .clickable { player.seekTo((seg.start * 1000).toLong()) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                formatSec(seg.start),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(48.dp),
                            )
                            Text(
                                seg.text,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isAd) Color(0xFFFCA5A5) else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            } else {
                Text(
                    "No transcription available",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

private fun formatSec(sec: Double): String {
    val m = (sec / 60).toInt()
    val s = (sec % 60).toInt()
    return "%d:%02d".format(m, s)
}
