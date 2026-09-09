package com.axzydev.puertonuevoapp.feature.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.DeviceSummaryDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.StatCard
import com.axzydev.puertonuevoapp.core.util.roleLabel
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.weight

@Composable
fun HomeScreen() {
    val navigator = LocalNavigator.current
    val authRepository = AppContainer.authRepository
    val authState by authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var ticketsAbiertos by remember { mutableStateOf(0) }
    var ticketsSeguimiento by remember { mutableStateOf(0) }
    var deviceSummary by remember { mutableStateOf<DeviceSummaryDto?>(null) }

    suspend fun load() {
        loading = true
        error = null
        try {
            val tickets = AppContainer.ticketsApi.list()
            ticketsAbiertos = tickets.data.count { it.status == "ABIERTO" }
            ticketsSeguimiento = tickets.data.count { it.status == "EN_SEGUIMIENTO" }
            deviceSummary = AppContainer.devicesApi.summary()
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el resumen"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.Surface)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Hola, ${user?.name ?: ""}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AppColors.TextPrimary,
                    )
                    Text(
                        roleLabel(user?.role),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                    )
                }
                IconButton(onClick = { authRepository.logout() }) {
                    Icon(Icons.Filled.Logout, contentDescription = "Cerrar sesión", tint = AppColors.TextMuted)
                }
            }
        }

        when {
            loading -> LoadingState(modifier = Modifier.height(240.dp))
            error != null -> ErrorState(
                message = error ?: "Error",
                modifier = Modifier.height(240.dp),
                onRetry = { scope.launch { load() } },
            )
            else -> Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "TICKETS",
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.TextMuted,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard(
                        value = ticketsAbiertos.toString(),
                        label = "Abiertos",
                        color = AppColors.Warning,
                        icon = { Icon(Icons.Filled.Build, contentDescription = null, tint = AppColors.Warning, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.switchTab(Screen.TicketsList) },
                    )
                    StatCard(
                        value = ticketsSeguimiento.toString(),
                        label = "En seguimiento",
                        color = AppColors.Info,
                        icon = { Icon(Icons.Filled.Visibility, contentDescription = null, tint = AppColors.Info, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.switchTab(Screen.TicketsList) },
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "DISPOSITIVOS",
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.TextMuted,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard(
                        value = (deviceSummary?.disponible ?: 0).toString(),
                        label = "Disponibles",
                        color = AppColors.Success,
                        icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AppColors.Success, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.switchTab(Screen.DevicesList) },
                    )
                    StatCard(
                        value = (deviceSummary?.asignado ?: 0).toString(),
                        label = "Asignados",
                        color = AppColors.Warning,
                        icon = { Icon(Icons.Filled.Devices, contentDescription = null, tint = AppColors.Warning, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f),
                        onClick = { navigator.switchTab(Screen.DevicesList) },
                    )
                }

                Spacer(Modifier.height(10.dp))

                StatCard(
                    value = (deviceSummary?.total ?: 0).toString(),
                    label = "Total de dispositivos en inventario",
                    color = AppColors.TextMuted,
                    icon = { Icon(Icons.Filled.Inventory, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navigator.switchTab(Screen.DevicesList) },
                )
            }
        }
    }
}
