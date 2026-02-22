package com.podpirate.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.podpirate.ui.screens.viewmodels.DownloadsViewModel

@Composable
fun DownloadsScreen(
    onEpisodeClick: (Long) -> Unit,
    viewModel: DownloadsViewModel = viewModel(),
) {
    val downloads by viewModel.downloads.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (downloads.isEmpty()) {
            Text(
                "No downloaded episodes. Download episodes from the player screen.",
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(downloads, key = { it.episodeId }) { dl ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (dl.downloadProgress >= 100) {
                                    Modifier.clickable { onEpisodeClick(dl.episodeId) }
                                } else Modifier
                            ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        dl.episodeTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        dl.podcastTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (dl.downloadProgress >= 100) {
                                        val sizeMb = dl.fileSizeBytes / (1024.0 * 1024.0)
                                        Text(
                                            "%.1f MB".format(sizeMb),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.delete(dl.episodeId) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }

                            if (dl.downloadProgress in 0..99) {
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { dl.downloadProgress / 100f },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
