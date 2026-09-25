package com.axzydev.puertonuevoapp.core.util

fun roleLabel(role: String?): String = when (role) {
    "ADMIN" -> "Administrador"
    "GERENTE" -> "Gerente"
    "JEFE_DE_AREA" -> "Jefe de área"
    "EMPLEADO" -> "Empleado"
    "RECURSOS_HUMANOS" -> "Recursos Humanos"
    "GUARD" -> "Guardia"
    else -> role ?: "—"
}

/** Iniciales (2) del nombre para avatares. */
fun initials(name: String): String =
    name.trim().split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase()

/** Etiqueta legible del tipo de descuento de un empleado. */
fun discountTypeLabel(tipo: String): String = when (tipo) {
    "INFONAVIT" -> "Infonavit"
    "IMSS" -> "IMSS"
    "DEUDOR_ALIMENTICIO" -> "Deudor alimenticio"
    else -> tipo
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
    "ABIERTO" -> "Abierto"
    "EN_SEGUIMIENTO" -> "En seguimiento"
    "CERRADO" -> "Cerrado"
    else -> status
}

fun ticketPriorityLabel(priority: String): String = when (priority) {
    "BAJA" -> "Baja"
    "MEDIA" -> "Media"
    "ALTA" -> "Alta"
    "URGENTE" -> "Urgente"
    else -> priority
}

fun assignmentStatusLabel(status: String): String = when (status) {
    "PENDIENTE" -> "Pendiente"
    "EN_PROGRESO" -> "En progreso"
    "EN_REVISION" -> "En revisión"
    "COMPLETADA" -> "Completada"
    else -> status
}

fun deviceEstadoLabel(estado: String): String = when (estado) {
    "DISPONIBLE" -> "Disponible"
    "ASIGNADO" -> "Asignado"
    "BAJA" -> "Baja"
    else -> estado
}

fun movementTypeLabel(tipo: String): String = when (tipo) {
    "ENTRADA" -> "Entrada"
    "SALIDA" -> "Salida"
    "TRASLADO" -> "Traslado"
    "BAJA" -> "Baja"
    "PRESTAMO" -> "Asignado"
    "DEVOLUCION" -> "Devolución"
    else -> tipo
}

fun condicionLabel(condicion: String): String = when (condicion) {
    "BUENO" -> "Bueno"
    "ACEPTABLE" -> "Aceptable"
    "MALO" -> "Malo"
    "ROTO" -> "Roto"
    else -> condicion
}

fun formatLocation(lugar: String?, subLugar: String?, numero: String?): String {
    val parts = listOfNotNull(lugar, subLugar, numero).filter { it.isNotBlank() }
    return if (parts.isNotEmpty()) parts.joinToString("-") else "Sin ubicación"
}
