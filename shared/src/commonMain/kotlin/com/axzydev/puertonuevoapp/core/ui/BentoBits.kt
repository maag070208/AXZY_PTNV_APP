package com.axzydev.puertonuevoapp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import kotlin.math.max

// Piezas pequeñas del tablero bento (barras de la semana, nota, mini lista). Reutilizables.

/** Un día de [BentoWeekBars]: etiqueta (L, M, X...), valor y si es hoy. */
data class BentoWeekDay(val label: String, val value: Int, val today: Boolean = false)

/**
 * Barras de la semana: una columna por día con la altura proporcional al máximo;
 * [todayColor] resalta hoy. Un día en cero deja solo un trazo mínimo.
 */
@Composable
fun BentoWeekBars(
    days: List<BentoWeekDay>,
    color: Color,
    todayColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    barHeight: Dp = 46.dp,
) {
    val maxValue = max(1, days.maxOfOrNull { it.value } ?: 1)
    val shown = rememberCountUp(1f, 900)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { day ->
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.height(barHeight).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    val fraction = (day.value.toFloat() / maxValue).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(max(4f, barHeight.value * fraction * shown).dp)
                            .clip(AppShape.row)
                            .background(if (day.today) todayColor else color.copy(alpha = if (day.value == 0) 0.25f else 1f)),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    day.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (day.today) FontWeight.Bold else FontWeight.Normal,
                    color = contentColor.copy(alpha = if (day.today) 0.95f else 0.6f),
                )
            }
        }
    }
}

/** Nota con fondo tenue al pie de un mosaico (equivale al `InsightNote` de la web). */
@Composable
fun BentoInsight(text: String, contentColor: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = contentColor.copy(alpha = 0.75f),
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShape.row)
            .background(contentColor.copy(alpha = 0.06f), AppShape.row)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

/** Fila de [BentoMiniList]: nombre a la izquierda y [right] tenue a la derecha. */
data class BentoMiniListItem(val id: String, val name: String, val right: String)

/**
 * Mini lista con título e ícono (seguimiento del día): hasta [maxItems] filas o, si no hay
 * ninguna, [emptyLabel] con una palomita. Pulsable con [onClick].
 */
@Composable
fun BentoMiniList(
    icon: ImageVector,
    title: String,
    items: List<BentoMiniListItem>,
    emptyLabel: String,
    contentColor: Color,
    modifier: Modifier = Modifier,
    maxItems: Int = 4,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShape.row)
            .background(contentColor.copy(alpha = 0.05f), AppShape.row)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = contentColor.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = contentColor)
        }
        if (items.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 24.dp)) {
                Text(emptyLabel, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.6f))
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Filled.Check, contentDescription = null, tint = AppColors.Success, modifier = Modifier.size(12.dp))
            }
        } else {
            items.take(maxItems).forEach { item ->
                Row(modifier = Modifier.padding(start = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(item.right, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.6f), textAlign = TextAlign.End)
                }
            }
        }
    }
}
