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
        "OPEN" -> Warning
        "IN_PROGRESS" -> Info
        "CLOSED" -> Success
        else -> TextFaint
    }

    fun ticketPriorityColor(priority: String): Color = when (priority) {
        "LOW" -> TextFaint
        "MEDIUM" -> Warning
        "HIGH" -> Color(0xFFEF4444)
        "URGENT" -> Danger
        else -> TextFaint
    }

    // Estados de dispositivo
    fun deviceStatusColor(status: String): Color = when (status) {
        "AVAILABLE" -> Success
        "ASSIGNED" -> Warning
        "DAMAGED" -> Danger
        "IN_MAINTENANCE" -> Info
        "RETIRED" -> TextFaint
        else -> TextFaint
    }

    fun movementTypeColor(type: String): Color = when (type) {
        "STOCK_IN" -> Success
        "STOCK_OUT" -> Warning
        "TRANSFER" -> Info
        "RETIREMENT" -> Danger
        "LOAN" -> Purple
        "RETURN" -> Info
        else -> TextFaint
    }

    fun conditionColor(condition: String): Color = when (condition) {
        "GOOD" -> Success
        "FAIR" -> Warning
        "POOR" -> Danger
        "BROKEN" -> Danger
        else -> TextFaint
    }

    /** Acento por rol para avatares/badges de personal (tonos mudos, sin marca). */
    fun roleAccent(role: String): Color = when (role) {
        "MANAGER", "ADMIN" -> EmeraldPrimary
        "AREA_HEAD" -> Info
        "HUMAN_RESOURCES" -> Purple
        else -> Slate
    }
}