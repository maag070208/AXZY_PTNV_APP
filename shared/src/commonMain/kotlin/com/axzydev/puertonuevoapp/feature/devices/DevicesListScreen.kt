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
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.devices.DeviceDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.deviceEstadoLabel

@Composable
fun DevicesListScreen(viewModel: DevicesListViewModel = viewModel { DevicesListViewModel(AppContainer.devicesApi) }) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val isAdmin = (authState as? AuthState.LoggedIn)?.user?.canManageCatalogs == true

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Inventario y disponibilidad",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
                modifier = Modifier.padding(top = 3.dp),
            )
            Spacer(Modifier.height(10.dp))
            AppSearchField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Buscar por activo, marca, modelo…",
            )
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                deviceEstadoFilters.forEach { (value, label) ->
                    val selected = state.estadoFilter == value
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) AppColors.Surface else AppColors.TextMuted,
                        modifier = Modifier
                            .background(if (selected) AppColors.EmeraldPrimary else AppColors.SurfaceVariant, RoundedCornerShape(20.dp))
                            .clickable { viewModel.onEstadoFilterChange(value) }
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
            state.loading -> LoadingState(modifier = Modifier.weight(1f))
            state.error != null -> ErrorState(message = state.error ?: "Error", modifier = Modifier.weight(1f), onRetry = viewModel::load)
            state.filtered.isEmpty() -> EmptyState("No hay dispositivos con estos filtros", modifier = Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(state.filtered, key = { it.id }) { device ->
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
        device.location?.lugar?.let { lugar ->
            Text(lugar, style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
        }
    }
}
