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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import com.podpirate.PlaybackController
import com.podpirate.data.api.ApiClient
import com.podpirate.data.download.DownloadManager
import com.podpirate.data.local.AppDatabase
import com.podpirate.data.local.entity.QueueItem
import com.podpirate.ui.screens.viewmodels.EpisodePlayerViewModel
import kotlinx.coroutines.launch

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
    val isPlaying by PlaybackController.isPlaying.collectAsState()
    val currentPosition by PlaybackController.currentPosition.collectAsState()
    val duration by PlaybackController.duration.collectAsState()
    val controller by PlaybackController.controller.collectAsState()
    val currentEpisodeId by PlaybackController.currentEpisodeId.collectAsState()

    var useProcessed by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    val db = remember { AppDatabase.getInstance(context) }
    var isDownloaded by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(-1) }

    LaunchedEffect(episodeId) { viewModel.load(episodeId) }

    // Check download status
    LaunchedEffect(episodeId) {
        val dl = db.downloadDao().get(episodeId)
        if (dl != null) {
            isDownloaded = dl.downloadProgress >= 100
            downloadProgress = dl.downloadProgress
        }
    }

    // Start playback when episode loads (only if not already playing this episode)
    LaunchedEffect(episode, useProcessed) {
        episode?.let { ep ->
            if (currentEpisodeId != ep.id) {
                val savedPosition = db.playbackPositionDao().get(ep.id)
                val seekTo = savedPosition?.positionMs ?: 0L
                val dl = db.downloadDao().get(ep.id)
                val localPath = if (dl != null && dl.downloadProgress >= 100) dl.filePath else null
                val url = ApiClient.audioUrl(ep.id, useProcessed)
                PlaybackController.playEpisode(ep.id, url, seekTo, localPath)
            }
        }
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
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                val artworkUrl = ep.imageUrl ?: ep.podcast?.artworkUrl
                if (artworkUrl != null) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = "Episode artwork",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(ep.title, style = MaterialTheme.typography.titleLarge)
                    ep.podcast?.title?.let { podcastTitle ->
                        Text(
                            podcastTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Status: ${ep.status}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ep.status == "READY") Color(0xFF4ADE80) else Color(0xFFFACC15),
                    )
                }
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
                        controller?.let { mc ->
                            if (currentEpisodeId != ep.id) {
                                val url = ApiClient.audioUrl(ep.id, useProcessed)
                                PlaybackController.playEpisode(ep.id, url)
                            } else if (isPlaying) {
                                mc.pause()
                            } else {
                                mc.play()
                            }
                        }
                    },
                ) {
                    Icon(
                        if (isPlaying && currentEpisodeId == ep.id) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                    )
                }

                val displayPosition = if (currentEpisodeId == ep.id) currentPosition else 0L
                Text(
                    formatMs(displayPosition),
                    style = MaterialTheme.typography.bodyMedium,
                )

                val displayDuration = if (currentEpisodeId == ep.id) duration else 0L
                Slider(
                    value = displayPosition.toFloat(),
                    onValueChange = { controller?.seekTo(it.toLong()) },
                    valueRange = 0f..(displayDuration.coerceAtLeast(1L).toFloat()),
                    modifier = Modifier.weight(1f),
                )
            }

            // Action buttons row
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = useProcessed, onCheckedChange = { useProcessed = it })
                    Text("Ad-free version", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.weight(1f))

                // Add to queue
                IconButton(onClick = {
                    coroutineScope.launch {
                        val queueDao = db.queueDao()
                        val maxOrder = queueDao.maxSortOrder()
                        queueDao.insert(
                            QueueItem(
                                episodeId = ep.id,
                                episodeTitle = ep.title,
                                podcastTitle = ep.podcast?.title ?: "",
                                audioUrl = ApiClient.audioUrl(ep.id, true),
                                artworkUrl = ep.podcast?.artworkUrl,
                                sortOrder = maxOrder + 1,
                            )
                        )
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Add to queue")
                }

                // Download
                IconButton(onClick = {
                    if (!isDownloaded && downloadProgress < 0) {
                        DownloadManager.enqueueDownload(
                            context = context,
                            episodeId = ep.id,
                            episodeTitle = ep.title,
                            podcastTitle = ep.podcast?.title ?: "",
                            audioUrl = ApiClient.audioUrl(ep.id, true),
                            artworkUrl = ep.podcast?.artworkUrl,
                        )
                        downloadProgress = 0
                    }
                }) {
                    Icon(
                        when {
                            isDownloaded -> Icons.Default.DownloadDone
                            downloadProgress >= 0 -> Icons.Default.Downloading
                            else -> Icons.Default.Download
                        },
                        contentDescription = "Download",
                    )
                }
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

                val currentTimeSec = if (currentEpisodeId == ep.id) currentPosition / 1000.0 else 0.0
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
                                .clickable { controller?.seekTo((seg.start * 1000).toLong()) }
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
