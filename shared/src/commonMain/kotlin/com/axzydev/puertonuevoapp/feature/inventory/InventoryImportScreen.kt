package com.axzydev.puertonuevoapp.feature.inventory

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypeDto
import com.axzydev.puertonuevoapp.core.network.inventory.DeviceImportPreviewDto
import com.axzydev.puertonuevoapp.core.network.inventory.DeviceImportResultDto
import com.axzydev.puertonuevoapp.core.network.inventory.DeviceImportRowDto
import com.axzydev.puertonuevoapp.core.network.inventory.InventoryImportApi
import com.axzydev.puertonuevoapp.core.platform.FileMimeTypes
import com.axzydev.puertonuevoapp.core.platform.rememberFilePicker
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip

/**
 * Carga masiva de dispositivos desde Excel: asistente subir → revisar →
 * confirmar. La plantilla la sirve el API con los encabezados y el catálogo de
 * tipos vigente; la previsualización no escribe nada y la confirmación es
 * todo-o-nada en una sola transacción. Requiere `devices.create` (el API lo
 * vuelve a validar).
 */
@Composable
fun InventoryImportScreen(
    viewModel: InventoryImportViewModel = viewModel {
        InventoryImportViewModel(
            importApi = InventoryImportApi(AppContainer.apiClient),
            deviceTypesApi = AppContainer.deviceTypesApi,
        )
    },
) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canCreate = user?.can(PermissionKeys.DEVICES_CREATE) == true

    val pickFile = rememberFilePicker(mimeTypes = listOf(FileMimeTypes.EXCEL)) { picked ->
        viewModel.onFilePicked(picked)
    }
    var showRepeatConfirm by remember { mutableStateOf(false) }

    if (!canCreate) {
        Column(modifier = Modifier.fillMaxSize().background(AppBento.canvas).padding(16.dp)) {
            BentoState(
                message = "Necesitas el permiso de alta de dispositivos para cargar el Excel.",
                icon = Icons.Filled.Lock,
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBento.canvas)) {
        when (state.step) {
            ImportStep.Upload -> UploadStep(
                state = state,
                onPickFile = pickFile,
                onDownloadTemplate = viewModel::downloadTemplate,
                onRetryPreview = viewModel::retryPreview,
                onDismissError = viewModel::dismissError,
            )
            ImportStep.Review -> ReviewStep(
                state = state,
                onTypeChange = viewModel::setTypeOverride,
                onBack = viewModel::reset,
                onConfirm = {
                    if (state.repeatedImport != null) showRepeatConfirm = true else viewModel.confirm()
                },
                onDismissError = viewModel::dismissError,
            )
            ImportStep.Done -> DoneStep(
                result = state.result,
                onAnotherFile = viewModel::reset,
                onClose = { navigator.pop() },
            )
        }
    }

    if (showRepeatConfirm) {
        AppModal(
            title = "Este archivo ya se cargó",
            icon = Icons.Filled.WarningAmber,
            tone = AppModalTone.Warning,
            onDismiss = { showRepeatConfirm = false },
            confirmLabel = "Cargar de todos modos",
            saving = state.confirming,
            onConfirm = {
                showRepeatConfirm = false
                viewModel.confirm(allowRepeat = true)
            },
        ) {
            Text(
                "Ese mismo Excel ya se importó antes" +
                    (state.repeatedImport?.fileName?.let { " ($it)" } ?: "") +
                    ". Volver a cargarlo SUMARÍA las unidades otra vez. Si de verdad quieres repetir la carga, continúa.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Paso 1 — subir el archivo
// ---------------------------------------------------------------------------

@Composable
private fun UploadStep(
    state: InventoryImportUiState,
    onPickFile: () -> Unit,
    onDownloadTemplate: () -> Unit,
    onRetryPreview: () -> Unit,
    onDismissError: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Carga masiva de dispositivos")
            Text(
                "Sube un Excel con los dispositivos y su cantidad. Primero verás exactamente qué va a pasar " +
                    "(altas, unidades que se suman y folios); nada se escribe hasta que confirmes, y la carga " +
                    "es todo o nada.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
        }

        AppCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FileDownload, contentDescription = null, tint = AppColors.EmeraldPrimary)
                Spacer(Modifier.width(8.dp))
                Text("1. Plantilla", style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "La plantilla trae los encabezados y el catálogo de tipos vigente. Una fila = un dispositivo con su CANTIDAD.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
            Spacer(Modifier.height(12.dp))
            PillButton(
                text = if (state.downloadingTemplate) "Preparando…" else "Descargar plantilla",
                onClick = onDownloadTemplate,
                variant = PillButtonVariant.Secondary,
                loading = state.downloadingTemplate,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        AppCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.UploadFile, contentDescription = null, tint = AppColors.EmeraldPrimary)
                Spacer(Modifier.width(8.dp))
                Text("2. Archivo Excel", style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                state.file?.name ?: "Formatos .xlsx o .xls. Hasta 500 filas y 5 000 unidades por archivo.",
                style = MaterialTheme.typography.bodySmall,
                color = if (state.file != null) AppColors.TextPrimary else AppColors.TextMuted,
            )
            Spacer(Modifier.height(12.dp))
            PillButton(
                text = if (state.file == null) "Elegir archivo" else "Elegir otro archivo",
                onClick = onPickFile,
                variant = PillButtonVariant.Primary,
                enabled = !state.loadingPreview,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (state.loadingPreview) {
            BentoState(message = "Leyendo el archivo y armando la previsualización…", loading = true)
        }

        state.error?.let { message ->
            BentoErrorState(
                message = message,
                onRetry = if (state.file != null) onRetryPreview else null,
            )
            PillButton(
                text = "Entendido",
                onClick = onDismissError,
                variant = PillButtonVariant.Ghost,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ---------------------------------------------------------------------------
// Paso 2 — revisar la previsualización
// ---------------------------------------------------------------------------

@Composable
private fun ReviewStep(
    state: InventoryImportUiState,
    onTypeChange: (Int, String) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    onDismissError: () -> Unit,
) {
    val preview = state.preview ?: return
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "summary") {
                PreviewSummaryCard(preview = preview, loading = state.loadingPreview)
            }
            preview.repeatedImport?.let { repeated ->
                item(key = "repeat") {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = AppColors.Warning.copy(alpha = 0.10f),
                        borderColor = AppColors.Warning.copy(alpha = 0.35f),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = AppColors.Warning)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Este archivo ya se cargó",
                                style = MaterialTheme.typography.titleSmall,
                                color = AppColors.TextPrimary,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Importado el ${repeated.importedAt}" +
                                (repeated.fileName?.let { " como $it" } ?: "") +
                                ". Confirmar de nuevo SUMARÍA las unidades otra vez.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextMuted,
                        )
                    }
                }
            }
            state.error?.let { message ->
                item(key = "error") {
                    Column {
                        BentoErrorState(message = message, onRetry = null)
                        PillButton(
                            text = "Entendido",
                            onClick = onDismissError,
                            variant = PillButtonVariant.Ghost,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item(key = "rowsTitle") {
                Text(
                    "Renglones del archivo · ${preview.rows.size}",
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.TextMuted,
                )
            }
            items(preview.rows, key = { it.row }) { row ->
                ImportRowCard(
                    row = row,
                    types = state.types,
                    busy = state.loadingPreview,
                    onTypeChange = { typeId -> onTypeChange(row.row, typeId) },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppBento.canvas)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PillButton(
                text = "Volver",
                onClick = onBack,
                variant = PillButtonVariant.Secondary,
                modifier = Modifier.weight(1f),
            )
            PillButton(
                text = if (preview.repeatedImport != null) "Revisar y cargar" else "Confirmar carga",
                onClick = onConfirm,
                variant = PillButtonVariant.Primary,
                loading = state.confirming,
                enabled = state.canConfirm && !state.loadingPreview,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PreviewSummaryCard(preview: DeviceImportPreviewDto, loading: Boolean) {
    val summary = preview.summary
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.TableChart, contentDescription = null, tint = AppColors.EmeraldPrimary)
            Spacer(Modifier.width(8.dp))
            Text("Resumen de la carga", style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
            if (loading) {
                Spacer(Modifier.weight(1f))
                Text("Recalculando…", style = MaterialTheme.typography.labelSmall, color = AppColors.TextMuted)
            }
        }
        Spacer(Modifier.height(8.dp))
        SummaryLine("Filas", summary.rows.toString())
        SummaryLine("Válidas", summary.valid.toString(), AppColors.Success)
        if (summary.invalid > 0) SummaryLine("Con errores", summary.invalid.toString(), AppColors.Danger)
        SummaryLine("Unidades", summary.units.toString())
        SummaryLine("Dispositivos nuevos", summary.newDevices.toString())
        SummaryLine("Unidades a dispositivos existentes", summary.existingDevices.toString())
        if (summary.genericRows > 0) SummaryLine("Al tipo genérico", summary.genericRows.toString(), AppColors.Warning)
        if (summary.unknownTypeRows > 0) SummaryLine("Con tipo desconocido", summary.unknownTypeRows.toString(), AppColors.Warning)
        if (summary.typesToCreate.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Se crearán tipos: ${summary.typesToCreate.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Warning,
            )
        }
        if (summary.invalid > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Corrige las filas con error en el Excel y vuelve a subirlo: mientras haya errores no se carga nada.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Danger,
            )
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = AppColors.TextPrimary) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ImportRowCard(
    row: DeviceImportRowDto,
    types: List<DeviceTypeDto>,
    busy: Boolean,
    onTypeChange: (String) -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = when {
            row.errors.isNotEmpty() -> AppColors.Danger.copy(alpha = 0.45f)
            row.warnings.isNotEmpty() -> AppColors.Warning.copy(alpha = 0.45f)
            else -> AppColors.Outline.copy(alpha = 0.6f)
        },
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Fila ${row.row}",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                row.name.ifBlank { "—" },
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            StatusChip(
                label = if (row.action == "ADD_UNITS") "Suma unidades" else "Alta",
                tone = if (row.action == "ADD_UNITS") BentoTone.Sea else BentoTone.Mint,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            listOf(row.brand, row.model).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Sin marca/modelo" },
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StatusChip(label = "×${row.quantity}", tone = BentoTone.Lavender)
            Spacer(Modifier.width(6.dp))
            StatusChip(
                label = row.resolvedTypeName.ifBlank { "Genérico" },
                tone = if (row.typeUnknown || row.typeMissing) BentoTone.Sun else BentoTone.Plain,
            )
            if (row.typeOverridden) {
                Spacer(Modifier.width(6.dp))
                StatusChip(label = "Elegido", tone = BentoTone.Rose)
            }
        }
        if (row.action == "ADD_UNITS") {
            Spacer(Modifier.height(4.dp))
            Text(
                "El dispositivo ya tiene ${row.currentUnits} unidad(es); se le suman ${row.quantity}.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
        }
        val from = row.assetTagFrom
        val to = row.assetTagTo
        if (from != null || to != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                "Folios: " + (if (from != null && to != null && from != to) "$from – $to" else from ?: to ?: "—"),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
        }
        if (row.mergedRows.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(
                "Se junta con las filas ${row.mergedRows.joinToString(", ")}.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
        }
        row.errors.forEach { error ->
            Spacer(Modifier.height(4.dp))
            Text("• $error", style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
        }
        row.warnings.forEach { warning ->
            Spacer(Modifier.height(4.dp))
            Text("• $warning", style = MaterialTheme.typography.bodySmall, color = AppColors.Warning)
        }
        Spacer(Modifier.height(8.dp))
        SimpleDropdownField(
            label = "Tipo de la fila",
            value = row.typeId.orEmpty(),
            options = listOf("" to "— Usar el del archivo") +
                types.map { type -> type.id to type.name },
            onSelect = { selected -> if (!busy) onTypeChange(selected) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---------------------------------------------------------------------------
// Paso 3 — resultado
// ---------------------------------------------------------------------------

@Composable
private fun DoneStep(
    result: DeviceImportResultDto?,
    onAnotherFile: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AppColors.Success)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (result?.repeated == true) "Carga ya registrada" else "Carga completada",
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.TextPrimary,
                )
            }
            Spacer(Modifier.height(6.dp))
            if (result == null) {
                Text("La carga terminó.", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
            } else {
                if (result.repeated) {
                    Text(
                        "Esa carga ya se había registrado: no se duplicó nada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Warning,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                SummaryLine("Dispositivos nuevos", result.devicesCreated.toString())
                SummaryLine("Dispositivos con unidades sumadas", result.devicesReused.toString())
                SummaryLine("Unidades creadas", result.unitsCreated.toString())
                SummaryLine("Filas procesadas", result.rows.toString())
                result.fileName?.let { SummaryLine("Archivo", it) }
                if (result.movementId.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Movimiento: ${result.movementId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextFaint,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        PillButton(
            text = "Cargar otro archivo",
            onClick = onAnotherFile,
            variant = PillButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
        PillButton(
            text = "Listo",
            onClick = onClose,
            variant = PillButtonVariant.Ink,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
