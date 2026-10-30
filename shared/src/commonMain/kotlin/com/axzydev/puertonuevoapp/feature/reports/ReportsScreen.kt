package com.axzydev.puertonuevoapp.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.materialoutputs.MaterialOutputDto
import com.axzydev.puertonuevoapp.core.network.reports.AssignedDeviceRowDto
import com.axzydev.puertonuevoapp.core.network.reports.DeliveryRowDto
import com.axzydev.puertonuevoapp.core.network.reports.DeviceAggregateDto
import com.axzydev.puertonuevoapp.core.network.reports.DeviceReportRowDto
import com.axzydev.puertonuevoapp.core.network.reports.PeriodBucketDto
import com.axzydev.puertonuevoapp.core.network.reports.RecipientAggregateDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.BentoTabItem
import com.axzydev.puertonuevoapp.core.ui.BentoTabs
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.RegisterTopBarAction
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.StatCard
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.deviceStatusLabel
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import androidx.compose.ui.text.style.TextOverflow
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import androidx.compose.ui.graphics.Color
import com.axzydev.puertonuevoapp.core.theme.BentoTone

/**
 * Reportes al nivel de la web: Asignados, Dispositivos y Salidas de material
 * (más el Resumen por periodo y la Bitácora de entregas que expone el API),
 * con un filtro de periodo compartido, KPIs del conjunto filtrado y exportación
 * CSV de la pestaña activa.
 *
 * Exportaciones: la Bitácora usa el CSV real del API (`GET /reports/.csv`);
 * las demás devuelven el universo filtrado en JSON y la app lo convierte a CSV
 * (no existe CSV del servidor para ellas). Requiere `reports.view`; exportar,
 * además, `reports.export`.
 */
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = viewModel {
        ReportsViewModel(AppContainer.reportsApi, AppContainer.materialOutputsApi)
    },
) {
    val state by viewModel.uiState.collectAsState()
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canView = user?.can(PermissionKeys.REPORTS_VIEW) == true
    val canExport = user?.can(PermissionKeys.REPORTS_EXPORT) == true
    // Editar/registrar salidas de material reutiliza la pantalla existente.
    val canRegisterOutputs = user?.can(PermissionKeys.MATERIAL_OUTPUTS_REGISTER) == true
    val navigator = LocalNavigator.current

    if (canExport) {
        RegisterTopBarAction(Icons.Filled.FileDownload, "Exportar CSV", viewModel::exportCsv)
    }

    if (!canView) {
        Column(modifier = Modifier.fillMaxSize().background(AppBento.canvas).padding(16.dp)) {
            BentoState(message = "Necesitas el permiso de reportes (reports.view).")
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBento.canvas)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 14.dp)) {
            BentoTabs(
                items = ReportTab.entries.map { BentoTabItem(label = it.label, icon = tabIcon(it)) },
                selectedIndex = state.tab.ordinal,
                onSelect = { index -> viewModel.setTab(ReportTab.entries[index]) },
            )
            Spacer(Modifier.height(12.dp))
            when (state.tab) {
                ReportTab.Assigned, ReportTab.Devices, ReportTab.Outputs -> PeriodBar(
                    period = state.period,
                    onPreset = viewModel::onPreset,
                    onApplyRange = viewModel::applyCustomRange,
                    onClear = viewModel::clearPeriod,
                )
                ReportTab.Summary, ReportTab.Deliveries -> SummaryPeriodBar(
                    period = state.summaryPeriod,
                    date = state.summaryDate,
                    onPeriod = viewModel::setSummaryPeriod,
                    onDate = viewModel::setSummaryDate,
                )
            }

            state.exportMessage?.let { message ->
                Spacer(Modifier.height(10.dp))
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = AppColors.Success.copy(alpha = 0.10f),
                    borderColor = AppColors.Success.copy(alpha = 0.35f),
                    contentPadding = PaddingValues(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
                        AppFilterChip(label = "Ocultar", selected = false, onClick = viewModel::clearExportFeedback)
                    }
                }
            }
            state.exportError?.let { message ->
                Spacer(Modifier.height(10.dp))
                BentoErrorState(message = message, onRetry = null)
            }
            Spacer(Modifier.height(12.dp))
        }

        when (state.tab) {
            ReportTab.Assigned -> AssignedTab(
                state = state,
                onRetry = { viewModel.loadAssigned(reset = true) },
                onLoadMore = viewModel::loadMoreActive,
            )
            ReportTab.Devices -> DevicesTab(
                state = state,
                onRetry = { viewModel.loadDevices(reset = true) },
                onLoadMore = viewModel::loadMoreActive,
            )
            ReportTab.Outputs -> OutputsTab(
                state = state,
                onRetry = { viewModel.loadOutputs(reset = true) },
                onLoadMore = viewModel::loadMoreActive,
                onOpen = if (canRegisterOutputs) {
                    { row -> navigator.push(Screen.MaterialOutputForm(row.id)) }
                } else {
                    null
                },
            )
            ReportTab.Summary -> SummaryTab(
                state = state,
                onRetry = viewModel::loadSummary,
            )
            ReportTab.Deliveries -> DeliveriesTab(
                state = state,
                onRetry = { viewModel.loadDeliveries(reset = true) },
                onLoadMore = viewModel::loadMoreActive,
            )
        }
    }
}

private fun tabIcon(tab: ReportTab): ImageVector = when (tab) {
    ReportTab.Assigned -> Icons.Filled.Assignment
    ReportTab.Devices -> Icons.Filled.Devices
    ReportTab.Outputs -> Icons.Filled.DeleteOutline
    ReportTab.Summary -> Icons.Filled.DateRange
    ReportTab.Deliveries -> Icons.Filled.ListAlt
}

// ---------------------------------------------------------------------------
// Filtros
// ---------------------------------------------------------------------------

/** Píldoras de rango rápido + rango libre (`YYYY-MM-DD`) compartido por las tablas. */
@Composable
private fun PeriodBar(
    period: PeriodRange,
    onPreset: (PeriodPreset) -> Unit,
    onApplyRange: (String, String) -> String?,
    onClear: () -> Unit,
) {
    var startText by remember(period) { mutableStateOf(period.start) }
    var endText by remember(period) { mutableStateOf(period.end) }
    val presets = listOf(PeriodPreset.All, PeriodPreset.Last7, PeriodPreset.Last30, PeriodPreset.Month)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            presets.forEach { preset ->
                AppFilterChip(
                    label = preset.label,
                    selected = period.preset == preset,
                    onClick = { onPreset(preset) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AppTextField(
                value = startText,
                onValueChange = { startText = it.filter { ch -> ch.isDigit() || ch == '-' }.take(10) },
                placeholder = { Text("Desde AAAA-MM-DD") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            AppTextField(
                value = endText,
                onValueChange = { endText = it.filter { ch -> ch.isDigit() || ch == '-' }.take(10) },
                placeholder = { Text("Hasta AAAA-MM-DD") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            AppFilterChip(
                label = "Aplicar",
                selected = true,
                onClick = {
                    onApplyRange(startText, endText)?.let { error -> AppSnackbar.showError(error) }
                },
            )
            AppFilterChip(
                label = "Limpiar",
                selected = false,
                onClick = onClear,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Periodo: ${period.label}",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextFaint,
        )
    }
}

/** Ventana del resumen y de la bitácora: granularidad (Día/Semana/Mes) + día ancla. */
@Composable
private fun SummaryPeriodBar(
    period: SummaryPeriod,
    date: String,
    onPeriod: (SummaryPeriod) -> Unit,
    onDate: (String) -> Unit,
) {
    var dateText by remember(date) { mutableStateOf(date) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryPeriod.entries.forEach { option ->
                AppFilterChip(
                    label = option.label,
                    selected = period == option,
                    onClick = { onPeriod(option) },
                )
            }
            AppTextField(
                value = dateText,
                onValueChange = {
                    val cleaned = it.filter { ch -> ch.isDigit() || ch == '-' }.take(10)
                    dateText = cleaned
                    onDate(cleaned)
                },
                placeholder = { Text("Día base AAAA-MM-DD") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(190.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Ventana según el día base (zona del dispositivo).",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextFaint,
        )
    }
}

// ---------------------------------------------------------------------------
// Pestaña Asignados
// ---------------------------------------------------------------------------

@Composable
private fun AssignedTab(state: ReportsUiState, onRetry: () -> Unit, onLoadMore: () -> Unit) {
    val list = state.assigned
    Column(modifier = Modifier.fillMaxSize()) {
        state.assignedStats?.let { stats ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatCard(
                    value = stats.assigned.toString(),
                    label = "Asignados",
                    color = AppColors.EmeraldPrimary,
                    icon = { Icon(Icons.Filled.Assignment, null, tint = AppColors.EmeraldPrimary) },
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = stats.averageDays.toInt().toString(),
                    label = "Días prom.",
                    color = AppColors.Warning,
                    icon = { Icon(Icons.Filled.Schedule, null, tint = AppColors.Warning) },
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = stats.over30.toString(),
                    label = "+30 días",
                    color = AppColors.Danger,
                    icon = { Icon(Icons.Filled.Warning, null, tint = AppColors.Danger) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        when {
            list.loading && list.items.isEmpty() -> LoadingState(modifier = Modifier.weight(1f))
            list.error != null && list.items.isEmpty() -> ErrorState(list.error, Modifier.weight(1f), onRetry = onRetry)
            list.items.isEmpty() -> EmptyState("No hay unidades asignadas en el periodo", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(list.items, key = { it.unitId.ifBlank { it.deviceId + it.assetTag } }) { row ->
                    AssignedReportCard(row)
                }
                if (list.hasMore) {
                    item(key = "more") { LoadMoreRow(list.loadingMore, onLoadMore) }
                }
            }
        }
    }
}

@Composable
private fun AssignedReportCard(row: AssignedDeviceRowDto) {
    AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.assetTag,
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Text(formatShortDate(row.date), style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
        }
        Text(
            "${row.description} · ${row.type}",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Responsable: ${row.custodian}" + (row.employeeNumber?.let { " (No. $it)" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        row.department?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            StatusChip(row.folio ?: "Sin folio", BentoTone.Sea)
            StatusChip(
                label = if (row.source == "CUSTODY_LETTER") "Carta" else "Movimiento",
                tone = if (row.source == "CUSTODY_LETTER") BentoTone.Mint else BentoTone.Sun,
            )
            val days = row.daysAssigned ?: 0
            StatusChip("$days día(s)", if (days > 30) BentoTone.Rose else BentoTone.Plain)
        }
    }
}

// ---------------------------------------------------------------------------
// Pestaña Dispositivos
// ---------------------------------------------------------------------------

@Composable
private fun DevicesTab(state: ReportsUiState, onRetry: () -> Unit, onLoadMore: () -> Unit) {
    val list = state.devices
    Column(modifier = Modifier.fillMaxSize()) {
        state.devicesStats?.let { stats ->
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        value = stats.total.toString(),
                        label = "Total",
                        color = AppColors.TextPrimary,
                        icon = { Icon(Icons.Filled.Devices, null, tint = AppColors.TextPrimary) },
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        value = stats.available.toString(),
                        label = "Disponibles",
                        color = AppColors.Success,
                        icon = { Icon(Icons.Filled.Devices, null, tint = AppColors.Success) },
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        value = stats.assigned.toString(),
                        label = "Asignados",
                        color = AppColors.Warning,
                        icon = { Icon(Icons.Filled.Assignment, null, tint = AppColors.Warning) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        value = stats.over30.toString(),
                        label = "+30 días",
                        color = AppColors.Danger,
                        icon = { Icon(Icons.Filled.Warning, null, tint = AppColors.Danger) },
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        value = stats.retired.toString(),
                        label = "Baja",
                        color = AppColors.TextMuted,
                        icon = { Icon(Icons.Filled.Warning, null, tint = AppColors.TextMuted) },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        when {
            list.loading && list.items.isEmpty() -> LoadingState(modifier = Modifier.weight(1f))
            list.error != null && list.items.isEmpty() -> ErrorState(list.error, Modifier.weight(1f), onRetry = onRetry)
            list.items.isEmpty() -> EmptyState("No hay dispositivos en el periodo", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(list.items, key = { it.unitId.ifBlank { it.deviceId + it.assetTag } }) { row ->
                    DeviceReportCard(row)
                }
                if (list.hasMore) {
                    item(key = "more") { LoadMoreRow(list.loadingMore, onLoadMore) }
                }
            }
        }
    }
}

@Composable
private fun DeviceReportCard(row: DeviceReportRowDto) {
    AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.assetTag,
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            StatusChip(deviceStatusLabel(row.status), toneOf(statusColor(row.status)))
        }
        Text(
            "${row.description} · ${row.type}",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "Área: ${row.area}",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        row.serialNumber?.takeIf { it.isNotBlank() }?.let {
            Text(
                "Serie: $it",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        row.hostname?.takeIf { it.isNotBlank() }?.let {
            Text(
                "Hostname: $it",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        row.custodian?.takeIf { it.isNotBlank() }?.let { custodian ->
            Text(
                "Responsable: $custodian" + (row.employeeNumber?.let { " (No. $it)" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            StatusChip("×${row.quantity}", BentoTone.Sea)
            row.folio?.takeIf { it.isNotBlank() }?.let { StatusChip(it, BentoTone.Plain) }
            row.daysAssigned?.let { days ->
                StatusChip("$days día(s)", if (days > 30) BentoTone.Rose else BentoTone.Plain)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pestaña Salidas de material
// ---------------------------------------------------------------------------

@Composable
private fun OutputsTab(
    state: ReportsUiState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpen: ((MaterialOutputDto) -> Unit)?,
) {
    val list = state.outputs
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatCard(
                value = list.total.toString(),
                label = "Salidas en el periodo",
                color = AppColors.EmeraldPrimary,
                icon = { Icon(Icons.Filled.DeleteOutline, null, tint = AppColors.EmeraldPrimary) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        when {
            list.loading && list.items.isEmpty() -> LoadingState(modifier = Modifier.weight(1f))
            list.error != null && list.items.isEmpty() -> ErrorState(list.error, Modifier.weight(1f), onRetry = onRetry)
            list.items.isEmpty() -> EmptyState("No hay salidas de material en el periodo", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(list.items, key = { it.id }) { row ->
                    OutputReportCard(row, onClick = onOpen?.let { open -> { open(row) } })
                }
                if (list.hasMore) {
                    item(key = "more") { LoadMoreRow(list.loadingMore, onLoadMore) }
                }
            }
        }
    }
}

@Composable
private fun OutputReportCard(row: MaterialOutputDto, onClick: (() -> Unit)? = null) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.description,
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(formatShortDate(row.date), style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("×${row.quantity}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
            Spacer(Modifier.weight(1f))
            StatusChip(outputReasonLabel(row.reason), BentoTone.Plain)
        }
        Text(
            "${row.departmentName} · ${row.userName}",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        listOfNotNull(row.project?.takeIf { it.isNotBlank() }, row.notes?.takeIf { it.isNotBlank() })
            .forEach { extra ->
                Text(
                    extra,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
    }
}

// ---------------------------------------------------------------------------
// Pestaña Resumen por periodo
// ---------------------------------------------------------------------------

@Composable
private fun SummaryTab(state: ReportsUiState, onRetry: () -> Unit) {
    when {
        state.summary.loading && state.summary.data == null -> LoadingState(modifier = Modifier.fillMaxSize())
        state.summary.error != null && state.summary.data == null ->
            ErrorState(state.summary.error, Modifier.fillMaxSize(), onRetry = onRetry)
        state.summary.data == null -> EmptyState("Elige el periodo y vuelve a intentar", Modifier.fillMaxSize())
        else -> {
            val summary = state.summary.data
            val totals = summary?.totals
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "range") {
                    Text(
                        "Ventana: ${formatShortDate(summary?.range?.start)} a ${formatShortDate(summary?.range?.end)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                    )
                }
                item(key = "totals1") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard(
                            value = (totals?.delivered ?: 0).toString(),
                            label = "Entregado",
                            color = AppColors.EmeraldPrimary,
                            icon = { Icon(Icons.Filled.Assignment, null, tint = AppColors.EmeraldPrimary) },
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            value = (totals?.returned ?: 0).toString(),
                            label = "Devuelto",
                            color = AppColors.Success,
                            icon = { Icon(Icons.Filled.Devices, null, tint = AppColors.Success) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item(key = "totals2") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard(
                            value = (totals?.admitted ?: 0).toString(),
                            label = "Altas",
                            color = AppColors.Info,
                            icon = { Icon(Icons.Filled.ListAlt, null, tint = AppColors.Info) },
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            value = (totals?.net ?: 0).toString(),
                            label = "Neto",
                            color = AppColors.Warning,
                            icon = { Icon(Icons.Filled.Schedule, null, tint = AppColors.Warning) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item(key = "peopleTitle") { SectionLabel("Personas") }
                if (summary?.recipients.isNullOrEmpty()) {
                    item(key = "peopleEmpty") { BentoState(message = "Sin movimientos de personas en el periodo") }
                } else {
                    items(summary?.recipients.orEmpty(), key = { it.userId }) { recipient -> RecipientRow(recipient) }
                }
                item(key = "devicesTitle") { SectionLabel("Por dispositivo") }
                if (summary?.byDevice.isNullOrEmpty()) {
                    item(key = "devicesEmpty") { BentoState(message = "Sin dispositivos movidos en el periodo") }
                } else {
                    items(summary?.byDevice.orEmpty(), key = { it.deviceId }) { device -> DeviceAggregateRow(device) }
                }
                item(key = "bucketsTitle") { SectionLabel("Serie diaria") }
                if (summary?.byPeriod.isNullOrEmpty()) {
                    item(key = "bucketsEmpty") { BentoState(message = "Sin días con movimientos") }
                } else {
                    items(summary?.byPeriod.orEmpty(), key = { it.day }) { bucket -> BucketRow(bucket) }
                }
            }
        }
    }
}

@Composable
private fun RecipientRow(row: RecipientAggregateDto) {
    AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.name,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            StatusChip("Pend. ${row.outstanding}", if (row.outstanding > 0) BentoTone.Sun else BentoTone.Plain)
        }
        row.departmentName?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            StatusChip("Cartas ${row.loans}", BentoTone.Plain)
            StatusChip("Entregado ${row.delivered}", BentoTone.Sea)
            StatusChip("Devuelto ${row.returned}", BentoTone.Mint)
        }
    }
}

@Composable
private fun DeviceAggregateRow(row: DeviceAggregateDto) {
    AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.description,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                row.typeName,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            StatusChip("Entregado ${row.delivered}", BentoTone.Sea)
            StatusChip("Devuelto ${row.returned}", BentoTone.Mint)
            StatusChip("Alta ${row.admitted}", BentoTone.Lavender)
            StatusChip("Neto ${row.net}", BentoTone.Sun)
        }
    }
}

@Composable
private fun BucketRow(row: PeriodBucketDto) {
    AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(formatShortDate(row.day), style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
            Text("Neto ${row.net}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
        }
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            StatusChip("Entregado ${row.delivered}", BentoTone.Sea)
            StatusChip("Devuelto ${row.returned}", BentoTone.Mint)
            StatusChip("Alta ${row.admitted}", BentoTone.Lavender)
        }
    }
}

// ---------------------------------------------------------------------------
// Pestaña Bitácora de entregas
// ---------------------------------------------------------------------------

@Composable
private fun DeliveriesTab(state: ReportsUiState, onRetry: () -> Unit, onLoadMore: () -> Unit) {
    val list = state.deliveries
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatCard(
                value = list.total.toString(),
                label = "Entregas del periodo",
                color = AppColors.EmeraldPrimary,
                icon = { Icon(Icons.Filled.ListAlt, null, tint = AppColors.EmeraldPrimary) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        when {
            list.loading && list.items.isEmpty() -> LoadingState(modifier = Modifier.weight(1f))
            list.error != null && list.items.isEmpty() -> ErrorState(list.error, Modifier.weight(1f), onRetry = onRetry)
            list.items.isEmpty() -> EmptyState("No hay entregas en el periodo", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(list.items, key = { it.id }) { row -> DeliveryReportCard(row) }
                if (list.hasMore) {
                    item(key = "more") { LoadMoreRow(list.loadingMore, onLoadMore) }
                }
            }
        }
    }
}

@Composable
private fun DeliveryReportCard(row: DeliveryRowDto) {
    AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.number,
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Text(formatShortDate(row.date), style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
        }
        Text(
            "${row.description} · ${row.typeName}",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            row.custodianName + (row.departmentName?.let { " · $it" } ?: ""),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            StatusChip("×${row.quantity}", BentoTone.Sea)
            StatusChip("Devuelto ${row.returnedQuantity}", BentoTone.Mint)
            StatusChip(deliveryStatusLabel(row.status), toneOf(deliveryStatusColor(row.status)))
        }
    }
}

// ---------------------------------------------------------------------------
// Piezas compartidas
// ---------------------------------------------------------------------------

@Composable
private fun LoadMoreRow(loading: Boolean, onLoadMore: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        PillButton(
            text = "Cargar más",
            onClick = onLoadMore,
            variant = PillButtonVariant.Ghost,
            loading = loading,
        )
    }
}

private fun statusColor(status: String) = when (status) {
    "AVAILABLE", "RETURNED" -> AppColors.Success
    "ASSIGNED", "PARTIAL" -> AppColors.Warning
    "DAMAGED" -> AppColors.Danger
    "IN_MAINTENANCE" -> AppColors.Info
    "ACTIVE" -> AppColors.Info
    else -> AppColors.TextFaint
}

private fun deliveryStatusColor(status: String) = when (status) {
    "RETURNED" -> AppColors.Success
    "PARTIAL" -> AppColors.Warning
    "ACTIVE" -> AppColors.Info
    else -> AppColors.TextFaint
}

/** Tono bento equivalente a un color semántico (chips con contraste AA). */
private fun toneOf(color: Color): BentoTone = when (color) {
    AppColors.Success -> BentoTone.Mint
    AppColors.Warning -> BentoTone.Sun
    AppColors.Danger -> BentoTone.Rose
    AppColors.Info -> BentoTone.Lavender
    AppColors.EmeraldPrimary -> BentoTone.Sea
    else -> BentoTone.Plain
}
