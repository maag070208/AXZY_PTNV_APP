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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.network.AssignmentCreateDto
import com.axzydev.puertonuevoapp.core.network.AssignmentUpdateDto
import com.axzydev.puertonuevoapp.core.network.KanbanAssignmentDto
import com.axzydev.puertonuevoapp.core.network.TicketDto
import com.axzydev.puertonuevoapp.core.network.UserDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.EmptyRow
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import kotlinx.coroutines.launch

private data class KanbanColumn(val status: String, val label: String)

private val columns = listOf(
    KanbanColumn("PENDIENTE", "Pendiente"),
    KanbanColumn("EN_PROGRESO", "En progreso"),
    KanbanColumn("EN_REVISION", "En revisión"),
    KanbanColumn("COMPLETADA", "Completada"),
)

@Composable
fun TicketKanbanScreen(ticketId: String? = null) {
    val scope = rememberCoroutineScope()
    val authState by AppContainer.authRepository.state.collectAsState()
    val role = (authState as? AuthState.LoggedIn)?.user?.role
    val isAdmin = role == "ADMIN"
    val isGerente = role == "GERENTE"
    val isEmpleado = role == "EMPLEADO"
    val canCreate = !isEmpleado
    val canComplete = isAdmin || isGerente

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var rows by remember { mutableStateOf<List<KanbanAssignmentDto>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var departmentFilter by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<KanbanAssignmentDto?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    suspend fun load() {
        loading = true
        error = null
        try {
            rows = AppContainer.ticketsApi.kanban(ticketId).data.filter { it.ticket.deletedAt == null }
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el tablero"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(ticketId) { load() }

    val departmentOptions by remember(rows) {
        derivedStateOf { rows.map { it.ticket.department?.name ?: "General" }.distinct().sorted() }
    }

    val filtered by remember(rows, search, departmentFilter) {
        derivedStateOf {
            rows.filter { r ->
                val deptName = r.ticket.department?.name ?: "General"
                (departmentFilter.isBlank() || deptName == departmentFilter) &&
                    (search.isBlank() || r.title.contains(search, ignoreCase = true) || r.ticket.titulo.contains(search, ignoreCase = true))
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Dashboard, contentDescription = null, tint = AppColors.TextFaint, modifier = Modifier.size(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canCreate) {
                        IconButton(onClick = { showCreate = true }) {
                            Icon(Icons.Filled.Add, contentDescription = "Nueva tarea", tint = AppColors.EmeraldPrimary)
                        }
                    }
                    IconButton(onClick = { scope.launch { load() } }) {
                        Icon(Icons.Filled.Sync, contentDescription = "Actualizar")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Buscar tarea o ticket…") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            if (departmentOptions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chips = listOf("" to "Todos") + departmentOptions.map { it to it }
                    chips.forEach { (value, label) ->
                        val isSelected = departmentFilter == value
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) AppColors.Surface else AppColors.TextMuted,
                            modifier = Modifier
                                .background(if (isSelected) AppColors.EmeraldPrimary else AppColors.SurfaceVariant, RoundedCornerShape(20.dp))
                                .clickable { departmentFilter = value }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        when {
            loading -> LoadingState(modifier = Modifier.weight(1f))
            error != null -> ErrorState(message = error ?: "Error", modifier = Modifier.weight(1f), onRetry = { scope.launch { load() } })
            else -> Row(
                modifier = Modifier.weight(1f).fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                columns.forEach { col ->
                    val colRows = filtered.filter { it.status == col.status }
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
                                    AssignmentRow(row, onClick = { selected = row })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { assignment ->
        AssignmentDetailModal(
            assignment = assignment,
            canComplete = canComplete,
            onDismiss = { selected = null },
            onStatusChange = { newStatus ->
                scope.launch {
                    try {
                        AppContainer.ticketsApi.updateAssignment(assignment.ticketId, assignment.id, AssignmentUpdateDto(status = newStatus))
                        selected = null
                        load()
                    } catch (e: Exception) {
                        error = e.message
                    }
                }
            },
        )
    }

    if (showCreate) {
        CreateAssignmentModal(
            fixedTicketId = ticketId,
            onDismiss = { showCreate = false },
            onCreated = {
                showCreate = false
                scope.launch { load() }
            },
        )
    }
}

@Composable
private fun AssignmentDetailModal(
    assignment: KanbanAssignmentDto,
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
private fun CreateAssignmentModal(
    fixedTicketId: String?,
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var tickets by remember { mutableStateOf<List<TicketDto>>(emptyList()) }
    var employees by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    var ticketId by remember { mutableStateOf(fixedTicketId ?: "") }
    var userId by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            employees = AppContainer.usersApi.empleados()
        } catch (_: Exception) { /* deja el picker vacío si falla */ }
        if (fixedTicketId == null) {
            try {
                tickets = AppContainer.ticketsApi.list().data
            } catch (_: Exception) { }
        }
    }

    fun isoOrNull(dateOnly: String): String? =
        dateOnly.trim().takeIf { it.length == 10 }?.let { "${it}T00:00:00.000Z" }

    fun submit() {
        if (ticketId.isBlank() || userId.isBlank() || title.isBlank() || saving) return
        saving = true
        error = null
        scope.launch {
            try {
                AppContainer.ticketsApi.addAssignment(
                    ticketId,
                    AssignmentCreateDto(
                        userId = userId,
                        title = title.trim(),
                        description = description.trim().ifBlank { null },
                        startDate = isoOrNull(startDate),
                        dueDate = isoOrNull(dueDate),
                    ),
                )
                onCreated()
            } catch (e: Exception) {
                error = e.message ?: "No se pudo asignar la tarea"
            } finally {
                saving = false
            }
        }
    }

    AppModal(
        title = "Nueva tarea",
        onDismiss = onDismiss,
        icon = Icons.Filled.Add,
        confirmLabel = if (saving) "Asignando…" else "Asignar tarea",
        onConfirm = { submit() },
        confirmEnabled = ticketId.isNotBlank() && userId.isNotBlank() && title.isNotBlank(),
        saving = saving,
    ) {
        if (fixedTicketId == null) {
            SimpleDropdownField(
                label = "Ticket",
                value = ticketId,
                options = tickets.map { it.id to it.titulo },
                onSelect = { ticketId = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
        }
        SimpleDropdownField(
            label = "Empleado",
            value = userId,
            options = employees.map { it.id to it.name },
            onSelect = { userId = it },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Título de la tarea") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Descripción") },
            minLines = 2,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = startDate,
                onValueChange = { startDate = it },
                label = { Text("Inicio (AAAA-MM-DD)") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = dueDate,
                onValueChange = { dueDate = it },
                label = { Text("Límite (AAAA-MM-DD)") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.weight(1f),
            )
        }
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error ?: "", color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
        }
    }
}
