package com.axzydev.puertonuevoapp.feature.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.util.conditionLabel
import com.axzydev.puertonuevoapp.core.util.deviceStatusLabel
import com.axzydev.puertonuevoapp.core.ui.AppTextField

@Composable
fun NewInventoryMovementScreen(deviceId: String? = null) {
    val navigator = LocalNavigator.current
    val viewModel: NewInventoryMovementViewModel = viewModel(key = "new-movement-${deviceId ?: "any"}") {
        NewInventoryMovementViewModel(deviceId, AppContainer.inventoryApi, AppContainer.devicesApi, AppContainer.locationsApi)
    }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saved) { if (state.saved) navigator.pop() }

    if (state.loading) {
        LoadingState(modifier = Modifier.fillMaxSize())
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(16.dp),
    ) {
        state.error?.let {
            Text(it, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
        }

        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Datos del movimiento")
            SimpleDropdownField(
                label = "Dispositivo",
                value = state.deviceId,
                options = state.devices.map { it.id to "${it.assetTag} - ${it.description}" },
                onSelect = viewModel::onDeviceChange,
                modifier = Modifier.fillMaxWidth(),
            )
            state.selectedDevice?.let { d ->
                Spacer(Modifier.height(10.dp))
                Column(modifier = Modifier.fillMaxWidth().background(AppColors.SurfaceVariant, RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Text("${d.assetTag} · ${deviceStatusLabel(d.status)}", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                    Text(
                        "Ubicación actual: ${d.location?.name ?: "Sin ubicación"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            SimpleDropdownField(
                label = "Tipo de movimiento",
                value = state.type,
                options = movementTypeFormOptions,
                onSelect = viewModel::onTypeChange,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.requiresLocation) {
                Spacer(Modifier.height(10.dp))
                SimpleDropdownField(
                    label = "Ubicación destino",
                    value = state.locationId,
                    options = state.locations.map { it.id to it.name },
                    onSelect = viewModel::onLocationChange,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.requiresLoan) {
                Spacer(Modifier.height(14.dp))
                Column(modifier = Modifier.fillMaxWidth().background(AppColors.Info.copy(alpha = 0.08f), RoundedCornerShape(14.dp)).padding(12.dp)) {
                    Text("Datos de la asignación", style = MaterialTheme.typography.labelSmall, color = AppColors.Info)
                    Spacer(Modifier.height(8.dp))
                    AppTextField(
                        value = state.loanedTo,
                        onValueChange = viewModel::onLoanedToChange,
                        label = { Text("Asignado a") },
                        placeholder = { Text("Nombre de quien recibe el equipo…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    AppTextField(
                        value = state.returnDate,
                        onValueChange = viewModel::onReturnDateChange,
                        label = { Text("Fecha de retorno esperada (AAAA-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (state.requiresReturn) {
                Spacer(Modifier.height(14.dp))
                Column(modifier = Modifier.fillMaxWidth().background(AppColors.Success.copy(alpha = 0.08f), RoundedCornerShape(14.dp)).padding(12.dp)) {
                    Text("Condición del equipo al regresar", style = MaterialTheme.typography.labelSmall, color = AppColors.Success)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        movementConditionCodes.forEach { c ->
                            val selected = state.condition == c
                            Text(
                                conditionLabel(c),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) AppColors.Surface else AppColors.TextMuted,
                                modifier = Modifier
                                    .clip(AppShape.pill)
                                    .background(if (selected) AppColors.conditionColor(c) else AppColors.SurfaceVariant, AppShape.pill)
                                    .clickable { viewModel.onConditionChange(c) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }

            if (state.isPoorCondition) {
                Spacer(Modifier.height(14.dp))
                Column(modifier = Modifier.fillMaxWidth().background(AppColors.Danger.copy(alpha = 0.08f), RoundedCornerShape(14.dp)).padding(12.dp)) {
                    Text("El equipo está en malas condiciones. ¿Qué acción deseas tomar?", style = MaterialTheme.typography.labelSmall, color = AppColors.Danger)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.onPoorConditionActionChange("RETIREMENT") },
                            colors = if (state.poorConditionAction == "RETIREMENT") ButtonDefaults.buttonColors(containerColor = AppColors.Danger, contentColor = AppColors.Surface) else ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Danger),
                        ) { Text("Dar de baja") }
                        OutlinedButton(
                            onClick = { viewModel.onPoorConditionActionChange("TICKET") },
                            colors = if (state.poorConditionAction == "TICKET") ButtonDefaults.buttonColors(containerColor = AppColors.Warning, contentColor = AppColors.Surface) else ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Warning),
                        ) { Text("Solo registrar") }
                    }
                    if (state.poorConditionAction == "RETIREMENT") {
                        Spacer(Modifier.height(8.dp))
                        Text("El dispositivo será dado de baja permanentemente.", style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
                    }
                }
            }

            val currentDeviceLocation = state.selectedDevice?.location
            if (state.type == "STOCK_OUT" && currentDeviceLocation != null) {
                Spacer(Modifier.height(10.dp))
                Text("El dispositivo se retirará de: ${currentDeviceLocation.name}", style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
            }

            if (state.type == "RETIREMENT") {
                Spacer(Modifier.height(10.dp))
                Text("Esta acción marcará el dispositivo como BAJA. No podrá ser usado nuevamente.", style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = state.retirementReason,
                    onValueChange = viewModel::onRetirementReasonChange,
                    label = { Text("Motivo de la baja (opcional)") },
                    placeholder = { Text("Ej. Equipo en mal estado, robado, etc.") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notas (opcional)") },
                placeholder = { Text("Observaciones adicionales…") },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = viewModel::submit,
            enabled = state.isValid && !state.saving,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            if (state.saving) CircularProgressIndicator(modifier = Modifier.height(20.dp), color = AppColors.Surface, strokeWidth = 2.dp)
            else Text("Registrar movimiento", style = MaterialTheme.typography.titleMedium)
        }
    }
}
