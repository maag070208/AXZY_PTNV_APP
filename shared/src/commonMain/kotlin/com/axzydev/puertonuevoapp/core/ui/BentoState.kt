package com.axzydev.puertonuevoapp.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone

/**
 * Estado vacío / error / cargando como tarjeta bento. Úsalo en un bloque de la
 * pantalla (no a pantalla completa) para que un fallo no tumbe el resto.
 * Con [onAction] muestra un botón píldora ([actionLabel]).
 */
@Composable
fun BentoState(
    message: String,
    modifier: Modifier = Modifier,
    tone: BentoTone = BentoTone.Plain,
    icon: ImageVector? = Icons.Filled.Inbox,
    loading: Boolean = false,
    actionLabel: String = "Reintentar",
    onAction: (() -> Unit)? = null,
) {
    val colors = AppBento.tone(tone)
    AppCard(
        modifier = modifier.fillMaxWidth(),
        shape = AppShape.bento,
        containerColor = colors.container,
        borderColor = null,
        contentPadding = PaddingValues(20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = colors.content, strokeWidth = 3.dp)
            } else if (icon != null) {
                Icon(icon, contentDescription = null, tint = colors.content.copy(alpha = 0.7f), modifier = Modifier.size(32.dp))
            }
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.content.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
            if (onAction != null) {
                Spacer(Modifier.height(2.dp))
                PillButton(
                    text = actionLabel,
                    onClick = onAction,
                    variant = PillButtonVariant.Ink,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

/** Atajo de [BentoState] para errores (tono rosa + botón de reintento). */
@Composable
fun BentoErrorState(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    BentoState(
        message = message,
        modifier = modifier,
        tone = BentoTone.Rose,
        icon = Icons.Filled.ErrorOutline,
        onAction = onRetry,
    )
}
