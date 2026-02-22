package com.podpirate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.podpirate.ui.components.PodcastCard
import com.podpirate.ui.screens.viewmodels.SubscriptionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    onPodcastClick: (Long) -> Unit,
    viewModel: SubscriptionsViewModel = viewModel(),
) {
    val subscriptions by viewModel.subscriptions.collectAsState()
    val loading by viewModel.loading.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = { viewModel.load() },
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        if (loading && subscriptions.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (subscriptions.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "No subscriptions yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(subscriptions, key = { it.id }) { sub ->
                    PodcastCard(
                        title = sub.podcast.title,
                        author = sub.podcast.author,
                        artworkUrl = sub.podcast.artworkUrl,
                        actionLabel = "Unsubscribe",
                        onAction = { viewModel.unsubscribe(sub.id) },
                        onClick = { onPodcastClick(sub.podcast.id) },
                    )
                }
            }
        }
    }
}
