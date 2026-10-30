package com.axzydev.puertonuevoapp.feature.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.devices.DeviceDto
import com.axzydev.puertonuevoapp.core.network.inventory.DeviceUnitDto
import com.axzydev.puertonuevoapp.core.network.inventory.StockLedgerRowDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoMiniStat
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.BentoTabItem
import com.axzydev.puertonuevoapp.core.ui.BentoTabs
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.deviceStatusLabel
import com.axzydev.puertonuevoapp.core.util.formatDateTime
import com.axzydev.puertonuevoapp.core.util.formatShortDate

/**
 * Detalle de un dispositivo: ficha, **existencias**, **piezas físicas** (con
 * edición de la identificación de cada una) y **kardex** del dispositivo. El
 * botón «Agregar unidades» (solo `devices.edit`) abre la entrada `STOCK_IN` de
 * piezas nuevas, que es el momento real de capturar su serie.
 *
 * Referencia web: `web/src/pages/inventory/DeviceDetailPage.tsx`.
 */
@Composable
fun DeviceDetailScreen(deviceId: String) {
    val viewModel: DeviceDetailViewModel = viewModel(key = "device-$deviceId") {
        DeviceDetailViewModel(deviceId, AppContainer.devicesApi, AppContainer.inventoryApi)
    }
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canEditDevice = user?.canEditDevices == true
    val canRegisterMovement = user?.canRegisterMovement == true

    // Al volver de "Agregar unidades" o de un movimiento, las piezas/kardex cambiaron.
    LaunchedEffect(Unit) { viewModel.refreshOnReturn() }
    DisposableEffect(Unit) { onDispose { viewModel.armRefreshOnReturn() } }

    LaunchedEffect(state.unitSaved) {
        if (state.unitSaved) {
            AppSnackbar.show("Identificación de la pieza actualizada")
            viewModel.consumeUnitSaved()
        }
    }

    when {
        state.loading -> LoadingState(modifier = Modifier.fillMaxSize())

        state.error != null || state.device == null -> ErrorState(
            message = state.error ?: "Dispositivo no encontrado",
            modifier = Modifier.fillMaxSize(),
            onRetry = viewModel::load,
        )

        else -> {
            val device = state.device ?: return
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    DeviceHeaderCard(
                        device = device,
                        state = state,
                        canEditDevice = canEditDevice,
                        canRegisterMovement = canRegisterMovement,
                        onEdit = { navigator.push(Screen.DeviceForm(device.id)) },
                        onMovement = { navigator.push(Screen.NewInventoryMovement(device.id)) },
                        onAddUnits = { navigator.push(Screen.DeviceStockIn(device.id)) },
                    )
                }

                item { StockCard(state = state) }

                item {
                    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        SectionLabel("Información")
                        InfoRow("Tipo", device.type?.name ?: "—")
                        InfoRow("Marca", device.brand)
                        InfoRow("Modelo", device.model)
                        device.type?.prefix?.let { InfoRow("Prefijo de folio", it) }
                        InfoRow("Área", device.area)
                    }
                }

                val hasSpecs = device.ip != null || device.macAddress != null || device.operatingSystem != null ||
                    device.ram != null || device.storage != null
                if (hasSpecs) {
                    item {
                        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            SectionLabel("Especificaciones técnicas")
                            device.operatingSystem?.let { InfoRow("Sistema operativo", it) }
                            device.ram?.let { InfoRow("RAM", it) }
                            device.storage?.let { InfoRow("Almacenamiento", it) }
                            device.ip?.let { InfoRow("IP", it) }
                            device.macAddress?.let { InfoRow("MAC", it) }
                        }
                    }
                }

                // --- Piezas físicas -------------------------------------------------
                item { SectionHeader(title = "Unidades físicas", count = state.units.size) }
                if (state.units.isNotEmpty()) {
                    item { UnitFilterTabs(state = state, onSelect = viewModel::onUnitFilterChange) }
                }
                when {
                    state.extrasLoading && state.units.isEmpty() -> item {
                        BentoState(message = "Cargando piezas…", loading = true)
                    }

                    state.extrasError != null && state.units.isEmpty() -> item {
                        BentoErrorState(message = state.extrasError ?: "", onRetry = viewModel::loadExtras)
                    }

                    state.filteredUnits.isEmpty() -> item {
                        BentoState(
                            message = if (state.units.isEmpty()) {
                                "Este dispositivo no tiene piezas registradas. Usa «Agregar unidades» cuando lleguen piezas nuevas."
                            } else {
                                "No hay piezas en este estado."
                            },
                        )
                    }

                    else -> items(items = state.filteredUnits, key = { it.id }) { unit ->
                        UnitCard(
                            unit = unit,
                            canEdit = canEditDevice,
                            onClick = { viewModel.startEditUnit(unit) },
                        )
                    }
                }

                // Fallo parcial (p. ej. el kardex) sin tumbar la ficha ni las piezas.
                state.extrasError?.let { message ->
                    if (state.units.isNotEmpty()) {
                        item { BentoErrorState(message = message, onRetry = viewModel::loadExtras) }
                    }
                }

                // --- Kardex ----------------------------------------------------------
                item { SectionHeader(title = "Kardex", count = state.ledger.size) }
                if (state.ledger.isEmpty()) {
                    item {
                        BentoState(
                            message = if (state.extrasLoading) "Cargando movimientos…" else "Sin movimientos registrados.",
                            loading = state.extrasLoading,
                        )
                    }
                } else {
                    val rows = state.ledger.asReversed()
                    itemsIndexed(
                        items = rows,
                        key = { index, row -> "${row.date}|${row.type}|$index" },
                    ) { _, row ->
                        KardexRow(row = row)
                    }
                }

                if (device.custodyLetterItems.isNotEmpty()) {
                    item {
                        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            SectionLabel("Cartas responsivas activas")
                            device.custodyLetterItems.forEach { item ->
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
                }

                item { Spacer(Modifier.height(24.dp)) }
            }

            state.editingUnit?.let { unit ->
                EditUnitDialog(
                    unit = unit,
                    state = state,
                    viewModel = viewModel,
                )
            }
        }
    }
}

/** Ficha del dispositivo + acciones por permiso. */
@Composable
private fun DeviceHeaderCard(
    device: DeviceDto,
    state: DeviceDetailUiState,
    canEditDevice: Boolean,
    canRegisterMovement: Boolean,
    onEdit: () -> Unit,
    onMovement: () -> Unit,
    onAddUnits: () -> Unit,
) {
    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    device.assetTag,
                    style = MaterialTheme.typography.headlineSmall,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                device.type?.name?.let { typeName ->
                    Text(typeName, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
                }
                Text(
                    listOfNotNull(device.brand.ifBlank { null }, device.model.ifBlank { null }).joinToString(" "),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextFaint,
                )
            }
            StatusChip(label = deviceStatusLabel(device.status), tone = deviceStatusTone(device.status))
        }

        if (canEditDevice || canRegisterMovement) {
            Spacer(Modifier.height(14.dp))
            if (canEditDevice) {
                PillButton(
                    text = "Agregar unidades",
                    onClick = onAddUnits,
                    leadingIcon = Icons.Filled.Add,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canEditDevice) {
                    PillButton(
                        text = "Editar",
                        onClick = onEdit,
                        variant = PillButtonVariant.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (canRegisterMovement) {
                    PillButton(
                        text = "Movimiento",
                        onClick = onMovement,
                        variant = PillButtonVariant.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            Spacer(Modifier.height(10.dp))
            Text(
                "Sin permiso para modificar el inventario (solo lectura).",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
            )
        }

        if (state.activeUnits > 0) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Activas: ${state.activeUnits} · Disponibles: ${state.available} · En uso: ${state.onLoan}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
        }
    }
}

/** Existencias por estado (`GET /inventory/devices/:id/stock`) como KPIs compactos. */
@Composable
private fun StockCard(state: DeviceDetailUiState) {
    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("Existencias")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            BentoMiniStat(
                label = "Disponibles",
                value = state.available.toString(),
                tone = if (state.available > 0) BentoTone.Mint else BentoTone.Plain,
                modifier = Modifier.weight(1f),
            )
            BentoMiniStat(
                label = "En uso",
                value = state.onLoan.toString(),
                tone = if (state.onLoan > 0) BentoTone.Sun else BentoTone.Plain,
                modifier = Modifier.weight(1f),
            )
            BentoMiniStat(
                label = "Dañadas",
                value = state.damaged.toString(),
                tone = if (state.damaged > 0) BentoTone.Rose else BentoTone.Plain,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            BentoMiniStat(
                label = "Mantenimiento",
                value = state.inMaintenance.toString(),
                tone = if (state.inMaintenance > 0) BentoTone.Sea else BentoTone.Plain,
                labelMaxLines = 2,
                modifier = Modifier.weight(1f),
            )
            BentoMiniStat(
                label = "Bajas",
                value = state.retired.toString(),
                tone = if (state.retired > 0) BentoTone.Plain else BentoTone.Plain,
                modifier = Modifier.weight(1f),
            )
            BentoMiniStat(
                label = "Históricas",
                value = state.historicalUnits.toString(),
                tone = BentoTone.Plain,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Activas: ${state.activeUnits} · Históricas: ${state.historicalUnits}",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextFaint,
        )
    }
}

/** Etiqueta de sección con contador (unidades, kardex). */
@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextFaint,
        )
    }
}

/** Pestañas de filtro por estado de pieza, con su conteo. */
@Composable
private fun UnitFilterTabs(state: DeviceDetailUiState, onSelect: (String) -> Unit) {
    val filters = buildList {
        add(DEVICE_UNIT_FILTER_ALL)
        deviceUnitStatusOrder.forEach { status ->
            if ((state.unitCounts[status] ?: 0) > 0) add(status)
        }
    }
    val tabs = filters.map { filter ->
        BentoTabItem(
            label = if (filter == DEVICE_UNIT_FILTER_ALL) {
                "Todas (${state.units.size})"
            } else {
                "${unitStatusLabel(filter)} (${state.unitCounts[filter] ?: 0})"
            },
        )
    }
    BentoTabs(
        items = tabs,
        selectedIndex = filters.indexOf(state.unitFilter).coerceAtLeast(0),
        onSelect = { index -> onSelect(filters.getOrElse(index) { DEVICE_UNIT_FILTER_ALL }) },
    )
}

/** Una pieza física: activo fijo, identificación y estado. Pulsable para editar. */
@Composable
private fun UnitCard(unit: DeviceUnitDto, canEdit: Boolean, onClick: () -> Unit) {
    val identity = listOfNotNull(
        unit.serialNumber?.takeIf { it.isNotBlank() }?.let { "Serie: $it" },
        unit.macAddress?.takeIf { it.isNotBlank() }?.let { "MAC: $it" },
        unit.ip?.takeIf { it.isNotBlank() }?.let { "IP: $it" },
        unit.hostname?.takeIf { it.isNotBlank() }?.let { "Equipo: $it" },
    )
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (canEdit) onClick else null,
        shape = AppShape.card,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    unit.assetTag.ifBlank { "Sin folio" },
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (identity.isEmpty()) "Sin identificación" else identity.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (identity.isEmpty()) AppColors.TextFaint else AppColors.TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (unit.area.isNotBlank()) {
                    Text("Área: ${unit.area}", style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(unitStatusLabel(unit.status), unitStatusTone(unit.status))
                if (canEdit) {
                    Spacer(Modifier.height(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Editar la identificación de la pieza",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/** Un renglón del kardex con su entrada/salida y el saldo resultante. */
@Composable
private fun KardexRow(row: StockLedgerRowDto) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShape.row,
        contentPadding = PaddingValues(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        kardexTypeLabel(row.type),
                        style = MaterialTheme.typography.titleSmall,
                        color = AppColors.TextPrimary,
                    )
                    when {
                        row.stockIn > 0 -> {
                            Spacer(Modifier.width(6.dp))
                            Text("+${row.stockIn}", style = MaterialTheme.typography.titleSmall, color = AppColors.Success)
                        }
                        row.stockOut > 0 -> {
                            Spacer(Modifier.width(6.dp))
                            Text("−${row.stockOut}", style = MaterialTheme.typography.titleSmall, color = AppColors.Danger)
                        }
                    }
                }
                Text(
                    listOfNotNull(
                        formatDateTime(row.date).takeIf { it != "—" },
                        row.user?.takeIf { it.isNotBlank() }?.let { "Por $it" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                )
                row.reason?.takeIf { it.isNotBlank() }?.let { reason ->
                    Text(
                        reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            StatusChip(label = "Saldo ${row.balance}", tone = BentoTone.Plain)
        }
    }
}

/** Diálogo de edición de la identificación de una pieza (`PUT /inventory/units/:id`). */
@Composable
private fun EditUnitDialog(
    unit: DeviceUnitDto,
    state: DeviceDetailUiState,
    viewModel: DeviceDetailViewModel,
) {
    AppModal(
        title = "Editar ${unit.assetTag}",
        onDismiss = viewModel::dismissEditUnit,
        icon = Icons.Filled.Edit,
        confirmLabel = "Guardar",
        onConfirm = viewModel::saveUnit,
        confirmEnabled = !state.editSaving,
        saving = state.editSaving,
    ) {
        if (state.showEditField("serialNumber")) {
            AppTextField(
                value = state.editSerialNumber,
                onValueChange = viewModel::onEditSerialNumberChange,
                label = { Text("No. de serie") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
        }
        if (state.showEditField("macAddress")) {
            AppTextField(
                value = state.editMacAddress,
                onValueChange = viewModel::onEditMacAddressChange,
                label = { Text("MAC") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
        }
        if (state.showEditField("ip")) {
            AppTextField(
                value = state.editIp,
                onValueChange = viewModel::onEditIpChange,
                label = { Text("IP") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
        }
        if (state.showEditField("hostname")) {
            AppTextField(
                value = state.editHostname,
                onValueChange = viewModel::onEditHostnameChange,
                label = { Text("Nombre del equipo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
        }
        AppTextField(
            value = state.editArea,
            onValueChange = viewModel::onEditAreaChange,
            label = { Text("Área") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        state.editError?.let { message ->
            Spacer(Modifier.height(10.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Danger,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "El departamento no se cambia aquí: se mueve con un traspaso para que quede en el kardex y en la bitácora.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextFaint,
        )
    }
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

// ---------------------------------------------------------------------------
// Etiquetas y tonos de piezas / kardex (los enums son texto en el API)
// ---------------------------------------------------------------------------

/** Tono del chip del estado del dispositivo (mismo criterio que las piezas). */
private fun deviceStatusTone(status: String): BentoTone = when (status) {
    "AVAILABLE" -> BentoTone.Mint
    "ASSIGNED" -> BentoTone.Sun
    "DAMAGED" -> BentoTone.Rose
    "IN_MAINTENANCE" -> BentoTone.Sea
    else -> BentoTone.Plain
}

private fun unitStatusLabel(status: String): String = when (status) {
    "AVAILABLE" -> "Disponible"
    "ON_LOAN" -> "En uso"
    "DAMAGED" -> "Dañada"
    "IN_MAINTENANCE" -> "Mantenimiento"
    "RETIRED" -> "Baja"
    else -> status
}

private fun unitStatusTone(status: String): BentoTone = when (status) {
    "AVAILABLE" -> BentoTone.Mint
    "ON_LOAN" -> BentoTone.Sun
    "DAMAGED" -> BentoTone.Rose
    "IN_MAINTENANCE" -> BentoTone.Lavender
    else -> BentoTone.Plain
}

private fun kardexTypeLabel(type: String): String = when (type) {
    "STOCK_IN" -> "Entrada"
    "STOCK_OUT" -> "Salida"
    "LOAN" -> "Préstamo"
    "RETURN" -> "Devolución"
    "RETIREMENT" -> "Baja"
    "TRANSFER" -> "Traslado"
    "ADJUSTMENT_IN" -> "Ajuste de entrada"
    "ADJUSTMENT_OUT" -> "Ajuste de salida"
    "MAINTENANCE_IN" -> "Entrada a mantenimiento"
    "MAINTENANCE_OUT" -> "Salida de mantenimiento"
    "REVERSAL" -> "Reversión"
    else -> type
}
