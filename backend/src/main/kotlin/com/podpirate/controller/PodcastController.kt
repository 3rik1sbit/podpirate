package com.podpirate.controller

import com.podpirate.model.Podcast
import com.podpirate.repository.PodcastRepository
import com.podpirate.service.PodcastSearchResult
import com.podpirate.service.PodcastSearchService
import com.podpirate.service.SubscribeRequest
import com.podpirate.service.SubscriptionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = ["*"])
class PodcastController(
    private val podcastSearchService: PodcastSearchService,
    private val podcastRepository: PodcastRepository,
    private val subscriptionService: SubscriptionService,
) {

    @GetMapping("/podcasts/search")
    fun search(@RequestParam q: String): List<PodcastSearchResult> {
        return podcastSearchService.search(q)
    }

    @GetMapping("/podcasts/{id}")
    fun getPodcast(@PathVariable id: Long): ResponseEntity<Podcast> {
        return podcastRepository.findById(id)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())
    }

    data class SubscribeBody(
        val feedUrl: String,
        val itunesId: Long? = null,
        val title: String? = null,
        val author: String? = null,
        val artworkUrl: String? = null,
    )

    @PostMapping("/subscriptions")
    fun subscribe(@RequestBody body: SubscribeBody): ResponseEntity<Any> {
        return try {
            val sub = subscriptionService.subscribe(
                SubscribeRequest(
                    feedUrl = body.feedUrl,
                    itunesId = body.itunesId,
                    title = body.title,
                    author = body.author,
                    artworkUrl = body.artworkUrl,
                )
            )
            ResponseEntity.ok(sub)
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping("/subscriptions/{id}")
    fun unsubscribe(@PathVariable id: Long): ResponseEntity<Void> {
        subscriptionService.unsubscribe(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/subscriptions")
    fun listSubscriptions() = subscriptionService.listSubscriptions()
}
