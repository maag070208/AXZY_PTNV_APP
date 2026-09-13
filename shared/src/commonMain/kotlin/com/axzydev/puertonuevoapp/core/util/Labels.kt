package com.axzydev.puertonuevoapp.core.util

fun roleLabel(role: String?): String = when (role) {
    "ADMIN" -> "Administrador"
    "GERENTE" -> "Gerente"
    "JEFE_DE_AREA" -> "Jefe de área"
    "EMPLEADO" -> "Empleado"
    else -> role ?: "—"
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
