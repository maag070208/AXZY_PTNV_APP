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
import com.axzydev.puertonuevoapp.core.network.reports.AsignadoRowDto
import com.axzydev.puertonuevoapp.core.network.reports.DeviceReportRowDto
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.EmptyRow
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.StatCard
import com.axzydev.puertonuevoapp.core.ui.StatusChip
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
            0 -> AsignadosTab(state, onRetry = viewModel::loadAsignados)
            else -> DevicesReportTab(state, onRetry = viewModel::loadDevices)
        }
    }
}

@Composable
private fun AsignadosTab(state: ReportsUiState, onRetry: () -> Unit) {
    when {
        state.asignadosLoading -> LoadingState(modifier = Modifier.fillMaxSize())
        state.asignadosError != null -> ErrorState(state.asignadosError, Modifier.fillMaxSize(), onRetry = onRetry)
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(state.asignados.size.toString(), "Asignados", AppColors.EmeraldPrimary, { Icon(Icons.Filled.Assignment, null, tint = AppColors.EmeraldPrimary) }, Modifier.weight(1f))
                    StatCard(state.promedioDias.toString(), "Días prom.", AppColors.Warning, { Icon(Icons.Filled.Schedule, null, tint = AppColors.Warning) }, Modifier.weight(1f))
                    StatCard(state.masDe30.toString(), "+30 días", AppColors.Danger, { Icon(Icons.Filled.Warning, null, tint = AppColors.Danger) }, Modifier.weight(1f))
                }
            }
            if (state.asignados.isEmpty()) {
                item { EmptyRow("No hay dispositivos asignados") }
            }
            items(state.asignados, key = { it.deviceId }) { r -> AsignadoCard(r) }
        }
    }
}

@Composable
private fun AsignadoCard(r: AsignadoRowDto) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = null,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(r.controlActivos, style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
            Text(formatShortDate(r.fecha), style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
        }
        Text("${r.descripcion} · ${r.tipo}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
        Spacer(Modifier.height(6.dp))
        Text(
            "Responsable: ${r.responsable}" + (r.numeroEmpleado?.let { " (No. $it)" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        r.departamento?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint) }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(r.folio ?: "Sin folio", AppColors.EmeraldPrimary)
            StatusChip(
                when (r.origen) { "CARTA" -> "Carta"; "MOVIMIENTO" -> "Movimiento"; else -> "Desconocido" },
                if (r.origen == "CARTA") AppColors.Success else AppColors.Warning,
            )
            val dias = r.diasAsignado ?: 0
            StatusChip("$dias día(s)", if (dias > 30) AppColors.Danger else AppColors.TextFaint)
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
                    StatCard(state.disponibles.toString(), "Disponibles", AppColors.Success, { Icon(Icons.Filled.Devices, null, tint = AppColors.Success) }, Modifier.weight(1f))
                    StatCard(state.asignadosCount.toString(), "Asignados", AppColors.Warning, { Icon(Icons.Filled.Assignment, null, tint = AppColors.Warning) }, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(state.bajas.toString(), "Baja", AppColors.TextMuted, { Icon(Icons.Filled.Warning, null, tint = AppColors.TextMuted) }, Modifier.weight(1f))
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
    val estadoColor = when (r.estado) {
        "DISPONIBLE" -> AppColors.Success
        "ASIGNADO" -> AppColors.Warning
        else -> AppColors.TextFaint
    }
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = null,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(r.controlActivos, style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
                if (r.cantidad > 1) {
                    Text("Lote ×${r.cantidad}", style = MaterialTheme.typography.labelSmall, color = AppColors.EmeraldPrimary)
                }
            }
            StatusChip(r.estado, estadoColor)
        }
        Text("${r.descripcion} · ${r.tipo} · ${r.marca} ${r.modelo}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
        Text(r.area, style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
        if (r.estado == "ASIGNADO") {
            Spacer(Modifier.height(6.dp))
            Text(
                "Responsable: ${r.responsable ?: "—"}" + (r.numeroEmpleado?.let { " (No. $it)" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
            r.departamento?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint) }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(r.folio ?: "Sin folio", AppColors.EmeraldPrimary)
                val dias = r.diasAsignado ?: 0
                StatusChip("$dias día(s)", if (dias > 30) AppColors.Danger else AppColors.TextFaint)
            }
        }
    }
}
