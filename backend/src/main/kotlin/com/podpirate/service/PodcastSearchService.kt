package com.podpirate.service

import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

data class PodcastSearchResult(
    val itunesId: Long,
    val title: String,
    val author: String?,
    val description: String?,
    val artworkUrl: String?,
    val feedUrl: String?,
)

@Service
class PodcastSearchService(private val webClient: WebClient) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun search(query: String): List<PodcastSearchResult> {
        val response = webClient.get()
            .uri("https://itunes.apple.com/search?media=podcast&term={query}", query)
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .block() ?: return emptyList()

        val results = response["results"] ?: return emptyList()

        return results.mapNotNull { node ->
            val feedUrl = node["feedUrl"]?.asText()
            if (feedUrl.isNullOrBlank()) return@mapNotNull null

            PodcastSearchResult(
                itunesId = node["collectionId"]?.asLong() ?: return@mapNotNull null,
                title = node["collectionName"]?.asText() ?: return@mapNotNull null,
                author = node["artistName"]?.asText(),
                description = null, // iTunes search doesn't return full description
                artworkUrl = node["artworkUrl600"]?.asText() ?: node["artworkUrl100"]?.asText(),
                feedUrl = feedUrl,
            )
        }
    }
}
