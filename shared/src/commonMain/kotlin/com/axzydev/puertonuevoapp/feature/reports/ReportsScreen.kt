package com.axzydev.puertonuevoapp.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.network.reports.AssignedDeviceRowDto
import com.axzydev.puertonuevoapp.core.network.reports.DeviceReportRowDto
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.EmptyRow
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.StatCard
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.deviceStatusLabel
import com.axzydev.puertonuevoapp.core.util.formatShortDate

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = viewModel { ReportsViewModel(AppContainer.reportsApi) }) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = state.tab, containerColor = AppColors.Surface, contentColor = AppColors.EmeraldPrimary) {
            Tab(selected = state.tab == 0, onClick = { viewModel.onTabChange(0) }, text = { Text("Asignados") })
            Tab(selected = state.tab == 1, onClick = { viewModel.onTabChange(1) }, text = { Text("Dispositivos") })
        }
        when (state.tab) {
            0 -> AssignedDevicesTab(state, onRetry = viewModel::loadAssignedDevices)
            else -> DevicesReportTab(state, onRetry = viewModel::loadDevices)
        }
    }
}

@Composable
private fun AssignedDevicesTab(state: ReportsUiState, onRetry: () -> Unit) {
    when {
        state.assignedDevicesLoading -> LoadingState(modifier = Modifier.fillMaxSize())
        state.assignedDevicesError != null -> ErrorState(state.assignedDevicesError, Modifier.fillMaxSize(), onRetry = onRetry)
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(state.assignedDevices.size.toString(), "Asignados", AppColors.EmeraldPrimary, { Icon(Icons.Filled.Assignment, null, tint = AppColors.EmeraldPrimary) }, Modifier.weight(1f))
                    StatCard(state.avgDays.toString(), "Días prom.", AppColors.Warning, { Icon(Icons.Filled.Schedule, null, tint = AppColors.Warning) }, Modifier.weight(1f))
                    StatCard(state.over30Days.toString(), "+30 días", AppColors.Danger, { Icon(Icons.Filled.Warning, null, tint = AppColors.Danger) }, Modifier.weight(1f))
                }
            }
            if (state.assignedDevices.isEmpty()) {
                item { EmptyRow("No hay dispositivos asignados") }
            }
            items(state.assignedDevices, key = { it.deviceId }) { r -> AssignedDeviceCard(r) }
        }
    }
}

@Composable
private fun AssignedDeviceCard(r: AssignedDeviceRowDto) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = null,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(r.assetTag, style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
            Text(formatShortDate(r.date), style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
        }
        Text("${r.description} · ${r.type}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
        Spacer(Modifier.height(6.dp))
        Text(
            "Responsable: ${r.custodian}" + (r.employeeNumber?.let { " (No. $it)" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        r.department?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint) }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(r.folio ?: "Sin folio", AppColors.EmeraldPrimary)
            StatusChip(
                when (r.source) { "CUSTODY_LETTER" -> "Carta"; "MOVEMENT" -> "Movimiento"; else -> "Desconocido" },
                if (r.source == "CUSTODY_LETTER") AppColors.Success else AppColors.Warning,
            )
            val days = r.daysAssigned ?: 0
            StatusChip("$days día(s)", if (days > 30) AppColors.Danger else AppColors.TextFaint)
        }
    }
}

@Composable
private fun DevicesReportTab(state: ReportsUiState, onRetry: () -> Unit) {
    when {
        state.devicesLoading -> LoadingState(modifier = Modifier.fillMaxSize())
        state.devicesError != null -> ErrorState(state.devicesError, Modifier.fillMaxSize(), onRetry = onRetry)
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(state.devices.size.toString(), "Total", AppColors.TextPrimary, { Icon(Icons.Filled.Devices, null, tint = AppColors.TextPrimary) }, Modifier.weight(1f))
                    StatCard(state.available.toString(), "Disponibles", AppColors.Success, { Icon(Icons.Filled.Devices, null, tint = AppColors.Success) }, Modifier.weight(1f))
                    StatCard(state.assignedDevicesCount.toString(), "Asignados", AppColors.Warning, { Icon(Icons.Filled.Assignment, null, tint = AppColors.Warning) }, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(state.retired.toString(), "Baja", AppColors.TextMuted, { Icon(Icons.Filled.Warning, null, tint = AppColors.TextMuted) }, Modifier.weight(1f))
                }
            }
            if (state.devices.isEmpty()) {
                item { EmptyRow("No hay dispositivos registrados") }
            }
            items(state.devices, key = { it.deviceId }) { r -> DeviceReportCard(r) }
        }
    }
}

@Composable
private fun DeviceReportCard(r: DeviceReportRowDto) {
    val statusColor = when (r.status) {
        "AVAILABLE" -> AppColors.Success
        "ASSIGNED" -> AppColors.Warning
        else -> AppColors.TextFaint
    }
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = null,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(r.assetTag, style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
                if (r.quantity > 1) {
                    Text("Lote ×${r.quantity}", style = MaterialTheme.typography.labelSmall, color = AppColors.EmeraldPrimary)
                }
            }
            StatusChip(deviceStatusLabel(r.status), statusColor)
        }
        Text("${r.description} · ${r.type} · ${r.brand} ${r.model}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
        Text(r.area, style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
        if (r.status == "ASSIGNED") {
            Spacer(Modifier.height(6.dp))
            Text(
                "Responsable: ${r.custodian ?: "—"}" + (r.employeeNumber?.let { " (No. $it)" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
            r.department?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint) }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(r.folio ?: "Sin folio", AppColors.EmeraldPrimary)
                val days = r.daysAssigned ?: 0
                StatusChip("$days día(s)", if (days > 30) AppColors.Danger else AppColors.TextFaint)
            }
        }
    }
}
