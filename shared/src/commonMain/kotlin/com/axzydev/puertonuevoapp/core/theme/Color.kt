package com.axzydev.puertonuevoapp.core.theme

import androidx.compose.ui.graphics.Color

// Paleta institucional Puerto Nuevo — misma familia "emerald" que ya usa
// la web (text-emerald-700 / bg-emerald-600), llevada a un tema Material3
// claro y minimalista.
object AppColors {
    val EmeraldPrimary = Color(0xFF059669)
    val EmeraldPrimaryDark = Color(0xFF047857)
    val EmeraldContainer = Color(0xFFD1FAE5)
    val EmeraldOnContainer = Color(0xFF065F46)

    val Background = Color(0xFFF8FAFC)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF1F5F9)
    val Outline = Color(0xFFE2E8F0)

    val TextPrimary = Color(0xFF1E293B)
    val TextMuted = Color(0xFF64748B)
    val TextFaint = Color(0xFF94A3B8)

    val Success = Color(0xFF10B981)
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
}
