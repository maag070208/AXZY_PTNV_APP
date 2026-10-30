package com.axzydev.puertonuevoapp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone

/** Evento del feed: ícono en chip pastel ([tone]), [title] y [meta] (hora). */
data class BentoFeedItem(
    val id: String,
    val title: String,
    val meta: String,
    val icon: ImageVector,
    val tone: BentoTone = BentoTone.Mint,
    val onClick: (() -> Unit)? = null,
)

/** Eventos de un mismo día bajo una etiqueta (Hoy / Ayer / fecha). */
data class BentoFeedGroup(val label: String, val items: List<BentoFeedItem>)

/**
 * Feed agrupado por día (estilo bento): etiqueta del día y filas con chip de
 * ícono pastel, título y hora. [contentColor] es el color de texto del mosaico que lo aloja.
 */
@Composable
fun BentoFeed(
    groups: List<BentoFeedGroup>,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        groups.forEachIndexed { index, group ->
            if (index > 0) Spacer(Modifier.height(10.dp))
            Text(
                group.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.55f),
                modifier = Modifier.padding(bottom = 4.dp),
            )
            group.items.forEach { item -> BentoFeedRow(item, contentColor) }
        }
    }
}

@Composable
private fun BentoFeedRow(item: BentoFeedItem, contentColor: Color) {
    val chip = AppBento.tone(item.tone)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShape.row)
            .then(if (item.onClick != null) Modifier.clickable(onClick = item.onClick) else Modifier)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(chip.container, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.icon, contentDescription = null, tint = chip.content, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            item.title,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(item.meta, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.55f))
    }
}
