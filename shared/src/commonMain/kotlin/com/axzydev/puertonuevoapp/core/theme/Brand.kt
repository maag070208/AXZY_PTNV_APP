package com.axzydev.puertonuevoapp.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta institucional Puerto Nuevo — tokens extraídos del logo oficial
 * (`composeResources/drawable/logo_puerto_nuevo.png`): azul océano
 * dominante, hielo claro, brick cálido y neutros azulados.
 *
 * Regla: aquí viven SOLO tokens de marca. Estados (éxito/alerta/error) van
 * en [AppColors]. Las pantallas usan `MaterialTheme.colorScheme.*` y
 * [AppColors] únicamente para semánticos.
 */
object Brand {
    // Azul océano — dominante del logo (3,88,125)
    val Primary = Color(0xFF03587D)
    val PrimaryDark = Color(0xFF024A69)
    val OnPrimary = Color(0xFFFFFFFF)

    // Hielo claro — contenedores (216,235,245)
    val PrimaryContainer = Color(0xFFD8EBF5)
    val OnPrimaryContainer = Color(0xFF013449)
    val IceMid = Color(0xFF79A6B7)
    val IceSoft = Color(0xFFB0CBD2)

    // Brick cálido — acento secundario del logo (100,66,62)
    val Accent = Color(0xFF6B413F)
    val OnAccent = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFF6EDEC)
    val OnSecondaryContainer = Color(0xFF5A322F)

    // Neutros
    val Background = Color(0xFFF4F9FB)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFE8F0F4)
    val Outline = Color(0xFFC9D8E0)

    val TextPrimary = Color(0xFF0E2231)
    val TextMuted = Color(0xFF5A6B78)
    val TextFaint = Color(0xFF8A9AA7)
}