package com.podpirate.ui.screens.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podpirate.data.api.ApiClient
import com.podpirate.data.model.Subscription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SubscriptionsViewModel : ViewModel() {
    private val _subscriptions = MutableStateFlow<List<Subscription>>(emptyList())
    val subscriptions = _subscriptions.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _subscriptions.value = ApiClient.api.getSubscriptions()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    fun unsubscribe(id: Long) {
        viewModelScope.launch {
            try {
                ApiClient.api.unsubscribe(id)
                _subscriptions.value = _subscriptions.value.filter { it.id != id }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
