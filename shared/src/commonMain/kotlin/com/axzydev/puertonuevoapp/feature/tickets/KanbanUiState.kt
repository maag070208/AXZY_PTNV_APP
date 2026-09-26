package com.axzydev.puertonuevoapp.feature.tickets

import com.axzydev.puertonuevoapp.core.network.tickets.KanbanAssignmentDto
import com.axzydev.puertonuevoapp.core.network.tickets.TicketDto
import com.axzydev.puertonuevoapp.core.network.users.UserDto

data class KanbanColumn(val status: String, val label: String)

val kanbanColumns = listOf(
    KanbanColumn("PENDING", "Pendiente"),
    KanbanColumn("IN_PROGRESS", "En progreso"),
    KanbanColumn("IN_REVIEW", "En revisión"),
    KanbanColumn("COMPLETED", "Completada"),
)

data class KanbanUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val rows: List<KanbanAssignmentDto> = emptyList(),
    val search: String = "",
    val departmentFilter: String = "",
    val selected: KanbanAssignmentDto? = null,
    val showCreate: Boolean = false,
    val createTickets: List<TicketDto> = emptyList(),
    val createEmployees: List<UserDto> = emptyList(),
    val createTicketId: String = "",
    val createUserId: String = "",
    val createTitle: String = "",
    val createDescription: String = "",
    val createStartDate: String = "",
    val createDueDate: String = "",
    val createSaving: Boolean = false,
    val createError: String? = null,
) {
    val departmentOptions: List<String>
        get() = rows.map { it.ticket.department?.name ?: "General" }.distinct().sorted()

    val filtered: List<KanbanAssignmentDto>
        get() = rows.filter { r ->
            val deptName = r.ticket.department?.name ?: "General"
            (departmentFilter.isBlank() || deptName == departmentFilter) &&
                (search.isBlank() || r.title.contains(search, ignoreCase = true) || r.ticket.title.contains(search, ignoreCase = true))
        }

    val createValid: Boolean
        get() = createTicketId.isNotBlank() && createUserId.isNotBlank() && createTitle.isNotBlank()
}
