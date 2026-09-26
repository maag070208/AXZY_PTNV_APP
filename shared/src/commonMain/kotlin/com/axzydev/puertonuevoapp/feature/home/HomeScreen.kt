package com.axzydev.puertonuevoapp.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.dashboard.DashboardActivityDto
import com.axzydev.puertonuevoapp.core.network.dashboard.DashboardSummaryDto
import com.axzydev.puertonuevoapp.core.network.tickets.KanbanAssignmentDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.SessionUser
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.ui.AppActionCard
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.StatCard
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatDateTime
import com.axzydev.puertonuevoapp.core.util.assignmentStatusLabel
import com.axzydev.puertonuevoapp.core.util.roleLabel
import com.axzydev.puertonuevoapp.core.util.ticketPriorityLabel
import com.axzydev.puertonuevoapp.feature.tickets.AssignmentRow
import com.axzydev.puertonuevoapp.feature.tickets.assignmentStatusColor

private val AGENT_COLORS = listOf(
    Color(0xFF6366F1), Color(0xFF0EA5E9), Color(0xFF10B981),
    Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF8B5CF6),
)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel {
        HomeViewModel(AppContainer.authRepository, AppContainer.ticketsApi, AppContainer.dashboardApi)
    },
) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user

    HomeContent(
        state = state,
        user = user,
        onRetry = viewModel::load,
        onNavigate = { navigator.push(it) },
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    user: SessionUser?,
    onRetry: () -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        if (user != null) {
            Text("Hola, ${user.name.substringBefore(' ')}", style = MaterialTheme.typography.titleLarge, color = AppColors.TextPrimary)
            Text(roleLabel(user.role), style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
            Spacer(Modifier.height(14.dp))
        }
        CredentialShortcut(onClick = { onNavigate(Screen.MyCredential) })
        Spacer(Modifier.height(20.dp))

        when {
            state.loading -> LoadingState(modifier = Modifier.fillMaxWidth().height(200.dp))
            state.error != null -> ErrorState(
                message = state.error ?: "Error",
                modifier = Modifier.fillMaxWidth().height(200.dp),
                onRetry = onRetry,
            )
            state.dashboard != null -> DashboardSection(state.dashboard, onNavigate, canSeeAudit = user?.canSeeAudit == true)
            else -> PendingTasksSection(state.tasks, title = pendingTasksTitle(user), onNavigate = onNavigate)
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel("Accesos rápidos")
        Spacer(Modifier.height(6.dp))

        // Lo operativo primero; lo administrativo, según el rol (igual que el menú).
        val actions = buildList {
            add(QuickActionSpec("Nuevo ticket", Icons.Filled.Add) { onNavigate(Screen.NewTicket) })
            add(QuickActionSpec("Tickets", Icons.Filled.ConfirmationNumber) { onNavigate(Screen.TicketsList) })
            add(QuickActionSpec("Tablero", Icons.Filled.Dashboard) { onNavigate(Screen.TicketsKanban()) })
            if (user?.canSeeMyTasks == true) {
                add(QuickActionSpec("Mis tareas", Icons.Filled.Assignment) { onNavigate(Screen.MyTasks) })
            }
            if (user?.canSeeAdminTasks == true) {
                add(QuickActionSpec("Tareas del equipo", Icons.Filled.AssignmentInd) { onNavigate(Screen.AdminTasks) })
            }
            if (user?.canScanCredential == true) {
                add(QuickActionSpec("Escanear credencial", Icons.Filled.QrCodeScanner) { onNavigate(Screen.AccessScan) })
            }
            if (user?.canViewAccessLog == true) {
                add(QuickActionSpec("Registros de acceso", Icons.Filled.History) { onNavigate(Screen.AccessLog) })
            }
            add(QuickActionSpec("Notificaciones", Icons.Filled.Notifications) { onNavigate(Screen.Notifications) })
            if (user?.canManageHR == true) {
                add(QuickActionSpec("Personal", Icons.Filled.Groups) { onNavigate(Screen.EmployeesList) })
                add(QuickActionSpec("Departamentos", Icons.Filled.Business) { onNavigate(Screen.DepartmentsList) })
            }
            if (user?.canManageCatalogs == true) {
                add(QuickActionSpec("Inventario", Icons.Filled.Inventory) { onNavigate(Screen.InventoryIndex) })
                add(QuickActionSpec("Salidas de material", Icons.Filled.ListAlt) { onNavigate(Screen.MaterialOutputsList) })
                add(QuickActionSpec("Cartas responsivas", Icons.Filled.Description) { onNavigate(Screen.CustodyLettersList) })
                add(QuickActionSpec("Reportes", Icons.Filled.QueryStats) { onNavigate(Screen.Reports) })
                add(QuickActionSpec("Tipos de dispositivo", Icons.Filled.Devices) { onNavigate(Screen.DeviceTypesList) })
            }
            if (user?.canSeeAudit == true) {
                add(QuickActionSpec("Auditoría", Icons.Filled.History) { onNavigate(Screen.AuditLogs) })
            }
        }

        actions.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { action ->
                    AppActionCard(
                        label = action.label,
                        icon = action.icon,
                        onClick = action.onClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DashboardSection(summary: DashboardSummaryDto, onNavigate: (Screen) -> Unit, canSeeAudit: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            "PANEL EN TIEMPO REAL",
            style = MaterialTheme.typography.titleSmall,
            color = AppColors.TextMuted,
            modifier = Modifier.weight(1f),
        )
        Box(modifier = Modifier.size(8.dp).background(AppColors.Success, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text("EN VIVO", style = MaterialTheme.typography.labelSmall, color = AppColors.Success)
    }
    Spacer(Modifier.height(14.dp))

    val metrics = summary.ticketMetrics
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard(
            value = metrics.resolvedTasks.toString(),
            label = "Tareas resueltas",
            color = AppColors.Purple,
            icon = { Icon(Icons.Filled.TaskAlt, null, tint = AppColors.Purple, modifier = Modifier.size(20.dp)) },
            modifier = Modifier.weight(1f),
            onClick = { onNavigate(Screen.AdminTasks) },
        )
        StatCard(
            value = (summary.tickets.open + summary.tickets.inProgress).toString(),
            label = "Tickets abiertos",
            color = AppColors.Warning,
            icon = { Icon(Icons.Filled.AssignmentInd, null, tint = AppColors.Warning, modifier = Modifier.size(20.dp)) },
            modifier = Modifier.weight(1f),
            onClick = { onNavigate(Screen.TicketsList) },
        )
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard(
            value = metrics.avgResolutionDays?.let { "$it d" } ?: "—",
            label = "Resolución promedio",
            color = AppColors.Success,
            icon = { Icon(Icons.Filled.AccessTime, null, tint = AppColors.Success, modifier = Modifier.size(20.dp)) },
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value = metrics.pendingTasks.toString(),
            label = "Tareas pendientes",
            color = AppColors.Danger,
            icon = { Icon(Icons.Filled.HourglassEmpty, null, tint = AppColors.Danger, modifier = Modifier.size(20.dp)) },
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(Modifier.height(20.dp))
    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("Eficiencia del equipo")
        if (summary.ticketEfficiency.isEmpty()) {
            Text("Sin datos de resolución", style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
        } else {
            DonutChart(
                segments = summary.ticketEfficiency.take(6).mapIndexed { i, e ->
                    DonutSegment(e.user.name, e.resolved, AGENT_COLORS[i % AGENT_COLORS.size])
                },
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("Tickets por estado")
        DonutChart(
            segments = listOf(
                DonutSegment("Abierto", summary.tickets.open, AppColors.Warning),
                DonutSegment("En seguimiento", summary.tickets.inProgress, AppColors.Info),
                DonutSegment("Cerrado", summary.tickets.closed, AppColors.Success),
            ),
        )
    }

    Spacer(Modifier.height(12.dp))
    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("Tickets antiguos")
        if (summary.urgentTickets.isEmpty()) {
            Text("Sin tickets antiguos. ¡Todo al día!", style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
        } else {
            summary.urgentTickets.take(10).forEach { t ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShape.row)
                        .background(Color.Transparent, AppShape.row)
                        .clickable { onNavigate(Screen.TicketDetail(t.id)) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t.title, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary, maxLines = 1)
                        Text(
                            "${t.assigned ?: "Sin asignar"} · ${ticketPriorityLabel(t.priority)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextFaint,
                        )
                    }
                    Text("${t.daysOnHold} d", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                    Spacer(Modifier.width(8.dp))
                    StatusChip(
                        if (t.daysOnHold >= 15) "URGENTE" else "EN ESPERA",
                        if (t.daysOnHold >= 15) AppColors.Danger else AppColors.Warning,
                    )
                }
            }
            if (summary.urgentTickets.size > 10) {
                SeeMoreButton("Ver más tickets") { onNavigate(Screen.TicketsList) }
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("Actividad reciente")
        if (summary.recentActivity.isEmpty()) {
            Text("Aún no hay actividad registrada.", style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
        } else {
            summary.recentActivity.take(10).forEach { a -> ActivityRow(a, onNavigate) }
            if (summary.recentActivity.size > 10 && canSeeAudit) {
                SeeMoreButton("Ver más actividad") { onNavigate(Screen.AuditLogs) }
            }
        }
    }
}

@Composable
private fun SeeMoreButton(label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ActivityRow(a: DashboardActivityDto, onNavigate: (Screen) -> Unit) {
    val screen = activityScreen(a)
    val color = when (a.scope) {
        "tickets" -> AppColors.Warning
        "custodyLetters" -> AppColors.Success
        "materialOutputs" -> AppColors.Danger
        "devices" -> AppColors.Info
        else -> AppColors.Purple
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShape.row)
            .background(Color.Transparent, AppShape.row)
            .then(if (screen != null) Modifier.clickable { onNavigate(screen) } else Modifier)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp).background(color.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Notifications, null, tint = color, modifier = Modifier.size(13.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(a.message, style = MaterialTheme.typography.bodySmall, color = AppColors.TextPrimary, maxLines = 1)
            Text(formatDateTime(a.at), style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
        }
    }
}

private fun activityScreen(a: DashboardActivityDto): Screen? = when (a.scope) {
    "tickets" -> a.targetId?.let { Screen.TicketDetail(it) }
    "custodyLetters" -> a.targetId?.let { Screen.CustodyLetterDetail(it) }
    "inventory" -> a.deviceId?.let { Screen.DeviceDetail(it) }
    else -> null
}

/** Acceso destacado a la credencial digital (todos los roles). */
@Composable
private fun CredentialShortcut(onClick: () -> Unit) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        containerColor = AppColors.EmeraldPrimary,
        borderColor = null,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.QrCode2, contentDescription = null, tint = AppColors.Surface, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Mi credencial", style = MaterialTheme.typography.titleMedium, color = AppColors.Surface)
                Text(
                    "Muéstrala al guardia al entrar y al salir",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Surface.copy(alpha = 0.85f),
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AppColors.Surface)
        }
    }
}

private fun pendingTasksTitle(user: SessionUser?): String = when {
    user?.isEmployee == true -> "Mis tareas pendientes"
    user?.isManager == true || user?.isAreaHead == true -> "Tareas pendientes de mi área"
    else -> "Tareas pendientes de mis tickets"
}

/** Pendientes por estado y las más próximas a vencer; cada una abre el tablero de su ticket. */
@Composable
private fun PendingTasksSection(tasks: List<KanbanAssignmentDto>, title: String, onNavigate: (Screen) -> Unit) {
    SectionLabel(title)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("PENDING", "IN_PROGRESS", "IN_REVIEW").forEach { status ->
            StatusChip("${assignmentStatusLabel(status)}: ${tasks.count { it.status == status }}", assignmentStatusColor(status))
        }
    }
    Spacer(Modifier.height(10.dp))
    if (tasks.isEmpty()) {
        Text("No hay tareas pendientes.", style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
        return
    }
    tasks.sortedWith(compareBy(nullsLast<String>()) { it.dueDate }).take(5).forEach { task ->
        AssignmentRow(task, onClick = { onNavigate(Screen.TicketsKanban(task.ticketId)) })
        Spacer(Modifier.height(8.dp))
    }
    if (tasks.size > 5) {
        SeeMoreButton("Ver todas (${tasks.size})") { onNavigate(Screen.TicketsKanban()) }
    }
}

private data class QuickActionSpec(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)
