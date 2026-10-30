package com.axzydev.puertonuevoapp.core.util

/** Desfase (ms) de la zona horaria del dispositivo respecto a UTC en [atMillis]. */
expect fun localOffsetMillis(atMillis: Long): Long

private const val DAY_MS = 86_400_000L

private val MONTHS_ES = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)
private val WEEKDAYS_ES = listOf("domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado")

/** Día local (días desde 1970-01-01 en la zona del dispositivo) de un instante. */
private fun localEpochDay(millis: Long): Long = floorDiv(millis + localOffsetMillis(millis), DAY_MS)

private fun floorDiv(a: Long, b: Long): Long = if (a >= 0) a / b else -((-a + b - 1) / b)

/** (año, mes 1..12, día) de un día-época; calendario civil de Howard Hinnant. */
private fun civilFromEpochDay(epochDay: Long): Triple<Int, Int, Int> {
    val z = epochDay + 719_468L
    val era = (if (z >= 0) z else z - 146_096L) / 146_097L
    val doe = z - era * 146_097L
    val yoe = (doe - doe / 1_460L + doe / 36_524L - doe / 146_096L) / 365L
    val y = yoe + era * 400L
    val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
    val mp = (5L * doy + 2L) / 153L
    val d = (doy - (153L * mp + 2L) / 5L + 1L).toInt()
    val m = (if (mp < 10L) mp + 3L else mp - 9L).toInt()
    val year = (if (m <= 2) y + 1L else y).toInt()
    return Triple(year, m, d)
}

/** "Jueves 29 de octubre" para hoy (zona del dispositivo). */
fun longDateEs(millis: Long = currentTimeMillis()): String {
    val day = localEpochDay(millis)
    val (_, month, dom) = civilFromEpochDay(day)
    // 1970-01-01 fue jueves (índice 4 con domingo = 0).
    val weekday = WEEKDAYS_ES[(((day + 4) % 7 + 7) % 7).toInt()]
    return "${weekday.replaceFirstChar { it.uppercase() }} $dom de ${MONTHS_ES[month - 1]}"
}

/** "Jue 29 oct" para hoy (zona del dispositivo): fecha corta para encabezados de una línea. */
fun shortDateEs(millis: Long = currentTimeMillis()): String {
    val day = localEpochDay(millis)
    val (_, month, dom) = civilFromEpochDay(day)
    val weekday = WEEKDAYS_ES[(((day + 4) % 7 + 7) % 7).toInt()]
    return "${weekday.take(3).replaceFirstChar { it.uppercase() }} $dom ${MONTHS_ES[month - 1].take(3)}"
}

/** Hora local "HH:mm" de un ISO-8601 UTC del backend. */
fun formatLocalTime(iso: String?): String {
    val millis = iso?.let(::parseIsoToEpochMillis) ?: return "—"
    val local = millis + localOffsetMillis(millis)
    val minutesOfDay = (floorDiv(local, 60_000L) % 1_440L + 1_440L) % 1_440L
    return "${(minutesOfDay / 60).toString().padStart(2, '0')}:${(minutesOfDay % 60).toString().padStart(2, '0')}"
}

/** Clave del día local de un ISO (para agrupar). `null` si no se puede leer. */
fun localDayKey(iso: String): Long? = parseIsoToEpochMillis(iso)?.let(::localEpochDay)

/** "Hoy", "Ayer" o "29 oct" según el día local del ISO. */
fun relativeDayLabel(iso: String, nowMillis: Long = currentTimeMillis()): String {
    val day = localDayKey(iso) ?: return formatShortDate(iso)
    val today = localEpochDay(nowMillis)
    return when (day) {
        today -> "Hoy"
        today - 1 -> "Ayer"
        else -> {
            val (_, month, dom) = civilFromEpochDay(day)
            "$dom ${MONTHS_ES[month - 1].take(3)}"
        }
    }
}

/** Minutos como "H:MM h" (horas extra). */
fun formatMinutesAsHours(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "$h:${m.toString().padStart(2, '0')} h"
}

/** Mes (1..12) y día del mes de un instante, en la zona del dispositivo. */
fun localMonthDay(millis: Long = currentTimeMillis()): Pair<Int, Int> {
    val (_, month, day) = civilFromEpochDay(localEpochDay(millis))
    return month to day
}

/** Hora local actual (0..23) del dispositivo. */
fun localHourNow(): Int {
    val now = currentTimeMillis()
    val local = now + localOffsetMillis(now)
    return ((floorDiv(local, 3_600_000L) % 24L + 24L) % 24L).toInt()
}

/** Saludo según la hora local: Buenos días / Buenas tardes / Buenas noches. */
fun greetingForNow(): String = when (localHourNow()) {
    in 5..11 -> "Buenos días"
    in 12..18 -> "Buenas tardes"
    else -> "Buenas noches"
}

/** Índice del día de la semana local (0 = lunes ... 6 = domingo) de un ISO. */
fun localWeekdayIndex(iso: String): Int? {
    val day = localDayKey(iso) ?: return null
    // 1970-01-01 fue jueves (3 con lunes = 0).
    return (((day + 3) % 7 + 7) % 7).toInt()
}

/** Días locales transcurridos desde [iso] hasta hoy (0 = hoy). `null` si no se lee. */
fun daysAgoLocal(iso: String, nowMillis: Long = currentTimeMillis()): Int? {
    val day = localDayKey(iso) ?: return null
    return (localEpochDay(nowMillis) - day).toInt()
}
