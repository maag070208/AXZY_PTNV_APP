package com.axzydev.puertonuevoapp.feature.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.network.devices.DeviceDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.StatusChip

/**
 * "Agregar unidades": entrada de piezas NUEVAS (`STOCK_IN`) a un dispositivo que
 * ya existe. Equivale a la web (`/inventory/movements/new?deviceId=…&type=STOCK_IN`)
 * y al botón «Agregar unidades» del detalle del dispositivo.
 *
 * - La cantidad manda: se crean N piezas con su folio de activo fijo (tope 5000).
 * - La identificación por pieza (serie/MAC/IP/hostname) es opcional y puede venir
 *   incompleta; las piezas sin datos se capturan después desde el detalle.
 * - El envío lleva `Idempotency-Key`: un doble toque o un reintento no duplica la entrada.
 */
@Composable
fun DeviceStockInScreen(deviceId: String) {
    val navigator = LocalNavigator.current
    val viewModel: DeviceStockInViewModel = viewModel(key = "device-stock-in-$deviceId") {
        DeviceStockInViewModel(deviceId, AppContainer.devicesApi, AppContainer.inventoryApi)
    }
    val state by viewModel.uiState.collectAsState()
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val denied = user != null && !user.can(PermissionKeys.DEVICES_EDIT)

    // Reabrir la pantalla no debe mostrar el éxito de la entrada anterior.
    LaunchedEffect(Unit) { viewModel.onEnterScreen() }
    DisposableEffect(Unit) { onDispose { viewModel.onLeaveScreen() } }

    when {
        state.loading -> LoadingState(modifier = Modifier.fillMaxSize())

        state.loadError != null -> Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            BentoErrorState(message = state.loadError ?: "", onRetry = viewModel::load)
        }

        denied -> Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            BentoErrorState(
                message = "No tienes permiso para agregar unidades. Pídeselo a quien administra los permisos.",
                onRetry = null,
            )
        }

        state.result != null -> StockInSuccess(
            state = state,
            onDone = { navigator.pop() },
        )

        else -> StockInForm(state = state, viewModel = viewModel)
    }
}

/** Pantalla de éxito: cuántas piezas entraron y qué folios se generaron. */
@Composable
private fun StockInSuccess(state: DeviceStockInUiState, onDone: () -> Unit) {
    val result = state.result ?: return
    val device = state.device
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = AppColors.Success,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Entrada registrada",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.TextPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (result.quantity == 1) {
                        "Se agregó 1 unidad a ${device?.assetTag ?: "el dispositivo"}."
                    } else {
                        "Se agregaron ${result.quantity} unidades a ${device?.assetTag ?: "el dispositivo"}."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextMuted,
                    textAlign = TextAlign.Center,
                )
                val folios = when {
                    result.firstFolio == null -> null
                    result.quantity <= 1 -> "Folio: ${result.firstFolio}"
                    else -> "Folios: de ${result.firstFolio} a ${result.lastFolio}"
                }
                if (folios != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(folios, style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
                }
                if (result.reason.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Motivo: ${result.reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextFaint,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Puedes capturar o corregir la serie de cada pieza desde el detalle del dispositivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
        PillButton(
            text = "Volver al dispositivo",
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Formulario de la entrada de unidades. */
@Composable
private fun StockInForm(state: DeviceStockInUiState, viewModel: DeviceStockInViewModel) {
    val device = state.device ?: return
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { DeviceStockSummaryCard(device = device, state = state) }

        item {
            AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                SectionLabel("Piezas nuevas")
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
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.reason,
                    onValueChange = viewModel::onReasonChange,
                    label = { Text("Motivo (opcional)") },
                    placeholder = { Text("Ej. Llegó mercancía nueva") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.notes,
                    onValueChange = viewModel::onNotesChange,
                    label = { Text("Notas (opcional)") },
                    placeholder = { Text("Observaciones de la entrada…") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "La cantidad manda: las piezas sin identidad se crean igual y se capturan después desde el detalle del dispositivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextFaint,
                )
            }
        }

        if (state.usesUnitIdentity && state.rows.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    Text(
                        "Identificación de las piezas (${state.identifiedCount} de ${state.rows.size})",
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
            }
            itemsIndexed(items = state.rows, key = { _, row -> row.id }) { index, row ->
                StockInIdentityRowCard(
                    label = "Pieza ${index + 1}",
                    row = row,
                    expanded = row.id in state.expandedRowIds,
                    showSerial = state.showField("serialNumber"),
                    showMac = state.showField("macAddress"),
                    showIp = state.showField("ip"),
                    showHostname = state.showField("hostname"),
                    onToggle = { viewModel.toggleRow(row.id) },
                    onChange = { field, value -> viewModel.onIdentityChange(row.id, field, value) },
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

        state.submitError?.let { message ->
            item {
                Text(message, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
            }
        }

        item {
            PillButton(
                text = if (state.quantity > 1) "Registrar ${state.quantity} unidades" else "Registrar entrada",
                onClick = viewModel::submit,
                enabled = state.canSubmit,
                loading = state.saving,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

/** Resumen del dispositivo que recibe la entrada y sus existencias actuales. */
@Composable
private fun DeviceStockSummaryCard(device: DeviceDto, state: DeviceStockInUiState) {
    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("Dispositivo")
        Text(
            device.assetTag,
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            listOfNotNull(device.brand.ifBlank { null }, device.model.ifBlank { null }).joinToString(" "),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        device.type?.name?.let { typeName ->
            Spacer(Modifier.height(2.dp))
            Text("Tipo: $typeName", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
        }
        if (state.stock != null) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(label = "Disponibles ${state.available}", tone = BentoTone.Mint)
                StatusChip(label = "Activas ${state.active}", tone = BentoTone.Plain)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "La entrada sumará ${state.quantity.coerceAtLeast(0)} piezas disponibles.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
            )
        }
    }
}

/** Renglón plegable con la identificación de una pieza nueva. */
@Composable
private fun StockInIdentityRowCard(
    label: String,
    row: StockInIdentityRow,
    expanded: Boolean,
    showSerial: Boolean,
    showMac: Boolean,
    showIp: Boolean,
    showHostname: Boolean,
    onToggle: () -> Unit,
    onChange: (StockInIdentityField, String) -> Unit,
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
    }
}
