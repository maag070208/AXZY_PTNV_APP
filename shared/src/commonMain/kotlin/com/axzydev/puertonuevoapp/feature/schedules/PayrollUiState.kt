package com.axzydev.puertonuevoapp.feature.schedules

import com.axzydev.puertonuevoapp.core.network.departments.DepartmentDto
import com.axzydev.puertonuevoapp.core.network.schedules.PeopleAttendanceResponseDto
import com.axzydev.puertonuevoapp.core.network.schedules.WeeklyAttendanceReportDto

/** Pestaña de pre nómina: reporte semanal de RH o entradas/salidas por persona. */
internal const val PAYROLL_TAB_WEEKLY: String = "WEEKLY"
internal const val PAYROLL_TAB_PEOPLE: String = "PEOPLE"

/** Fuente de las entradas/salidas por persona (`/schedules/attendance/…`). */
internal const val ATTENDANCE_SOURCE_CLOCK: String = "TIME_CLOCK"
internal const val ATTENDANCE_SOURCE_ACCESS: String = "ACCESS"

/** Vistas rápidas de la tabla de entradas/salidas. */
internal val ATTENDANCE_VIEWS: List<Pair<String, String>> = listOf(
    "ALL" to "Todas",
    "INCIDENTS" to "Incidencias",
    "ON_SITE" to "En sitio",
    "WITHOUT_RECORDS" to "Sin registros",
)

/**
 * Estado de pre nómina (`payroll.view`).
 *
 * Dos vistas sobre la misma pantalla:
 * - **Asistencia semanal** (`POST /schedules/weekly-attendance`): 7 días por
 *   persona con horas, extra, faltas y aprobaciones.
 * - **Entradas y salidas** (`POST /schedules/attendance/{time-clock,access}`):
 *   cada día del periodo contra el horario de la persona, con su fuente
 *   (checadas del reloj o bitácora de acceso).
 */
data class PayrollUiState(
    val tab: String = PAYROLL_TAB_WEEKLY,
    val source: String = ATTENDANCE_SOURCE_CLOCK,
    val period: String = "WEEK",
    val dateKey: String = todayDateKey(),
    val departmentId: String = "",
    val query: String = "",
    val view: String = "ALL",
    val departments: List<DepartmentDto> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val weekly: WeeklyAttendanceReportDto? = null,
    val people: PeopleAttendanceResponseDto? = null,
) {
    val isWeekly: Boolean get() = tab == PAYROLL_TAB_WEEKLY

    val rangeLabel: String
        get() {
            if (isWeekly) {
                val range = weekly?.range ?: return formatDateKey(dateKey)
                return formatRange(range.start, range.end)
            }
            val range = people?.summary?.range ?: return formatDateKey(dateKey)
            return formatRange(range.start, range.end)
        }
}
