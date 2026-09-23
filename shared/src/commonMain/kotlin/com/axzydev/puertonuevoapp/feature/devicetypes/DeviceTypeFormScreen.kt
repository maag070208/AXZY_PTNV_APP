package com.axzydev.puertonuevoapp.feature.devicetypes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.AppTextField

@Composable
fun DeviceTypeFormScreen(typeId: String? = null) {
    val navigator = LocalNavigator.current
    val viewModel: DeviceTypeFormViewModel = viewModel(key = "device-type-form-${typeId ?: "new"}") {
        DeviceTypeFormViewModel(typeId, AppContainer.deviceTypesApi)
    }
    val state by viewModel.uiState.collectAsState()
    val isEdit = typeId != null

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
        if (state.error != null) {
            Text(state.error ?: "", color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
        }

        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Datos del tipo")
            AppTextField(
                value = state.code,
                onValueChange = viewModel::onCodeChange,
                label = { Text("Código interno") },
                placeholder = { Text("LAPTOP") },
                enabled = !isEdit,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nombre") },
                placeholder = { Text("Laptop") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = state.prefix,
                onValueChange = viewModel::onPrefixChange,
                label = { Text("Prefijo (consecutivo)") },
                placeholder = { Text("LPT") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Ej: ${state.prefix.ifBlank { "LPT" }}-0001, ${state.prefix.ifBlank { "LPT" }}-0002…",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (isEdit) {
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Activo", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                    Switch(checked = state.active, onCheckedChange = viewModel::onActiveChange)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Campos de este tipo")
            Text(
                "Define qué datos aparecen al dar de alta cada dispositivo y cuáles son obligatorios.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
            )
            Spacer(Modifier.height(8.dp))
            deviceTypeFieldDefs.forEach { def ->
                val setting = state.fieldConfig.settingFor(def.key)
                FieldConfigRow(
                    label = def.label,
                    enabled = setting.enabled,
                    required = setting.required,
                    onEnabledChange = { viewModel.onFieldEnabledChange(def.key, it) },
                    onRequiredChange = { viewModel.onFieldRequiredChange(def.key, it) },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = viewModel::submit,
            enabled = state.isValid && !state.saving,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            if (state.saving) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), color = AppColors.Surface, strokeWidth = 2.dp)
            } else {
                Text(if (isEdit) "Guardar cambios" else "Crear tipo", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun FieldConfigRow(
    label: String,
    enabled: Boolean,
    required: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onRequiredChange: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = enabled, onCheckedChange = onEnabledChange)
            Text("Mostrar", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
            Spacer(Modifier.width(16.dp))
            Checkbox(checked = required, onCheckedChange = onRequiredChange, enabled = enabled)
            Text("Obligatorio", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
        }
    }
}
