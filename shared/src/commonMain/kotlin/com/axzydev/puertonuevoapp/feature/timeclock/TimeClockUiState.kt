package com.axzydev.puertonuevoapp.feature.timeclock

import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockStatusDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockSummaryDto
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.util.currentTimeMillis
import com.axzydev.puertonuevoapp.core.util.formatIsoUtc
import com.axzydev.puertonuevoapp.core.util.localOffsetMillis
import com.axzydev.puertonuevoapp.core.util.parseIsoToEpochMillis

// ── Asistencia: rango de fechas ─────────────────────────────────────────────

/** Presets de rango de la pantalla de Asistencia (mismos que la web) + rango a mano. */
enum class TimeClockRangeMode(val label: String) {
    Today("Hoy"),
    Yesterday("Ayer"),
    Last7("Últimos 7"),
    Custom("Personalizado"),
}

/** Día(s) que consulta la Asistencia, como claves locales `YYYY-MM-DD` (las entiende el API). */
data class TimeClockRange(
    val mode: TimeClockRangeMode = TimeClockRangeMode.Today,
    val from: String = todayDayKey(),
    val to: String = todayDayKey(),
) {
    /** Un rango personalizado solo se consulta con fechas válidas y ordenadas. */
    val isValid: Boolean
        get() = mode != TimeClockRangeMode.Custom ||
            (isDayKey(from) && isDayKey(to) && from <= to)
}

/** Estado del tablero de relojes que acompaña a la tabla de checadas. */
data class TimeClockOverviewUiState(
    val loading: Boolean = true,
    val status: TimeClockStatusDto? = null,
    /** `GET /time-clock/summary` exige `time_clocks.manage`/`time_clock.write`: puede ser null. */
    val summary: TimeClockSummaryDto? = null,
    val error: String? = null,
) {
    val devices get() = status?.devices.orEmpty()

    /** Relojes sin responder o con la última corrida fallida. */
    val failedDevices: Int
        get() = devices.count { it.pausedByCredentials || it.lastRun?.ok == false }

    /** Checadas de hoy según el tablero (solo si el resumen está disponible). */
    val punchesToday: Int? get() = summary?.let { s -> s.clocks.sumOf { it.punchesToday } }

    /** Alertas del tablero (reloj caído, credenciales, padrón, sin vincular...). */
    val alertCount: Int get() = summary?.alerts?.size ?: 0
}

// ── Entradas y salidas: periodo del reporte ─────────────────────────────────

/** Periodo del reporte por persona (`period` del API) con su etiqueta. */
enum class AttendancePeriod(val apiValue: String, val label: String) {
    Day("DAY", "Día"),
    Week("WEEK", "Semana"),
    Fortnight("FORTNIGHT", "Quincena"),
    Month("MONTH", "Mes"),
}

/** Vistas rápidas del reporte (`view` del API). */
enum class AttendanceView(val apiValue: String, val label: String) {
    All("ALL", "Todos"),
    Incidents("INCIDENTS", "Incidencias"),
    OnSite("ON_SITE", "En sitio"),
    WithoutRecords("WITHOUT_RECORDS", "Sin registros"),
}

// ── Cálculo de fechas (sin kotlinx-datetime) ───────────────────────────────

internal const val DAY_MILLIS: Long = 86_400_000L

private val DAY_KEY_REGEX = Regex("""\d{4}-\d{2}-\d{2}""")

/** ¿La cadena es una clave de día `YYYY-MM-DD`? */
internal fun isDayKey(value: String): Boolean = DAY_KEY_REGEX.matches(value)

/** Clave del día local de hoy (o de hace/para [offsetDays] días), en la zona del dispositivo. */
internal fun todayDayKey(offsetDays: Int = 0): String {
    val now = currentTimeMillis()
    val local = now + localOffsetMillis(now)
    val epochDay = if (local >= 0) local / DAY_MILLIS else -((-local + DAY_MILLIS - 1) / DAY_MILLIS)
    return formatIsoUtc((epochDay + offsetDays) * DAY_MILLIS).take(10)
}

/** Suma [days] días a una clave `YYYY-MM-DD`. */
internal fun dayKeyPlusDays(dayKey: String, days: Int): String {
    val millis = parseIsoToEpochMillis("${dayKey}T00:00:00.000Z") ?: return dayKey
    return formatIsoUtc(millis + days * DAY_MILLIS).take(10)
}

/** Clave del día de referencia al moverse un periodo completo hacia atrás/adelante. */
internal fun shiftPeriodDayKey(dayKey: String, period: AttendancePeriod, step: Int): String {
    if (period == AttendancePeriod.Day) return dayKeyPlusDays(dayKey, step)
    if (period == AttendancePeriod.Week) return dayKeyPlusDays(dayKey, 7 * step)
    val year = dayKey.take(4).toIntOrNull() ?: return dayKey
    val month = dayKey.substring(5, 7).toIntOrNull() ?: return dayKey
    val day = dayKey.takeLast(2).toIntOrNull() ?: return dayKey
    if (period == AttendancePeriod.Month) {
        return monthKey(year, month + step) + "-01"
    }
    // Quincena: 1–15 ↔ 16–fin de mes (igual que la web).
    return if (day <= 15) {
        if (step > 0) monthKey(year, month) + "-16" else monthKey(year, month - 1) + "-16"
    } else {
        if (step > 0) monthKey(year, month + 1) + "-01" else monthKey(year, month) + "-01"
    }
}

/** `YYYY-MM` de un año/mes 1..12 desbordado (mes 13 → enero del año siguiente). */
private fun monthKey(year: Int, month: Int): String {
    val shifted = year * 12 + (month - 1)
    val y = if (shifted >= 0) shifted / 12 else -((-shifted + 11) / 12)
    val m = shifted - y * 12 + 1
    return "${y.toString().padStart(4, '0')}-${m.toString().padStart(2, '0')}"
}

private val MONTHS_SHORT = listOf(
    "ene", "feb", "mar", "abr", "may", "jun",
    "jul", "ago", "sep", "oct", "nov", "dic",
)

/** "07 oct" para una clave de día. */
internal fun shortDayLabel(dayKey: String): String {
    if (!isDayKey(dayKey)) return dayKey
    val month = dayKey.substring(5, 7).toIntOrNull() ?: return dayKey
    val label = MONTHS_SHORT.getOrNull(month - 1) ?: return dayKey
    return "${dayKey.takeLast(2)} $label"
}

/** Letra del día de la semana (L M M J V S D) para la tira de días. */
internal fun weekdayLetter(dayKey: String): String {
    val millis = parseIsoToEpochMillis("${dayKey}T00:00:00.000Z") ?: return "·"
    val epochDay = millis / DAY_MILLIS
    // 1970-01-01 fue jueves (índice 3 con lunes = 0).
    val index = (((epochDay + 3) % 7 + 7) % 7).toInt()
    return listOf("L", "M", "M", "J", "V", "S", "D")[index]
}

/** Rango legible del reporte ("Semana · 01 oct – 07 oct"). */
internal fun attendanceRangeLabel(
    period: AttendancePeriod,
    days: List<String>,
    fallbackDate: String,
): String {
    if (days.isEmpty()) return "${period.label} · ${shortDayLabel(fallbackDate)}"
    if (days.size == 1) return "${period.label} · ${shortDayLabel(days.first())}"
    return "${period.label} · ${shortDayLabel(days.first())} – ${shortDayLabel(days.last())}"
}

// ── Etiquetas de checadas, estados del día e incidencias ───────────────────

/** Nombre en español del método con el que se identificó la persona en el reloj. */
internal fun punchMethodLabel(method: String): String = when (method) {
    "FACE" -> "Facial"
    "FINGERPRINT" -> "Huella"
    "CARD" -> "Tarjeta"
    else -> "Otro"
}

/** Tono bento del método de checada. */
internal fun punchMethodTone(method: String): BentoTone = when (method) {
    "FACE" -> BentoTone.Sea
    "FINGERPRINT" -> BentoTone.Mint
    "CARD" -> BentoTone.Lavender
    else -> BentoTone.Sun
}

/** Nombre del estado de un día del reporte por persona. */
internal fun attendanceDayLabel(status: String): String = when (status) {
    "ATTENDED" -> "Asistió"
    "LATE" -> "Retardo"
    "ABSENCE" -> "Falta"
    "REST" -> "Descanso"
    "PENDING" -> "Pendiente"
    else -> "Sin información"
}

/** Tono bento del estado de un día (mismo significado que los colores de la web). */
internal fun attendanceDayTone(status: String): BentoTone = when (status) {
    "ATTENDED" -> BentoTone.Mint
    "LATE" -> BentoTone.Sun
    "ABSENCE" -> BentoTone.Rose
    "REST" -> BentoTone.Lavender
    else -> BentoTone.Plain
}

/** Nombre de la incidencia de una sesión del reloj. */
internal fun incidentLabel(incident: String): String = when (incident) {
    "ENTRY_WITHOUT_EXIT", "OPEN_ENTRY" -> "Sin salida"
    "EXIT_WITHOUT_ENTRY" -> "Sin entrada"
    else -> incident
}

/** Minutos como "H:MM" (para celdas y CSV, sin el sufijo de horas extra). */
internal fun workedTimeLabel(minutes: Int): String {
    if (minutes <= 0) return "—"
    return "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}"
}
