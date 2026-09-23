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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
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
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.SessionUser
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.ui.AppActionCard
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.StatCard
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatDateTime

private val AGENT_COLORS = listOf(
    Color(0xFF6366F1), Color(0xFF0EA5E9), Color(0xFF10B981),
    Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF8B5CF6),
)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel {
        HomeViewModel(AppContainer.authRepository, AppContainer.devicesApi, AppContainer.dashboardApi)
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
        when {
            state.loading -> LoadingState(modifier = Modifier.fillMaxWidth().height(200.dp))
            state.error != null -> ErrorState(
                message = state.error ?: "Error",
                modifier = Modifier.fillMaxWidth().height(200.dp),
                onRetry = onRetry,
            )
            state.dashboard != null -> DashboardSection(state.dashboard, onNavigate, canSeeAudit = user?.canSeeAudit == true)
            state.summary != null -> DeviceSummarySection(state.summary, onNavigate)
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel("Accesos rápidos")
        Spacer(Modifier.height(6.dp))

        val actions = buildList {
            add(QuickActionSpec("Nuevo ticket", Icons.Filled.Add) { onNavigate(Screen.NewTicket) })
            add(QuickActionSpec("Mis tareas", Icons.Filled.Assignment) { onNavigate(Screen.MyTasks) })
            if (user?.canSeeAdminTasks == true) {
                add(QuickActionSpec("Tareas del equipo", Icons.Filled.AssignmentInd) { onNavigate(Screen.AdminTasks) })
            }
            if (user?.canScanCredential == true) {
                add(QuickActionSpec("Escanear credencial", Icons.Filled.QrCodeScanner) { onNavigate(Screen.AccessScan) })
            }
            if (user?.canViewAccessLog == true) {
                add(QuickActionSpec("Registros de acceso", Icons.Filled.History) { onNavigate(Screen.AccessLog) })
            }
            add(QuickActionSpec("Departamentos", Icons.Filled.Business) { onNavigate(Screen.DepartmentsList) })
            add(QuickActionSpec("Personal", Icons.Filled.Groups) { onNavigate(Screen.PersonalList) })
            add(QuickActionSpec("Inventario", Icons.Filled.Inventory) { onNavigate(Screen.InventoryIndex) })
            add(QuickActionSpec("Salidas de material", Icons.Filled.ListAlt) { onNavigate(Screen.SalidasList) })
            add(QuickActionSpec("Cartas responsivas", Icons.Filled.Description) { onNavigate(Screen.CartasList) })
            add(QuickActionSpec("Reportes", Icons.Filled.QueryStats) { onNavigate(Screen.Reports) })
            add(QuickActionSpec("Notificaciones", Icons.Filled.Notifications) { onNavigate(Screen.Notifications) })
            if (user?.canManageCatalogs == true) {
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

    val metricas = summary.ticketMetricas
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard(
            value = metricas.tareasResueltas.toString(),
            label = "Tareas resueltas",
            color = AppColors.Purple,
            icon = { Icon(Icons.Filled.TaskAlt, null, tint = AppColors.Purple, modifier = Modifier.size(20.dp)) },
            modifier = Modifier.weight(1f),
            onClick = { onNavigate(Screen.AdminTasks) },
        )
        StatCard(
            value = (summary.tickets.abierto + summary.tickets.enSeguimiento).toString(),
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
            value = metricas.avgResolucionDias?.let { "$it d" } ?: "—",
            label = "Resolución promedio",
            color = AppColors.Success,
            icon = { Icon(Icons.Filled.AccessTime, null, tint = AppColors.Success, modifier = Modifier.size(20.dp)) },
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value = metricas.tareasPendientes.toString(),
            label = "Tareas pendientes",
            color = AppColors.Danger,
            icon = { Icon(Icons.Filled.HourglassEmpty, null, tint = AppColors.Danger, modifier = Modifier.size(20.dp)) },
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(Modifier.height(20.dp))
    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("Eficiencia del equipo")
        if (summary.ticketEficiencia.isEmpty()) {
            Text("Sin datos de resolución", style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
        } else {
            DonutChart(
                segments = summary.ticketEficiencia.take(6).mapIndexed { i, e ->
                    DonutSegment(e.user.name, e.resueltas, AGENT_COLORS[i % AGENT_COLORS.size])
                },
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("Tickets por estado")
        DonutChart(
            segments = listOf(
                DonutSegment("Abierto", summary.tickets.abierto, AppColors.Warning),
                DonutSegment("En seguimiento", summary.tickets.enSeguimiento, AppColors.Info),
                DonutSegment("Cerrado", summary.tickets.cerrado, AppColors.Success),
            ),
        )
    }

    Spacer(Modifier.height(12.dp))
    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("Tickets antiguos")
        if (summary.ticketsUrgentes.isEmpty()) {
            Text("Sin tickets antiguos. ¡Todo al día!", style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
        } else {
            summary.ticketsUrgentes.take(10).forEach { t ->
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
                        Text(t.titulo, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary, maxLines = 1)
                        Text(
                            "${t.asignado ?: "Sin asignar"} · ${t.prioridad}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextFaint,
                        )
                    }
                    Text("${t.diasEnEspera} d", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                    Spacer(Modifier.width(8.dp))
                    StatusChip(
                        if (t.diasEnEspera >= 15) "URGENTE" else "EN ESPERA",
                        if (t.diasEnEspera >= 15) AppColors.Danger else AppColors.Warning,
                    )
                }
            }
            if (summary.ticketsUrgentes.size > 10) {
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
        "cartas" -> AppColors.Success
        "salidas" -> AppColors.Danger
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
    "cartas" -> a.targetId?.let { Screen.CartaDetail(it) }
    "inventory" -> a.deviceId?.let { Screen.DeviceDetail(it) }
    else -> null
}

@Composable
private fun DeviceSummarySection(summary: com.axzydev.puertonuevoapp.core.network.devices.DeviceSummaryDto, onNavigate: (Screen) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard(
            value = summary.total.toString(),
            label = "Total de dispositivos",
            color = AppColors.EmeraldPrimary,
            icon = { Icon(Icons.Filled.Devices, null, tint = AppColors.EmeraldPrimary, modifier = Modifier.size(20.dp)) },
            modifier = Modifier.weight(1f),
            onClick = { onNavigate(Screen.DevicesList) },
        )
        StatCard(
            value = summary.disponible.toString(),
            label = "Disponibles",
            color = AppColors.Success,
            icon = { Icon(Icons.Filled.CheckCircle, null, tint = AppColors.Success, modifier = Modifier.size(20.dp)) },
            modifier = Modifier.weight(1f),
            onClick = { onNavigate(Screen.DevicesList) },
        )
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard(
            value = summary.asignado.toString(),
            label = "Asignados",
            color = AppColors.Warning,
            icon = { Icon(Icons.Filled.Person, null, tint = AppColors.Warning, modifier = Modifier.size(20.dp)) },
            modifier = Modifier.weight(1f),
            onClick = { onNavigate(Screen.DevicesList) },
        )
        StatCard(
            value = summary.baja.toString(),
            label = "Baja",
            color = AppColors.TextFaint,
            icon = { Icon(Icons.Filled.Delete, null, tint = AppColors.TextFaint, modifier = Modifier.size(20.dp)) },
            modifier = Modifier.weight(1f),
            onClick = { onNavigate(Screen.DevicesList) },
        )
    }
}

private data class QuickActionSpec(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)
