package com.axzydev.puertonuevoapp.feature.langostin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.axzydev.puertonuevoapp.core.network.tickets.TicketCategoryRefDto
import com.axzydev.puertonuevoapp.core.network.tickets.TicketCreateInput
import com.axzydev.puertonuevoapp.core.session.SessionUser
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoIconButton
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.util.currentTimeMillis
import com.axzydev.puertonuevoapp.feature.tickets.ticketPriorityOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.StatusChip

/** El loader de envío se queda un mínimo en pantalla, como en la web. */
private const val MIN_LOADER_MS = 700L

private const val TOTAL_STEPS = 4

private enum class TicketStep {
    Title,
    Description,
    Category,
    Priority,
    Confirm,
    Sending,
    Done,
    Cancelled,
    Error,
}

/**
 * Alta de ticket dentro del panel de Langostín: un chat de una columna con los cuatro datos del
 * ticket (asunto, detalle, categoría y urgencia), confirmación y envío con loader mínimo. Crea el
 * ticket con el mismo endpoint que la pantalla de alta (`POST /tickets`); la API vuelve a validar.
 */
@Composable
fun TicketChatAgent(
    user: SessionUser?,
    onBack: () -> Unit,
    onViewTicket: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var step by remember { mutableStateOf(TicketStep.Title) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf("") }
    var categoryName by remember { mutableStateOf("Sin categoría") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var priorityLabel by remember { mutableStateOf(priorityLabelOf("MEDIUM")) }
    var draft by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf(emptyList<TicketCategoryRefDto>()) }
    var ticketId by remember { mutableStateOf<String?>(null) }
    var messages by remember {
        mutableStateOf(
            listOf<LangostinMessage>(
                LangostinMessage.Agent("Vamos con el ticket. ¿Qué está pasando? Escríbeme el asunto en una línea."),
            ),
        )
    }

    LaunchedEffect(Unit) {
        categories = runCatching { AppContainer.ticketsApi.categories() }.getOrDefault(emptyList())
    }
    LaunchedEffect(messages.size, step) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    fun sendDraft() {
        val value = draft.trim()
        if (value.isEmpty()) return
        draft = ""
        when (step) {
            TicketStep.Title -> {
                title = value
                messages = messages + LangostinMessage.User(value) +
                    LangostinMessage.Agent("Perfecto. Ahora cuéntame el detalle: qué equipo es, dónde está y desde cuándo.")
                step = TicketStep.Description
            }

            TicketStep.Description -> {
                description = value
                messages = messages + LangostinMessage.User(value) +
                    LangostinMessage.Agent("Entendido. ¿De qué categoría es? Si no aplica, elige «Sin categoría».")
                step = TicketStep.Category
            }

            else -> Unit
        }
    }

    fun chooseCategory(id: String, name: String) {
        categoryId = id
        categoryName = name
        messages = messages + LangostinMessage.User(name) +
            LangostinMessage.Agent("¿Qué tan urgente es?")
        step = TicketStep.Priority
    }

    fun choosePriority(code: String, label: String) {
        priority = code
        priorityLabel = label
        messages = messages + LangostinMessage.User(label) + LangostinMessage.Agent(
            buildString {
                append("Reviso: «$title»\n")
                append("Categoría: $categoryName · Urgencia: $label\n\n")
                append("¿Lo creo?")
            },
        )
        step = TicketStep.Confirm
    }

    fun cancel() {
        messages = messages + LangostinMessage.Agent("Va, no lo creo. Si cambias de idea, aquí estoy.")
        step = TicketStep.Cancelled
    }

    fun createTicket() {
        if (step == TicketStep.Sending) return
        step = TicketStep.Sending
        messages = messages + LangostinMessage.Agent("Va, lo estoy registrando…")
        scope.launch {
            val startedAt = currentTimeMillis()
            val result = runCatching {
                AppContainer.ticketsApi.create(
                    TicketCreateInput(
                        title = title.trim(),
                        description = description.trim(),
                        priority = priority,
                        categoryId = categoryId.ifBlank { null },
                    ),
                )
            }
            val elapsed = currentTimeMillis() - startedAt
            if (elapsed < MIN_LOADER_MS) delay(MIN_LOADER_MS - elapsed)
            result.onSuccess { ticket ->
                ticketId = ticket.id
                messages = messages +
                    LangostinMessage.Agent("¡Listo! Tu ticket «${ticket.title}» ya entró. ¿Lo abrimos?")
                step = TicketStep.Done
            }.onFailure { error ->
                messages = messages + LangostinMessage.Agent(
                    "No pude crear el ticket: ${networkMessage(error)}",
                )
                step = TicketStep.Error
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        AgentHeader(step = step, onBack = onBack)

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
                TicketStep.Title, TicketStep.Description -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    AppTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(if (step == TicketStep.Title) "Asunto del ticket" else "Detalle del problema")
                        },
                        singleLine = step == TicketStep.Title,
                        maxLines = if (step == TicketStep.Title) 1 else 3,
                        minLines = 1,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSend = { sendDraft() },
                        ),
                        testTag = "langostin.input",
                    )
                    Spacer(Modifier.width(10.dp))
                    PillButton(
                        text = "Enviar",
                        onClick = { sendDraft() },
                        enabled = draft.isNotBlank(),
                    )
                }

                TicketStep.Category -> {
                    StepLabel("¿De qué categoría es?")
                    LangostinOptionChips(
                        (listOf<Pair<String, String>>("" to "Sin categoría") + categories.map { it.id to it.name })
                            .map { (id, name) -> LangostinChip(name) { chooseCategory(id, name) } },
                    )
                }

                TicketStep.Priority -> {
                    StepLabel("¿Qué tan urgente es?")
                    LangostinOptionChips(
                        ticketPriorityOptions.map { (code, label) ->
                            LangostinChip(label) { choosePriority(code, label) }
                        },
                    )
                }

                TicketStep.Confirm -> {
                    StepLabel("Revisa y confirma")
                    TicketSummaryCard(
                        title = title,
                        rows = listOf(
                            "Categoría" to categoryName,
                            "Urgencia" to priorityLabel,
                        ),
                    )
                    Spacer(Modifier.height(10.dp))
                    LangostinOptionChips(
                        listOf(
                            LangostinChip("Crear ticket", onClick = { createTicket() }),
                            LangostinChip("Cancelar", onClick = { cancel() }),
                        ),
                    )
                }

                TicketStep.Sending -> Row(
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

                TicketStep.Done -> Column {
                    StatusChip(label = "Ticket creado", tone = BentoTone.Mint)
                    Spacer(Modifier.height(10.dp))
                    LangostinOptionChips(
                        buildList {
                            ticketId?.let { id ->
                                add(LangostinChip("Ver ticket") { onViewTicket(id) })
                            }
                            add(LangostinChip("Cerrar", onClick = onBack))
                        },
                    )
                }

                TicketStep.Cancelled -> Column {
                    StatusChip(label = "Cancelado", tone = BentoTone.Plain)
                    Spacer(Modifier.height(10.dp))
                    LangostinOptionChips(
                        listOf(LangostinChip("Cerrar", onClick = onBack)),
                    )
                }

                TicketStep.Error -> Column {
                    StatusChip(label = "No se pudo crear el ticket", tone = BentoTone.Rose)
                    Spacer(Modifier.height(10.dp))
                    LangostinOptionChips(
                        listOf(
                            LangostinChip("Reintentar", onClick = { createTicket() }),
                            LangostinChip("Cancelar", onClick = { cancel() }),
                        ),
                    )
                }
            }
        }
    }
}

/** Barra del formulario dentro del panel: ← volver, nombre del formulario y paso. */
@Composable
private fun AgentHeader(step: TicketStep, onBack: () -> Unit) {
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
                "Nuevo ticket",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            StepProgress(step = step)
            Spacer(Modifier.height(3.dp))
            Text(
                stepLabel(step),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StepLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/**
 * Progreso del alta (4 segmentos): llenos los pasos ya capturados. Ayuda a leer
 * "dónde voy" sin depender solo del texto del encabezado.
 */
@Composable
private fun StepProgress(step: TicketStep) {
    val done = when (step) {
        TicketStep.Title -> 1
        TicketStep.Description -> 2
        TicketStep.Category -> 3
        TicketStep.Priority, TicketStep.Confirm -> 4
        else -> 4
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(TOTAL_STEPS) { index ->
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

/** Resumen de lo capturado: da jerarquía a la confirmación antes de crear. */
@Composable
private fun TicketSummaryCard(title: String, rows: List<Pair<String, String>>) {
    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        rows.forEach { (label, value) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(86.dp),
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

private fun stepLabel(step: TicketStep): String = when (step) {
    TicketStep.Title -> "Paso 1 de $TOTAL_STEPS · asunto"
    TicketStep.Description -> "Paso 2 de $TOTAL_STEPS · detalle"
    TicketStep.Category -> "Paso 3 de $TOTAL_STEPS · categoría"
    TicketStep.Priority -> "Paso 4 de $TOTAL_STEPS · urgencia"
    TicketStep.Confirm -> "Confirmación"
    TicketStep.Sending -> "Enviando"
    TicketStep.Done -> "Listo"
    TicketStep.Cancelled -> "Cancelado"
    TicketStep.Error -> "Error"
}

private fun priorityLabelOf(code: String): String =
    ticketPriorityOptions.firstOrNull { it.first == code }?.second ?: code
