package com.axzydev.puertonuevoapp.feature.tickets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.EmptyRow
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.assignmentStatusLabel
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import com.axzydev.puertonuevoapp.core.ui.AppTextField

@Composable
fun TicketsKanbanScreen(ticketId: String? = null) {
    val viewModel: KanbanViewModel = viewModel(key = "kanban-${ticketId ?: "all"}") {
        KanbanViewModel(ticketId, AppContainer.ticketsApi, AppContainer.usersApi)
    }
    val state by viewModel.uiState.collectAsState()
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canCreate = user?.canCreateTicket == true
    val canComplete = user?.canSeeAdminTasks == true

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Dashboard, contentDescription = null, tint = AppColors.TextFaint, modifier = Modifier.size(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canCreate) {
                        IconButton(onClick = viewModel::openCreate) {
                            Icon(Icons.Filled.Add, contentDescription = "Nueva tarea", tint = AppColors.EmeraldPrimary)
                        }
                    }
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Filled.Sync, contentDescription = "Actualizar")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            AppTextField(
                value = state.search,
                onValueChange = viewModel::onSearchChange,
                placeholder = { Text("Buscar tarea o ticket…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.departmentOptions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chips = listOf("" to "Todos") + state.departmentOptions.map { it to it }
                    chips.forEach { (value, label) ->
                        val isSelected = state.departmentFilter == value
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) AppColors.Surface else AppColors.TextMuted,
                            modifier = Modifier
                                .background(if (isSelected) AppColors.EmeraldPrimary else AppColors.SurfaceVariant, RoundedCornerShape(20.dp))
                                .clickable { viewModel.onDepartmentFilterChange(value) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        when {
            state.loading -> LoadingState(modifier = Modifier.weight(1f))
            state.error != null -> ErrorState(message = state.error ?: "Error", modifier = Modifier.weight(1f), onRetry = viewModel::load)
            else -> Row(
                modifier = Modifier.weight(1f).fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                kanbanColumns.forEach { col ->
                    val colRows = state.filtered.filter { it.status == col.status }
                    Column(
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight()
                            .background(AppColors.SurfaceVariant, RoundedCornerShape(18.dp))
                            .padding(10.dp),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(col.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = AppColors.TextMuted)
                            Box(
                                modifier = Modifier.size(20.dp).background(AppColors.Surface, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) { Text("${colRows.size}", style = MaterialTheme.typography.labelSmall, color = AppColors.TextMuted) }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (colRows.isEmpty()) {
                            EmptyRow("Sin tareas")
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(colRows, key = { it.id }) { row ->
                                    AssignmentRow(row, onClick = { viewModel.select(row.id) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    state.selected?.let { assignment ->
        AssignmentDetailModal(
            assignment = assignment,
            canComplete = canComplete,
            onDismiss = viewModel::dismissSelected,
            onStatusChange = viewModel::onAssignmentStatusChange,
        )
    }

    if (state.showCreate) {
        CreateAssignmentModal(state = state, viewModel = viewModel)
    }
}

@Composable
private fun AssignmentDetailModal(
    assignment: com.axzydev.puertonuevoapp.core.network.tickets.KanbanAssignmentDto,
    canComplete: Boolean,
    onDismiss: () -> Unit,
    onStatusChange: (String) -> Unit,
) {
    AppModal(title = assignment.title, onDismiss = onDismiss, icon = Icons.Filled.Dashboard) {
        Text(assignment.ticket.titulo, style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
        Spacer(Modifier.height(8.dp))
        if (assignment.description.isNotBlank()) {
            Text(assignment.description, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
        }
        Text("Asignada a: ${assignment.user.name}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
        assignment.dueDate?.let {
            Text("Fecha límite: ${formatShortDate(it)}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
        }
        Spacer(Modifier.height(16.dp))
        Text("MOVER A", style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            assignmentStatusOrder.forEach { status ->
                val disabled = status == assignment.status || (status == "COMPLETADA" && !canComplete)
                StatusChip(
                    label = assignmentStatusLabel(status),
                    color = if (disabled) AppColors.TextFaint else assignmentStatusColor(status),
                    modifier = Modifier.clickable(enabled = !disabled) { onStatusChange(status) },
                )
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
        if (state.createTickets.isNotEmpty()) {
            SimpleDropdownField(
                label = "Ticket",
                value = state.createTicketId,
                options = state.createTickets.map { it.id to it.titulo },
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
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextField(
                value = state.createStartDate,
                onValueChange = viewModel::onCreateStartDateChange,
                label = { Text("Inicio (AAAA-MM-DD)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            AppTextField(
                value = state.createDueDate,
                onValueChange = viewModel::onCreateDueDateChange,
                label = { Text("Límite (AAAA-MM-DD)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        if (state.createError != null) {
            Spacer(Modifier.height(10.dp))
            Text(state.createError, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
        }
    }
}
