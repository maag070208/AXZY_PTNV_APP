package com.axzydev.puertonuevoapp.feature.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.network.DeviceDto
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.deviceEstadoLabel
import com.axzydev.puertonuevoapp.core.util.formatDateTime
import kotlinx.coroutines.launch

@Composable
fun DeviceDetailScreen(deviceId: String) {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var device by remember { mutableStateOf<DeviceDto?>(null) }

    suspend fun load() {
        loading = true
        error = null
        try {
            device = AppContainer.devicesApi.get(deviceId)
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el dispositivo"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(deviceId) { load() }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Dispositivo", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Surface),
            )
        },
    ) { padding ->
        when {
            loading -> LoadingState(modifier = Modifier.fillMaxSize().padding(padding))
            error != null || device == null -> ErrorState(
                message = error ?: "Dispositivo no encontrado",
                modifier = Modifier.fillMaxSize().padding(padding),
                onRetry = { scope.launch { load() } },
            )
            else -> {
                val d = device!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
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

                    Spacer(Modifier.height(16.dp))
                    InfoCard {
                        SectionLabel("Información")
                        InfoRow("Tipo", d.type?.name ?: "—")
                        InfoRow("Marca", d.marca)
                        InfoRow("Modelo", d.modelo)
                        d.numeroSerie?.let { InfoRow("No. de serie", it) }
                        d.nombreEquipo?.let { InfoRow("Nombre de equipo", it) }
                        InfoRow("Área", d.area)
                        d.location?.let { loc ->
                            val parts = listOfNotNull(loc.lugar, loc.subLugar, loc.numero).joinToString("-")
                            if (parts.isNotBlank()) InfoRow("Ubicación", parts)
                        }
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
