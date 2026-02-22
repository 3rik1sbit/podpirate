package com.podpirate.ui.screens.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.podpirate.data.local.AppDatabase
import com.podpirate.data.local.entity.QueueItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QueueViewModel(app: Application) : AndroidViewModel(app) {

    private val queueDao = AppDatabase.getInstance(app).queueDao()

    val items = queueDao.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun remove(episodeId: Long) {
        viewModelScope.launch { queueDao.deleteByEpisodeId(episodeId) }
    }

    fun clearAll() {
        viewModelScope.launch { queueDao.deleteAll() }
    }
}
