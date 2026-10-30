package com.axzydev.puertonuevoapp.core.ui.langostin

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.util.localHourNow
import com.axzydev.puertonuevoapp.generated.resources.Res
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_hotel_0
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_hotel_1
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_hotel_2
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_hotel_3
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_hotel_4
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_hotel_5
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_hotel_6
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_hotel_7
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_navidad_0
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_navidad_1
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_navidad_2
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_navidad_3
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_navidad_4
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_navidad_5
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_navidad_6
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_navidad_7
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_noche_0
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_noche_1
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_noche_2
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_noche_3
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_noche_4
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_noche_5
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_noche_6
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_noche_7
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_playa_0
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_playa_1
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_playa_2
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_playa_3
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_playa_4
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_playa_5
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_playa_6
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_playa_7
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_sistemas_0
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_sistemas_1
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_sistemas_2
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_sistemas_3
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_sistemas_4
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_sistemas_5
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_sistemas_6
import com.axzydev.puertonuevoapp.generated.resources.langostin_scene_sistemas_7
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** Milisegundos entre cuadros de la escena (mismo ritmo que `Escena.tsx` de la web). */
private const val SceneFrameMillis = 140L

/** Cuadros por escena: `f = 0..7` rasterizados a 512x320. */
private const val SceneFrameCount = 8

/** Escena de Langostín: nombre y etiqueta legibles; los cuadros van en `drawable/`. */
enum class LangostinSceneSpec(val scene: String, val label: String) {
    Hotel("hotel", "Hotel"),
    Sistemas("sistemas", "Sistemas"),
    Navidad("navidad", "Navidad"),
    Playa("playa", "Playa"),
    Noche("noche", "Noche"),
}

/**
 * Escena que toca según temporada, hora local y pantalla (en ese orden):
 * Navidad → [LangostinSceneSpec.Navidad]; Verano → [LangostinSceneSpec.Playa];
 * fuera de 6..18 h → [LangostinSceneSpec.Noche]; pantallas de tickets,
 * dispositivos, inventario, cartas, salidas y auditoría → [LangostinSceneSpec.Sistemas];
 * el resto → [LangostinSceneSpec.Hotel].
 */
fun langostinSceneFor(season: LangostinSeason, screen: Screen?): LangostinSceneSpec = when {
    season == LangostinSeason.Navidad -> LangostinSceneSpec.Navidad
    season == LangostinSeason.Verano -> LangostinSceneSpec.Playa
    localHourNow() !in 6..18 -> LangostinSceneSpec.Noche
    screen != null && isBackOfficeScreen(screen) -> LangostinSceneSpec.Sistemas
    else -> LangostinSceneSpec.Hotel
}

/** Pantallas de tickets/inventario/cartas/salidas/auditoría: escena de Sistemas. */
private fun isBackOfficeScreen(screen: Screen): Boolean = when (screen) {
    Screen.TicketsList,
    is Screen.TicketDetail,
    Screen.NewTicket,
    is Screen.EditTicket,
    is Screen.TicketsKanban,
    Screen.DevicesList,
    is Screen.DeviceDetail,
    is Screen.DeviceForm,
    Screen.DeviceTypesList,
    is Screen.DeviceTypeForm,
    Screen.InventoryIndex,
    Screen.InventoryMovements,
    is Screen.NewInventoryMovement,
    Screen.CustodyLettersList,
    is Screen.CustodyLetterDetail,
    is Screen.CustodyLetterForm,
    Screen.MaterialOutputsList,
    is Screen.MaterialOutputForm,
    Screen.AuditLogs -> true

    else -> false
}

/**
 * Escena 8-bit de Langostín: el fondo de la pantalla con la mascota ya incrustada
 * (los PNG salen de `drawScene` de la web, 64x40 escalados 8x a 512x320).
 *
 * @param modifier modificador externo; conviene fijarle el tamaño o un `aspectRatio(64f/40f)`.
 * @param animated `true` cicla los 8 cuadros cada 140 ms; `false` deja el cuadro 0.
 */
@Composable
fun LangostinScene(
    scene: LangostinSceneSpec,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    var frame by remember(animated) { mutableIntStateOf(0) }

    LaunchedEffect(animated) {
        if (!animated) return@LaunchedEffect
        while (true) {
            delay(SceneFrameMillis)
            frame = (frame + 1) % SceneFrameCount
        }
    }

    Image(
        painter = painterResource(sceneFrame(scene, frame)),
        contentDescription = "Escena de Langostín: ${scene.label}",
        modifier = modifier,
        contentScale = ContentScale.Crop,
    )
}

/** Cuadro [frame] (0..7) de [scene]; cualquier otro índice cae al cuadro 0. */
private fun sceneFrame(scene: LangostinSceneSpec, frame: Int): DrawableResource = when (scene) {
    LangostinSceneSpec.Hotel -> when (frame) {
        1 -> Res.drawable.langostin_scene_hotel_1
        2 -> Res.drawable.langostin_scene_hotel_2
        3 -> Res.drawable.langostin_scene_hotel_3
        4 -> Res.drawable.langostin_scene_hotel_4
        5 -> Res.drawable.langostin_scene_hotel_5
        6 -> Res.drawable.langostin_scene_hotel_6
        7 -> Res.drawable.langostin_scene_hotel_7
        else -> Res.drawable.langostin_scene_hotel_0
    }

    LangostinSceneSpec.Sistemas -> when (frame) {
        1 -> Res.drawable.langostin_scene_sistemas_1
        2 -> Res.drawable.langostin_scene_sistemas_2
        3 -> Res.drawable.langostin_scene_sistemas_3
        4 -> Res.drawable.langostin_scene_sistemas_4
        5 -> Res.drawable.langostin_scene_sistemas_5
        6 -> Res.drawable.langostin_scene_sistemas_6
        7 -> Res.drawable.langostin_scene_sistemas_7
        else -> Res.drawable.langostin_scene_sistemas_0
    }

    LangostinSceneSpec.Navidad -> when (frame) {
        1 -> Res.drawable.langostin_scene_navidad_1
        2 -> Res.drawable.langostin_scene_navidad_2
        3 -> Res.drawable.langostin_scene_navidad_3
        4 -> Res.drawable.langostin_scene_navidad_4
        5 -> Res.drawable.langostin_scene_navidad_5
        6 -> Res.drawable.langostin_scene_navidad_6
        7 -> Res.drawable.langostin_scene_navidad_7
        else -> Res.drawable.langostin_scene_navidad_0
    }

    LangostinSceneSpec.Playa -> when (frame) {
        1 -> Res.drawable.langostin_scene_playa_1
        2 -> Res.drawable.langostin_scene_playa_2
        3 -> Res.drawable.langostin_scene_playa_3
        4 -> Res.drawable.langostin_scene_playa_4
        5 -> Res.drawable.langostin_scene_playa_5
        6 -> Res.drawable.langostin_scene_playa_6
        7 -> Res.drawable.langostin_scene_playa_7
        else -> Res.drawable.langostin_scene_playa_0
    }

    LangostinSceneSpec.Noche -> when (frame) {
        1 -> Res.drawable.langostin_scene_noche_1
        2 -> Res.drawable.langostin_scene_noche_2
        3 -> Res.drawable.langostin_scene_noche_3
        4 -> Res.drawable.langostin_scene_noche_4
        5 -> Res.drawable.langostin_scene_noche_5
        6 -> Res.drawable.langostin_scene_noche_6
        7 -> Res.drawable.langostin_scene_noche_7
        else -> Res.drawable.langostin_scene_noche_0
    }
}
