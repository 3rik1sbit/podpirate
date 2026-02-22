package com.podpirate.data.api

import com.podpirate.data.model.*
import retrofit2.http.*

interface PodPirateApi {

    @GET("api/podcasts/search")
    suspend fun searchPodcasts(@Query("q") query: String): List<PodcastSearchResult>

    @GET("api/subscriptions")
    suspend fun getSubscriptions(): List<Subscription>

    @POST("api/subscriptions")
    suspend fun subscribe(@Body request: SubscribeRequest): Subscription

    @DELETE("api/subscriptions/{id}")
    suspend fun unsubscribe(@Path("id") id: Long)

    @GET("api/feed")
    suspend fun getFeed(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): Page<Episode>

    @GET("api/podcasts/{id}")
    suspend fun getPodcast(@Path("id") id: Long): Podcast

    @GET("api/podcasts/{podcastId}/episodes")
    suspend fun getEpisodes(
        @Path("podcastId") podcastId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): Page<Episode>

    @GET("api/episodes/{id}")
    suspend fun getEpisode(@Path("id") id: Long): Episode

    @GET("api/episodes/{id}/transcription")
    suspend fun getTranscription(@Path("id") episodeId: Long): Transcription

    @GET("api/episodes/{id}/ad-segments")
    suspend fun getAdSegments(@Path("id") episodeId: Long): List<AdSegment>

    @PUT("api/episodes/{id}/ad-segments")
    suspend fun updateAdSegments(
        @Path("id") episodeId: Long,
        @Body segments: List<AdSegment>,
    ): List<AdSegment>

    @POST("api/episodes/{id}/reprocess")
    suspend fun reprocessEpisode(@Path("id") episodeId: Long): MessageResponse
}
