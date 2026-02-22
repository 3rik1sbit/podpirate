package com.podpirate.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
fun EpisodeRow(episode: Episode, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    episode.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    episode.publishedAt?.let {
                        try {
                            val instant = Instant.parse(it)
                            val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                            Text(
                                date.format(dateFormatter),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } catch (_: Exception) {}
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

            Text(
                episode.status,
                style = MaterialTheme.typography.labelSmall,
                color = statusColors[episode.status] ?: MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
