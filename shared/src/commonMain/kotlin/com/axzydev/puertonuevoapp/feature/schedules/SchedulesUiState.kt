package com.axzydev.puertonuevoapp.feature.schedules

import com.axzydev.puertonuevoapp.core.network.schedules.ScheduleDto

/** Filtro rápido del listado de horarios. */
enum class SchedulesFilter { ALL, ACTIVE, INACTIVE }

/**
 * Estado del listado de horarios. El catálogo es pequeño (decenas de filas) y
 * el API lo devuelve completo (`GET /schedules?includeInactive=true`), así que
 * el filtrado por texto/estado es local, como en la web.
 */
data class SchedulesUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val schedules: List<ScheduleDto> = emptyList(),
    val query: String = "",
    val filter: SchedulesFilter = SchedulesFilter.ALL,
    /** Operación (activar/desactivar/eliminar) en curso. */
    val busy: Boolean = false,
    /** Horario pendiente de confirmar para desactivar/eliminar. */
    val pendingDeactivate: ScheduleDto? = null,
    val pendingDelete: ScheduleDto? = null,
) {
    val visible: List<ScheduleDto>
        get() {
            val term = query.trim().lowercase()
            return schedules.filter { schedule ->
                val matchesFilter = when (filter) {
                    SchedulesFilter.ALL -> true
                    SchedulesFilter.ACTIVE -> schedule.active
                    SchedulesFilter.INACTIVE -> !schedule.active
                }
                val matchesQuery = term.isEmpty() || schedule.name.lowercase().contains(term)
                matchesFilter && matchesQuery
            }
        }

    val activeCount: Int get() = schedules.count { it.active }

    /** Personas asignadas sumando todos los horarios (una persona puede estar en uno solo vigente). */
    val assignedTotal: Int get() = schedules.sumOf { it.assigned ?: 0 }
}
