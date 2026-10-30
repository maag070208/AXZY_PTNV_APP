package com.axzydev.puertonuevoapp.feature.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.tickets.TicketCategoryRefDto
import com.axzydev.puertonuevoapp.core.network.tickets.TicketCreateInput
import com.axzydev.puertonuevoapp.core.network.tickets.TicketDto
import com.axzydev.puertonuevoapp.core.network.tickets.TicketUpdateInput
import com.axzydev.puertonuevoapp.core.network.tickets.TicketsApi
import com.axzydev.puertonuevoapp.core.network.users.UserDto
import com.axzydev.puertonuevoapp.core.network.users.UsersApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * Formulario de ticket: [ticketId] null = alta, no-null = edición.
 *
 * En el alta de un ADMIN ([canCaptureForOthers]) se puede elegir a nombre de
 * quién queda el ticket: por defecto "Yo" y, si se elige otra persona, el API
 * lo crea con `onBehalfOfId` (ticket creado por ella y notificación para ella).
 * El selector de personas usa `GET /users/employees` (activos). El API exige el
 * rol ADMIN para `onBehalfOfId`; la app solo oculta el campo.
 */
class TicketFormViewModel(
    private val ticketId: String?,
    private val ticketsApi: TicketsApi,
    private val usersApi: UsersApi,
    private val currentUserId: String? = null,
    currentUserName: String = "",
    canCaptureForOthers: Boolean = false,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TicketFormUiState(
            loading = true,
            // Solo el alta permite capturar a nombre de otra persona.
            canCaptureForOthers = canCaptureForOthers && ticketId == null,
            currentUserId = currentUserId.orEmpty(),
            currentUserName = currentUserName,
        ),
    )
    val uiState: StateFlow<TicketFormUiState> = _uiState.asStateFlow()

    init {
        load()
        if (_uiState.value.canCaptureForOthers) loadEmployees()
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
                    title = t?.title ?: it.title,
                    description = t?.description ?: it.description,
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
            .sortedBy { it.name.lowercase() }
            .map { it.id to it.name }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value, error = null) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value, error = null) }
    fun onPriorityChange(value: String) = _uiState.update { it.copy(priority = value) }
    fun onCategoryChange(value: String) = _uiState.update { it.copy(categoryId = value) }

    // ── "A nombre de" ─────────────────────────────────────────────────────────

    /** Abre el selector de personas (carga el personal la primera vez). */
    fun openRequesterPicker() {
        if (!_uiState.value.canCaptureForOthers) return
        _uiState.update { it.copy(showRequesterPicker = true, employeeQuery = "") }
        if (_uiState.value.employees.isEmpty()) loadEmployees()
    }

    fun dismissRequesterPicker() {
        _uiState.update { it.copy(showRequesterPicker = false, employeeQuery = "") }
    }

    fun onEmployeeQueryChange(value: String) = _uiState.update { it.copy(employeeQuery = value) }

    /** Deja el ticket a nombre de la cuenta elegida (solo en el alta de ADMIN). */
    fun selectRequester(user: UserDto) {
        if (!_uiState.value.canCaptureForOthers) return
        _uiState.update {
            it.copy(
                requesterId = user.id,
                requesterName = user.name,
                showRequesterPicker = false,
                employeeQuery = "",
            )
        }
    }

    /** Vuelve a "A mi nombre". */
    fun clearRequester() {
        _uiState.update {
            it.copy(
                requesterId = "",
                requesterName = "",
                showRequesterPicker = false,
                employeeQuery = "",
            )
        }
    }

    /** Reintenta la carga del personal para el selector. */
    fun reloadEmployees() = loadEmployees()

    private fun loadEmployees() {
        if (_uiState.value.loadingEmployees) return
        _uiState.update { it.copy(loadingEmployees = true, employeesError = null) }
        viewModelScope.launch {
            val result = runCatching { usersApi.employees() }
            _uiState.update {
                it.copy(
                    loadingEmployees = false,
                    employees = result.getOrDefault(it.employees),
                    employeesError = result.exceptionOrNull()?.let(::networkMessage),
                )
            }
        }
    }

    // ── Guardar ───────────────────────────────────────────────────────────────

    fun submit() {
        val state = _uiState.value
        if (!state.isValid || state.saving) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                if (ticketId == null) {
                    ticketsApi.create(
                        TicketCreateInput(
                            title = state.title.trim(),
                            description = state.description.trim(),
                            priority = state.priority,
                            categoryId = state.categoryId.ifBlank { null },
                            // A nombre de otra persona: el ticket queda creado por
                            // ella (el API valida que sea ADMIN).
                            onBehalfOfId = state.requesterId.takeIf { state.isOnBehalf },
                        ),
                    )
                } else {
                    ticketsApi.update(
                        ticketId,
                        TicketUpdateInput(
                            title = state.title.trim(),
                            description = state.description.trim(),
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
