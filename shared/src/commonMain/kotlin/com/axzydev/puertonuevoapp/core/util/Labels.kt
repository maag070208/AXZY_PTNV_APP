package com.axzydev.puertonuevoapp.core.util

fun roleLabel(role: String?): String = when (role) {
    "ADMIN" -> "Administrador"
    "GERENTE" -> "Gerente"
    "JEFE_DE_AREA" -> "Jefe de área"
    "EMPLEADO" -> "Empleado"
    "GUARD" -> "Guardia"
    else -> role ?: "—"
}

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

fun ticketCategoryLabel(category: String): String = when (category) {
    "MANTENIMIENTO" -> "Mantenimiento"
    "EQUIPO" -> "Equipo"
    "SISTEMA" -> "Sistema"
    "OTRO" -> "Otro"
    else -> category
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
