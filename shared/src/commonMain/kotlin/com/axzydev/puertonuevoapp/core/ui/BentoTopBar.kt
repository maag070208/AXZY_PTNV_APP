package com.axzydev.puertonuevoapp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.session.SessionUser
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors

/**
 * Barra superior del shell. Sustituye al `TopAppBar` de Material por una fila
 * "flush" con el mismo color del lienzo de la pantalla (sin banda distinta):
 *
 * - Izquierda: botón circular blanco (hamburguesa o flecha atrás).
 * - Centro: título de la pantalla (+ subtítulo con el módulo cuando hay atrás). La
 *   identidad de marca vive en el hero del drawer, no en el header.
 * - Derecha: acción opcional de la pantalla ([action]), campana con insignia de
 *   no leídas y avatar del usuario (abre Mi credencial).
 *
 * Los controles son círculos blancos flotantes de 44dp, el mismo lenguaje que
 * los mosaicos bento: así el header se lee sobre cualquier lienzo y la campana
 * y el avatar dejan de ser exclusivos del Inicio.
 */
@Composable
fun BentoTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    onMenu: () -> Unit = {},
    unreadCount: Int = 0,
    onNotifications: () -> Unit = {},
    user: SessionUser? = null,
    onProfile: () -> Unit = {},
    action: TopBarAction? = null,
    containerColor: Color = AppBento.canvas,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderCircleButton(
            icon = if (showBack) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Menu,
            contentDescription = if (showBack) "Atrás" else "Menú",
            onClick = if (showBack) onBack else onMenu,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (action != null) {
            HeaderCircleButton(
                icon = action.icon,
                contentDescription = action.contentDescription,
                onClick = action.onClick,
            )
            Spacer(Modifier.width(8.dp))
        }
        HeaderCircleButton(
            icon = Icons.Filled.Notifications,
            contentDescription = "Notificaciones",
            onClick = onNotifications,
            badge = unreadCount,
        )
        if (user != null) {
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(6.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.14f))
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .clickable(onClick = onProfile)
                    .padding(2.dp),
                contentAlignment = Alignment.Center,
            ) {
                BentoAvatar(
                    name = user.name,
                    color = MaterialTheme.colorScheme.primary,
                    size = 40.dp,
                )
            }
        }
    }
}

/**
 * Control circular blanco del header (hamburguesa, atrás, campana, acción de la
 * pantalla). El ícono va en tinta; [badge] > 0 pinta el contador rose arriba a
 * la derecha, igual que [BentoIconButton].
 */
@Composable
private fun HeaderCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    badge: Int = 0,
) {
    Box(modifier = Modifier.size(44.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(6.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.14f))
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = AppBento.ink,
                modifier = Modifier.size(22.dp),
            )
        }
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                    .background(AppColors.Danger, CircleShape)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (badge > 99) "99+" else badge.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
