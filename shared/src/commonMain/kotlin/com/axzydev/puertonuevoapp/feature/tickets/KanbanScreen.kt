package com.axzydev.puertonuevoapp.feature.tickets

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.tickets.KanbanAssignmentDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoAvatar
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoIconButton
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.RegisterTopBarAction
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.AppEvents
import com.axzydev.puertonuevoapp.core.util.assignmentStatusLabel
import com.axzydev.puertonuevoapp.core.util.formatShortDate

/**
 * Tablero kanban de tareas: columnas suaves por estado (con su contador),
 * tarjetas con título, ticket de origen y responsable; tocar una tarjeta abre su
 * detalle para moverla. La acción "Nueva tarea" vive en la barra superior y solo
 * aparece con `tasks.assign`.
 */
@Composable
fun TicketsKanbanScreen(ticketId: String? = null) {
    val viewModel: KanbanViewModel = viewModel(key = "kanban-${ticketId ?: "all"}") {
        KanbanViewModel(ticketId, AppContainer.ticketsApi, AppContainer.usersApi)
    }
    val state by viewModel.uiState.collectAsState()
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canCreate = user?.canCreateAssignments == true
    val navigator = LocalNavigator.current

    if (canCreate) {
        RegisterTopBarAction(Icons.Filled.Add, "Nueva tarea") { viewModel.openCreate() }
    }

    // Una tarea creada desde el panel de Langostín avisa con `tickets:changed`:
    // se recarga solo entonces (no en cada recomposición; la API es la verdad).
    val ticketsVersion by AppEvents.version("tickets:changed").collectAsState()
    LaunchedEffect(ticketsVersion) {
        if (ticketsVersion != null) viewModel.load()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (ticketId == null) "Tareas del equipo por estado." else "Tareas de este ticket por estado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                    modifier = Modifier.weight(1f),
                )
                BentoIconButton(
                    icon = Icons.Filled.Sync,
                    contentDescription = "Actualizar tablero",
                    onClick = viewModel::load,
                )
            }
            Spacer(Modifier.height(10.dp))
            AppSearchField(
                value = state.search,
                onValueChange = viewModel::onSearchChange,
                placeholder = "Buscar tarea o ticket…",
            )
            if (state.departmentOptions.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val chips = listOf("" to "Todos") + state.departmentOptions.map { it to it }
                    chips.forEach { (value, label) ->
                        AppFilterChip(
                            label = label,
                            selected = state.departmentFilter == value,
                            onClick = { viewModel.onDepartmentFilterChange(value) },
                        )
                    }
                }
            }
        }

        when {
            state.loading -> LoadingState(modifier = Modifier.weight(1f))
            state.error != null -> ErrorState(
                message = state.error ?: "No se pudo cargar el tablero",
                modifier = Modifier.weight(1f),
                onRetry = viewModel::load,
            )
            else -> Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                kanbanColumns.forEach { column ->
                    KanbanColumnView(
                        column = column,
                        rows = state.filtered.filter { it.status == column.status },
                        onSelect = viewModel::select,
                    )
                }
            }
        }
    }

    state.selected?.let { assignment ->
        AssignmentDetailModal(
            assignment = assignment,
            allowedMoves = user?.let {
                TicketPermissions.allowedAssignmentMoves(it, assignment.accessInfo(), assignment.userId, assignment.status)
            }.orEmpty(),
            onDismiss = viewModel::dismissSelected,
            onStatusChange = viewModel::onAssignmentStatusChange,
            // Desde el tablero de un ticket ya se viene de su detalle.
            onOpenTicket = if (ticketId == null) {
                {
                    viewModel.dismissSelected()
                    navigator.push(Screen.TicketDetail(assignment.ticketId))
                }
            } else {
                null
            },
        )
    }

    if (state.showCreate) {
        CreateAssignmentModal(state = state, viewModel = viewModel)
    }
}

/** Una columna del tablero: fondo pastel por estado, título y contador. */
@Composable
private fun KanbanColumnView(
    column: KanbanColumn,
    rows: List<KanbanAssignmentDto>,
    onSelect: (String) -> Unit,
) {
    val tone = assignmentStatusTone(column.status)
    val colors = AppBento.tone(tone)
    val container = if (tone == BentoTone.Plain) AppColors.SurfaceVariant else colors.container
    val content = if (tone == BentoTone.Plain) AppColors.TextMuted else colors.content

    Column(
        modifier = Modifier
            .width(296.dp)
            .fillMaxHeight()
            .clip(AppShape.bento)
            .background(container, AppShape.bento)
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                column.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(AppShape.pill)
                    .background(AppColors.Surface, AppShape.pill),
                contentAlignment = Alignment.Center,
            ) {
                Text("${rows.size}", style = MaterialTheme.typography.labelSmall, color = AppColors.TextMuted)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (rows.isEmpty()) {
            Text(
                "Sin tareas",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 14.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp),
            ) {
                items(rows, key = { it.id }) { row ->
                    AssignmentRow(
                        assignment = row,
                        onClick = { onSelect(row.id) },
                        showStatus = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun AssignmentDetailModal(
    assignment: KanbanAssignmentDto,
    allowedMoves: Set<String>,
    onDismiss: () -> Unit,
    onStatusChange: (String) -> Unit,
    onOpenTicket: (() -> Unit)?,
) {
    AppModal(title = assignment.title, onDismiss = onDismiss, icon = Icons.Filled.Dashboard) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BentoAvatar(name = assignment.user.name, color = AppColors.EmeraldPrimary, size = 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    assignment.user.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    assignment.ticket.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusChip(assignmentStatusLabel(assignment.status), assignmentStatusTone(assignment.status))
        }

        if (assignment.description.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                assignment.description,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary,
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            assignment.dueDate?.let { dueDate ->
                Text(
                    "Fecha límite: ${formatShortDate(dueDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextFaint,
                )
            }
            Spacer(Modifier.weight(1f))
            if (assignment.overdue()) {
                StatusChip("Vencida", BentoTone.Rose)
            }
        }

        if (onOpenTicket != null) {
            Spacer(Modifier.height(12.dp))
            PillButton(
                text = "Ver ticket",
                onClick = onOpenTicket,
                variant = PillButtonVariant.Ghost,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("Mover a")
        if (allowedMoves.isEmpty()) {
            Text(
                "No puedes mover esta tarea.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                assignmentStatusOrder.filter { it in allowedMoves }.forEach { status ->
                    AppFilterChip(
                        label = assignmentStatusLabel(status),
                        selected = false,
                        onClick = { onStatusChange(status) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateAssignmentModal(state: KanbanUiState, viewModel: KanbanViewModel) {
    AppModal(
        title = "Nueva tarea",
        onDismiss = viewModel::dismissCreate,
        icon = Icons.Filled.Add,
        confirmLabel = if (state.createSaving) "Asignando…" else "Asignar tarea",
        onConfirm = viewModel::submitCreate,
        confirmEnabled = state.createValid,
        saving = state.createSaving,
    ) {
        SectionLabel("Datos de la tarea")
        if (state.createTickets.isNotEmpty()) {
            SimpleDropdownField(
                label = "Ticket",
                value = state.createTicketId,
                options = state.createTickets.map { it.id to it.title },
                onSelect = viewModel::onCreateTicketChange,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
        }
        SimpleDropdownField(
            label = "Empleado",
            value = state.createUserId,
            options = state.createEmployees.map { it.id to it.name },
            onSelect = viewModel::onCreateUserChange,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = state.createTitle,
            onValueChange = viewModel::onCreateTitleChange,
            label = { Text("Título de la tarea") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = state.createDescription,
            onValueChange = viewModel::onCreateDescriptionChange,
            label = { Text("Descripción") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        // Fechas en una sola columna (en móvil dos campos de fecha lado a lado
        // quedaban apretados y el teclado los tapaba).
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = state.createStartDate,
            onValueChange = viewModel::onCreateStartDateChange,
            label = { Text("Inicio (AAAA-MM-DD)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = state.createDueDate,
            onValueChange = viewModel::onCreateDueDateChange,
            label = { Text("Límite (AAAA-MM-DD)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        state.createError?.let { message ->
            Spacer(Modifier.height(10.dp))
            BentoErrorState(message = message, onRetry = null)
        }
    }
}
