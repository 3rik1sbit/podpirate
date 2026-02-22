package com.podpirate.ui.screens.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podpirate.data.api.ApiClient
import com.podpirate.data.model.AdSegment
import com.podpirate.data.model.Episode
import com.podpirate.data.model.TranscriptionSegment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class EpisodePlayerViewModel : ViewModel() {
    private val json = Json { ignoreUnknownKeys = true }

    private val _episode = MutableStateFlow<Episode?>(null)
    val episode = _episode.asStateFlow()

    private val _segments = MutableStateFlow<List<TranscriptionSegment>>(emptyList())
    val segments = _segments.asStateFlow()

    private val _adSegments = MutableStateFlow<List<AdSegment>>(emptyList())
    val adSegments = _adSegments.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    fun load(episodeId: Long) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _episode.value = ApiClient.api.getEpisode(episodeId)

                try {
                    val transcription = ApiClient.api.getTranscription(episodeId)
                    _segments.value = json.decodeFromString(transcription.segments)
                } catch (_: Exception) {
                    _segments.value = emptyList()
                }

                try {
                    _adSegments.value = ApiClient.api.getAdSegments(episodeId)
                } catch (_: Exception) {
                    _adSegments.value = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }
}
