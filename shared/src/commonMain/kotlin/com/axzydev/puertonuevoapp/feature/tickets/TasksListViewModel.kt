package com.axzydev.puertonuevoapp.feature.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.tickets.TicketsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * "Mis tareas" / "Tareas del equipo": el backend filtra `/tickets/kanban`
 * según el rol en sesión, así que ambas pantallas reusan el mismo endpoint —
 * la diferencia de alcance la resuelve el servidor.
 */
class TasksListViewModel(
    private val ticketsApi: TicketsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksListUiState())
    val uiState: StateFlow<TasksListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { ticketsApi.kanban().data.filter { it.ticket.deletedAt == null } }
            _uiState.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let(::networkMessage),
                    rows = result.getOrDefault(it.rows),
                )
            }
        }
    }
}
