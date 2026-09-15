package com.axzydev.puertonuevoapp.feature.devices

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField

/**
 * Alta/edición de UN dispositivo (paridad con DeviceFormPage del web, sin el
 * modo de alta por lote ni la importación por Excel — flujos de escritorio
 * para muchas unidades a la vez, fuera de alcance en esta pasada).
 */
@Composable
fun DeviceFormScreen(deviceId: String?) {
    val navigator = LocalNavigator.current
    val viewModel: DeviceFormViewModel = viewModel(key = "device-form-${deviceId ?: "new"}") {
        DeviceFormViewModel(deviceId, AppContainer.devicesApi, AppContainer.deviceTypesApi)
    }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saved) { if (state.saved) navigator.pop() }

    if (state.loading) {
        LoadingState(modifier = Modifier.fillMaxSize())
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(16.dp),
    ) {
        if (state.blocked) {
            AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Text("Bloqueado", style = MaterialTheme.typography.titleSmall, color = AppColors.Warning)
                Spacer(Modifier.height(4.dp))
                Text(
                    "El dispositivo (activo ${state.blockedCode}) está asignado y no se puede editar. Registra su devolución primero.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                )
            }
            Spacer(Modifier.height(14.dp))
        }
        if (state.error != null) {
            Text(state.error ?: "", color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
        }

        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Datos del dispositivo")
            SimpleDropdownField(
                label = "Tipo",
                value = state.typeId,
                options = state.types.map { it.id to "${it.name} (${it.prefix})" },
                onSelect = viewModel::onTypeChange,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.descripcion,
                onValueChange = viewModel::onDescripcionChange,
                label = { Text("Descripción") },
                enabled = !state.blocked,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.marca,
                onValueChange = viewModel::onMarcaChange,
                label = { Text("Marca") },
                enabled = !state.blocked,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.modelo,
                onValueChange = viewModel::onModeloChange,
                label = { Text("Modelo") },
                enabled = !state.blocked,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.area,
                onValueChange = viewModel::onAreaChange,
                label = { Text("Área") },
                enabled = !state.blocked,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.showField("numeroSerie")) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.numeroSerie,
                    onValueChange = viewModel::onNumeroSerieChange,
                    label = { Text("No. de serie") },
                    enabled = !state.blocked,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.showField("nombreEquipo")) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.nombreEquipo,
                    onValueChange = viewModel::onNombreEquipoChange,
                    label = { Text("Nombre del equipo") },
                    enabled = !state.blocked,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (state.showSpecs) {
            Spacer(Modifier.height(14.dp))
            AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                SectionLabel("Especificaciones técnicas")
                if (state.showField("ip")) {
                    OutlinedTextField(
                        value = state.ip,
                        onValueChange = viewModel::onIpChange,
                        label = { Text("IP") },
                        placeholder = { Text("192.168.0.1") },
                        enabled = !state.blocked,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (state.showField("macAddress")) {
                    OutlinedTextField(
                        value = state.macAddress,
                        onValueChange = viewModel::onMacAddressChange,
                        label = { Text("MAC") },
                        placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                        enabled = !state.blocked,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (state.showField("sistemaOp")) {
                    OutlinedTextField(
                        value = state.sistemaOp,
                        onValueChange = viewModel::onSistemaOpChange,
                        label = { Text("Sistema operativo") },
                        enabled = !state.blocked,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (state.showField("ram")) {
                    OutlinedTextField(
                        value = state.ram,
                        onValueChange = viewModel::onRamChange,
                        label = { Text("RAM") },
                        enabled = !state.blocked,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (state.showField("almacenamiento")) {
                    OutlinedTextField(
                        value = state.almacenamiento,
                        onValueChange = viewModel::onAlmacenamientoChange,
                        label = { Text("Almacenamiento") },
                        enabled = !state.blocked,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = viewModel::submit,
            enabled = state.isValid && !state.saving && !state.blocked,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            if (state.saving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AppColors.Surface, strokeWidth = 2.dp)
            } else {
                Text(if (deviceId == null) "Dar de alta" else "Guardar cambios", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
