package com.axzydev.puertonuevoapp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape

/** Una pestaña de [BentoTabs]. */
data class BentoTabItem(val label: String, val icon: ImageVector? = null)

/**
 * Pestañas en píldora desplazables: la activa en "tinta", el resto neutras.
 * [trailing] se pinta a la derecha del scroll (p. ej. [BentoLivePill]).
 */
@Composable
fun BentoTabs(
    items: List<BentoTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            // Ocupa el ancho disponible (weight) y se desplaza en horizontal; las píldoras no se comprimen.
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val container = if (selected) AppBento.ink else AppBento.field
                val content = if (selected) AppBento.onInk else MaterialTheme.colorScheme.onSurfaceVariant
                Row(
                    modifier = Modifier
                        .clip(AppShape.pill)
                        .background(container, AppShape.pill)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (item.icon != null) {
                        Icon(item.icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    // Una línea sin recorte: si no caben todas, la fila se desplaza.
                    Text(item.label, color = content, style = MaterialTheme.typography.titleSmall, maxLines = 1, softWrap = false)
                }
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

/** Píldora "● En vivo" (datos del panel en tiempo real). */
@Composable
fun BentoLivePill(label: String = "En vivo", modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(AppBento.field, AppShape.pill)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(8.dp).background(AppColors.Success, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}
