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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.platform.testTag
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
 * y [trailing] a la derecha (botón, chevron, check). [muted] marca un elemento ya resuelto: la
 * tarjeta queda blanca y plana (sin sombra) y el estado conserva el tono, sin capas translúcidas
 * (la tarjeta gris translúcida sobre el lienzo se leía como un error de pintado).
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
    /** Etiqueta estable para Maestro (`id:`): filas de lista. */
    testTag: String? = null,
) {
    val colors = AppBento.tone(tone)
    AppCard(
        modifier = modifier.fillMaxWidth().then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        onClick = onClick,
        shape = AppShape.listCard,
        containerColor = AppColors.Surface,
        borderColor = null,
        // Ya resuelto = tarjeta plana: sin la sombra extra de las filas pendientes.
        elevation = if (muted) 0.dp else 1.dp,
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
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    status,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.content,
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
    /** Etiqueta estable para Maestro (`id:`): acción de fila. */
    testTag: String? = null,
) {
    val container = if (enabled) containerColor else containerColor.copy(alpha = 0.4f)
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 72.dp, minHeight = 44.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
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

/** Acceso rápido de [BentoShortcutGrid]: etiqueta, ícono, tono y destino. */
data class BentoShortcutItem(
    val label: String,
    val icon: ImageVector,
    val tone: BentoTone,
    val onClick: () -> Unit,
)

/**
 * Rejilla de accesos rápidos ([BentoShortcut]) en filas de a lo más [columns] columnas iguales.
 *
 * Se usa en lugar de una sola fila cuando hay 4+ accesos: en una fila única las etiquetas largas
 * ("Habitaciones") se recortaban a una o dos letras en teléfonos de 360dp. Las columnas faltantes
 * de la última fila se rellenan con espacio en blanco para que las tarjetas no se estiren.
 */
@Composable
fun BentoShortcutGrid(
    items: List<BentoShortcutItem>,
    modifier: Modifier = Modifier,
    columns: Int = 3,
) {
    val perRow = columns.coerceAtLeast(1)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(perRow).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    BentoShortcut(
                        label = item.label,
                        icon = item.icon,
                        tone = item.tone,
                        onClick = item.onClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Control compacto de cantidad ("−  valor  +") para los diálogos de captura: una sola pastilla con
 * fondo de campo, 44dp de alto y botones de 44dp. Antes cada botón medía 48dp y el valor 40dp, así
 * que en los modales de Blancos el nombre del artículo se partía en dos renglones.
 */
@Composable
fun BentoQtyStepper(
    value: Double,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    /** Etiqueta estable para Maestro (`id:`): botón "+". */
    plusTestTag: String? = null,
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(AppShape.pill)
            .background(AppColors.SurfaceVariant, AppShape.pill),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QtyStepButton(Icons.Filled.Remove, "Quitar", onMinus)
        Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) {
            Text(
                text = qtyStepperText(value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                maxLines = 1,
            )
        }
        QtyStepButton(Icons.Filled.Add, "Agregar", onPlus, plusTestTag)
    }
}

@Composable
private fun QtyStepButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String? = null,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clip(AppShape.pill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = AppColors.TextMuted, modifier = Modifier.size(18.dp))
    }
}

/** "3" para enteros y "2.5" para fracciones (las cantidades de blancos son piezas o fracciones). */
private fun qtyStepperText(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

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
