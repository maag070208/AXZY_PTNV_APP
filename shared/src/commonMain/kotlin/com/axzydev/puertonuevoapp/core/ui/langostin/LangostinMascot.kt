package com.axzydev.puertonuevoapp.core.ui.langostin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.util.localMonthDay
import com.axzydev.puertonuevoapp.generated.resources.Res
import com.axzydev.puertonuevoapp.generated.resources.langostin_idle_0
import com.axzydev.puertonuevoapp.generated.resources.langostin_idle_1
import com.axzydev.puertonuevoapp.generated.resources.langostin_idle_2
import com.axzydev.puertonuevoapp.generated.resources.langostin_idle_3
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

/** Milisegundos entre cuadros de la animación `idle` (4 cuadros). */
private const val FrameMillis = 300L

/** Cuadros `idle` rasterizados del sprite 32x32 de la web (2, 6, 10 y 14). */
private const val IdleFrameCount = 4

/**
 * Temporada de Langostín. La primera declarada ([Halloween]) es la temporada
 * vigente al escribir esto (20 oct – 2 nov); el orden no tiene otra semántica.
 *
 * Son sólo decorativas, igual que en la web: no alteran ninguna respuesta ni
 * dato del sistema. Los colores replican `web/src/widgets/assistant/model/seasons.ts`.
 */
enum class LangostinSeason(val emoji: String, val label: String, val ring: Color) {
    Halloween("🎃", "Halloween", Color(0xFFF28C28)),
    Navidad("🎄", "Navidad", Color(0xFFE0342F)),
    Verano("🏖️", "Verano en Baja", Color(0xFF7FD0EB)),
    Amor("💘", "San Valentín", Color(0xFFE0342F)),
    Patrias("🇲🇽", "Fiestas patrias", Color(0xFF3FAE5A)),
    AnioNuevo("🎉", "Año Nuevo", Color(0xFFF2C230)),
    Ninguna("", "", AppColors.Outline),
}

/**
 * Temporada según la fecha local del dispositivo (mismas reglas que la web):
 * Año Nuevo (31 dic y 1 ene), Navidad (diciembre y 1–6 ene), San Valentín
 * (10–14 feb y 10 may), Fiestas patrias (13–16 sep), Halloween y Día de
 * Muertos (20 oct – 2 nov) y Verano en Baja (jun–ago).
 */
fun langostinSeasonNow(): LangostinSeason {
    val (month, day) = localMonthDay()
    return when {
        (month == 12 && day == 31) || (month == 1 && day == 1) -> LangostinSeason.AnioNuevo
        month == 12 || (month == 1 && day <= 6) -> LangostinSeason.Navidad
        (month == 2 && day in 10..14) || (month == 5 && day == 10) -> LangostinSeason.Amor
        month == 9 && day in 13..16 -> LangostinSeason.Patrias
        (month == 10 && day >= 20) || (month == 11 && day <= 2) -> LangostinSeason.Halloween
        month in 6..8 -> LangostinSeason.Verano
        else -> LangostinSeason.Ninguna
    }
}

/**
 * Langostín, la mascota de Puerto Nuevo, en un círculo blanco con sombra suave
 * y anillo del color de la temporada. La imagen (pixel-art 32x32 escalada con
 * filtro NEAREST) ocupa ~88 % del círculo; el badge es el emoji de la temporada.
 *
 * @param modifier modificador externo (un `size` del llamador gana sobre [size]).
 * @param size diámetro del círculo.
 * @param animated `true` cicla los 4 cuadros cada 300 ms; `false` deja el cuadro 0.
 * @param showBadge pinta el emoji de temporada arriba a la derecha.
 */
@Composable
fun LangostinMascot(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    animated: Boolean = true,
    showBadge: Boolean = true,
) {
    val season = remember { langostinSeasonNow() }
    var frame by remember(animated) { mutableIntStateOf(0) }

    LaunchedEffect(animated) {
        if (!animated) return@LaunchedEffect
        while (true) {
            delay(FrameMillis)
            frame = (frame + 1) % IdleFrameCount
        }
    }

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.10f),
                    spotColor = Color.Black.copy(alpha = 0.18f),
                )
                .clip(CircleShape)
                .background(AppColors.Surface)
                .border(1.5.dp, season.ring, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(
                    when (frame) {
                        1 -> Res.drawable.langostin_idle_1
                        2 -> Res.drawable.langostin_idle_2
                        3 -> Res.drawable.langostin_idle_3
                        else -> Res.drawable.langostin_idle_0
                    }
                ),
                contentDescription = "Langostín, la mascota de Puerto Nuevo",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(size * 0.06f),
                contentScale = ContentScale.Fit,
            )
        }
        if (showBadge && season.emoji.isNotEmpty()) {
            Text(
                text = season.emoji,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp),
            )
        }
    }
}
