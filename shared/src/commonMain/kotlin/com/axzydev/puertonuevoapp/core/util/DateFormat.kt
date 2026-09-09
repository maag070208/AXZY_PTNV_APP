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
