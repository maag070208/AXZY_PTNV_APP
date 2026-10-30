package com.axzydev.puertonuevoapp.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Esquema claro: **idéntico** al de siempre (los hex salen de [BrandPalette]),
 * para que el modo claro no cambie ni un píxel.
 */
private val LightColors = lightColorScheme(
    primary = BrandPalette.Primary,
    onPrimary = BrandPalette.OnPrimary,
    primaryContainer = BrandPalette.PrimaryContainer,
    onPrimaryContainer = BrandPalette.OnPrimaryContainer,
    secondary = BrandPalette.Accent,
    onSecondary = BrandPalette.OnAccent,
    secondaryContainer = BrandPalette.SecondaryContainer,
    onSecondaryContainer = BrandPalette.OnSecondaryContainer,
    background = BrandPalette.Background,
    onBackground = BrandPalette.TextPrimary,
    surface = BrandPalette.Surface,
    onSurface = BrandPalette.TextPrimary,
    surfaceVariant = BrandPalette.SurfaceVariant,
    onSurfaceVariant = BrandPalette.TextMuted,
    outline = BrandPalette.Outline,
    error = AppColors.Danger,
    onError = BrandPalette.OnPrimary,
)

/**
 * Esquema oscuro derivado de la marca: fondos casi negros azulados
 * (`#0E1116` fondo, `#161B22` superficie, `#21262D` variante), textos claros y
 * un primario más luminoso (`#4FA3C7`) para conservar contraste AA sobre los
 * fondos oscuros. El error es el mismo rojo del tema claro.
 */
private val DarkColors = darkColorScheme(
    primary = Color(0xFF4FA3C7),
    onPrimary = Color(0xFF06121A),
    primaryContainer = Color(0xFF12303D),
    onPrimaryContainer = Color(0xFFC9E8F4),
    secondary = Color(0xFFC98F8A),
    onSecondary = Color(0xFF241413),
    secondaryContainer = Color(0xFF3A2523),
    onSecondaryContainer = Color(0xFFF0D9D6),
    background = Color(0xFF0E1116),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF21262D),
    onSurfaceVariant = Color(0xFFA9B4BE),
    outline = Color(0xFF3D444D),
    error = AppColors.Danger,
    onError = Color(0xFFFFFFFF),
)

/**
 * Esquema activo para los **tokens de compatibilidad** ([AppColors], [Brand]),
 * que se leen como getters normales desde cualquier sitio (incluidos helpers no
 * `@Composable`). [PuertoNuevoTheme] lo actualiza tras cada composición del
 * tema; al ser estado de Compose, quien lea un token se suscribe y se
 * recompone solo cuando cambia el tema.
 */
internal object ThemeBridge {
    var current: ColorScheme by mutableStateOf(LightColors)
        internal set

    /** ¿El esquema vigente es oscuro? (se decide por la luminancia del fondo). */
    val isDark: Boolean get() = current.background.luminance() < 0.5f
}

/** Modo oscuro activo, para componentes que necesiten ramificar su dibujo. */
val LocalDarkTheme = staticCompositionLocalOf { false }

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.2.sp),
)

/**
 * Tema de la app. Sigue al sistema por defecto (`isSystemInDarkTheme()`); se
 * puede forzar con [darkTheme] (p. ej. desde una preferencia o un preview).
 */
@Composable
fun PuertoNuevoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    // Puentea el esquema a los tokens de compatibilidad (AppColors/Brand) que no
    // pueden leer CompositionLocal desde un getter normal. `SideEffect` evita
    // escribir estado durante la composición: el primer frame usa el esquema
    // anterior y el siguiente ya el correcto (imperceptible: el árbol se
    // recompone con el cambio de state).
    SideEffect { ThemeBridge.current = colors }
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colors,
            shapes = AppShapes,
            typography = AppTypography,
            content = content,
        )
    }
}
