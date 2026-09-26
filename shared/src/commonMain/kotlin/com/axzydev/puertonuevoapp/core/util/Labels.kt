package com.axzydev.puertonuevoapp.core.util

fun roleLabel(role: String?): String = when (role) {
    "ADMIN" -> "Administrador"
    "MANAGER" -> "Gerente"
    "AREA_HEAD" -> "Jefe de área"
    "EMPLOYEE" -> "Empleado"
    "HUMAN_RESOURCES" -> "Recursos Humanos"
    "GUARD" -> "Guardia"
    else -> role ?: "—"
}

/** Iniciales (2) del nombre para avatares. */
fun initials(name: String): String =
    name.trim().split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase()

/** Etiqueta legible del tipo de descuento de un empleado. */
fun discountTypeLabel(type: String): String = when (type) {
    "INFONAVIT" -> "Infonavit"
    "IMSS" -> "IMSS"
    "CHILD_SUPPORT" -> "Deudor alimenticio"
    else -> type
}

/** Tamaño de archivo en KB/MB (el backend manda bytes). */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${kb.roundToMaxOne().format()} KB"
    val mb = kb / 1024.0
    return "${mb.roundToMaxOne().format()} MB"
}

private fun Double.roundToMaxOne(): Double = (this * 10).toInt() / 10.0
private fun Double.format(): String = if (this % 1.0 == 0.0) this.toInt().toString() else this.toString()

/** Tipo de evento de control de acceso (ENTRY/EXIT). */
fun accessEventTypeLabel(type: String?): String = when (type) {
    "ENTRY" -> "Entrada"
    "EXIT" -> "Salida"
    else -> type ?: "—"
}

/** Cómo se obtuvo el "dónde" de un evento de acceso. */
fun accessLocationSourceLabel(source: String?): String = when (source) {
    "GPS" -> "GPS"
    "SITE_ONLY" -> "Solo sitio"
    "MANUAL" -> "Manual"
    else -> source ?: "—"
}

/** Medio por el que se registró el evento de acceso. */
fun accessMethodLabel(method: String?): String = when (method) {
    "QR_SCAN" -> "Escaneo QR"
    "MANUAL" -> "Manual"
    else -> method ?: "—"
}

fun ticketStatusLabel(status: String): String = when (status) {
    "OPEN" -> "Abierto"
    "IN_PROGRESS" -> "En seguimiento"
    "CLOSED" -> "Cerrado"
    else -> status
}

fun ticketPriorityLabel(priority: String): String = when (priority) {
    "LOW" -> "Baja"
    "MEDIUM" -> "Media"
    "HIGH" -> "Alta"
    "URGENT" -> "Urgente"
    else -> priority
}

fun assignmentStatusLabel(status: String): String = when (status) {
    "PENDING" -> "Pendiente"
    "IN_PROGRESS" -> "En progreso"
    "IN_REVIEW" -> "En revisión"
    "COMPLETED" -> "Completada"
    else -> status
}

fun deviceStatusLabel(status: String): String = when (status) {
    "AVAILABLE" -> "Disponible"
    "ASSIGNED" -> "Asignado"
    "DAMAGED" -> "Dañado"
    "IN_MAINTENANCE" -> "Mantenimiento"
    "RETIRED" -> "Baja"
    else -> status
}

fun movementTypeLabel(type: String): String = when (type) {
    "STOCK_IN" -> "Entrada"
    "STOCK_OUT" -> "Salida"
    "TRANSFER" -> "Traslado"
    "RETIREMENT" -> "Baja"
    "LOAN" -> "Asignado"
    "RETURN" -> "Devolución"
    else -> type
}

fun conditionLabel(condition: String): String = when (condition) {
    "GOOD" -> "Bueno"
    "FAIR" -> "Aceptable"
    "POOR" -> "Malo"
    "BROKEN" -> "Roto"
    else -> condition
}

fun formatLocation(name: String?, subLocation: String?, number: String?): String {
    val parts = listOfNotNull(name, subLocation, number).filter { it.isNotBlank() }
    return if (parts.isNotEmpty()) parts.joinToString("-") else "Sin ubicación"
}
