package com.podpirate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.podpirate.data.api.ApiClient
import com.podpirate.data.local.AppDatabase
import com.podpirate.data.local.entity.QueueItem
import com.podpirate.ui.components.EpisodeRow
import com.podpirate.ui.screens.viewmodels.PodcastDetailViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastDetailScreen(
    podcastId: Long,
    onEpisodeClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: PodcastDetailViewModel = viewModel(),
) {
    val podcast by viewModel.podcast.collectAsState()
    val episodes by viewModel.episodes.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(podcastId) { viewModel.load(podcastId) }

    Column(modifier = Modifier.fillMaxSize()) {
        IconButton(onClick = onBack, modifier = Modifier.padding(4.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
        } else {
            PullToRefreshBox(
                isRefreshing = loading,
                onRefresh = { viewModel.load(podcastId) },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    podcast?.let { p ->
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                p.artworkUrl?.let { url ->
                                    AsyncImage(
                                        model = url,
                                        contentDescription = null,
                                        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                                Column {
                                    Text(p.title, style = MaterialTheme.typography.headlineSmall)
                                    p.author?.let {
                                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Episodes (${episodes.size})", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    items(episodes, key = { it.id }) { episode ->
                        EpisodeRow(
                            episode = episode,
                            onClick = { onEpisodeClick(episode.id) },
                            onAddToQueue = {
                                coroutineScope.launch {
                                    val queueDao = db.queueDao()
                                    val maxOrder = queueDao.maxSortOrder()
                                    queueDao.insert(
                                        QueueItem(
                                            episodeId = episode.id,
                                            episodeTitle = episode.title,
                                            podcastTitle = podcast?.title ?: "",
                                            audioUrl = ApiClient.audioUrl(episode.id, true),
                                            artworkUrl = podcast?.artworkUrl,
                                            sortOrder = maxOrder + 1,
                                        )
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
