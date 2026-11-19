package com.axzydev.puertonuevoapp.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axzydev.puertonuevoapp.core.theme.AppChrome
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape

/**
 * Destino de [BentoBottomBar]. Con [avatarName] se pinta el avatar (iniciales) en vez de
 * [icon] (p. ej. el perfil del usuario). [badge] > 0 dibuja un contador pequeño sobre el
 * ícono (mismo lenguaje que [BentoIconButton]); 0 = sin insignia.
 */
data class BentoNavItem(
    val icon: ImageVector?,
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val avatarName: String? = null,
    val badge: Int = 0,
    /** Descripción accesible si la etiqueta visible es ambigua (p. ej. el botón central). */
    val contentDescription: String? = null,
)

/**
 * Barra de navegación inferior flotante del diseño "C": pastilla blanca (72dp, radio 24dp) con
 * márgenes de 12dp a los lados, 8dp arriba y 14dp abajo (más el inset del sistema) y sombra suave.
 *
 * Cada destino es una columna ícono (20dp) + etiqueta (12sp). El activo lleva píldora de 48x30 y
 * se pinta en el color de marca (etiqueta bold); el inactivo va en gris. [action] (p. ej. el
 * asistente) se coloca como un destino más, en el centro, sin muesca; [actionContent] reemplaza su
 * ícono. El avatar del perfil conserva su anillo cuando está activo.
 */
@Composable
fun BentoBottomBar(
    items: List<BentoNavItem>,
    modifier: Modifier = Modifier,
    action: BentoNavItem? = null,
    /** Contenido del botón central (p. ej. la mascota de Langostín) en vez del ícono. */
    actionContent: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = AppChrome.BarSideMargin,
                end = AppChrome.BarSideMargin,
                top = AppChrome.BarTopMargin,
                bottom = AppChrome.BarBottomMargin,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppChrome.BarHeight)
                .shadow(
                    elevation = 12.dp,
                    shape = AppShape.chromeBar,
                    ambientColor = Color.Black.copy(alpha = 0.10f),
                    spotColor = Color.Black.copy(alpha = 0.14f),
                )
                .clip(AppShape.chromeBar)
                .background(MaterialTheme.colorScheme.surface, AppShape.chromeBar)
                .padding(horizontal = AppChrome.BarPaddingHorizontal),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (action != null) {
                val left = items.take((items.size + 1) / 2)
                val right = items.drop(left.size)
                left.forEach { NavDestination(it) }
                NavDestination(action, customIcon = actionContent)
                right.forEach { NavDestination(it) }
            } else {
                items.forEach { NavDestination(it) }
            }
        }
    }
}

@Composable
private fun RowScope.NavDestination(item: BentoNavItem, customIcon: (@Composable () -> Unit)? = null) {
    val avatarName = item.avatarName
    val icon = item.icon
    val active = MaterialTheme.colorScheme.primary
    val inactive = AppColors.TextMuted
    val tint by animateColorAsState(
        targetValue = if (item.selected) active else inactive,
        animationSpec = tween(220),
        label = "bottomBarTint",
    )
    val pillColor by animateColorAsState(
        targetValue = if (item.selected) AppChrome.navPill else Color.Transparent,
        animationSpec = tween(220),
        label = "bottomBarPillColor",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 52.dp)
            .clip(AppShape.row)
            .selectable(selected = item.selected, role = Role.Tab, onClick = item.onClick)
            .semantics { item.contentDescription?.let { contentDescription = it } },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .width(AppChrome.NavPillWidth)
                .height(AppChrome.NavPillHeight)
                .clip(AppShape.pill)
                .background(if (avatarName == null && customIcon == null) pillColor else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            when {
                customIcon != null -> customIcon()
                avatarName != null -> BentoAvatar(
                    name = avatarName,
                    color = if (item.selected) active else MaterialTheme.colorScheme.onSurfaceVariant,
                    size = NavAvatarSize,
                    modifier = Modifier
                        .then(if (item.selected) Modifier.border(2.dp, active, CircleShape) else Modifier)
                        // Las iniciales son decorativas: el item ya se anuncia con su etiqueta.
                        .clearAndSetSemantics {},
                )
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(AppChrome.NavIconSize),
                )
            }
            if (item.badge > 0) {
                NavBadge(
                    count = item.badge,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-2).dp, y = (-4).dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            fontWeight = if (item.selected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val NavAvatarSize = 24.dp

/** Contador pequeño sobre el ícono de un destino (mismo tono rose que las campanas del header). */
@Composable
private fun NavBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .background(AppColors.Danger, CircleShape)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}
