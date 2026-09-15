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
