package com.axzydev.puertonuevoapp.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
) {
    Box(
        modifier = modifier
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
        Box(Modifier.matchParentSize(), contentAlignment = contentAlignment) {
            content()
        }
    }
}