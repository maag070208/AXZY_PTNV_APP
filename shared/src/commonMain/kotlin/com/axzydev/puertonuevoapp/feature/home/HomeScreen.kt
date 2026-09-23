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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.SessionUser
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.StatCard

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel { HomeViewModel(AppContainer.authRepository, AppContainer.devicesApi) }) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user

    HomeContent(
        state = state,
        userName = viewModel.userName,
        user = user,
        onRetry = viewModel::load,
        onNavigate = { navigator.push(it) },
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    userName: String,
    user: SessionUser?,
    onRetry: () -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            text = if (userName.isBlank()) "Bienvenido" else "Hola, $userName",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Así va el inventario en Puerto Nuevo hoy",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp, bottom = 20.dp),
        )

        when {
            state.loading -> LoadingState(modifier = Modifier.fillMaxWidth().height(220.dp))
            state.error != null -> ErrorState(
                message = state.error ?: "Error",
                modifier = Modifier.fillMaxWidth().height(220.dp),
                onRetry = onRetry,
            )
            else -> state.summary?.let { summary ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        value = summary.total.toString(),
                        label = "Total de dispositivos",
                        color = AppColors.EmeraldPrimary,
                        icon = { Icon(Icons.Filled.Devices, contentDescription = null, tint = AppColors.EmeraldPrimary, modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.DevicesList) },
                    )
                    StatCard(
                        value = summary.disponible.toString(),
                        label = "Disponibles",
                        color = AppColors.Success,
                        icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AppColors.Success, modifier = Modifier.size(20.dp)) },
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
                        icon = { Icon(Icons.Filled.Person, contentDescription = null, tint = AppColors.Warning, modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.DevicesList) },
                    )
                    StatCard(
                        value = summary.baja.toString(),
                        label = "Baja",
                        color = AppColors.TextFaint,
                        icon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = AppColors.TextFaint, modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.DevicesList) },
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
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
            add(QuickActionSpec("Empleados", Icons.Filled.Groups) { onNavigate(Screen.EmployeesList) })
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
                    QuickAction(action, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

private data class QuickActionSpec(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
private fun QuickAction(spec: QuickActionSpec, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(AppColors.Surface, RoundedCornerShape(16.dp))
            .clickable(onClick = spec.onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(AppColors.EmeraldPrimary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(spec.icon, contentDescription = null, tint = AppColors.EmeraldPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(spec.label, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
    }
}
