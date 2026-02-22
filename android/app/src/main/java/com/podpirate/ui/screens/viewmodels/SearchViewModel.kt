package com.podpirate.ui.screens.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podpirate.data.api.ApiClient
import com.podpirate.data.model.PodcastSearchResult
import com.podpirate.data.model.SubscribeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    val query = MutableStateFlow("")

    private val _results = MutableStateFlow<List<PodcastSearchResult>>(emptyList())
    val results = _results.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _subscribing = MutableStateFlow<Set<Long>>(emptySet())
    val subscribing = _subscribing.asStateFlow()

    fun search() {
        val q = query.value.trim()
        if (q.isBlank()) return

        viewModelScope.launch {
            _loading.value = true
            try {
                _results.value = ApiClient.api.searchPodcasts(q)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    fun subscribe(result: PodcastSearchResult) {
        val feedUrl = result.feedUrl ?: return

        viewModelScope.launch {
            _subscribing.value = _subscribing.value + result.itunesId
            try {
                ApiClient.api.subscribe(
                    SubscribeRequest(
                        feedUrl = feedUrl,
                        itunesId = result.itunesId,
                        title = result.title,
                        author = result.author,
                        artworkUrl = result.artworkUrl,
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _subscribing.value = _subscribing.value - result.itunesId
            }
        }
    }
}
