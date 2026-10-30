package com.axzydev.puertonuevoapp.feature.devices

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField

/**
 * Alta/edición de un dispositivo.
 *
 * - **Alta**: la web y el API dan de alta **N unidades** con su identificación
 *   por pieza (`unit-identities`). Se captura la cantidad y, si el tipo lo
 *   pide, la serie/MAC/IP/hostname de cada pieza en el orden en que se crean;
 *   es opcional y se puede completar después desde el detalle del dispositivo.
 * - **Edición**: se conserva el formulario de siempre (no se toca la cantidad
 *   ni las piezas; sus identidades se editan en el detalle).
 */
@Composable
fun DeviceFormScreen(deviceId: String?) {
    val navigator = LocalNavigator.current
    val viewModel: DeviceFormViewModel = viewModel(key = "device-form-${deviceId ?: "new"}") {
        DeviceFormViewModel(deviceId, AppContainer.devicesApi, AppContainer.deviceTypesApi)
    }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) {
            navigator.pop()
            // Reabrir "Nuevo/Editar" no debe cerrarse solo por la bandera vieja.
            viewModel.consumeSaved()
        }
    }

    if (state.loading) {
        LoadingState(modifier = Modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (state.blocked) {
            item {
                AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Bloqueado", style = MaterialTheme.typography.titleSmall, color = AppColors.Warning)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "El dispositivo (activo ${state.blockedCode}) está asignado y no se puede editar. Registra su devolución primero.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                    )
                }
            }
        }

        state.error?.let { message ->
            item {
                Text(message, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
            }
        }

        item {
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
                AppTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text("Descripción") },
                    enabled = !state.blocked,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.brand,
                    onValueChange = viewModel::onBrandChange,
                    label = { Text("Marca") },
                    enabled = !state.blocked,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.model,
                    onValueChange = viewModel::onModelChange,
                    label = { Text("Modelo") },
                    enabled = !state.blocked,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.area,
                    onValueChange = viewModel::onAreaChange,
                    label = { Text("Área") },
                    enabled = !state.blocked,
                    modifier = Modifier.fillMaxWidth(),
                )
                // En EDICIÓN se conservan los campos de identificación de la app;
                // en el ALTA viven por pieza (editor de abajo).
                if (!state.creating) {
                    if (state.showField("serialNumber")) {
                        Spacer(Modifier.height(10.dp))
                        AppTextField(
                            value = state.serialNumber,
                            onValueChange = viewModel::onSerialNumberChange,
                            label = { Text("No. de serie") },
                            enabled = !state.blocked,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (state.showField("hostname")) {
                        Spacer(Modifier.height(10.dp))
                        AppTextField(
                            value = state.hostname,
                            onValueChange = viewModel::onHostnameChange,
                            label = { Text("Nombre del equipo") },
                            enabled = !state.blocked,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        if (state.creating) {
            item {
                AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    SectionLabel("Unidades a registrar")
                    AppTextField(
                        value = state.quantityText,
                        onValueChange = viewModel::onQuantityChange,
                        label = { Text("Cantidad de piezas (1 a 5000)") },
                        singleLine = true,
                        enabled = !state.blocked,
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
                        "Las piezas se crean disponibles. La identificación es opcional: las que queden sin datos se capturan después desde el detalle del dispositivo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextFaint,
                    )
                }
            }

            if (state.usesUnitIdentity && state.unitRows.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                        Text(
                            "Identificación por pieza (${state.identifiedCount} de ${state.unitRows.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = AppColors.TextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Opcional y por pieza; el tipo del dispositivo decide qué campos se piden.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextMuted,
                        )
                    }
                }
                itemsIndexed(items = state.unitRows, key = { _, row -> row.id }) { index, row ->
                    UnitIdentityRowCard(
                        label = "Unidad ${index + 1}",
                        row = row,
                        expanded = row.id in state.expandedUnitIds,
                        showSerial = state.showField("serialNumber"),
                        showMac = state.showField("macAddress"),
                        showIp = state.showField("ip"),
                        showHostname = state.showField("hostname"),
                        onToggle = { viewModel.toggleUnitRow(row.id) },
                        onChange = { field, value -> viewModel.onUnitIdentityChange(row.id, field, value) },
                    )
                }
            } else if (!state.usesUnitIdentity) {
                item {
                    Text(
                        "El tipo de dispositivo no pide serie/MAC/IP/hostname por pieza; solo se registrará la cantidad.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextFaint,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        } else if (state.showSpecs) {
            item {
                AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    SectionLabel("Especificaciones técnicas")
                    if (state.showField("ip")) {
                        AppTextField(
                            value = state.ip,
                            onValueChange = viewModel::onIpChange,
                            label = { Text("IP") },
                            placeholder = { Text("192.168.0.1") },
                            enabled = !state.blocked,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    if (state.showField("macAddress")) {
                        AppTextField(
                            value = state.macAddress,
                            onValueChange = viewModel::onMacAddressChange,
                            label = { Text("MAC") },
                            placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                            enabled = !state.blocked,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    if (state.showField("operatingSystem")) {
                        AppTextField(
                            value = state.operatingSystem,
                            onValueChange = viewModel::onOperatingSystemChange,
                            label = { Text("Sistema operativo") },
                            enabled = !state.blocked,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    if (state.showField("ram")) {
                        AppTextField(
                            value = state.ram,
                            onValueChange = viewModel::onRamChange,
                            label = { Text("RAM") },
                            enabled = !state.blocked,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    if (state.showField("storage")) {
                        AppTextField(
                            value = state.storage,
                            onValueChange = viewModel::onStorageChange,
                            label = { Text("Almacenamiento") },
                            enabled = !state.blocked,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        item {
            PillButton(
                text = if (state.creating) {
                    if (state.quantity > 1) "Dar de alta ${state.quantity} unidades" else "Dar de alta"
                } else {
                    "Guardar cambios"
                },
                onClick = viewModel::submit,
                enabled = state.isValid && !state.blocked,
                loading = state.saving,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

/** Renglón plegable con la identificación de una pieza del alta. */
@Composable
private fun UnitIdentityRowCard(
    label: String,
    row: UnitIdentityRow,
    expanded: Boolean,
    showSerial: Boolean,
    showMac: Boolean,
    showIp: Boolean,
    showHostname: Boolean,
    onToggle: () -> Unit,
    onChange: (UnitIdentityField, String) -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShape.card,
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShape.card)
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = AppColors.TextMuted,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (row.filled) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Con datos de identificación",
                    tint = AppColors.Success,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = row.serialNumber.ifBlank { "—" },
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp),
            )
        }

        if (expanded) {
            Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                if (showSerial) {
                    AppTextField(
                        value = row.serialNumber,
                        onValueChange = { onChange(UnitIdentityField.SerialNumber, it) },
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
                            onValueChange = { onChange(UnitIdentityField.Mac, it) },
                            label = { Text("MAC") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        AppTextField(
                            value = row.ip,
                            onValueChange = { onChange(UnitIdentityField.Ip, it) },
                            label = { Text("IP") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else if (showMac) {
                    Spacer(Modifier.height(10.dp))
                    AppTextField(
                        value = row.macAddress,
                        onValueChange = { onChange(UnitIdentityField.Mac, it) },
                        label = { Text("MAC") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else if (showIp) {
                    Spacer(Modifier.height(10.dp))
                    AppTextField(
                        value = row.ip,
                        onValueChange = { onChange(UnitIdentityField.Ip, it) },
                        label = { Text("IP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (showHostname) {
                    Spacer(Modifier.height(10.dp))
                    AppTextField(
                        value = row.hostname,
                        onValueChange = { onChange(UnitIdentityField.Hostname, it) },
                        label = { Text("Nombre del equipo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
