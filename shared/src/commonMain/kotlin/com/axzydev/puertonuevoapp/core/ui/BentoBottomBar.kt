package com.axzydev.puertonuevoapp.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

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
)

/** Alto de la barra (sin el inset de la barra de navegación del sistema). */
private val BarHeight = 66.dp

/** Radio de las esquinas superiores: el mismo del header/hojas (`AppShape.sheetTop`). */
private val BarCorner = 28.dp

private val BarSidePadding = 8.dp

/** Círculo ámbar del botón "Nuevo". */
private val ActionSize = 58.dp

/** Anillo blanco que separa el botón de la muesca. */
private val ActionRing = 4.dp

/** Diámetro total del botón (relleno + anillo). */
private val ActionOuter = ActionSize + ActionRing * 2f

/** Hueco de la fila que ocupa el botón: un poco más ancho que la muesca, sin tocar destinos. */
private val ActionSlot = ActionOuter + 6.dp

/** Cuánto sobresale el botón por encima del borde de la barra (≤32dp: no se come la pantalla). */
private val ActionOverhang = 32.dp

private val ActionIconSize = 26.dp

/** Separación entre el anillo del botón y la muesca (deja ver el lienzo, como antes). */
private val NotchGap = 2.dp

private val NotchRadius = ActionOuter / 2f + NotchGap

/** Centro de la muesca respecto del borde superior de la barra (positivo = por dentro). */
private val NotchCenterY = ActionOuter / 2f - ActionOverhang

private val PillWidth = 40.dp
private val PillHeight = 28.dp
private val NavIconSize = 24.dp
private val NavAvatarSize = 26.dp

/** Opacidad del hairline superior sobre el lienzo. */
private const val HairlineAlpha = 0.5f

/**
 * Barra con esquinas superiores redondeadas y, si [notchRadius] > 0dp, una muesca circular en el
 * borde superior centrada (el botón central queda "encajado"). [notchCenterY] es la altura del
 * centro de la muesca respecto del borde superior: positiva cuando el centro cae dentro de la barra.
 */
private class NotchedBarShape(
    private val corner: Dp,
    private val notchRadius: Dp,
    private val notchCenterY: Dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val c = with(density) { corner.toPx() }
        if (notchRadius <= 0.dp) {
            return Outline.Rounded(
                RoundRect(
                    0f, 0f, size.width, size.height,
                    CornerRadius(c), CornerRadius(c), CornerRadius.Zero, CornerRadius.Zero,
                ),
            )
        }
        val r = with(density) { notchRadius.toPx() }
        val cy = with(density) { notchCenterY.toPx() }
        val path = topEdgePath(size.width, c, r, cy).apply {
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Contorno del borde superior de la barra (en px): esquinas redondeadas + muesca. Se usa tanto
 * para el `Outline` de [NotchedBarShape] como para dibujar el hairline superior, así los dos
 * coinciden exactamente.
 */
private fun topEdgePath(widthPx: Float, cornerPx: Float, notchRadiusPx: Float, notchCenterYPx: Float): Path =
    Path().apply {
        moveTo(0f, cornerPx)
        arcTo(Rect(0f, 0f, 2 * cornerPx, 2 * cornerPx), 180f, 90f, false)
        if (notchRadiusPx > 0f) {
            val cx = widthPx / 2f
            // Puntos donde el círculo de la muesca corta el borde superior.
            val dx = sqrt((notchRadiusPx * notchRadiusPx - notchCenterYPx * notchCenterYPx).coerceAtLeast(0f))
            // Ángulos (grados, 0 = las 3, sentido horario) de los dos cortes: el arco va del corte
            // izquierdo al derecho bajando por el fondo del círculo (barrido negativo = antihorario).
            // Se calcula con atan2 para que funcione también cuando el centro de la muesca cae
            // dentro de la barra (notchCenterY > 0), no solo cuando queda por encima.
            val startAngle = atan2(-notchCenterYPx, -dx) * 180f / PI.toFloat()
            val endAngle = atan2(-notchCenterYPx, dx) * 180f / PI.toFloat()
            val sweep = if (endAngle < startAngle) endAngle - startAngle else endAngle - startAngle - 360f
            lineTo(cx - dx, 0f)
            arcTo(
                Rect(cx - notchRadiusPx, notchCenterYPx - notchRadiusPx, cx + notchRadiusPx, notchCenterYPx + notchRadiusPx),
                startAngle,
                sweep,
                false,
            )
        }
        lineTo(widthPx - cornerPx, 0f)
        arcTo(Rect(widthPx - 2 * cornerPx, 0f, widthPx, 2 * cornerPx), 270f, 90f, false)
    }

/**
 * Barra de navegación inferior del shell: superficie blanca a todo el ancho, 66dp, esquinas
 * superiores de 28dp, sombra suave y hairline superior para despegarla del lienzo.
 *
 * Cada destino es ícono + etiqueta pequeña (activo en tinta con píldora detrás del ícono,
 * inactivo en gris); el avatar del perfil conserva su anillo tinta cuando está activo.
 * [action] agrega el botón central "Nuevo" (ámbar con anillo blanco) encajado en una muesca;
 * sin él, los destinos se reparten parejo. Conserva la API pública histórica.
 */
@Composable
fun BentoBottomBar(
    items: List<BentoNavItem>,
    modifier: Modifier = Modifier,
    action: BentoNavItem? = null,
) {
    val hasAction = action != null
    val surface = MaterialTheme.colorScheme.surface
    val notchRadius = if (hasAction) NotchRadius else 0.dp
    val notchCenterY = if (hasAction) NotchCenterY else 0.dp
    val shape = remember(notchRadius, notchCenterY) {
        NotchedBarShape(BarCorner, notchRadius, notchCenterY)
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (hasAction) ActionOverhang else 0.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.10f),
                    spotColor = Color.Black.copy(alpha = 0.16f),
                )
                .background(surface, shape)
                // Recorta el contenido (y el hairline) a la forma: las esquinas y la muesca
                // también cortan lo que va encima, y el borde de 2dp queda a 1dp por dentro.
                .clip(shape)
                .navigationBarsPadding(),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BarHeight)
                        .padding(horizontal = BarSidePadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (action != null) {
                        // Dos grupos de igual peso alrededor del botón: el botón (y la muesca)
                        // quedan siempre centrados, aunque el número de destinos sea impar.
                        val left = items.take((items.size + 1) / 2)
                        val right = items.drop(left.size)
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            left.forEach { NavDestination(it) }
                        }
                        Box(
                            modifier = Modifier.width(ActionSlot).height(BarHeight),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            // La etiqueta del botón vive dentro de la barra, bajo la muesca.
                            Text(
                                text = action.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppBento.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            right.forEach { NavDestination(it) }
                        }
                    } else {
                        items.forEach { NavDestination(it) }
                    }
                }
                // Hairline superior: sigue el mismo contorno que la forma (esquinas y muesca).
                // El contenido va recortado a la forma, así que se traza 2dp y solo queda visible
                // la mitad interior (1dp) pegada al borde.
                Canvas(modifier = Modifier.matchParentSize()) {
                    val cornerPx = BarCorner.toPx()
                    val radiusPx = if (hasAction) NotchRadius.toPx() else 0f
                    val centerYPx = if (hasAction) NotchCenterY.toPx() else 0f
                    drawPath(
                        path = topEdgePath(size.width, cornerPx, radiusPx, centerYPx),
                        color = AppColors.Outline.copy(alpha = HairlineAlpha),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
        }
        if (action != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(ActionOuter)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.12f),
                        spotColor = Color.Black.copy(alpha = 0.20f),
                    )
                    .clip(CircleShape)
                    .background(surface, CircleShape)
                    .clickable(role = Role.Button, onClick = action.onClick)
                    .semantics { contentDescription = action.label },
                contentAlignment = Alignment.Center,
            ) {
                // Relleno ámbar dentro del anillo blanco de ActionRing.
                Box(
                    modifier = Modifier
                        .size(ActionSize)
                        .clip(CircleShape)
                        .background(AppColors.Warning, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = action.icon ?: Icons.Filled.Add,
                        contentDescription = null,
                        tint = AppBento.ink,
                        modifier = Modifier.size(ActionIconSize),
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.NavDestination(item: BentoNavItem) {
    val avatarName = item.avatarName
    val icon = item.icon
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    val iconColor by animateColorAsState(
        targetValue = if (item.selected) AppBento.onInk else inactive,
        animationSpec = tween(220),
        label = "bottomBarIconColor",
    )
    // La etiqueta vive fuera de la píldora (sobre la barra blanca): en tinta para poder leerse.
    val labelColor by animateColorAsState(
        targetValue = if (item.selected) AppBento.ink else inactive,
        animationSpec = tween(220),
        label = "bottomBarLabelColor",
    )
    val pillColor by animateColorAsState(
        targetValue = if (item.selected) AppBento.ink else Color.Transparent,
        animationSpec = tween(220),
        label = "bottomBarPillColor",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(AppShape.row)
            .selectable(selected = item.selected, role = Role.Tab, onClick = item.onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (avatarName == null) {
                // Píldora tinta detrás del ícono (40x28); en los inactivos es transparente.
                Box(
                    modifier = Modifier
                        .width(PillWidth)
                        .height(PillHeight)
                        .clip(AppShape.pill)
                        .background(pillColor),
                )
            }
            Box(contentAlignment = Alignment.Center) {
                if (avatarName != null) {
                    BentoAvatar(
                        name = avatarName,
                        color = if (item.selected) AppBento.ink else MaterialTheme.colorScheme.onSurfaceVariant,
                        size = NavAvatarSize,
                        modifier = Modifier
                            .then(if (item.selected) Modifier.border(2.dp, AppBento.ink, CircleShape) else Modifier)
                            // Las iniciales son decorativas: el item ya se anuncia con su etiqueta.
                            .clearAndSetSemantics {},
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(NavIconSize),
                    )
                }
                if (item.badge > 0) {
                    NavBadge(
                        count = item.badge,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 5.dp, y = (-4).dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (item.selected) FontWeight.Bold else FontWeight.Normal,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

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
