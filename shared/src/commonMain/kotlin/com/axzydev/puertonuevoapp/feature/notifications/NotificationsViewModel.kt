package com.axzydev.puertonuevoapp.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.notifications.NotificationDto
import com.axzydev.puertonuevoapp.core.network.notifications.NotificationsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val notificationsApi: NotificationsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { notificationsApi.list() }
            _uiState.update {
                it.copy(loading = false, error = result.exceptionOrNull()?.let(::networkMessage), notifications = result.getOrDefault(it.notifications))
            }
        }
    }

    fun markRead(notification: NotificationDto) {
        if (notification.read) return
        _uiState.update { state -> state.copy(notifications = state.notifications.map { if (it.id == notification.id) it.copy(read = true) else it }) }
        viewModelScope.launch { runCatching { notificationsApi.markRead(notification.id) } }
    }

    fun markAllRead() {
        _uiState.update { state -> state.copy(notifications = state.notifications.map { it.copy(read = true) }) }
        viewModelScope.launch { runCatching { notificationsApi.markAllRead() } }
    }

    fun remove(notification: NotificationDto) {
        _uiState.update { state -> state.copy(notifications = state.notifications.filter { it.id != notification.id }) }
        viewModelScope.launch { runCatching { notificationsApi.remove(notification.id) } }
    }
}
