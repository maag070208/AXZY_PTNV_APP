package com.axzydev.puertonuevoapp.feature.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.tickets.TicketAssignmentCreateInput
import com.axzydev.puertonuevoapp.core.network.tickets.TicketAssignmentUpdateInput
import com.axzydev.puertonuevoapp.core.network.tickets.TicketsApi
import com.axzydev.puertonuevoapp.core.network.users.UsersApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Tablero kanban de tareas. [fixedTicketId] null = todas las tareas visibles al rol. */
class KanbanViewModel(
    private val fixedTicketId: String?,
    private val ticketsApi: TicketsApi,
    private val usersApi: UsersApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(KanbanUiState())
    val uiState: StateFlow<KanbanUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                ticketsApi.kanban().data
                    .filter { it.ticket.deletedAt == null }
                    .let { rows -> if (fixedTicketId != null) rows.filter { it.ticketId == fixedTicketId } else rows }
            }
            _uiState.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let(::networkMessage),
                    rows = result.getOrDefault(it.rows),
                )
            }
        }
    }

    fun onSearchChange(value: String) = _uiState.update { it.copy(search = value) }
    fun onDepartmentFilterChange(value: String) = _uiState.update { it.copy(departmentFilter = value) }
    fun select(id: String) = _uiState.update { state -> state.copy(selected = state.rows.firstOrNull { it.id == id }) }
    fun dismissSelected() = _uiState.update { it.copy(selected = null) }

    fun onAssignmentStatusChange(newStatus: String) {
        val assignment = _uiState.value.selected ?: return
        viewModelScope.launch {
            val result = runCatching {
                ticketsApi.updateAssignment(assignment.ticketId, assignment.id, TicketAssignmentUpdateInput(status = newStatus))
            }
            if (result.isSuccess) {
                _uiState.update { it.copy(selected = null) }
                load()
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.let(::networkMessage)) }
            }
        }
    }

    fun openCreate() {
        _uiState.update {
            it.copy(
                showCreate = true,
                createTicketId = fixedTicketId ?: "",
                createUserId = "",
                createTitle = "",
                createDescription = "",
                createStartDate = "",
                createDueDate = "",
                createError = null,
            )
        }
        viewModelScope.launch {
            val employees = runCatching { usersApi.employees() }.getOrDefault(emptyList())
            val tickets = if (fixedTicketId == null) runCatching { ticketsApi.list().data }.getOrDefault(emptyList()) else emptyList()
            _uiState.update { it.copy(createEmployees = employees, createTickets = tickets) }
        }
    }

    fun dismissCreate() = _uiState.update { it.copy(showCreate = false) }

    fun onCreateTicketChange(value: String) = _uiState.update { it.copy(createTicketId = value) }
    fun onCreateUserChange(value: String) = _uiState.update { it.copy(createUserId = value) }
    fun onCreateTitleChange(value: String) = _uiState.update { it.copy(createTitle = value) }
    fun onCreateDescriptionChange(value: String) = _uiState.update { it.copy(createDescription = value) }
    fun onCreateStartDateChange(value: String) = _uiState.update { it.copy(createStartDate = value) }
    fun onCreateDueDateChange(value: String) = _uiState.update { it.copy(createDueDate = value) }

    private fun isoOrNull(dateOnly: String): String? =
        dateOnly.trim().takeIf { it.length == 10 }?.let { "${it}T00:00:00.000Z" }

    fun submitCreate() {
        val state = _uiState.value
        if (!state.createValid || state.createSaving) return
        _uiState.update { it.copy(createSaving = true, createError = null) }
        viewModelScope.launch {
            val result = runCatching {
                ticketsApi.createAssignment(
                    state.createTicketId,
                    TicketAssignmentCreateInput(
                        userId = state.createUserId,
                        title = state.createTitle.trim(),
                        description = state.createDescription.trim().ifBlank { null },
                        startDate = isoOrNull(state.createStartDate),
                        dueDate = isoOrNull(state.createDueDate),
                    ),
                )
            }
            if (result.isSuccess) {
                _uiState.update { it.copy(createSaving = false, showCreate = false) }
                load()
            } else {
                _uiState.update { it.copy(createSaving = false, createError = result.exceptionOrNull()?.let(::networkMessage)) }
            }
        }
    }
}
