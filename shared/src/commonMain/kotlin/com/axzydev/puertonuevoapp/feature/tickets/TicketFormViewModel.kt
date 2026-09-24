package com.axzydev.puertonuevoapp.feature.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.tickets.TicketCategoryRefDto
import com.axzydev.puertonuevoapp.core.network.tickets.TicketCreateInput
import com.axzydev.puertonuevoapp.core.network.tickets.TicketDto
import com.axzydev.puertonuevoapp.core.network.tickets.TicketUpdateInput
import com.axzydev.puertonuevoapp.core.network.tickets.TicketsApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/** Formulario de ticket: [ticketId] null = alta, no-null = edición. */
class TicketFormViewModel(
    private val ticketId: String?,
    private val ticketsApi: TicketsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketFormUiState(loading = true))
    val uiState: StateFlow<TicketFormUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            // Sin catálogo el formulario sigue sirviendo: el ticket va sin categoría.
            val categories = async { runCatching { ticketsApi.categories() }.getOrDefault(emptyList()) }
            val ticket = ticketId?.let { id -> runCatching { ticketsApi.get(id) } }
            val options = categoryOptions(categories.await(), ticket?.getOrNull())
            val t = ticket?.getOrNull()
            _uiState.update {
                it.copy(
                    loading = false,
                    error = ticket?.exceptionOrNull()?.let(::networkMessage),
                    categoryOptions = options,
                    titulo = t?.titulo ?: it.titulo,
                    descripcion = t?.descripcion ?: it.descripcion,
                    priority = t?.priority ?: it.priority,
                    categoryId = t?.categoryId.orEmpty(),
                    initialCategoryId = t?.categoryId.orEmpty(),
                )
            }
        }
    }

    /** Catálogo activo + la categoría actual del ticket aunque ya no esté activa. */
    private fun categoryOptions(active: List<TicketCategoryRefDto>, ticket: TicketDto?): List<Pair<String, String>> {
        val current = ticket?.category?.takeIf { cat -> active.none { it.id == cat.id } }
        return listOf(NO_CATEGORY) + (active + listOfNotNull(current))
            .sortedBy { it.nombre.lowercase() }
            .map { it.id to it.nombre }
    }

    fun onTituloChange(value: String) = _uiState.update { it.copy(titulo = value, error = null) }
    fun onDescripcionChange(value: String) = _uiState.update { it.copy(descripcion = value, error = null) }
    fun onPriorityChange(value: String) = _uiState.update { it.copy(priority = value) }
    fun onCategoryChange(value: String) = _uiState.update { it.copy(categoryId = value) }

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
                            categoryId = state.categoryId.ifBlank { null },
                        ),
                    )
                } else {
                    ticketsApi.update(
                        ticketId,
                        TicketUpdateInput(
                            titulo = state.titulo.trim(),
                            descripcion = state.descripcion.trim(),
                            priority = state.priority,
                            // Solo si cambió; `JsonNull` la quita.
                            categoryId = when {
                                state.categoryId == state.initialCategoryId -> null
                                state.categoryId.isBlank() -> JsonNull
                                else -> JsonPrimitive(state.categoryId)
                            },
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
