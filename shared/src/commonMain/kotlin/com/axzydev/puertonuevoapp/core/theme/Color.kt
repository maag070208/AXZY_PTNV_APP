package com.axzydev.puertonuevoapp.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Capa de compatibilidad + semánticos.
 *
 * Los miembros *Emerald* son alias hacia el primario del tema (marca) para no
 * tocar el resto de pantallas: el valor vive en UN punto (el `ColorScheme`) y
 * cambia toda la app junto. Regla hacia adelante: las pantallas usan
 * `MaterialTheme.colorScheme.*` y aquí solo estados/helpers de dominio.
 *
 * **Modo oscuro**: los colores de superficie/texto/brand son *getters
 * theme-aware* (leen [ThemeBridge], el esquema activo) en lugar de constantes.
 * Así el mismo nombre (`AppColors.TextPrimary`, `AppColors.Surface`, …) cambia
 * solo con el tema, dentro o fuera de un `@Composable` — no hay call sites que
 * migrar. Los semánticos (éxito/alerta/error/info) se quedan fijos: el error
 * es el mismo rojo en ambos temas y el resto de estados conserva su
 * significado.
 */
object AppColors {
    private val scheme get() = ThemeBridge.current
    private val dark get() = ThemeBridge.isDark

    // Compat → primario del esquema (marca)
    val EmeraldPrimary: Color get() = scheme.primary

    val EmeraldPrimaryDark: Color
        get() = if (dark) lerp(scheme.primary, Color.Black, 0.10f) else BrandPalette.PrimaryDark

    val EmeraldContainer: Color get() = scheme.primaryContainer
    val EmeraldOnContainer: Color get() = scheme.onPrimaryContainer

    val Background: Color get() = scheme.background
    val Surface: Color get() = scheme.surface
    val SurfaceVariant: Color get() = scheme.surfaceVariant
    val Outline: Color get() = scheme.outline

    val TextPrimary: Color get() = scheme.onSurface
    val TextMuted: Color get() = scheme.onSurfaceVariant
    /** Gris tenue: en oscuro se aclara para conservar contraste AA sobre los fondos. */
    val TextFaint: Color get() = if (dark) Color(0xFF9BA9B6) else BrandPalette.TextFaint

    /**
     * Fondo del QR de la credencial: SIEMPRE blanco (el lector necesita contraste
     * y la zona libre alrededor); no sigue el tema oscuro.
     */
    val QrSurface = Color(0xFFFFFFFF)

    // Semánticos — estado, no marca. Fijos en ambos temas: el error es el mismo
    // rojo (contraste suficiente sobre claro y oscuro) y el resto de estados
    // mantiene su significado (verde = bien, ámbar = atención).
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
