package com.axzydev.puertonuevoapp.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Capa de compatibilidad + semánticos.
 *
 * Los miembros *Emerald* son alias hacia la paleta del logo (Brand) para no
 * tocar el resto de pantallas: el valor vive en UN punto (Brand) y cambia
 * toda la app junto. Regla hacia adelante: las pantallas usan
 * `MaterialTheme.colorScheme.*` y aquí solo estados/helpers de dominio.
 */
object AppColors {
    // Compat → paleta del logo (Brand)
    val EmeraldPrimary = Brand.Primary
    val EmeraldPrimaryDark = Brand.PrimaryDark
    val EmeraldContainer = Brand.PrimaryContainer
    val EmeraldOnContainer = Brand.OnPrimaryContainer

    val Background = Brand.Background
    val Surface = Brand.Surface
    val SurfaceVariant = Brand.SurfaceVariant
    val Outline = Brand.Outline

    val TextPrimary = Brand.TextPrimary
    val TextMuted = Brand.TextMuted
    val TextFaint = Brand.TextFaint

    // Semánticos — estado, no marca
    val Success = Color(0xFF06C167)
    val Warning = Color(0xFFF59E0B)
    val Danger = Color(0xFFEF4444)
    val Info = Color(0xFF3B82F6)
    val Purple = Color(0xFF8B5CF6)
    val Slate = Color(0xFF64748B)

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

    /** Acento por rol para avatares/badges de personal (tonos mudos, sin marca). */
    fun roleAccent(role: String): Color = when (role) {
        "GERENTE", "ADMIN" -> EmeraldPrimary
        "JEFE_DE_AREA" -> Info
        "RECURSOS_HUMANOS" -> Purple
        else -> Slate
    }
}