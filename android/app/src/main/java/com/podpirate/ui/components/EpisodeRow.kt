package com.podpirate.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.podpirate.data.model.Episode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val statusColors = mapOf(
    "READY" to Color(0xFF4ADE80),
    "PENDING" to Color(0xFF9CA3AF),
    "DOWNLOADING" to Color(0xFF60A5FA),
    "DOWNLOADED" to Color(0xFF93C5FD),
    "TRANSCRIBING" to Color(0xFFFACC15),
    "DETECTING_ADS" to Color(0xFFFB923C),
    "PROCESSING" to Color(0xFFC084FC),
    "ERROR" to Color(0xFFF87171),
)

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
fun EpisodeRow(episode: Episode, onClick: () -> Unit, onAddToQueue: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val artworkUrl = episode.imageUrl ?: episode.podcast?.artworkUrl
            if (artworkUrl != null) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = "Episode artwork",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    episode.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                episode.podcast?.title?.let { podcastTitle ->
                    Text(
                        podcastTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    episode.publishedAt?.let { raw ->
                        val formatted = runCatching {
                            val instant = Instant.parse(raw)
                            instant.atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
                        }.getOrNull()
                        if (formatted != null) {
                            Text(
                                formatted,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    episode.duration?.let {
                        val h = it / 3600
                        val m = (it % 3600) / 60
                        val text = if (h > 0) "${h}h ${m}m" else "${m}m"
                        Text(
                            text,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (onAddToQueue != null) {
                IconButton(onClick = onAddToQueue, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Add to queue", modifier = Modifier.size(18.dp))
                }
            }

            Text(
                episode.status,
                style = MaterialTheme.typography.labelSmall,
                color = statusColors[episode.status] ?: MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
