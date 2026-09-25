package com.axzydev.puertonuevoapp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.dashboard.DashboardApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.tickets.TicketsApi
import com.axzydev.puertonuevoapp.core.session.AuthRepository
import com.axzydev.puertonuevoapp.core.session.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del Inicio. El ADMIN carga el panel (`/dashboard/summary`); el resto,
 * sus pendientes: las tareas del tablero (`/tickets/kanban`, que la API ya acota
 * por rol) que no están completadas.
 */
class HomeViewModel(
    private val authRepository: AuthRepository,
    private val ticketsApi: TicketsApi,
    private val dashboardApi: DashboardApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val isAdmin: Boolean
        get() = (authRepository.state.value as? AuthState.LoggedIn)?.user?.isAdmin == true

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            if (isAdmin) {
                val result = runCatching { dashboardApi.summary() }
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = result.exceptionOrNull()?.let(::networkMessage),
                        dashboard = result.getOrNull(),
                    )
                }
            } else {
                val result = runCatching {
                    ticketsApi.kanban().data.filter { it.ticket.deletedAt == null && it.status != "COMPLETADA" }
                }
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = result.exceptionOrNull()?.let(::networkMessage),
                        tasks = result.getOrDefault(it.tasks),
                    )
                }
            }
        }
    }
}
