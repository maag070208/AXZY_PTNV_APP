package com.axzydev.puertonuevoapp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axzydev.puertonuevoapp.core.session.SessionUser
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppChrome
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape

/**
 * Header compartido del diseño "C": fila sin banda propia (mismo lienzo de la pantalla), 20dp de
 * margen lateral, 12dp entre elementos y controles cuadrados redondeados de 44dp (radio 16).
 *
 * Variantes:
 * - Raíz con saludo ([rootAvatar] != null, sin atrás): avatar circular 44dp a la izquierda (abre el
 *   menú), [subtitle] como línea superior pequeña y [title] como saludo, campana con punto a la derecha.
 * - Raíz estándar: botón de menú + título.
 * - Interna ([showBack]): botón atrás + título (+ [subtitle]) y a la derecha [endText], la acción
 *   de la pantalla ([action]) y la campana.
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
    /** Si no es null (y no hay atrás): variante raíz con avatar a la izquierda que abre el menú. */
    rootAvatar: SessionUser? = null,
    /** Texto discreto a la derecha (p. ej. "Hoy"). */
    endText: String? = null,
    /** Muestra la campana (por defecto sí). */
    showNotifications: Boolean = true,
) {
    val greetingRoot = rootAvatar != null && !showBack
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .statusBarsPadding()
            .padding(start = AppChrome.HeaderHorizontal, end = AppChrome.HeaderHorizontal, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (greetingRoot && rootAvatar != null) {
            Box(
                modifier = Modifier
                    .size(AppChrome.ButtonSize)
                    .shadow(2.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.12f))
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .clickable(onClick = onMenu),
                contentAlignment = Alignment.Center,
            ) {
                BentoAvatar(name = rootAvatar.name, color = MaterialTheme.colorScheme.primary, size = AppChrome.ButtonSize - 4.dp)
            }
        } else {
            HeaderSquareButton(
                icon = if (showBack) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Menu,
                contentDescription = if (showBack) "Atrás" else "Menú",
                onClick = if (showBack) onBack else onMenu,
            )
        }
        Spacer(Modifier.width(AppChrome.HeaderGap))
        Column(modifier = Modifier.weight(1f)) {
            if (greetingRoot && subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = AppColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!greetingRoot && subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = AppColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (endText != null) {
            Text(endText, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = AppColors.TextMuted)
            Spacer(Modifier.width(AppChrome.HeaderGap))
        }
        if (action != null) {
            HeaderSquareButton(
                icon = action.icon,
                contentDescription = action.contentDescription,
                onClick = action.onClick,
            )
            Spacer(Modifier.width(AppChrome.HeaderGap))
        }
        if (showNotifications) {
            HeaderSquareButton(
                icon = Icons.Filled.Notifications,
                contentDescription = "Notificaciones",
                onClick = onNotifications,
                dot = unreadCount > 0,
            )
        }
        if (user != null && !greetingRoot) {
            Spacer(Modifier.width(AppChrome.HeaderGap))
            Box(
                modifier = Modifier
                    .size(AppChrome.ButtonSize)
                    .shadow(2.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.12f))
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .clickable(onClick = onProfile),
                contentAlignment = Alignment.Center,
            ) {
                BentoAvatar(name = user.name, color = MaterialTheme.colorScheme.primary, size = AppChrome.ButtonSize - 4.dp)
            }
        }
    }
}

/**
 * Botón cuadrado redondeado del header (44dp, radio [AppShape.chromeButton], superficie blanca con
 * sombra suave, ícono 20dp en tinta). [dot] pinta el punto de "hay novedades" arriba a la derecha.
 */
@Composable
private fun HeaderSquareButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    dot: Boolean = false,
) {
    Box(modifier = Modifier.size(AppChrome.ButtonSize)) {
        Box(
            modifier = Modifier
                .size(AppChrome.ButtonSize)
                .shadow(2.dp, AppShape.chromeButton, ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.12f))
                .clip(AppShape.chromeButton)
                .background(MaterialTheme.colorScheme.surface, AppShape.chromeButton)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = AppColors.TextPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        if (dot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 11.dp)
                    .size(AppChrome.DotSize + AppChrome.DotBorder * 2)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .padding(AppChrome.DotBorder)
                    .background(AppChrome.Dot, CircleShape),
            )
        }
    }
}
