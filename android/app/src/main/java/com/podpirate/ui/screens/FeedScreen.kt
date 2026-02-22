package com.podpirate.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.podpirate.data.model.Episode
import com.podpirate.ui.components.EpisodeRow
import com.podpirate.ui.screens.viewmodels.FeedViewModel

@Composable
fun FeedScreen(
    onEpisodeClick: (Long) -> Unit,
    viewModel: FeedViewModel = viewModel(),
) {
    val episodes by viewModel.episodes.collectAsState()
    val loading by viewModel.loading.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (loading && episodes.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (episodes.isEmpty()) {
            Text(
                "No episodes yet. Subscribe to some podcasts!",
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(episodes, key = { it.id }) { episode ->
                    EpisodeRow(episode = episode, onClick = { onEpisodeClick(episode.id) })
                }
            }
        }
    }
}
