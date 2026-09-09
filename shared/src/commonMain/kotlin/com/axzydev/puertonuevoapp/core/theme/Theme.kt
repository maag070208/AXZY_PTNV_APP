package com.axzydev.puertonuevoapp.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Tema único, claro — "moderno minimalista". El modo oscuro puede agregarse
// más adelante; por ahora se mantiene un solo look consistente en toda la app.
private val LightColors = lightColorScheme(
    primary = AppColors.EmeraldPrimary,
    onPrimary = AppColors.Surface,
    primaryContainer = AppColors.EmeraldContainer,
    onPrimaryContainer = AppColors.EmeraldOnContainer,
    secondary = AppColors.TextMuted,
    onSecondary = AppColors.Surface,
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.SurfaceVariant,
    onSurfaceVariant = AppColors.TextMuted,
    outline = AppColors.Outline,
    error = AppColors.Danger,
    onError = AppColors.Surface,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 17.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.4.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 9.5.sp, letterSpacing = 0.5.sp),
)

@Composable
fun PuertoNuevoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
