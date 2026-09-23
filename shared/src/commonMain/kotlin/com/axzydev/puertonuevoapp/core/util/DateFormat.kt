package com.axzydev.puertonuevoapp.core.util

/**
 * Formateo de fechas sin librería de fecha/hora: el backend siempre manda
 * ISO-8601 UTC (ej. "2026-08-20T10:00:00.000Z"), así que basta con
 * recortar el string — evita sumar kotlinx-datetime como dependencia.
 */
fun formatShortDate(iso: String?): String {
    if (iso == null || iso.length < 10) return "—"
    val datePart = iso.take(10)
    val parts = datePart.split("-")
    if (parts.size != 3) return datePart
    return "${parts[2]}/${parts[1]}/${parts[0]}"
}

fun formatDateTime(iso: String?): String {
    if (iso == null || iso.length < 16) return formatShortDate(iso)
    val date = formatShortDate(iso)
    val time = iso.substring(11, 16)
    return "$date $time"
}

/**
 * Convierte un timestamp ISO-8601 UTC del backend (ej. "2026-08-20T10:00:00.000Z")
 * a epoch millis, sin sumar kotlinx-datetime como dependencia. Sólo soporta el
 * formato fijo que manda Prisma/Postgres (fecha-hora en UTC con sufijo Z).
 */
fun parseIsoToEpochMillis(iso: String): Long? {
    if (iso.length < 19) return null
    return try {
        val year = iso.substring(0, 4).toInt()
        val month = iso.substring(5, 7).toInt()
        val day = iso.substring(8, 10).toInt()
        val hour = iso.substring(11, 13).toInt()
        val minute = iso.substring(14, 16).toInt()
        val second = iso.substring(17, 19).toInt()
        val millis = if (iso.length >= 23 && iso[19] == '.') iso.substring(20, 23).toIntOrNull() ?: 0 else 0

        // Días desde 1970-01-01 (algoritmo del calendario civil de Howard Hinnant).
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        val jdn = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
        val epochDay = jdn - 2440588L

        epochDay * 86_400_000L + hour * 3_600_000L + minute * 60_000L + second * 1_000L + millis
    } catch (e: Exception) {
        null
    }
}

/** Replica `new Date(dueDate) < new Date()` del web, sin librería de fechas. */
fun isOverdue(dueDateIso: String?, completed: Boolean): Boolean {
    if (completed || dueDateIso == null) return false
    val due = parseIsoToEpochMillis(dueDateIso) ?: return false
    return due < currentTimeMillis()
}

/**
 * Inverso de [parseIsoToEpochMillis]: epoch millis → ISO-8601 UTC con sufijo
 * `Z` (ej. "2026-08-20T10:00:00.000Z"), el formato que acepta `new Date(...)`
 * en el backend. Se usa para el `deviceTimestamp` (auditoría) del evento.
 */
fun formatIsoUtc(millis: Long): String {
    val epochDay = if (millis >= 0) millis / 86_400_000L else (millis - 86_399_999L) / 86_400_000L
    val msOfDay = millis - epochDay * 86_400_000L

    // civil_from_days (Howard Hinnant), inverso del usado en parseIsoToEpochMillis.
    val z = epochDay + 719_468L
    val era = (if (z >= 0) z else z - 146_096L) / 146_097L
    val doe = z - era * 146_097L
    val yoe = (doe - doe / 1_460L + doe / 36_524L - doe / 146_096L) / 365L
    val y = yoe + era * 400L
    val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
    val mp = (5L * doy + 2L) / 153L
    val d = doy - (153L * mp + 2L) / 5L + 1L
    val m = if (mp < 10L) mp + 3L else mp - 9L
    val year = if (m <= 2L) y + 1L else y

    val hour = (msOfDay / 3_600_000L).toInt()
    val minute = ((msOfDay % 3_600_000L) / 60_000L).toInt()
    val second = ((msOfDay % 60_000L) / 1_000L).toInt()
    val milli = (msOfDay % 1_000L).toInt()

    fun pad(value: Long, width: Int) = value.toString().padStart(width, '0')
    return "${pad(year, 4)}-${pad(m, 2)}-${pad(d, 2)}T${pad(hour.toLong(), 2)}:${pad(minute.toLong(), 2)}:${pad(second.toLong(), 2)}.${pad(milli.toLong(), 3)}Z"
}
