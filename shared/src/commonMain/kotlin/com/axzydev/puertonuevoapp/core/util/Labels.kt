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
