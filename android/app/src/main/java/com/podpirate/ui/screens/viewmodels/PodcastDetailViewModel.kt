package com.podpirate.ui.screens.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podpirate.data.api.ApiClient
import com.podpirate.data.model.Episode
import com.podpirate.data.model.Podcast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PodcastDetailViewModel : ViewModel() {
    private val _podcast = MutableStateFlow<Podcast?>(null)
    val podcast = _podcast.asStateFlow()

    private val _episodes = MutableStateFlow<List<Episode>>(emptyList())
    val episodes = _episodes.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    fun load(podcastId: Long) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _podcast.value = ApiClient.api.getPodcast(podcastId)
                _episodes.value = ApiClient.api.getEpisodes(podcastId).content
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }
}
