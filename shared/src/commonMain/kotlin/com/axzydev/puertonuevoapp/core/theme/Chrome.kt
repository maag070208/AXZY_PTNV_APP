package com.axzydev.puertonuevoapp.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * Valores exactos del chrome compartido del diseño "C" (header y barra inferior flotante).
 * Medidas en dp tal como están en los tableros 390x844.
 */
object AppChrome {
    /** Lado del botón cuadrado del header (atrás, campana, acción, avatar). */
    val ButtonSize = 44.dp

    /** Padding horizontal del header. */
    val HeaderHorizontal = 20.dp

    /** Separación entre elementos del header. */
    val HeaderGap = 12.dp

    /** Diámetro del punto de notificación y su borde. */
    val DotSize = 8.dp
    val DotBorder = 2.dp

    /** Márgenes laterales / inferior y alto de la barra inferior flotante. */
    val BarSideMargin = 12.dp
    val BarBottomMargin = 14.dp
    val BarHeight = 72.dp
    val BarPaddingHorizontal = 8.dp

    /** Píldora del ícono activo (48x30) y tamaño del ícono. */
    val NavPillWidth = 48.dp
    val NavPillHeight = 30.dp
    val NavIconSize = 20.dp

    /** Color del punto de notificación (#B5523B). */
    val Dot = Color(0xFFB5523B)

    /** Tramos de la barra segmentada del hero (pendientes / en proceso / listas). */
    val SegmentPending = Color(0xFFE7917F)
    val SegmentInProgress = Color(0xFFE9C46A)
    val SegmentDone = Color(0xFF9FD3AE)

    /** Fondo de la píldora del destino activo: mismo tinte que [BentoTone.Sea] (claro) o primario translúcido (oscuro). */
    val navPill: Color
        @Composable get() =
            if (MaterialTheme.colorScheme.background.luminance() > 0.5f) AppBento.tone(BentoTone.Sea).container
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
}
