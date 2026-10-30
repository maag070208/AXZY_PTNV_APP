package com.axzydev.puertonuevoapp.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.theme.Brand
import com.axzydev.puertonuevoapp.generated.resources.Res
import com.axzydev.puertonuevoapp.generated.resources.logo_puerto_nuevo
import org.jetbrains.compose.resources.painterResource

/** Logo oficial de Puerto Nuevo cargado desde los recursos compartidos. */
@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    contentDescription: String? = "Puerto Nuevo",
) {
    Image(
        painter = painterResource(Res.drawable.logo_puerto_nuevo),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

/** Insignia blanca redondeada con el logo dentro (para fondos de color). */
@Composable
fun BrandLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    cornerRadius: Dp = 28.dp,
    innerPadding: Dp = 12.dp,
    floating: Boolean = false,
) {
    // Flotado sutil (desplazamiento + escala); no hay API común de "reducir
    // movimiento", por eso la amplitud es mínima y solo se activa con [floating].
    val phase = if (floating) {
        val transition = rememberInfiniteTransition(label = "logoFloat")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "logoFloatPhase",
        )
        animated
    } else {
        0f
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = -6.dp.toPx() * phase
                val scale = 1f + 0.03f * phase
                scaleX = scale
                scaleY = scale
            }
            .size(size)
            .shadow(
                elevation = (size.value / 8f).dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color(0x44000000),
                spotColor = Color(0x44000000),
            )
            .background(Brand.Surface, RoundedCornerShape(cornerRadius))
            .border(1.dp, Brand.Outline.copy(alpha = 0.6f), RoundedCornerShape(cornerRadius))
            .padding(innerPadding),
        contentAlignment = Alignment.Center,
    ) {
        BrandLogo(Modifier.fillMaxSize())
    }
}

/**
 * Encabezado de marca bento: gradiente diagonal primario → primario oscuro
 * con círculos translúcidos decorativos. Va debajo de un [BentoSheet].
 */
@Composable
fun BrandGradientBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val deep = lerp(primary, Color.Black, 0.35f)
    val glow = MaterialTheme.colorScheme.onPrimary
    Box(
        modifier = modifier
            .background(Brush.linearGradient(listOf(primary, deep), start = Offset.Zero, end = Offset(900f, 1400f)))
            .drawBehind {
                drawCircle(glow.copy(alpha = 0.08f), radius = size.width * 0.55f, center = Offset(size.width * 0.95f, size.height * 0.05f))
                drawCircle(glow.copy(alpha = 0.06f), radius = size.width * 0.38f, center = Offset(size.width * 0.02f, size.height * 0.55f))
                drawCircle(glow.copy(alpha = 0.05f), radius = size.width * 0.22f, center = Offset(size.width * 0.78f, size.height * 0.88f))
            },
        content = content,
    )
}

/**
 * Fondo hero de marca: gradiente vertical océano profundo + olas suaves en
 * tonos hielo que "anclan" la cabecera al logo (identidad náutica).
 */
@Composable
fun OceanBackdrop(
    modifier: Modifier = Modifier,
    from: Color = Brand.PrimaryDark,
    to: Color = Brand.Primary,
    contentAlignment: Alignment = Alignment.TopStart,
    showWaves: Boolean = true,
    /**
     * `false` (por defecto): el contenido se estira al tamaño del padre, así que el
     * fondo necesita un tamaño acotado (`fillMaxSize`). `true`: el fondo mide lo que
     * mide su contenido (cabeceras que se ajustan a su alto, p. ej. el hero del drawer).
     */
    wrapContent: Boolean = false,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    Box(modifier = modifier.background(Brush.verticalGradient(listOf(from, to)))) {
        if (showWaves) {
            val tint1 = Brand.IceSoft.copy(alpha = 0.20f)
            val tint2 = Brand.IceMid.copy(alpha = 0.10f)
            Canvas(Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val waveA = Path().apply {
                    moveTo(0f, h * 0.62f)
                    cubicTo(w * 0.25f, h * 0.44f, w * 0.5f, h * 0.82f, w, h * 0.52f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                val waveB = Path().apply {
                    moveTo(0f, h * 0.80f)
                    cubicTo(w * 0.3f, h * 0.62f, w * 0.6f, h * 0.94f, w, h * 0.72f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(waveA, tint1)
                drawPath(waveB, tint2)
                drawCircle(Brand.IceSoft.copy(alpha = 0.12f), radius = w * 0.42f, center = Offset(w * 0.90f, h * 0.05f))
            }
        }
        if (wrapContent) {
            Box(contentAlignment = contentAlignment) { content() }
        } else {
            Box(Modifier.matchParentSize(), contentAlignment = contentAlignment) { content() }
        }
    }
}