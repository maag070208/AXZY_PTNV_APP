package com.axzydev.puertonuevoapp.feature.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.deviceStatusLabel
import com.axzydev.puertonuevoapp.core.util.formatDateTime
import com.axzydev.puertonuevoapp.core.util.formatShortDate

@Composable
fun DeviceDetailScreen(deviceId: String) {
    val viewModel: DeviceDetailViewModel = viewModel(key = "device-$deviceId") {
        DeviceDetailViewModel(deviceId, AppContainer.devicesApi)
    }
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canEditDevice = user?.canEditDevices == true
    val canRegisterMovement = user?.canRegisterMovement == true

    when {
        state.loading -> LoadingState(modifier = Modifier.fillMaxSize())
        state.error != null || state.device == null -> ErrorState(
            message = state.error ?: "Dispositivo no encontrado",
            modifier = Modifier.fillMaxSize(),
            onRetry = viewModel::load,
        )
        else -> {
            val d = state.device!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(d.assetTag, style = MaterialTheme.typography.headlineSmall, color = AppColors.TextPrimary)
                        Text(d.description, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
                    }
                    StatusChip(deviceStatusLabel(d.status), AppColors.deviceStatusColor(d.status))
                }

                if (canEditDevice || canRegisterMovement) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (canEditDevice) {
                            Text(
                                "Editar",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.EmeraldPrimary,
                                modifier = Modifier
                                    .clip(AppShape.pill)
                                    .background(AppColors.SurfaceVariant, AppShape.pill)
                                    .clickable { navigator.push(Screen.DeviceForm(d.id)) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                        if (canRegisterMovement) {
                            Text(
                                "Movimiento",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.EmeraldPrimary,
                                modifier = Modifier
                                    .clip(AppShape.pill)
                                    .background(AppColors.SurfaceVariant, AppShape.pill)
                                    .clickable { navigator.push(Screen.NewInventoryMovement(d.id)) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                InfoCard {
                    SectionLabel("Información")
                    InfoRow("Tipo", d.type?.name ?: "—")
                    InfoRow("Marca", d.brand)
                    InfoRow("Modelo", d.model)
                    d.serialNumber?.let { InfoRow("No. de serie", it) }
                    d.hostname?.let { InfoRow("Nombre de equipo", it) }
                    InfoRow("Área", d.area)
                    d.location?.name?.let { InfoRow("Ubicación", it) }
                    d.batchId?.let { InfoRow("Lote", "${d.batchSize ?: 1} unidad(es)") }
                }

                val hasSpecs = d.ip != null || d.macAddress != null || d.operatingSystem != null || d.ram != null || d.storage != null
                if (hasSpecs) {
                    Spacer(Modifier.height(14.dp))
                    InfoCard {
                        SectionLabel("Especificaciones técnicas")
                        d.operatingSystem?.let { InfoRow("Sistema operativo", it) }
                        d.ram?.let { InfoRow("RAM", it) }
                        d.storage?.let { InfoRow("Almacenamiento", it) }
                        d.ip?.let { InfoRow("IP", it) }
                        d.macAddress?.let { InfoRow("MAC", it) }
                    }
                }

                if (d.custodyLetterItems.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    InfoCard {
                        SectionLabel("Cartas responsivas activas")
                        d.custodyLetterItems.forEach { item ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    "${item.custodyLetter.consecutive} · ${item.custodyLetter.custodian?.name ?: item.custodyLetter.employeeNumber}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppColors.TextPrimary,
                                )
                                Text(
                                    "${item.custodyLetter.department} · ${formatShortDate(item.custodyLetter.date)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextFaint,
                                )
                            }
                        }
                    }
                }

                if (d.history.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    InfoCard {
                        SectionLabel("Historial (${d.history.size})")
                        d.history.forEach { h ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(h.detail ?: h.type, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                                Text(
                                    "${h.author?.name ?: "Sistema"} · ${formatDateTime(h.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextFaint,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun InfoCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = null,
        contentPadding = PaddingValues(14.dp),
        content = content,
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
    }
}
