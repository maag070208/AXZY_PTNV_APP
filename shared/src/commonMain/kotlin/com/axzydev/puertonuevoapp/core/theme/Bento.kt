package com.axzydev.puertonuevoapp.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Tonos pastel de los mosaicos / píldoras del diseño "bento".
 * [Plain] = superficie lisa (sin tinte); [Brand] = mosaico destacado con el
 * color primario de marca (tarjeta "hero", texto claro).
 */
enum class BentoTone { Plain, Sea, Lavender, Mint, Sun, Rose, Brand }

/** Par de colores de un tono: fondo ([container]) y texto/íconos ([content]). */
@Immutable
data class BentoToneColors(val container: Color, val content: Color)

/**
 * Tokens del diseño bento (lienzo gris claro, tarjetas de radio grande,
 * mosaicos pastel y "tinta" casi negra). Reconocen modo oscuro leyendo la
 * luminancia del esquema activo, así que funcionan con cualquier
 * `colorScheme` sin depender de `isSystemInDarkTheme()`.
 */
object AppBento {
    private val isLight: Boolean
        @Composable get() = MaterialTheme.colorScheme.background.luminance() > 0.5f

    /** Lienzo de las pantallas (gris neutro muy claro / casi negro). */
    val canvas: Color
        @Composable get() = if (isLight) Color(0xFFF4F4F6) else Color(0xFF0E1116)

    /** Relleno neutro de campos de texto (sin borde en reposo). */
    val field: Color
        @Composable get() = if (isLight) Color(0xFFEDEFF2) else Color(0xFF232831)

    /** "Tinta": píldora activa / botón oscuro. */
    val ink: Color
        @Composable get() = if (isLight) Color(0xFF14171C) else Color(0xFFF1F3F6)

    /** Contenido sobre [ink]. */
    val onInk: Color
        @Composable get() = if (isLight) Color(0xFFFFFFFF) else Color(0xFF14171C)

    /** Paleta de series para gráficas (dona, barras con leyenda), en orden de uso. */
    val series: List<Color> = listOf(
        AppColors.Info, AppColors.Success, AppColors.Warning,
        AppColors.Purple, AppColors.Danger, AppColors.Slate,
    )

    @Composable
    fun tone(tone: BentoTone): BentoToneColors {
        val light = isLight
        val scheme = MaterialTheme.colorScheme
        return when (tone) {
            BentoTone.Plain -> BentoToneColors(scheme.surface, scheme.onSurface)
            BentoTone.Brand -> BentoToneColors(scheme.primary, scheme.onPrimary)
            BentoTone.Sea ->
                if (light) BentoToneColors(Color(0xFFE0F0F7), Color(0xFF0B4A66))
                else BentoToneColors(Color(0xFF12303D), Color(0xFFA9DCF0))
            BentoTone.Lavender ->
                if (light) BentoToneColors(Color(0xFFECE7FA), Color(0xFF4A3A8C))
                else BentoToneColors(Color(0xFF2A2447), Color(0xFFCFC4F5))
            BentoTone.Mint ->
                if (light) BentoToneColors(Color(0xFFDDF3E8), Color(0xFF12643F))
                else BentoToneColors(Color(0xFF143A2A), Color(0xFF9BE0BE))
            BentoTone.Sun ->
                if (light) BentoToneColors(Color(0xFFFDF1D2), Color(0xFF7A5200))
                else BentoToneColors(Color(0xFF3F3114), Color(0xFFF3D187))
            BentoTone.Rose ->
                if (light) BentoToneColors(Color(0xFFFCE4E6), Color(0xFF9B2335))
                else BentoToneColors(Color(0xFF41191F), Color(0xFFF5B5BD))
        }
    }
}
