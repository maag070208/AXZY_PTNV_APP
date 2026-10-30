package com.axzydev.puertonuevoapp.feature.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.BentoIconButton
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.util.conditionLabel
import com.axzydev.puertonuevoapp.core.util.deviceStatusLabel

/**
 * Registrar un movimiento de inventario. El tipo **Entrada** (`STOCK_IN`) captura
 * piezas NUEVAS: cantidad 1..5000 e identidades opcionales por pieza (serie/MAC/
 * IP/hostname según el tipo del dispositivo), sin ubicación obligatoria, igual que
 * la web; el resto de tipos mueve una pieza/estado como antes.
 */
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

            if (state.isEntry) {
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.Success.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                ) {
                    Text("Entrada de piezas nuevas", style = MaterialTheme.typography.labelSmall, color = AppColors.Success)
                    Spacer(Modifier.height(8.dp))
                    AppTextField(
                        value = state.quantityText,
                        onValueChange = viewModel::onQuantityChange,
                        label = { Text("Cantidad de piezas (1 a 5000)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.folioHint?.let { hint ->
                        Spacer(Modifier.height(8.dp))
                        Text(hint, style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                    }
                    if (!state.quantityValid) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "La cantidad debe estar entre 1 y 5000 piezas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.Danger,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "La entrada no pide ubicación: las piezas se crean en el dispositivo seleccionado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextFaint,
                    )
                    Spacer(Modifier.height(10.dp))
                    AppTextField(
                        value = state.entryReason,
                        onValueChange = viewModel::onEntryReasonChange,
                        label = { Text("Motivo de la entrada (opcional)") },
                        placeholder = { Text("Ej. Llegó mercancía nueva") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

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

        // Identidades por pieza: solo para la entrada y cuando el tipo pide datos.
        if (state.isEntry && state.selectedDevice != null) {
            Spacer(Modifier.height(16.dp))
            if (state.usesUnitIdentity) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    Text(
                        "Identificación de las piezas (${state.identityCount} de ${state.quantity.coerceAtLeast(0)})",
                        style = MaterialTheme.typography.titleSmall,
                        color = AppColors.TextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Opcional y por pieza; se aplica en el orden en que se crean. El tipo del dispositivo decide qué campos se piden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                    )
                }
                Spacer(Modifier.height(10.dp))
                state.identityRows.forEachIndexed { index, row ->
                    EntryIdentityCard(
                        label = "Pieza ${index + 1}",
                        row = row,
                        showSerial = state.showField("serialNumber"),
                        showMac = state.showField("macAddress"),
                        showIp = state.showField("ip"),
                        showHostname = state.showField("hostname"),
                        onChange = { field, value -> viewModel.onIdentityChange(row.id, field, value) },
                        onRemove = { viewModel.removeIdentityRow(row.id) },
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    PillButton(
                        text = "Agregar identidad",
                        onClick = viewModel::addIdentityRow,
                        enabled = state.canAddIdentity,
                        variant = PillButtonVariant.Secondary,
                        leadingIcon = Icons.Filled.Add,
                    )
                }
                if (!state.canAddIdentity) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Ya hay una identidad por cada pieza de la cantidad indicada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextFaint,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            } else {
                Text(
                    "El tipo de dispositivo no pide serie/MAC/IP/hostname por pieza; solo se registrará la cantidad.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextFaint,
                    modifier = Modifier.padding(horizontal = 4.dp),
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
                Text(
                    text = when {
                        state.isEntry && state.quantity > 1 -> "Registrar ${state.quantity} piezas"
                        state.isEntry -> "Registrar entrada"
                        else -> "Registrar movimiento"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

/** Identificación opcional de una pieza nueva (todos los campos que pide el tipo). */
@Composable
private fun EntryIdentityCard(
    label: String,
    row: StockInIdentityRow,
    showSerial: Boolean,
    showMac: Boolean,
    showIp: Boolean,
    showHostname: Boolean,
    onChange: (StockInIdentityField, String) -> Unit,
    onRemove: () -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            BentoIconButton(
                icon = Icons.Filled.Close,
                contentDescription = "Quitar identidad de la pieza",
                onClick = onRemove,
            )
        }
        if (showSerial) {
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = row.serialNumber,
                onValueChange = { onChange(StockInIdentityField.SerialNumber, it) },
                label = { Text("No. de serie") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (showMac && showIp) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(
                    value = row.macAddress,
                    onValueChange = { onChange(StockInIdentityField.Mac, it) },
                    label = { Text("MAC") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                AppTextField(
                    value = row.ip,
                    onValueChange = { onChange(StockInIdentityField.Ip, it) },
                    label = { Text("IP") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        } else if (showMac) {
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = row.macAddress,
                onValueChange = { onChange(StockInIdentityField.Mac, it) },
                label = { Text("MAC") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        } else if (showIp) {
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = row.ip,
                onValueChange = { onChange(StockInIdentityField.Ip, it) },
                label = { Text("IP") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (showHostname) {
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = row.hostname,
                onValueChange = { onChange(StockInIdentityField.Hostname, it) },
                label = { Text("Nombre del equipo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
