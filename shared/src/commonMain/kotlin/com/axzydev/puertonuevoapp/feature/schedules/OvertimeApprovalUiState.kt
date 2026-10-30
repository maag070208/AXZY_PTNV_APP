package com.axzydev.puertonuevoapp.feature.schedules

import com.axzydev.puertonuevoapp.core.network.departments.DepartmentDto
import com.axzydev.puertonuevoapp.core.network.schedules.OvertimeDayRowDto
import com.axzydev.puertonuevoapp.core.network.schedules.OvertimeDecisionItem
import com.axzydev.puertonuevoapp.core.network.schedules.OvertimeSummaryDto

/** Llave estable de un día de tiempo extra (persona + fecha). */
internal fun overtimeDayKey(row: OvertimeDayRowDto): String = "${row.userId}|${row.date}"

/**
 * Estado de la aprobación de tiempo extra (`overtime.view`; decidir exige
 * `overtime.approve`).
 *
 * La API calcula los días pendientes al vuelo y guarda solo las decisiones.
 * Quien no puede aprobar (p. ej. RH) queda en modo solo lectura y el servidor,
 * además, le devuelve únicamente lo aprobado.
 */
data class OvertimeApprovalUiState(
    val canApprove: Boolean = false,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val period: String = "WEEK",
    val dateKey: String = todayDateKey(),
    val departmentId: String = "",
    val query: String = "",
    val statusTab: String = "PENDING",
    val departments: List<DepartmentDto> = emptyList(),
    val rows: List<OvertimeDayRowDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val summary: OvertimeSummaryDto? = null,
    val selected: Map<String, OvertimeDecisionItem> = emptyMap(),
    val saving: Boolean = false,
    val rejectOpen: Boolean = false,
    val rejectNote: String = "",
) {
    val effectiveTab: String get() = if (canApprove) statusTab else "APPROVED"

    val hasMore: Boolean get() = rows.size < total

    /** Solo los días pendientes se pueden seleccionar (y solo si se puede aprobar). */
    val selectablePending: List<OvertimeDayRowDto>
        get() = if (canApprove) rows.filter { it.status == "PENDING" } else emptyList()

    val allVisibleSelected: Boolean
        get() = selectablePending.isNotEmpty() &&
            selectablePending.all { selected.containsKey(overtimeDayKey(it)) }

    val rangeLabel: String
        get() {
            val range = summary?.range ?: return formatDateKey(dateKey)
            return formatRange(range.start, range.end)
        }
}
