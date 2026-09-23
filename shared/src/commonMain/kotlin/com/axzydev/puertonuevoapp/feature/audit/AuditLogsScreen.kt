package com.axzydev.puertonuevoapp.feature.audit

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.network.audit.AuditLogDto
import com.axzydev.puertonuevoapp.core.network.audit.auditActionOptions
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import com.axzydev.puertonuevoapp.core.ui.AppTextField

private fun actionLabel(action: String): String = auditActionOptions.firstOrNull { it.first == action }?.second ?: action

// Lectura defensiva: los campos Json (previousState/newState/metadata) los
// escriben varios módulos distintos (tickets, inventario, cartas,
// dispositivos) sin un shape garantizado.
private fun jsonString(obj: JsonObject?, key: String): String? = (obj?.get(key) as? JsonPrimitive)?.contentOrNull

private fun actionColor(action: String): Color = when {
    action.contains("ENTRADA") -> AppColors.Success
    action.contains("SALIDA") -> AppColors.Warning
    action.contains("TRASLADO") -> AppColors.Info
    action.contains("BAJA") -> AppColors.Danger
    action.contains("PRESTAMO") -> AppColors.EmeraldPrimary
    action.contains("DEVOLUCION") -> AppColors.Info
    action.contains("CARTA") -> AppColors.EmeraldPrimary
    else -> AppColors.TextFaint
}

@Composable
fun AuditLogsScreen(viewModel: AuditLogsViewModel = viewModel { AuditLogsViewModel(AppContainer.auditApi) }) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("${state.total} registro(s)", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
            Spacer(Modifier.height(10.dp))
            SimpleDropdownField(
                label = "Tipo de acción",
                value = state.filterAction,
                options = auditActionOptions,
                onSelect = viewModel::onFilterActionChange,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(
                    value = state.filterStart,
                    onValueChange = viewModel::onFilterStartChange,
                    label = { Text("Desde (AAAA-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                AppTextField(
                    value = state.filterEnd,
                    onValueChange = viewModel::onFilterEndChange,
                    label = { Text("Hasta (AAAA-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = viewModel::applyFilters, modifier = Modifier.fillMaxWidth()) {
                Text("Aplicar filtros")
            }
        }

        when {
            state.loading -> LoadingState(modifier = Modifier.weight(1f))
            state.error != null -> ErrorState(state.error ?: "Error", Modifier.weight(1f), onRetry = viewModel::load)
            state.logs.isEmpty() -> EmptyState("No hay registros de auditoría", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.logs, key = { it.id }) { log -> AuditLogCard(log) }
            }
        }

        if (state.totalPages > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = viewModel::previousPage, enabled = state.page > 1) { Text("Anterior") }
                Spacer(Modifier.width(12.dp))
                Text("Página ${state.page} de ${state.totalPages}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = viewModel::nextPage, enabled = state.page < state.totalPages) { Text("Siguiente") }
            }
        }
    }
}

@Composable
private fun AuditLogCard(log: AuditLogDto) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = null,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(actionLabel(log.action), actionColor(log.action))
                Spacer(Modifier.width(8.dp))
                Text(log.deviceCode ?: "—", style = MaterialTheme.typography.bodySmall, color = AppColors.TextPrimary)
            }
            Text(formatDateTime(log.createdAt), style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Usuario: ${log.userName ?: "—"}  ·  Entidad: ${log.entityType}",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        val newState = log.newState
        val notas = jsonString(log.metadata, "notas")
        val tipo = jsonString(newState, "tipo")
        val estado = jsonString(newState, "estado")
        val condicion = jsonString(newState, "condicion")
        val consecutive = jsonString(newState, "consecutive")
        if (tipo != null || estado != null || condicion != null || consecutive != null || notas != null) {
            Spacer(Modifier.height(6.dp))
            Column(
                modifier = Modifier.fillMaxWidth().background(AppColors.SurfaceVariant, RoundedCornerShape(8.dp)).padding(10.dp),
            ) {
                tipo?.let { Text("Tipo: $it", style = MaterialTheme.typography.labelSmall, color = AppColors.TextMuted) }
                estado?.let { Text("Estado: $it", style = MaterialTheme.typography.labelSmall, color = AppColors.TextMuted) }
                condicion?.let { Text("Condición: $it", style = MaterialTheme.typography.labelSmall, color = AppColors.TextMuted) }
                consecutive?.let { Text("Carta: $it", style = MaterialTheme.typography.labelSmall, color = AppColors.TextMuted) }
                notas?.let { Text("Notas: $it", style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint) }
            }
        }
    }
}
