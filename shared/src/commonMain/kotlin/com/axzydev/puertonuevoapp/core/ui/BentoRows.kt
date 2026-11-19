package com.axzydev.puertonuevoapp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone

/** Cuadro de ícono tintado con los colores pastel de un [BentoTone] (filas, accesos, listas). */
@Composable
fun BentoIconSquare(
    icon: ImageVector,
    tone: BentoTone,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = AppShape.icon,
) {
    val colors = AppBento.tone(tone)
    Box(
        modifier = modifier.size(size).clip(shape).background(colors.container, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = colors.content, modifier = Modifier.size(size * 0.45f))
    }
}

/**
 * Fila de lista bento: cuadro de ícono tintado + título + línea de estado (en el color del tono)
 * y [trailing] a la derecha (botón, chevron, check). [muted] atenúa la fila (elementos ya resueltos).
 */
@Composable
fun BentoStatusRow(
    title: String,
    status: String,
    icon: ImageVector,
    tone: BentoTone,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = AppBento.tone(tone)
    val alpha = if (muted) 0.6f else 1f
    AppCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = AppShape.listCard,
        containerColor = if (muted) AppColors.Surface.copy(alpha = 0.6f) else AppColors.Surface,
        borderColor = null,
        elevation = 1.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BentoIconSquare(icon, tone, size = 46.dp, shape = AppShape.iconRowLarge)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary.copy(alpha = alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    status,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.content.copy(alpha = alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

/** Botón compacto de acción principal (color de marca) para filas ([BentoStatusRow]) y tarjetas hero. */
@Composable
fun BentoActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** Por defecto color de marca; sobre una tarjeta oscura (hero) se invierte. */
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val container = if (enabled) containerColor else containerColor.copy(alpha = 0.4f)
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 72.dp, minHeight = 44.dp)
            .clip(AppShape.iconRowSmall)
            .background(container, AppShape.iconRowSmall)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = contentColor)
    }
}

/** Acceso rápido: cuadro de ícono tintado centrado con la etiqueta debajo. */
@Composable
fun BentoShortcut(label: String, icon: ImageVector, tone: BentoTone, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(AppShape.row).clickable(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BentoIconSquare(icon, tone, size = 52.dp, shape = AppShape.iconTile)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Barra de progreso segmentada (un tramo por segmento con valor > 0, separados por un hueco). */
@Composable
fun BentoSegmentedBar(segments: List<BentoSegment>, modifier: Modifier = Modifier) {
    val visible = segments.filter { it.value > 0 }
    Row(modifier = modifier.fillMaxWidth().height(8.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        if (visible.isEmpty()) {
            Box(Modifier.weight(1f).height(8.dp).background(AppColors.Outline.copy(alpha = 0.5f), AppShape.segment))
        }
        visible.forEach { Box(Modifier.weight(it.value.toFloat()).height(8.dp).background(it.color, AppShape.segment)) }
    }
}
