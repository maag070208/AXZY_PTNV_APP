package com.axzydev.puertonuevoapp.feature.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.tickets.TicketUpdateInput
import com.axzydev.puertonuevoapp.core.network.tickets.TicketsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TicketDetailViewModel(
    private val ticketId: String,
    private val ticketsApi: TicketsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketDetailUiState())
    val uiState: StateFlow<TicketDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { ticketsApi.get(ticketId) }
            _uiState.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let(::networkMessage),
                    ticket = result.getOrNull(),
                )
            }
        }
    }

    fun onStatusChange(newStatus: String) {
        val current = _uiState.value.ticket ?: return
        if (newStatus == current.status) return
        _uiState.update { it.copy(updatingStatus = true) }
        viewModelScope.launch {
            val result = runCatching { ticketsApi.update(ticketId, TicketUpdateInput(status = newStatus)) }
            _uiState.update {
                it.copy(
                    updatingStatus = false,
                    error = result.exceptionOrNull()?.let(::networkMessage) ?: it.error,
                    ticket = result.getOrNull() ?: it.ticket,
                )
            }
        }
    }

    fun onCommentChange(value: String) = _uiState.update { it.copy(newComment = value) }

    fun sendComment() {
        val text = _uiState.value.newComment.trim()
        if (text.isBlank() || _uiState.value.sendingComment) return
        _uiState.update { it.copy(sendingComment = true) }
        viewModelScope.launch {
            val result = runCatching {
                ticketsApi.addComment(ticketId, text)
                ticketsApi.get(ticketId)
            }
            _uiState.update {
                it.copy(
                    sendingComment = false,
                    newComment = if (result.isSuccess) "" else it.newComment,
                    error = result.exceptionOrNull()?.let(::networkMessage) ?: it.error,
                    ticket = result.getOrNull() ?: it.ticket,
                )
            }
        }
    }
}
