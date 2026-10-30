package com.axzydev.puertonuevoapp.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.util.initials
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// Gráficas y números animados del diseño bento. Reutilizables en cualquier pantalla.

/**
 * Valor que sube de 0 a [target] una vez al aparecer (y vuelve a animar si [target]
 * cambia). Úsalo para los números grandes de los mosaicos.
 */
@Composable
fun rememberCountUp(target: Float, durationMillis: Int = 900): Float {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(target) {
        progress.animateTo(target, tween(durationMillis))
    }
    return progress.value
}

/** Versión entera de [rememberCountUp]. */
@Composable
fun rememberCountUp(target: Int, durationMillis: Int = 900): Int =
    rememberCountUp(target.toFloat(), durationMillis).roundToInt()

/** Un segmento (valor + color) de [BentoDonut] / barras con leyenda. */
data class BentoSegment(val label: String, val value: Int, val color: Color)

/**
 * Medidor semicircular: [fraction] (0..1) del arco relleno con [color] sobre
 * [trackColor]. El arco se dibuja de 0 a [fraction] al aparecer.
 */
@Composable
fun BentoGauge(
    fraction: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 96.dp,
    stroke: Dp = 14.dp,
) {
    val shown = rememberCountUp(fraction.coerceIn(0f, 1f), 1000)
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val strokePx = stroke.toPx()
        val radius = min((size.width - strokePx) / 2f, size.height - strokePx / 2f)
        val topLeft = Offset(size.width / 2f - radius, size.height - strokePx / 2f - radius)
        val arcSize = Size(radius * 2f, radius * 2f)
        drawArc(trackColor, 180f, 180f, false, topLeft, arcSize, style = Stroke(strokePx, cap = StrokeCap.Round))
        if (shown > 0f) {
            drawArc(color, 180f, 180f * shown, false, topLeft, arcSize, style = Stroke(strokePx, cap = StrokeCap.Round))
        }
    }
}

/**
 * Avance en segmentos: [done] de [total] (hasta [maxSegments] barritas; si hay más
 * unidades se escala la proporción). No dibuja nada si [total] es 0.
 */
@Composable
fun BentoSegments(
    total: Int,
    done: Int,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 28.dp,
    maxSegments: Int = 20,
) {
    if (total <= 0) return
    val count = min(total, maxSegments)
    val filled = ((done.coerceIn(0, total).toFloat() / total) * count).roundToInt()
    Row(
        modifier = modifier.fillMaxWidth().height(height),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(count) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(AppShape.pill)
                    .background(if (i < filled) color else trackColor),
            )
        }
    }
}

/**
 * Olas animadas que llenan el [level] (0..1) del mosaico desde abajo. No dibuja
 * nada si [level] es 0.
 */
@Composable
fun BentoWave(
    level: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
) {
    if (level <= 0f) return
    val shownLevel = rememberCountUp(level.coerceIn(0f, 1f), 1000)
    val phase by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
    )
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val amplitude = size.height * 0.12f
        val top = size.height * (1f - shownLevel).coerceIn(0.1f, 1f)
        fun wave(shift: Float): Path = Path().apply {
            moveTo(0f, size.height)
            var x = 0f
            while (x <= size.width) {
                lineTo(x, top + amplitude * sin(x / size.width * 2f * PI.toFloat() * 1.5f + phase + shift))
                x += 4f
            }
            lineTo(size.width, size.height)
            close()
        }
        drawPath(wave(1.6f), color.copy(alpha = 0.35f))
        drawPath(wave(0f), color.copy(alpha = 0.7f))
    }
}

/**
 * Dona con [center] (texto grande) y [caption] opcional al centro. Los segmentos
 * con valor 0 no se dibujan; con total 0 solo queda la pista.
 */
@Composable
fun BentoDonut(
    segments: List<BentoSegment>,
    trackColor: Color,
    centerColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    stroke: Dp = 14.dp,
    center: String? = null,
    caption: String? = null,
) {
    val total = segments.sumOf { it.value }
    val sweepProgress = rememberCountUp(1f, 1000)
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = stroke.toPx()
            val radius = (this.size.minDimension - strokePx) / 2f
            val topLeft = Offset((this.size.width - radius * 2f) / 2f, (this.size.height - radius * 2f) / 2f)
            val arcSize = Size(radius * 2f, radius * 2f)
            drawCircle(trackColor, radius, style = Stroke(strokePx))
            if (total > 0) {
                var start = -90f
                segments.forEach { seg ->
                    if (seg.value > 0) {
                        val sweep = 360f * seg.value / total * sweepProgress
                        drawArc(seg.color, start, sweep, false, topLeft, arcSize, style = Stroke(strokePx))
                        start += sweep
                    }
                }
            }
        }
        if (center != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    center,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = centerColor,
                )
                if (caption != null) {
                    Text(
                        caption,
                        style = MaterialTheme.typography.labelSmall,
                        color = centerColor.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * Fila "lista con barra": [leading] (avatar / punto), [label], [valueText] a la
 * derecha y una barra delgada con [fraction] (0..1) debajo.
 */
@Composable
fun BentoBarRow(
    label: String,
    valueText: String,
    fraction: Float,
    color: Color,
    trackColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailingText: String? = null,
) {
    val shown = rememberCountUp(fraction.coerceIn(0f, 1f), 900)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(10.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(valueText, style = MaterialTheme.typography.titleSmall, color = contentColor)
            if (trailingText != null) {
                Spacer(Modifier.width(6.dp))
                Text(trailingText, style = MaterialTheme.typography.bodySmall, color = contentColor.copy(alpha = 0.6f))
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(AppShape.pill)
                .background(trackColor),
        ) {
            if (shown > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(shown)
                        .fillMaxHeight()
                        .clip(AppShape.pill)
                        .background(color),
                )
            }
        }
    }
}

/** Círculo con las iniciales de [name] (avatar de listas de personas). */
@Composable
fun BentoAvatar(
    name: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials(name),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}
