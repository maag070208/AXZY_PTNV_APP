package com.axzydev.puertonuevoapp.feature.tickets

import com.axzydev.puertonuevoapp.core.network.tickets.KanbanAssignmentDto

data class TasksListUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val rows: List<KanbanAssignmentDto> = emptyList(),
) {
    val overdueCount: Int get() = rows.count { it.overdue() }
}
