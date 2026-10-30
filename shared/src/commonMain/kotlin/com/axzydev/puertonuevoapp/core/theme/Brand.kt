package com.axzydev.puertonuevoapp.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Paleta institucional Puerto Nuevo — tokens extraídos del logo oficial
 * (`composeResources/drawable/logo_puerto_nuevo.png`): azul océano
 * dominante, hielo claro, brick cálido y neutros azulados.
 *
 * Regla: aquí viven SOLO tokens de marca. Estados (éxito/alerta/error) van
 * en [AppColors]. Las pantallas usan `MaterialTheme.colorScheme.*` y
 * [AppColors] únicamente para semánticos.
 *
 * **Modo oscuro**: los miembros de [Brand] son *getters theme-aware*, no
 * constantes. Leen el esquema vigente de [ThemeBridge] y devuelven el valor
 * del tema claro o del oscuro; así los call sites no cambian (ni siquiera los
 * no `@Composable`, que leen el valor del tema activo). Las constantes hex de
 * la marca viven en [BrandPalette], fuente de verdad de los `ColorScheme`.
 */
internal object BrandPalette {
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

object Brand {
    private val scheme get() = ThemeBridge.current
    private val dark get() = ThemeBridge.isDark

    // Azul océano — primario del esquema (en oscuro, más luminoso para AA).
    val Primary: Color get() = scheme.primary

    /**
     * Azul profundo: en claro es el del logo; en oscuro se aclara para que el
     * [com.axzydev.puertonuevoapp.core.ui.OceanBackdrop] (degradado de marca)
     * siga legible con el texto `onPrimary` oscuro del tema oscuro.
     */
    val PrimaryDark: Color
        get() = if (dark) lerp(scheme.primary, Color.White, 0.35f) else BrandPalette.PrimaryDark

    val OnPrimary: Color get() = scheme.onPrimary

    val PrimaryContainer: Color get() = scheme.primaryContainer
    val OnPrimaryContainer: Color get() = scheme.onPrimaryContainer

    // Hielo: en oscuro se invierten a tintes profundos para que las olas del
    // OceanBackdrop se vean sobre su degradado claro.
    val IceMid: Color get() = if (dark) Color(0xFF145A73) else BrandPalette.IceMid
    val IceSoft: Color get() = if (dark) Color(0xFF0B3A4D) else BrandPalette.IceSoft

    // Brick cálido
    val Accent: Color get() = scheme.secondary
    val OnAccent: Color get() = scheme.onSecondary
    val SecondaryContainer: Color get() = scheme.secondaryContainer
    val OnSecondaryContainer: Color get() = scheme.onSecondaryContainer

    // Neutros
    val Background: Color get() = scheme.background

    /**
     * Blanco de la insignia del logo ([com.axzydev.puertonuevoapp.core.ui.BrandLogoBadge]):
     * se mantiene blanco en ambos temas porque el logo necesita su recuadro claro.
     */
    val Surface: Color get() = BrandPalette.Surface
    val SurfaceVariant: Color get() = scheme.surfaceVariant
    val Outline: Color get() = scheme.outline

    val TextPrimary: Color get() = scheme.onSurface
    val TextMuted: Color get() = scheme.onSurfaceVariant
    /** Gris tenue: en oscuro se aclara para conservar contraste AA sobre los fondos. */
    val TextFaint: Color get() = if (dark) Color(0xFF9BA9B6) else BrandPalette.TextFaint
}
