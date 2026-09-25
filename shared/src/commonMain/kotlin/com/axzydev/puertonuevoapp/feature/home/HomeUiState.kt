package com.axzydev.puertonuevoapp.feature.home

import com.axzydev.puertonuevoapp.core.network.dashboard.DashboardSummaryDto
import com.axzydev.puertonuevoapp.core.network.tickets.KanbanAssignmentDto

/** Inicio: el ADMIN ve el panel; los demás, las tareas abiertas que les tocan. */
data class HomeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val dashboard: DashboardSummaryDto? = null,
    /** Tareas sin completar del tablero, según el rol (el empleado: las suyas). */
    val tasks: List<KanbanAssignmentDto> = emptyList(),
)
