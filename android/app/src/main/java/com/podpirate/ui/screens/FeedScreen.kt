package com.podpirate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.podpirate.data.api.ApiClient
import com.podpirate.data.local.AppDatabase
import com.podpirate.data.local.entity.QueueItem
import com.podpirate.ui.components.EpisodeRow
import com.podpirate.ui.screens.viewmodels.FeedViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onEpisodeClick: (Long) -> Unit,
    viewModel: FeedViewModel = viewModel(),
) {
    val episodes by viewModel.episodes.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.load() }

    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = { viewModel.load() },
        modifier = Modifier.fillMaxSize(),
    ) {
        if (loading && episodes.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (episodes.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "No episodes yet. Subscribe to some podcasts!",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
                                        podcastTitle = episode.podcast?.title ?: "",
                                        audioUrl = ApiClient.audioUrl(episode.id, true),
                                        artworkUrl = episode.podcast?.artworkUrl,
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
