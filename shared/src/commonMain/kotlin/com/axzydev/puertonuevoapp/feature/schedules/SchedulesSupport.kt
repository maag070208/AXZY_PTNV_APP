package com.axzydev.puertonuevoapp.feature.schedules

import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.network.schedules.ScheduleDayDto
import com.axzydev.puertonuevoapp.core.network.schedules.SchedulesApi
import com.axzydev.puertonuevoapp.core.util.currentTimeMillis
import com.axzydev.puertonuevoapp.core.util.formatMinutesAsHours
import com.axzydev.puertonuevoapp.core.util.localOffsetMillis

/**
 * API del módulo para las pantallas de horarios.
 *
 * `AppContainer` todavía no expone `SchedulesApi` (el composition root lo
 * cablea otra tarea), así que la feature lo construye una sola vez aquí con el
 * `ApiClient` compartido. Cuando se agregue al contenedor, esto se reemplaza
 * por `AppContainer.schedulesApi` sin tocar las pantallas.
 */
internal object SchedulesServices {
    val api: SchedulesApi by lazy { SchedulesApi(AppContainer.apiClient) }
}

/** Milisegundos de un día. */
internal const val DAY_MILLIS: Long = 86_400_000L

/** Períodos de reporte que acepta el API (`REPORT_PERIODS`). */
internal val REPORT_PERIODS: List<Pair<String, String>> = listOf(
    "DAY" to "Día",
    "WEEK" to "Semana",
    "FORTNIGHT" to "Quincena",
    "MONTH" to "Mes",
)

/** Pestañas de estatus de tiempo extra (`ALL` = todas). */
internal val OVERTIME_STATUS_TABS: List<Pair<String, String>> = listOf(
    "PENDING" to "Pendientes",
    "APPROVED" to "Aprobadas",
    "REJECTED" to "Rechazadas",
    "ALL" to "Todas",
)

internal fun periodLabel(period: String): String =
    REPORT_PERIODS.firstOrNull { it.first == period }?.second ?: period

internal fun overtimeStatusLabel(status: String): String = when (status) {
    "PENDING" -> "Pendiente"
    "APPROVED" -> "Aprobada"
    "REJECTED" -> "Rechazada"
    else -> status
}

/** Estado de un día del reporte semanal (`WEEKLY` es `WeeklyAttendanceDayDto.status`). */
internal fun attendanceDayStatusLabel(status: String): String = when (status) {
    "WORKED" -> "Trabajado"
    "OVERTIME" -> "Extra"
    "ABSENCE" -> "Falta"
    "INCOMPLETE" -> "Incompleto"
    "REST" -> "Descanso"
    "REST_WORKED" -> "Descanso trabajado"
    "NO_INFO" -> "Sin datos"
    "FUTURE" -> "Futuro"
    "ATTENDED" -> "Asistió"
    "LATE" -> "Retardo"
    "PENDING" -> "Pendiente"
    else -> status
}

internal fun attendanceDayStatusCode(status: String): String = when (status) {
    "WORKED", "ATTENDED" -> "A"
    "OVERTIME", "REST_WORKED" -> "E"
    "LATE" -> "R"
    "ABSENCE" -> "F"
    "INCOMPLETE" -> "I"
    "REST" -> "D"
    "PENDING", "FUTURE" -> "·"
    else -> "·"
}

/** Minutos de la jornada de un día, descontando comida y sumando el segundo tramo. */
internal fun dayMinutes(day: ScheduleDayDto, mealBreakMin: Int): Int {
    if (day.restDay || day.startTime == null || day.endTime == null) return 0
    val span = minutesBetween(day.startTime, day.endTime)
    val second = if (day.splitStartTime != null && day.splitEndTime != null) {
        minutesBetween(day.splitStartTime, day.splitEndTime)
    } else {
        0
    }
    return (span + second - mealBreakMin).coerceAtLeast(0)
}

/** Distancia en minutos entre dos horas "de pared" (cruza medianoche si hace falta). */
internal fun minutesBetween(from: String, to: String): Int {
    val diff = timeToMinutes(to) - timeToMinutes(from)
    return if (diff > 0) diff else diff + 24 * 60
}

internal fun timeToMinutes(value: String): Int {
    val parts = value.split(":")
    val hours = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return hours * 60 + minutes
}

internal fun isValidTime(value: String): Boolean =
    Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(value)

/** Minutos semanales de un horario (suma de sus días). */
internal fun weeklyMinutes(days: List<ScheduleDayDto>, mealBreakMin: Int): Int =
    days.sumOf { dayMinutes(it, mealBreakMin) }

internal fun daysWorked(days: List<ScheduleDayDto>): Int = days.count { !it.restDay }

internal fun hoursLabel(minutes: Int): String = formatMinutesAsHours(minutes)

/** "08:00–16:00 · descanso 14:00–15:00" del primer día laborable, o "—". */
internal fun daySummary(days: List<ScheduleDayDto>): String {
    val first = days.sortedBy { it.weekday }.firstOrNull { !it.restDay }
    val start = first?.startTime ?: return "—"
    val end = first.endTime ?: return "—"
    val split = if (first.splitStartTime != null && first.splitEndTime != null) {
        " · descanso ${first.splitStartTime}–${first.splitEndTime}"
    } else {
        ""
    }
    return "$start–$end$split"
}

internal val WEEKDAY_NAMES: List<String> =
    listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

internal val WEEKDAY_SHORT: List<String> = listOf("L", "M", "X", "J", "V", "S", "D")

internal val MONTH_SHORT: List<String> =
    listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** Día de la semana ISO (1 = lunes … 7 = domingo) de una fecha `YYYY-MM-DD`. */
internal fun weekdayOf(dateKey: String): Int {
    val days = daysFromDateKey(dateKey) ?: return 1
    return floorMod(days + 3, 7L).toInt() + 1
}

/** Fecha local `YYYY-MM-DD` de hoy. */
internal fun todayDateKey(): String {
    val millis = currentTimeMillis()
    return dateKeyOfLocalMillis(millis)
}

internal fun dateKeyOfLocalMillis(millis: Long): String =
    dateKeyOfEpochDay(floorDiv(millis + localOffsetMillis(millis), DAY_MILLIS))

/** Suma (o resta) días a una fecha `YYYY-MM-DD`; la clave original si no es válida. */
internal fun shiftDateKey(dateKey: String, days: Int): String {
    val base = daysFromDateKey(dateKey) ?: return dateKey
    return dateKeyOfEpochDay(base + days)
}

/** "lun 27 oct" de una fecha `YYYY-MM-DD`. */
/**
 * Clave de día `YYYY-MM-DD` a partir de lo que manda el API, que a veces es un
 * instante ISO (`2026-10-28T06:00:00.000Z`): se toman los primeros 10 caracteres.
 */
internal fun dateKeyOf(raw: String): String = raw.take(10)

internal fun formatDateKey(rawKey: String): String {
    val dateKey = dateKeyOf(rawKey)
    val day = dateKey.substringAfterLast('-').toIntOrNull() ?: return rawKey
    val month = dateKey.substringAfter('-').take(2).toIntOrNull()?.minus(1) ?: return rawKey
    val weekday = weekdayOf(dateKey)
    val prefix = WEEKDAY_NAMES[weekday - 1].lowercase().take(3)
    val monthName = MONTH_SHORT.getOrElse(month) { "" }
    return "$prefix $day $monthName"
}

/**
 * Rango legible "27 oct – 2 nov" de dos fechas del API. El `end` puede venir como
 * instante exclusivo (medianoche del día siguiente): se muestra el último día
 * incluido, igual que los reportes de la web.
 */
internal fun formatRange(start: String, end: String): String {
    if (start.isBlank() || end.isBlank()) return "—"
    val startKey = dateKeyOf(start)
    val endKey = shiftDateKey(dateKeyOf(end), -1)
    return if (startKey == endKey) formatDateKey(startKey) else "${formatDateKey(startKey)} – ${formatDateKey(endKey)}"
}

private fun dateKeyOfEpochDay(epochDay: Long): String {
    var z = epochDay + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val year = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val day = doy - (153 * mp + 2) / 5 + 1
    val month = if (mp < 10) mp + 3 else mp - 9
    val y = if (month <= 2) year + 1 else year
    val mm = month.toString().padStart(2, '0')
    val dd = day.toString().padStart(2, '0')
    return "${y.toString().padStart(4, '0')}-$mm-$dd"
}

private fun daysFromDateKey(dateKey: String): Long? {
    val parts = dateKey.split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    if (month !in 1..12 || day !in 1..31) return null
    val yy = if (month <= 2) year - 1 else year
    val era = (if (yy >= 0) yy else yy - 399) / 400
    val yoe = yy - era * 400
    val mp = if (month > 2) month - 3 else month + 9
    val doy = (153 * mp + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097L + doe - 719468
}

private fun floorDiv(value: Long, divisor: Long): Long {
    val q = value / divisor
    return if (value % divisor != 0L && (value xor divisor) < 0) q - 1 else q
}

private fun floorMod(value: Long, divisor: Long): Long {
    val mod = value % divisor
    return if (mod < 0) mod + divisor else mod
}

/** Agrega un renglón CSV escapando comillas (los exports que arma la app). */
internal fun StringBuilder.appendCsvRow(vararg values: String) {
    append(values.joinToString(",") { value -> "\"${value.replace("\"", "\"\"")}\"" })
    appendLine()
}
