package com.axzydev.puertonuevoapp.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.theme.AppColors

data class DonutSegment(val label: String, val value: Int, val color: Color)

/** Dona simple (Canvas) con leyenda — equivalente al DonutChart de la web. */
@Composable
fun DonutChart(
    segments: List<DonutSegment>,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
) {
    val total = segments.sumOf { it.value }
    val stroke = 12.dp

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(size)) {
                val strokePx = stroke.toPx()
                val radius = (this.size.minDimension - strokePx) / 2f
                val topLeft = Offset(
                    (this.size.width - radius * 2f) / 2f,
                    (this.size.height - radius * 2f) / 2f,
                )
                val arcSize = Size(radius * 2f, radius * 2f)
                drawCircle(
                    color = Color(0xFFE8F0F4),
                    radius = radius,
                    style = Stroke(strokePx),
                )
                if (total > 0) {
                    var start = -90f
                    segments.forEach { seg ->
                        if (seg.value > 0) {
                            val sweep = 360f * seg.value / total
                            drawArc(
                                color = seg.color,
                                startAngle = start,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(strokePx),
                            )
                            start += sweep
                        }
                    }
                }
            }
            Text(
                total.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = AppColors.TextPrimary,
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            segments.forEach { seg ->
                val pct = if (total > 0) (seg.value * 100 / total) else 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(color = seg.color)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        seg.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${seg.value} · $pct%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                    )
                }
            }
        }
    }
}
