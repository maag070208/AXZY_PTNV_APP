package com.axzydev.puertonuevoapp.feature.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.tickets.TicketCreateInput
import com.axzydev.puertonuevoapp.core.network.tickets.TicketUpdateInput
import com.axzydev.puertonuevoapp.core.network.tickets.TicketsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Formulario de ticket: [ticketId] null = alta, no-null = edición. */
class TicketFormViewModel(
    private val ticketId: String?,
    private val ticketsApi: TicketsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketFormUiState(loading = ticketId != null))
    val uiState: StateFlow<TicketFormUiState> = _uiState.asStateFlow()

    init {
        if (ticketId != null) load(ticketId)
    }

    private fun load(id: String) {
        viewModelScope.launch {
            val result = runCatching { ticketsApi.get(id) }
            result.fold(
                onSuccess = { t ->
                    _uiState.update {
                        it.copy(loading = false, titulo = t.titulo, descripcion = t.descripcion, priority = t.priority, category = t.category)
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(loading = false, error = networkMessage(e)) } },
            )
        }
    }

    fun onTituloChange(value: String) = _uiState.update { it.copy(titulo = value, error = null) }
    fun onDescripcionChange(value: String) = _uiState.update { it.copy(descripcion = value, error = null) }
    fun onPriorityChange(value: String) = _uiState.update { it.copy(priority = value) }
    fun onCategoryChange(value: String) = _uiState.update { it.copy(category = value) }

    fun submit() {
        val state = _uiState.value
        if (!state.isValid || state.saving) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                if (ticketId == null) {
                    ticketsApi.create(
                        TicketCreateInput(
                            titulo = state.titulo.trim(),
                            descripcion = state.descripcion.trim(),
                            priority = state.priority,
                            category = state.category,
                        ),
                    )
                } else {
                    ticketsApi.update(
                        ticketId,
                        TicketUpdateInput(
                            titulo = state.titulo.trim(),
                            descripcion = state.descripcion.trim(),
                            priority = state.priority,
                            category = state.category,
                        ),
                    )
                }
            }
            _uiState.update {
                it.copy(
                    saving = false,
                    error = result.exceptionOrNull()?.let(::networkMessage),
                    savedTicketId = result.getOrNull()?.id,
                )
            }
        }
    }
}
