package com.axzydev.puertonuevoapp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.axzydev.puertonuevoapp.core.theme.AppColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone

/**
 * Mosaico bento: tarjeta de radio 24dp sin borde, teñida por [tone].
 * Etiqueta arriba a la izquierda (con [icon] opcional a la derecha), [content]
 * opcional al centro y, abajo, el [value] grande a la izquierda con un [aside]
 * tenue (o un [cornerIcon] de línea) a la derecha. [watermark] pinta un ícono
 * grande y tenue como decoración de la esquina inferior derecha. Si el mosaico
 * es más alto que su contenido (p. ej. "hero" de dos filas), el valor se pega abajo.
 */
@Composable
fun BentoTile(
    label: String,
    modifier: Modifier = Modifier,
    tone: BentoTone = BentoTone.Plain,
    icon: ImageVector? = null,
    value: String? = null,
    aside: String? = null,
    onClick: (() -> Unit)? = null,
    /** Número destacado (hero / KPI): tipografía grande y muy negrita. */
    bigValue: Boolean = false,
    /** Color del [value] si debe distinguirse del contenido del tono (p. ej. alerta). */
    valueColor: Color? = null,
    /** Ícono de línea pequeño abajo a la derecha (sustituye al [aside]). */
    cornerIcon: ImageVector? = null,
    /** Ícono grande y tenue de decoración (esquina inferior derecha). */
    watermark: ImageVector? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val colors = AppBento.tone(tone)
    AppCard(
        modifier = modifier,
        onClick = onClick,
        shape = AppShape.bento,
        containerColor = colors.container,
        borderColor = null,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (watermark != null) {
                Icon(
                    watermark,
                    contentDescription = null,
                    tint = colors.content.copy(alpha = 0.10f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 18.dp, y = 18.dp)
                        .size(96.dp),
                )
            }
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = label,
                        color = colors.content.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    if (icon != null) {
                        Spacer(Modifier.width(8.dp))
                        Icon(icon, contentDescription = null, tint = colors.content, modifier = Modifier.size(20.dp))
                    }
                }
                if (content != null) {
                    Spacer(Modifier.height(12.dp))
                    content()
                }
                if (value != null || aside != null || cornerIcon != null) {
                    // Pega el valor al fondo cuando el mosaico es más alto que su contenido.
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        if (value != null) {
                            Text(
                                text = value,
                                color = valueColor ?: colors.content,
                                style = if (bigValue) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                            )
                        }
                        if (aside != null) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = aside,
                                color = colors.content.copy(alpha = 0.65f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (cornerIcon != null) {
                            Spacer(Modifier.width(8.dp))
                            Icon(cornerIcon, contentDescription = null, tint = colors.content.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Mini mosaico para contadores dentro de una tarjeta (estado de tickets, días de
 * la semana): [value] en negrita y [label] debajo, teñido por [tone]. [highlight]
 * lo remarca (p. ej. el día de hoy) con el color de tinta.
 */
@Composable
fun BentoMiniStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tone: BentoTone = BentoTone.Plain,
    highlight: Boolean = false,
    onClick: (() -> Unit)? = null,
    /** Líneas máximas de la etiqueta (etiquetas largas en cuadrículas de 3 columnas). */
    labelMaxLines: Int = 1,
) {
    val colors = AppBento.tone(tone)
    val container = if (highlight) AppBento.ink else colors.container
    val content = if (highlight) AppBento.onInk else colors.content
    AppCard(
        modifier = modifier,
        onClick = onClick,
        shape = AppShape.card,
        containerColor = container,
        borderColor = null,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
    ) {
        Text(
            value,
            color = content,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Text(
            label,
            color = content.copy(alpha = 0.75f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = labelMaxLines,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

/** Botón circular neutro con ícono y, opcionalmente, una insignia con [badge] (> 0). */
@Composable
fun BentoIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Int = 0,
) {
    Box(modifier = modifier.size(44.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
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
                    if (badge > 99) "99+" else badge.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

/** Variantes visuales de [PillButton]. */
enum class PillButtonVariant {
    /** Relleno del color primario. */
    Primary,

    /** Relleno pastel (contenedor primario) con texto primario. */
    Secondary,

    /** Relleno "tinta" (casi negro). */
    Ink,

    /** Sin relleno, solo texto primario. */
    Ghost,
}

/**
 * Botón píldora estándar bento (52dp). Con [loading] muestra un spinner y
 * se deshabilita sin cambiar de color.
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PillButtonVariant = PillButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (variant) {
        PillButtonVariant.Primary -> scheme.primary to scheme.onPrimary
        PillButtonVariant.Secondary -> scheme.primaryContainer to scheme.onPrimaryContainer
        PillButtonVariant.Ink -> AppBento.ink to AppBento.onInk
        PillButtonVariant.Ghost -> Color.Transparent to scheme.primary
    }
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = AppShape.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            // Mientras carga conserva el color; deshabilitado normal se atenúa.
            disabledContainerColor = if (loading) container else container.copy(alpha = 0.4f),
            disabledContentColor = if (loading) content else content.copy(alpha = 0.6f),
        ),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = modifier.height(52.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = content,
                strokeWidth = 2.dp,
            )
        } else {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * Chip translúcido para fondos de marca (encabezados sobre el gradiente
 * primario). Texto e ícono blancos sobre blanco al 14%.
 */
@Composable
fun OnBrandChip(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val fg = MaterialTheme.colorScheme.onPrimary
    Row(
        modifier = modifier
            .background(fg.copy(alpha = 0.14f), AppShape.pill)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = fg, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Hoja bento: superficie con esquinas superiores de 28dp pensada para
 * superponerse ([overlap]) sobre un encabezado de marca.
 */
@Composable
fun BentoSheet(
    modifier: Modifier = Modifier,
    overlap: Dp = 28.dp,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            // Sube la hoja [overlap] sobre el encabezado y recorta ese alto del layout.
            .layout { measurable, constraints ->
                val up = overlap.roundToPx()
                val placeable = measurable.measure(constraints)
                layout(placeable.width, (placeable.height - up).coerceAtLeast(0)) {
                    placeable.place(0, -up)
                }
            }
            .background(MaterialTheme.colorScheme.surface, AppShape.sheetTop),
        content = content,
    )
}
