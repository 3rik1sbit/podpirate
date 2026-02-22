package com.podpirate.ui.screens.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.podpirate.data.download.DownloadManager
import com.podpirate.data.local.AppDatabase
import com.podpirate.data.local.entity.DownloadedEpisode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(app: Application) : AndroidViewModel(app) {

    private val downloadDao = AppDatabase.getInstance(app).downloadDao()

    val downloads = downloadDao.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(episodeId: Long) {
        viewModelScope.launch {
            DownloadManager.deleteDownload(getApplication(), episodeId)
        }
    }
}
