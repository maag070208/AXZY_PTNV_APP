package com.axzydev.puertonuevoapp.core.theme

import androidx.compose.ui.graphics.Color

// Paleta institucional Puerto Nuevo — misma familia "emerald" que ya usa
// la web (text-emerald-700 / bg-emerald-600), llevada a un tema Material3
// claro y minimalista.
object AppColors {
    // Uber-inspired foundation: black actions, white surfaces, green signal.
    val EmeraldPrimary = Color(0xFF000000)
    val EmeraldPrimaryDark = Color(0xFF171717)
    val EmeraldContainer = Color(0xFFE8E8E8)
    val EmeraldOnContainer = Color(0xFF000000)

    val Background = Color(0xFFF6F6F6)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFEEEEEE)
    val Outline = Color(0xFFE0E0E0)

    val TextPrimary = Color(0xFF000000)
    val TextMuted = Color(0xFF545454)
    val TextFaint = Color(0xFF858585)

    val Success = Color(0xFF06C167)
    val Warning = Color(0xFFF59E0B)
    val Danger = Color(0xFFEF4444)
    val Info = Color(0xFF3B82F6)
    val Purple = Color(0xFF8B5CF6)

    // Estados de ticket
    fun ticketStatusColor(status: String): Color = when (status) {
        "ABIERTO" -> Warning
        "EN_SEGUIMIENTO" -> Info
        "CERRADO" -> Success
        else -> TextFaint
    }

    fun ticketPriorityColor(priority: String): Color = when (priority) {
        "BAJA" -> TextFaint
        "MEDIA" -> Warning
        "ALTA" -> Color(0xFFEF4444)
        "URGENTE" -> Danger
        else -> TextFaint
    }

    // Estados de dispositivo
    fun deviceEstadoColor(estado: String): Color = when (estado) {
        "DISPONIBLE" -> Success
        "ASIGNADO" -> Warning
        "BAJA" -> TextFaint
        else -> TextFaint
    }

    fun movementTypeColor(tipo: String): Color = when (tipo) {
        "ENTRADA" -> Success
        "SALIDA" -> Warning
        "TRASLADO" -> Info
        "BAJA" -> Danger
        "PRESTAMO" -> Purple
        "DEVOLUCION" -> Info
        else -> TextFaint
    }

    fun condicionColor(condicion: String): Color = when (condicion) {
        "BUENO" -> Success
        "ACEPTABLE" -> Warning
        "MALO" -> Danger
        "ROTO" -> Danger
        else -> TextFaint
    }
}
