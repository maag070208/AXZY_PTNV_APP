package com.axzydev.puertonuevoapp.feature.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.DeviceDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.deviceEstadoLabel
import kotlinx.coroutines.launch

private val estadoFilters = listOf(
    null to "Todos",
    "DISPONIBLE" to "Disponibles",
    "ASIGNADO" to "Asignados",
    "BAJA" to "Baja",
)

@Composable
fun DevicesListScreen() {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val authState by AppContainer.authRepository.state.collectAsState()
    val isAdmin = (authState as? AuthState.LoggedIn)?.user?.role == "ADMIN"

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var devices by remember { mutableStateOf<List<DeviceDto>>(emptyList()) }
    var estadoFilter by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }

    suspend fun load() {
        loading = true
        error = null
        try {
            devices = AppContainer.devicesApi.list().data
        } catch (e: Exception) {
            error = e.message ?: "No se pudieron cargar los dispositivos"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    val filtered by remember(devices, estadoFilter, query) {
        derivedStateOf {
            devices
                .filter { estadoFilter == null || it.estado == estadoFilter }
                .filter {
                    query.isBlank() ||
                        it.controlActivos.contains(query, ignoreCase = true) ||
                        it.descripcion.contains(query, ignoreCase = true) ||
                        it.marca.contains(query, ignoreCase = true) ||
                        it.modelo.contains(query, ignoreCase = true)
                }
                .sortedBy { it.controlActivos }
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Inventario y disponibilidad", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted, modifier = Modifier.padding(top = 3.dp))
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar por activo, marca, modelo…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    estadoFilters.forEach { (value, label) ->
                        val selected = estadoFilter == value
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selected) AppColors.Surface else AppColors.TextMuted,
                            modifier = Modifier
                                .background(
                                    if (selected) AppColors.EmeraldPrimary else AppColors.SurfaceVariant,
                                    RoundedCornerShape(20.dp),
                                )
                                .clickable { estadoFilter = value }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    if (isAdmin) {
                        Text(
                            text = "Tipos de equipo",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.EmeraldPrimary,
                            modifier = Modifier
                                .background(AppColors.SurfaceVariant, RoundedCornerShape(20.dp))
                                .clickable { navigator.push(Screen.DeviceTypesList) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            when {
                loading -> LoadingState(modifier = Modifier.weight(1f))
                error != null -> ErrorState(
                    message = error ?: "Error",
                    modifier = Modifier.weight(1f),
                    onRetry = { scope.launch { load() } },
                )
                filtered.isEmpty() -> EmptyState("No hay dispositivos con estos filtros", modifier = Modifier.weight(1f))
                else -> LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    items(filtered, key = { it.id }) { device ->
                        DeviceRow(device) { navigator.push(Screen.DeviceDetail(device.id)) }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
    }
}

@Composable
private fun DeviceRow(device: DeviceDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(device.controlActivos, style = MaterialTheme.typography.titleMedium, color = AppColors.EmeraldPrimaryDark)
                Text(device.descripcion, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
            }
            StatusChip(deviceEstadoLabel(device.estado), AppColors.deviceEstadoColor(device.estado))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "${device.marca} ${device.modelo} · ${device.type?.name ?: ""}",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        device.location?.let { loc ->
            val parts = listOfNotNull(loc.lugar, loc.subLugar, loc.numero).joinToString("-")
            if (parts.isNotBlank()) {
                Text(parts, style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
            }
        }
    }
}
