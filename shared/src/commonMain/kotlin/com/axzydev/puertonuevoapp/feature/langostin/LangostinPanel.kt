package com.axzydev.puertonuevoapp.feature.langostin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.session.SessionUser
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.theme.LocalDarkTheme
import com.axzydev.puertonuevoapp.core.ui.BentoAvatar
import com.axzydev.puertonuevoapp.core.ui.BentoIconButton
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.ui.langostin.LangostinMascot
import com.axzydev.puertonuevoapp.core.ui.langostin.LangostinScene
import com.axzydev.puertonuevoapp.core.ui.langostin.LangostinSceneSpec
import com.axzydev.puertonuevoapp.core.ui.langostin.LangostinSeason
import com.axzydev.puertonuevoapp.core.ui.langostin.langostinSceneFor
import com.axzydev.puertonuevoapp.core.ui.langostin.langostinSeasonNow
import com.axzydev.puertonuevoapp.feature.langostin.model.LangostinAction
import com.axzydev.puertonuevoapp.feature.langostin.model.LangostinHelp
import com.axzydev.puertonuevoapp.feature.langostin.model.LangostinScreen
import com.axzydev.puertonuevoapp.feature.langostin.model.isVisibleFor

/**
 * Mensaje del hilo de Langostín. Lo comparten el panel de ayuda y el alta de ticket.
 */
internal sealed interface LangostinMessage {
    data class Agent(val text: String) : LangostinMessage
    data class User(val text: String) : LangostinMessage
}

/**
 * Panel de Langostín como hoja inferior sobre el shell: scrim + superficie de 93 % de
 * alto con esquinas superiores de 28dp. Arriba el encabezado (escena animada de la
 * temporada con la mascota, píldora de nombre/estado y botones de reiniciar/cerrar) y el
 * chip de contexto; debajo, el hilo de ayuda con las opciones de la pantalla actual
 * ("Ir a…", "En esta pantalla", "Langostín") o el alta de ticket/tarea en chat.
 *
 * Es determinista: solo ofrece lo registrado en [LangostinHelp] y respeta los permisos de
 * la sesión; la API vuelve a validar cada escritura. Al abrirse (o al cambiar de pantalla)
 * saluda una sola vez.
 */
@Composable
fun LangostinOverlay(
    current: Screen,
    user: SessionUser?,
    onNavigate: (Screen) -> Unit,
    onClose: () -> Unit,
    /** Agente que debe abrirse de entrada (globo proactivo o detalle del ticket). */
    initialAgent: LangostinAgentKind? = null,
    /** Solo para el agente de tareas: ticket ya elegido desde su detalle. */
    preselectedTicketId: String? = null,
) {
    val help = remember(current) { LangostinHelp.forScreen(current) }
    val season = remember { langostinSeasonNow() }
    val scene = remember(current, season) { langostinSceneFor(season, current) }
    var sessionKey by remember { mutableIntStateOf(0) }
    // record(current, sessionKey): cambiar de pantalla o reiniciar rearma el panel y
    // vuelve al agente con el que se abrió (el globo de Tickets entra directo al alta).
    var activeAgent by remember(current, sessionKey) { mutableStateOf(initialAgent) }
    var messages by remember { mutableStateOf(emptyList<LangostinMessage>()) }

    LaunchedEffect(current, sessionKey) {
        messages = buildList {
            add(LangostinMessage.Agent(greeting(help, season)))
            if (user?.canCreateTickets == true) {
                add(LangostinMessage.Agent("¿Te levanto un ticket? Te hago cuatro preguntas y lo creo aquí mismo."))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim: tocar fuera cierra el panel.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.38f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose,
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.93f)
                .clip(AppShape.sheetTop)
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
        ) {
            PanelHero(
                season = season,
                scene = scene,
                onReset = { sessionKey += 1 },
                onClose = onClose,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
            ) {
                StatusChip(label = "Estás en: ${help.title}", tone = BentoTone.Sea)
            }
            val showTicketAgent = activeAgent == LangostinAgentKind.TICKET && user?.canCreateTickets == true
            val showTaskAgent = activeAgent == LangostinAgentKind.TASK &&
                user?.canCreateAssignments == true && isTaskAgentScreen(current)
            if (showTicketAgent || showTaskAgent) {
                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    key(sessionKey) {
                        if (showTicketAgent) {
                            TicketChatAgent(
                                user = user,
                                onBack = { activeAgent = null },
                                onViewTicket = { id ->
                                    onClose()
                                    onNavigate(Screen.TicketDetail(id))
                                },
                            )
                        } else {
                            TaskChatAgent(
                                user = user,
                                preselectedTicketId = preselectedTicketId,
                                onBack = { activeAgent = null },
                                onViewTicket = { id ->
                                    onClose()
                                    onNavigate(Screen.TicketDetail(id))
                                },
                            )
                        }
                    }
                }
            } else {
                HelpBody(
                    help = help,
                    current = current,
                    user = user,
                    messages = messages,
                    onAction = { action ->
                        when (action) {
                            is LangostinAction.Answer ->
                                messages = messages +
                                    LangostinMessage.User(action.label) +
                                    LangostinMessage.Agent(action.answer)

                            is LangostinAction.GoTo -> {
                                onClose()
                                onNavigate(action.to)
                            }
                        }
                    },
                    onOpenTicket = { activeAgent = LangostinAgentKind.TICKET },
                    onOpenTask = { activeAgent = LangostinAgentKind.TASK },
                )
            }
        }
    }
}

/** Saludo con el contexto de la pantalla y el ánimo de la temporada. */
private fun greeting(help: LangostinScreen, season: LangostinSeason): String {
    val hello = if (season.emoji.isNotEmpty()) "¡Hola! Soy Langostín ${season.emoji}" else "¡Hola! Soy Langostín"
    return "$hello Estás en ${help.title}. ${help.explain}"
}

/**
 * Hero del panel: escena 8-bit animada de Langostín (la mascota ya viene dibujada dentro,
 * en el centro) con la píldora de nombre/estado arriba a la izquierda y los botones de
 * reiniciar/cerrar arriba a la derecha, los tres con el mismo velo translúcido para no
 * perderse sobre la escena. En oscuro se oscurece un poco la escena para que la cabecera
 * no deslumbre, y el pie se funde con la superficie del panel.
 */
@Composable
private fun PanelHero(
    season: LangostinSeason,
    scene: LangostinSceneSpec,
    onReset: () -> Unit,
    onClose: () -> Unit,
) {
    val dark = LocalDarkTheme.current
    val veil = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    Box(modifier = Modifier.fillMaxWidth().height(176.dp)) {
        LangostinScene(
            scene = scene,
            modifier = Modifier.fillMaxSize(),
        )
        if (dark) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.14f)),
            )
        }
        // Velo inferior para fundir la escena con la superficie del panel.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                ),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .widthIn(max = 208.dp)
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(AppShape.pill)
                    .background(veil)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f), AppShape.pill)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(7.dp).background(AppColors.Success, CircleShape))
                Spacer(Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        "Langostín",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildString {
                            append(scene.label)
                            if (season.emoji.isNotEmpty()) append(" · ${season.emoji} ${season.label}")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(AppShape.pill)
                    .background(veil)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BentoIconButton(
                    icon = Icons.Filled.Refresh,
                    contentDescription = "Reiniciar",
                    onClick = onReset,
                )
                BentoIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = "Cerrar",
                    onClick = onClose,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.HelpBody(
    help: LangostinScreen,
    current: Screen,
    user: SessionUser?,
    messages: List<LangostinMessage>,
    onAction: (LangostinAction) -> Unit,
    onOpenTicket: () -> Unit,
    onOpenTask: () -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    val lastAgentIndex = messages.indexOfLast { it is LangostinMessage.Agent }

    LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(messages) { index, message ->
            LangostinBubble(
                message = message,
                showMascot = message is LangostinMessage.Agent && index == lastAgentIndex,
                authorName = user?.name,
            )
        }
    }

    val actions = help.actions.filter { it.isVisibleFor(user) }
    val goTo = actions.filterIsInstance<LangostinAction.GoTo>()
    val answers = actions.filterIsInstance<LangostinAction.Answer>()
    val canTask = user?.canCreateAssignments == true && isTaskAgentScreen(current)

    // Las tres secciones no deben empujar el hilo fuera de la hoja: si no caben
    // (pantallas con muchas respuestas o teclado abierto) se desplazan en vertical.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
    ) {
        if (goTo.isNotEmpty()) {
            SectionLabel("Ir a…")
            LangostinOptionChips(goTo.map { LangostinChip(it.label, Icons.AutoMirrored.Filled.ArrowForward) { onAction(it) } })
        }
        if (answers.isNotEmpty()) {
            SectionLabel("En esta pantalla")
            LangostinOptionChips(answers.map { LangostinChip(it.label, null) { onAction(it) } })
        }
        if (user?.canCreateTickets == true || canTask) {
            SectionLabel("Langostín")
            LangostinOptionChips(
                buildList {
                    if (user.canCreateTickets) {
                        add(LangostinChip("Nuevo ticket", Icons.Filled.ConfirmationNumber, onOpenTicket))
                    }
                    if (canTask) {
                        add(LangostinChip("Nueva tarea", Icons.Filled.TaskAlt, onOpenTask))
                    }
                },
            )
        }
    }
}

/** Burbuja del hilo: Langostín a la izquierda (mascota en el último mensaje) y la persona a la derecha. */
@Composable
internal fun LangostinBubble(
    message: LangostinMessage,
    showMascot: Boolean,
    authorName: String?,
) {
    when (message) {
        is LangostinMessage.Agent -> Row(verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.width(32.dp)) {
                if (showMascot) LangostinMascot(size = 30.dp, animated = false, showBadge = false)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(AppShape.row)
                    .background(AppBento.field)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        is LangostinMessage.User -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(AppShape.row)
                    .background(AppBento.ink)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppBento.onInk,
                )
            }
            if (authorName != null) {
                Spacer(Modifier.width(8.dp))
                BentoAvatar(
                    name = authorName,
                    color = MaterialTheme.colorScheme.primary,
                    size = 28.dp,
                )
            }
        }
    }
}

/** Opción en píldora del panel de Langostín (ayuda y formularios). */
internal data class LangostinChip(
    val label: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
)

/**
 * Fila de opciones en píldoras, desplazable en horizontal. Lleva un velo degradado en el
 * borde derecho cuando hay más opciones y deja un aire final para que la última píldora
 * no quede recortada ni debajo del velo al llegar al final.
 */
@Composable
internal fun LangostinOptionChips(chips: List<LangostinChip>) {
    val scrollState = rememberScrollState()
    val moreToRight by remember { derivedStateOf { scrollState.canScrollForward } }
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chips.forEach { chip ->
                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 44.dp)
                        .clip(AppShape.pill)
                        .background(AppBento.field)
                        .clickable(onClick = chip.onClick)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (chip.icon != null) {
                        Icon(
                            imageVector = chip.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = chip.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // Aire final: cubre el ancho del velo (32dp) para que la última píldora
            // quede completamente visible al llegar al final del scroll.
            Spacer(Modifier.width(36.dp))
        }
        if (moreToRight) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(32.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.surface,
                            ),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
    )
}

/**
 * El agente de tareas se ofrece donde se ven tareas de varios tickets (tablero y
 * administración de tareas) y también en Tickets: en la app el panel solo abre
 * desde la barra inferior, que esas dos pantallas ocultan, así que sin Tickets el
 * agente quedaría inalcanzable hasta que el shell centralice la apertura. En el
 * detalle del ticket la tarea se crea desde su propio chat.
 */
private fun isTaskAgentScreen(screen: Screen): Boolean =
    screen is Screen.TicketsKanban || screen is Screen.AdminTasks || screen is Screen.TicketsList
