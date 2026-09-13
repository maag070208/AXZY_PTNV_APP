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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.DeviceSummaryDto
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var ticketsOpen by remember { mutableStateOf(0) }
    var ticketsProgress by remember { mutableStateOf(0) }
    var devices by remember { mutableStateOf<DeviceSummaryDto?>(null) }

    suspend fun load() {
        loading = true
        error = null
        try {
            val tickets = AppContainer.ticketsApi.list().data
            ticketsOpen = tickets.count { it.status == "ABIERTO" }
            ticketsProgress = tickets.count { it.status == "EN_SEGUIMIENTO" }
            devices = AppContainer.devicesApi.summary()
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el resumen"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .verticalScroll(rememberScrollState()),
    ) {
        when {
            loading -> Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.Success)
            }
            error != null -> ErrorState(
                message = error ?: "Error",
                modifier = Modifier.height(280.dp),
                onRetry = { scope.launch { load() } },
            )
            else -> Column(modifier = Modifier.padding(20.dp)) {
                SectionTitle("Resumen de hoy", "Lo importante, de un vistazo")
                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricTile(
                        value = ticketsOpen.toString(),
                        label = "Tickets abiertos",
                        icon = Icons.Filled.Build,
                        accent = AppColors.Warning,
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.switchTab(Screen.TicketsList) },
                    )
                    MetricTile(
                        value = ticketsProgress.toString(),
                        label = "En seguimiento",
                        icon = Icons.Filled.TrendingUp,
                        accent = AppColors.Info,
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.switchTab(Screen.TicketsList) },
                    )
                }

                Spacer(Modifier.height(28.dp))
                SectionTitle("Acciones rápidas", "Atajos para el equipo")
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickAction(
                        label = "Nuevo ticket",
                        icon = Icons.Filled.Add,
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.push(Screen.NewTicket) },
                    )
                    QuickAction(
                        label = "Ver inventario",
                        icon = Icons.Filled.Inventory,
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.switchTab(Screen.DevicesList) },
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickAction(
                        label = "Mis tareas",
                        icon = Icons.Filled.Assignment,
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.push(Screen.MyTasks) },
                    )
                    QuickAction(
                        label = "Departamentos",
                        icon = Icons.Filled.Business,
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.push(Screen.DepartmentsList) },
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickAction(
                        label = "Empleados",
                        icon = Icons.Filled.Groups,
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.push(Screen.EmployeesList) },
                    )
                    QuickAction(
                        label = "Inventario",
                        icon = Icons.Filled.Inventory,
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.push(Screen.InventoryIndex) },
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickAction(
                        label = "Salidas de material",
                        icon = Icons.Filled.ListAlt,
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.push(Screen.SalidasList) },
                    )
                    QuickAction(
                        label = "Cartas responsivas",
                        icon = Icons.Filled.Description,
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.push(Screen.CartasList) },
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickAction(
                        label = "Reportes",
                        icon = Icons.Filled.BarChart,
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.push(Screen.Reports) },
                    )
                    QuickAction(
                        label = "Notificaciones",
                        icon = Icons.Filled.Notifications,
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.push(Screen.Notifications) },
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickAction(
                        label = "Auditoría",
                        icon = Icons.Filled.History,
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.push(Screen.AuditLogs) },
                    )
                }

                Spacer(Modifier.height(28.dp))
                SectionTitle("Inventario", "Estado actual de dispositivos")
                Spacer(Modifier.height(14.dp))
                InventoryCard(
                    total = devices?.total ?: 0,
                    available = devices?.disponible ?: 0,
                    assigned = devices?.asignado ?: 0,
                    onClick = { navigator.switchTab(Screen.DevicesList) },
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, color = AppColors.TextPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun MetricTile(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .background(AppColors.Surface, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Box(Modifier.size(38.dp).background(accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, color = AppColors.TextPrimary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .background(AppColors.SurfaceVariant, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).background(AppColors.EmeraldPrimary, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = AppColors.Surface, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
    }
}

@Composable
private fun InventoryCard(total: Int, available: Int, assigned: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.EmeraldPrimary, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Devices, contentDescription = null, tint = AppColors.Surface, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Dispositivos", style = MaterialTheme.typography.titleMedium, color = AppColors.Surface)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.ConfirmationNumber, contentDescription = null, tint = AppColors.TextFaint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            InventoryValue(total.toString(), "Total")
            InventoryValue(available.toString(), "Disponibles", AppColors.Success)
            InventoryValue(assigned.toString(), "Asignados", AppColors.Warning)
        }
    }
}

@Composable
private fun InventoryValue(value: String, label: String, color: Color = AppColors.Surface) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint, modifier = Modifier.padding(top = 2.dp))
    }
}
