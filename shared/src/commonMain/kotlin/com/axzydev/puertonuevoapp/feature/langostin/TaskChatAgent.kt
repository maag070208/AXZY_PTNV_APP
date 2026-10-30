package com.axzydev.puertonuevoapp.feature.langostin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.tickets.TicketAssignmentCreateInput
import com.axzydev.puertonuevoapp.core.network.tickets.TicketDto
import com.axzydev.puertonuevoapp.core.network.users.UserDto
import com.axzydev.puertonuevoapp.core.session.SessionUser
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoIconButton
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.AppEvents
import com.axzydev.puertonuevoapp.core.util.currentTimeMillis
import com.axzydev.puertonuevoapp.core.util.localOffsetMillis
import com.axzydev.puertonuevoapp.core.util.ticketStatusLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import androidx.compose.foundation.layout.height

/** El loader de envío se queda un mínimo en pantalla, como en el alta de ticket. */
private const val MIN_TASK_LOADER_MS = 700L

private const val DAY_MILLIS = 86_400_000L

private enum class TaskStep {
    Ticket,
    Who,
    What,
    Notes,
    When,
    Confirm,
    Sending,
    Done,
    Error,
    Cancelled,
}

/**
 * Agente **Nueva tarea** del panel de Langostín: el mismo chat de una columna
 * que el alta de ticket, pero para armar una tarea de un ticket.
 *
 * Pasos: ticket (buscador; se salta si el panel llega desde el detalle del
 * ticket) → persona (búsqueda de empleados) → qué hay que hacer → detalles
 * opcionales → para cuándo (píldoras) → confirmación. Al confirmar manda
 * `POST /tickets/:id/assignments` (`TicketsApi.createAssignment`) con loader y
 * aviso; la API vuelve a validar el alcance y el permiso `tasks.assign`.
 */
@Composable
fun TaskChatAgent(
    user: SessionUser?,
    preselectedTicketId: String? = null,
    onBack: () -> Unit,
    onViewTicket: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val steps = remember(preselectedTicketId) {
        if (preselectedTicketId == null) {
            listOf(TaskStep.Ticket, TaskStep.Who, TaskStep.What, TaskStep.Notes, TaskStep.When, TaskStep.Confirm)
        } else {
            listOf(TaskStep.Who, TaskStep.What, TaskStep.Notes, TaskStep.When, TaskStep.Confirm)
        }
    }

    var step by remember { mutableStateOf(if (preselectedTicketId == null) TaskStep.Ticket else TaskStep.Who) }
    var tickets by remember { mutableStateOf(emptyList<TicketDto>()) }
    var loadingTickets by remember { mutableStateOf(preselectedTicketId == null) }
    var people by remember { mutableStateOf(emptyList<UserDto>()) }
    var loadingPeople by remember { mutableStateOf(true) }

    var ticketQuery by remember { mutableStateOf("") }
    var personQuery by remember { mutableStateOf("") }
    var titleDraft by remember { mutableStateOf("") }
    var notesDraft by remember { mutableStateOf("") }

    var ticketId by remember { mutableStateOf(preselectedTicketId.orEmpty()) }
    var ticketName by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var personName by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var dueLabel by remember { mutableStateOf("Sin fecha") }

    var messages by remember {
        mutableStateOf(
            listOf<LangostinMessage>(
                LangostinMessage.Agent("Va, armamos la tarea. Te hago cuatro preguntas y la dejo lista."),
            ),
        )
    }

    // Personas que se pueden encargar (cualquier sesión puede listarlas).
    LaunchedEffect(Unit) {
        people = runCatching { AppContainer.usersApi.employees() }.getOrDefault(emptyList())
        loadingPeople = false
    }

    // Tickets del paso 1: la lista ya respeta el alcance de la sesión.
    LaunchedEffect(preselectedTicketId) {
        if (preselectedTicketId != null) {
            val ticket = runCatching { AppContainer.ticketsApi.get(preselectedTicketId) }.getOrNull()
            if (ticket != null) {
                ticketName = ticket.title
                messages = messages + LangostinMessage.Agent(
                    "Vamos con una tarea del ticket «${ticket.title}». ¿Quién la hace?",
                )
            } else {
                messages = messages + LangostinMessage.Agent("¿Quién la hace?")
            }
            return@LaunchedEffect
        }
        tickets = runCatching { AppContainer.ticketsApi.list().data }
            .getOrDefault(emptyList())
            .filter { it.deletedAt == null }
        loadingTickets = false
        messages = messages + LangostinMessage.Agent("Primero, ¿de qué ticket es la tarea?")
    }

    LaunchedEffect(messages.size, step) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    fun say(text: String) {
        messages = messages + LangostinMessage.Agent(text)
    }

    fun sayUser(text: String) {
        messages = messages + LangostinMessage.User(text)
    }

    fun pickTicket(ticket: TicketDto) {
        ticketId = ticket.id
        ticketName = ticket.title
        ticketQuery = ""
        sayUser(ticket.title)
        say("Anotado. ¿Quién la hace?")
        step = TaskStep.Who
    }

    fun pickPerson(person: UserDto) {
        userId = person.id
        personName = person.name
        personQuery = ""
        sayUser(person.name)
        say("¿Qué hay que hacer? Escríbelo en una línea.")
        step = TaskStep.What
    }

    fun sendTitle() {
        val value = titleDraft.trim()
        if (value.length < 3) return
        title = value
        titleDraft = ""
        sayUser(value)
        say("¿Algún detalle extra? Si no hace falta, dime «sin detalles».")
        step = TaskStep.Notes
    }

    fun sendNotes(value: String) {
        description = value.trim()
        notesDraft = ""
        sayUser(description.ifBlank { "Sin detalles" })
        say("¿Para cuándo?")
        step = TaskStep.When
    }

    fun pickWhen(label: String, due: String) {
        dueDate = due
        dueLabel = label
        sayUser(label)
        say(
            buildString {
                append("Reviso: «$title»\n")
                append("Se la encargo a $personName" + (if (ticketName.isNotBlank()) " en «$ticketName»" else "") + ".\n")
                append("Para: $label\n\n")
                append("¿La creo?")
            },
        )
        step = TaskStep.Confirm
    }

    fun cancel() {
        say("Va, no la creo. Si cambias de idea, aquí estoy.")
        step = TaskStep.Cancelled
    }

    fun createTask() {
        if (step == TaskStep.Sending) return
        if (ticketId.isBlank() || userId.isBlank() || title.isBlank()) {
            say("Me falta un dato para crearla (ticket, persona o tarea). Empecemos de nuevo.")
            step = TaskStep.Error
            return
        }
        step = TaskStep.Sending
        say("Va, la estoy registrando…")
        scope.launch {
            val startedAt = currentTimeMillis()
            val today = localTaskDateKey(0)
            val result = runCatching {
                AppContainer.ticketsApi.createAssignment(
                    ticketId,
                    TicketAssignmentCreateInput(
                        userId = userId,
                        title = title,
                        description = description.ifBlank { null },
                        startDate = today,
                        dueDate = dueDate.ifBlank { null },
                    ),
                )
            }
            val elapsed = currentTimeMillis() - startedAt
            if (elapsed < MIN_TASK_LOADER_MS) delay(MIN_TASK_LOADER_MS - elapsed)
            result
                .onSuccess {
                    // Aviso local: el tablero y la administración de tareas recargan
                    // al escuchar `tickets:changed` (la API sigue siendo la verdad).
                    AppEvents.emit("task:created")
                    AppEvents.emit("tickets:changed")
                    say("¡Listo! La tarea «$title» ya quedó con $personName.")
                    step = TaskStep.Done
                }
                .onFailure { error ->
                    say("No pude crear la tarea: ${networkMessage(error)}")
                    step = TaskStep.Error
                }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        TaskAgentHeader(step = step, steps = steps, ticketName = ticketName, onBack = onBack)

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val lastAgentIndex = messages.indexOfLast { it is LangostinMessage.Agent }
            itemsIndexed(messages) { index, message ->
                LangostinBubble(
                    message = message,
                    showMascot = message is LangostinMessage.Agent && index == lastAgentIndex,
                    authorName = user?.name,
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            when (step) {
                TaskStep.Ticket -> {
                    ChoiceSearch(
                        value = ticketQuery,
                        onValueChange = { ticketQuery = it },
                        placeholder = "Buscar ticket por título…",
                        loading = loadingTickets,
                        emptyLabel = "No hay tickets que coincidan.",
                        options = tickets
                            .filter {
                                ticketQuery.isBlank() ||
                                    it.title.lowercase().contains(ticketQuery.trim().lowercase())
                            }
                            .take(40),
                        label = { it.title },
                        badge = { ticketStatusLabel(it.status) },
                        badgeTone = { ticketStatusTone(it.status) },
                        onPick = ::pickTicket,
                    )
                }

                TaskStep.Who -> {
                    ChoiceSearch(
                        value = personQuery,
                        onValueChange = { personQuery = it },
                        placeholder = "Buscar persona por nombre o número…",
                        loading = loadingPeople,
                        emptyLabel = "No hay personas que coincidan.",
                        options = people
                            .filter {
                                val term = personQuery.trim().lowercase()
                                term.isBlank() ||
                                    it.name.lowercase().contains(term) ||
                                    (it.employeeNumber ?: "").lowercase().contains(term)
                            }
                            .take(40),
                        label = { it.name },
                        detail = { listOfNotNull(it.employeeNumber?.let { n -> "#$n" }, it.department?.name).joinToString(" · ") },
                        onPick = ::pickPerson,
                    )
                }

                TaskStep.What -> Column {
                    AppTextField(
                        value = titleDraft,
                        onValueChange = { titleDraft = it.take(120) },
                        placeholder = { Text("Ej. Revisar la bomba del cuarto 4") },
                        singleLine = false,
                        maxLines = 3,
                        minLines = 1,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { sendTitle() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        PillButton(
                            text = "Continuar",
                            onClick = { sendTitle() },
                            enabled = titleDraft.trim().length >= 3,
                        )
                    }
                }

                TaskStep.Notes -> Column {
                    AppTextField(
                        value = notesDraft,
                        onValueChange = { notesDraft = it.take(500) },
                        placeholder = { Text("Detalles, herramientas, dónde está…") },
                        singleLine = false,
                        maxLines = 4,
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                        PillButton(
                            text = "Sin detalles",
                            onClick = { sendNotes("") },
                            variant = PillButtonVariant.Secondary,
                        )
                        Spacer(Modifier.width(8.dp))
                        PillButton(
                            text = "Continuar",
                            onClick = { sendNotes(notesDraft) },
                            enabled = notesDraft.trim().isNotEmpty(),
                        )
                    }
                }

                TaskStep.When -> {
                    TaskStepLabel("¿Para cuándo?")
                    LangostinOptionChips(
                        listOf(
                            LangostinChip("Hoy") { pickWhen("Hoy", localTaskDateKey(0)) },
                            LangostinChip("Mañana") { pickWhen("Mañana", localTaskDateKey(1)) },
                            LangostinChip("3 días") { pickWhen("En 3 días", localTaskDateKey(3)) },
                            LangostinChip("Una semana") { pickWhen("En una semana", localTaskDateKey(7)) },
                            LangostinChip("Sin fecha") { pickWhen("Sin fecha", "") },
                        ),
                    )
                }

                TaskStep.Confirm -> {
                    TaskStepLabel("Revisa y confirma")
                    TaskSummaryCard(
                        rows = buildList {
                            if (ticketName.isNotBlank()) add("Ticket" to ticketName)
                            add("Se la encargo a" to personName)
                            add("Tarea" to title)
                            if (description.isNotBlank()) add("Detalles" to description)
                            add("Para" to dueLabel)
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    LangostinOptionChips(
                        listOf(
                            LangostinChip("Crear tarea") { createTask() },
                            LangostinChip("Cancelar") { cancel() },
                        ),
                    )
                }

                TaskStep.Sending -> Row(
                    modifier = Modifier.heightIn(min = 44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Enviando…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                TaskStep.Done -> Column {
                    StatusChip(label = "Tarea creada", tone = BentoTone.Mint)
                    Spacer(Modifier.height(10.dp))
                    LangostinOptionChips(
                        listOf(
                            LangostinChip("Ver ticket") { onViewTicket(ticketId) },
                            LangostinChip("Cerrar") { onBack() },
                        ),
                    )
                }

                TaskStep.Cancelled -> Column {
                    StatusChip(label = "Cancelado", tone = BentoTone.Plain)
                    Spacer(Modifier.height(10.dp))
                    LangostinOptionChips(
                        listOf(LangostinChip("Cerrar") { onBack() }),
                    )
                }

                TaskStep.Error -> Column {
                    StatusChip(label = "No se pudo crear la tarea", tone = BentoTone.Rose)
                    Spacer(Modifier.height(10.dp))
                    LangostinOptionChips(
                        listOf(
                            LangostinChip("Reintentar") { createTask() },
                            LangostinChip("Cancelar") { cancel() },
                        ),
                    )
                }
            }
        }
    }
}

/** Buscador + lista de opciones con scroll propio (tickets o personas). */
@Composable
private fun <T> ChoiceSearch(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    loading: Boolean,
    emptyLabel: String,
    options: List<T>,
    label: (T) -> String,
    onPick: (T) -> Unit,
    detail: (T) -> String = { "" },
    badge: (T) -> String? = { null },
    badgeTone: (T) -> BentoTone = { BentoTone.Plain },
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.width(8.dp))
    when {
        loading -> Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Cargando…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        options.isEmpty() -> Text(
            emptyLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )

        else -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 208.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { option ->
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onPick(option) },
                    borderColor = AppColors.Outline.copy(alpha = 0.5f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                label(option),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val meta = detail(option)
                            if (meta.isNotBlank()) {
                                Text(
                                    meta,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        badge(option)?.let { text ->
                            StatusChip(label = text, tone = badgeTone(option))
                        }
                    }
                }
            }
        }
    }
}

/** Barra del formulario: ← volver, nombre del formulario y paso actual. */
@Composable
private fun TaskAgentHeader(
    step: TaskStep,
    steps: List<TaskStep>,
    ticketName: String,
    onBack: () -> Unit,
) {
    val current = steps.indexOf(step).takeIf { it >= 0 }?.plus(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BentoIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Volver a la ayuda",
            onClick = onBack,
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Nueva tarea",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            TaskProgress(current = current, total = steps.size)
            Spacer(Modifier.height(3.dp))
            Text(
                buildString {
                    if (current != null) append("Paso $current de ${steps.size}")
                    else append(taskStepStateLabel(step))
                    if (ticketName.isNotBlank()) append(" · $ticketName")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TaskStepLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/** Progreso del formulario de tarea (un segmento por paso, llenos los ya capturados). */
@Composable
private fun TaskProgress(current: Int?, total: Int) {
    val done = current ?: total
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(4.dp)
                    .clip(AppShape.pill)
                    .background(
                        if (index < done) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    ),
            )
        }
    }
}

/** Resumen de la tarea antes de crearla: ticket, persona, qué y para cuándo. */
@Composable
private fun TaskSummaryCard(rows: List<Pair<String, String>>) {
    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        rows.forEach { (label, value) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(104.dp),
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Tono del estado del ticket en las opciones del buscador (mismo significado que en Tickets). */
private fun ticketStatusTone(status: String): BentoTone = when (status) {
    "CLOSED", "RESOLVED", "CANCELLED" -> BentoTone.Mint
    "IN_PROGRESS" -> BentoTone.Sea
    "OPEN" -> BentoTone.Sun
    else -> BentoTone.Plain
}

private fun taskStepStateLabel(step: TaskStep): String = when (step) {
    TaskStep.Confirm -> "Confirmación"
    TaskStep.Sending -> "Enviando"
    TaskStep.Done -> "Listo"
    TaskStep.Cancelled -> "Cancelado"
    TaskStep.Error -> "Error"
    else -> ""
}

/**
 * Fecha local `YYYY-MM-DD` de hoy + [daysFromToday] (sin correr a UTC), para
 * `startDate` y `dueDate` de la tarea.
 */
private fun localTaskDateKey(daysFromToday: Int): String {
    val millis = currentTimeMillis()
    val localDay = floorDiv(millis + localOffsetMillis(millis), DAY_MILLIS) + daysFromToday
    return civilDateKey(localDay)
}

private fun civilDateKey(epochDay: Long): String {
    var z = epochDay + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val year = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val day = doy - (153 * mp + 2) / 5 + 1
    val month = if (mp < 10) mp + 3 else mp - 9
    val y = if (month <= 2) year + 1 else year
    val mm = month.toString().padStart(2, '0')
    val dd = day.toString().padStart(2, '0')
    return "${y.toString().padStart(4, '0')}-$mm-$dd"
}

private fun floorDiv(value: Long, divisor: Long): Long {
    val q = value / divisor
    return if (value % divisor != 0L && (value xor divisor) < 0) q - 1 else q
}
