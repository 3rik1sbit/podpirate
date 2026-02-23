package com.podpirate.service

import com.podpirate.model.Episode
import com.podpirate.model.EpisodeStatus
import com.podpirate.model.Podcast
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URI
import java.time.Instant

data class FeedEpisode(
    val title: String,
    val description: String?,
    val audioUrl: String,
    val publishedAt: Instant?,
    val guid: String?,
    val duration: Long?,
    val imageUrl: String?,
)

@Service
class RssFeedService {

    private val log = LoggerFactory.getLogger(javaClass)

    fun fetchEpisodes(feedUrl: String): List<FeedEpisode> {
        return try {
            val input = SyndFeedInput()
            val feed = input.build(XmlReader(URI(feedUrl).toURL()))

            feed.entries.mapNotNull { entry ->
                val enclosure = entry.enclosures.firstOrNull { it.type?.startsWith("audio") == true }
                    ?: entry.enclosures.firstOrNull()
                    ?: return@mapNotNull null

                FeedEpisode(
                    title = entry.title ?: return@mapNotNull null,
                    description = entry.description?.value,
                    audioUrl = enclosure.url ?: return@mapNotNull null,
                    publishedAt = entry.publishedDate?.toInstant(),
                    guid = entry.uri,
                    duration = parseDuration(entry.foreignMarkup),
                    imageUrl = parseImageUrl(entry.foreignMarkup),
                )
            }
        } catch (e: Exception) {
            log.error("Failed to parse feed: $feedUrl", e)
            emptyList()
        }
    }

    fun fetchPodcastMetadata(feedUrl: String): Pair<String, String?>? {
        return try {
            val input = SyndFeedInput()
            val feed = input.build(XmlReader(URI(feedUrl).toURL()))
            Pair(feed.title ?: return null, feed.description)
        } catch (e: Exception) {
            log.error("Failed to fetch podcast metadata: $feedUrl", e)
            null
        }
    }

    private fun parseImageUrl(foreignMarkup: List<org.jdom2.Element>): String? {
        return foreignMarkup.find { it.name == "image" }?.getAttributeValue("href")
    }

    private fun parseDuration(foreignMarkup: List<org.jdom2.Element>): Long? {
        val durationStr = foreignMarkup.find { it.name == "duration" }?.text ?: return null
        return try {
            val parts = durationStr.split(":")
            when (parts.size) {
                3 -> parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()
                2 -> parts[0].toLong() * 60 + parts[1].toLong()
                1 -> parts[0].toLong()
                else -> null
            }
        } catch (e: NumberFormatException) {
            null
        }
    }
}
