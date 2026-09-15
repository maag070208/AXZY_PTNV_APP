package com.axzydev.puertonuevoapp.feature.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.deviceEstadoLabel
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
    val isAdmin = user?.canManageCatalogs == true
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
                        Text(d.controlActivos, style = MaterialTheme.typography.headlineSmall, color = AppColors.TextPrimary)
                        Text(d.descripcion, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
                    }
                    StatusChip(deviceEstadoLabel(d.estado), AppColors.deviceEstadoColor(d.estado))
                }

                if (isAdmin || canRegisterMovement) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isAdmin) {
                            Text(
                                "Editar",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.EmeraldPrimary,
                                modifier = Modifier
                                    .background(AppColors.SurfaceVariant, RoundedCornerShape(20.dp))
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
                                    .background(AppColors.SurfaceVariant, RoundedCornerShape(20.dp))
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
                    InfoRow("Marca", d.marca)
                    InfoRow("Modelo", d.modelo)
                    d.numeroSerie?.let { InfoRow("No. de serie", it) }
                    d.nombreEquipo?.let { InfoRow("Nombre de equipo", it) }
                    InfoRow("Área", d.area)
                    d.location?.lugar?.let { InfoRow("Ubicación", it) }
                    d.loteId?.let { InfoRow("Lote", "${d.loteSize ?: 1} unidad(es)") }
                }

                val hasSpecs = d.ip != null || d.macAddress != null || d.sistemaOp != null || d.ram != null || d.almacenamiento != null
                if (hasSpecs) {
                    Spacer(Modifier.height(14.dp))
                    InfoCard {
                        SectionLabel("Especificaciones técnicas")
                        d.sistemaOp?.let { InfoRow("Sistema operativo", it) }
                        d.ram?.let { InfoRow("RAM", it) }
                        d.almacenamiento?.let { InfoRow("Almacenamiento", it) }
                        d.ip?.let { InfoRow("IP", it) }
                        d.macAddress?.let { InfoRow("MAC", it) }
                    }
                }

                if (d.cartaItems.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    InfoCard {
                        SectionLabel("Cartas responsivas activas")
                        d.cartaItems.forEach { item ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    "${item.carta.consecutive} · ${item.carta.responsable?.name ?: item.carta.numeroEmpleado}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppColors.TextPrimary,
                                )
                                Text(
                                    "${item.carta.departamento} · ${formatShortDate(item.carta.fecha)}",
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
                                    "${h.autor?.name ?: "Sistema"} · ${formatDateTime(h.createdAt)}",
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(16.dp))
            .padding(14.dp),
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
