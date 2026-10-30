package com.axzydev.puertonuevoapp.feature.devices

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.devices.DeviceDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.BentoIconButton
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.deviceStatusLabel

@Composable
fun DevicesListScreen(viewModel: DevicesListViewModel = viewModel { DevicesListViewModel(AppContainer.devicesApi) }) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canManageTypes = user?.canManageCatalogs == true
    // "Cargar Excel" es el asistente de importación (`/inventory/devices/import`).
    val canImportDevices = user?.canCreateDevices == true

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Encabezado: descripción + acción primaria (cargar Excel) a la derecha.
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Inventario y disponibilidad",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextMuted,
                    modifier = Modifier.weight(1f),
                )
                if (canManageTypes) {
                    BentoIconButton(
                        icon = Icons.Filled.Settings,
                        contentDescription = "Administrar los tipos de equipo",
                        onClick = { navigator.push(Screen.DeviceTypesList) },
                    )
                }
                if (canImportDevices) {
                    Spacer(Modifier.width(10.dp))
                    PillButton(
                        text = "Cargar Excel",
                        onClick = { navigator.push(Screen.DeviceImport) },
                        variant = PillButtonVariant.Secondary,
                        leadingIcon = Icons.Filled.UploadFile,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            AppSearchField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Buscar por activo, marca, modelo…",
            )
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                deviceStatusFilters.forEach { (value, label) ->
                    AppFilterChip(
                        label = label,
                        selected = state.statusFilter == value,
                        onClick = { viewModel.onStatusFilterChange(value) },
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

/**
 * Fila de un dispositivo: **nombre** como título (2 líneas máx.), **marca/modelo**
 * como subtítulo solo cuando aportan algo distinto al nombre (el API repite el
 * nombre en `description` y en `assetTag`, y la marca+modelo suele ser el mismo
 * texto) y **tipo** como meta. La ubicación, si la tiene, va al final.
 */
@Composable
private fun DeviceRow(device: DeviceDto, onClick: () -> Unit) {
    val name = device.description.ifBlank { device.assetTag }.trim()
    val brandModel = listOf(device.brand, device.model)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")
    val showBrandModel = brandModel.isNotEmpty() && !brandModel.equals(name, ignoreCase = true)
    val category = device.type?.name?.trim()?.takeIf { it.isNotEmpty() }

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        borderColor = null,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.EmeraldPrimaryDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showBrandModel) {
                    Text(
                        brandModel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            StatusChip(deviceStatusLabel(device.status), AppColors.deviceStatusColor(device.status))
        }
        if (category != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                category,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        device.location?.name?.let { location ->
            Text(
                location,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
