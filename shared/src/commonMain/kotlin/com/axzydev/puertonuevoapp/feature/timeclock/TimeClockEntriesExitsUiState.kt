package com.axzydev.puertonuevoapp.feature.timeclock

import com.axzydev.puertonuevoapp.core.network.schedules.PeopleAttendanceDayDto
import com.axzydev.puertonuevoapp.core.network.schedules.PeopleAttendanceRowDto
import com.axzydev.puertonuevoapp.core.network.schedules.PeopleAttendanceSummaryDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockReportSessionRowDto
import com.axzydev.puertonuevoapp.core.util.formatLocalTime

/**
 * Estado de la pantalla de Entradas y salidas: una fila por persona con cada
 * día del periodo contra su horario (`POST /schedules/attendance/time-clock`,
 * la misma fuente que la web) + resumen, paginación y exportación CSV.
 */
data class TimeClockEntriesExitsUiState(
    val rows: List<PeopleAttendanceRowDto> = emptyList(),
    val summary: PeopleAttendanceSummaryDto? = null,
    val total: Int = 0,
    val page: Int = 1,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    /** Exportación CSV en curso (el archivo se comparte al terminar). */
    val exporting: Boolean = false,
    val exportError: String? = null,
    val period: AttendancePeriod = AttendancePeriod.Week,
    /** Día local de referencia del periodo (`YYYY-MM-DD`). */
    val date: String = todayDayKey(),
    val q: String = "",
    val includeInactive: Boolean = false,
    val view: AttendanceView = AttendanceView.All,
) {
    val hasMore: Boolean get() = rows.size < total

    /** Incidencias del periodo según el resumen del API. */
    val incidents: Int
        get() = summary?.let {
            it.absences + it.withoutExit + it.withoutEntry
        } ?: 0

    /** Días del periodo en orden (claves `YYYY-MM-DD`), para la tira de días. */
    val rangeDays: List<String> get() = summary?.range?.days.orEmpty()
}

/** Texto legible de la persona de la fila (el reloj puede no tener su nombre). */
internal fun PeopleAttendanceRowDto.displayName(): String =
    employeeName.ifBlank { employeeNumber?.let { "Número $it" } ?: "Sin nombre" }

/** Entrada/salida de un día como "07:58 – 16:10" (o un guion si falta alguna). */
internal fun PeopleAttendanceRowDto.timesLabel(day: PeopleAttendanceDayDto?): String {
    if (day == null) return "Sin registro"
    val entry = formatLocalTime(day.entryAt)
    val exit = formatLocalTime(day.exitAt)
    return if (day.entryAt != null || day.exitAt != null) "$entry – $exit" else "Sin registro"
}

/**
 * CSV de las sesiones del periodo (mismo detalle que exporta la web: una fila
 * por sesión, no por persona). Se arma en la app a partir de
 * `POST /time-clock/report/export`; [shareCsv] lo entrega al sistema.
 */
internal fun buildReportCsv(rows: List<TimeClockReportSessionRowDto>): String {
    fun escape(cell: String): String = "\"" + cell.replace("\"", "\"\"") + "\""
    val header = listOf(
        "Día",
        "Persona",
        "No. de empleado",
        "Departamento",
        "Puesto",
        "Entrada",
        "Salida",
        "Tiempo trabajado",
        "Incidencia",
    )
    val lines = rows.map { row ->
        listOf(
            row.date,
            row.employeeName,
            row.employeeNumber ?: "",
            row.departmentName ?: "",
            row.jobTitle ?: "",
            row.entryAt?.let(::formatLocalTime) ?: "",
            row.exitAt?.let(::formatLocalTime) ?: "",
            if (row.entryAt != null && row.exitAt != null) workedTimeLabel(row.workedMinutes) else "",
            row.incident?.let(::incidentLabel) ?: "",
        )
    }
    // BOM: Excel abre el archivo en UTF-8 sin preguntar.
    return "\uFEFF" + (listOf(header) + lines).joinToString("\r\n") { row -> row.joinToString(",") { escape(it) } }
}
