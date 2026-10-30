package com.axzydev.puertonuevoapp.feature.hr

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.hr.EmployeeDocumentDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.BentoAvatar
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatCard
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatFileSize
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import com.axzydev.puertonuevoapp.core.theme.BentoTone

/**
 * Tablero de expedientes de RH (`/employees/records`): estado de cada
 * expediente (documentos obligatorios, otros documentos y datos personales),
 * filtros, avisos de faltantes (individual y masivo con selección), vista de
 * documentos con descarga/compartir y atajo al expediente completo
 * (`Screen.EmployeeProfile`). Requiere `hr.records`.
 */
@Composable
fun EmployeeRecordsScreen(
    viewModel: EmployeeRecordsViewModel = viewModel {
        EmployeeRecordsViewModel(AppContainer.hrApi, AppContainer.dashboardApi)
    },
) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canView = user?.can(PermissionKeys.HR_RECORDS) == true
    var confirmBulk by remember { mutableStateOf(false) }

    if (!canView) {
        Column(modifier = Modifier.fillMaxSize().background(AppBento.canvas).padding(16.dp)) {
            BentoState(message = "Necesitas el permiso de expedientes (hr.records).")
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBento.canvas)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    value = state.total.toString(),
                    label = "Expedientes",
                    color = AppColors.EmeraldPrimary,
                    icon = { Icon(Icons.Filled.Person, null, tint = AppColors.EmeraldPrimary) },
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = state.complete.toString(),
                    label = "Completos",
                    color = AppColors.Success,
                    icon = { Icon(Icons.Filled.CheckCircle, null, tint = AppColors.Success) },
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = state.required.toString(),
                    label = "Faltan obligatorios",
                    color = AppColors.Danger,
                    icon = { Icon(Icons.Filled.Warning, null, tint = AppColors.Danger) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    value = state.incomplete.toString(),
                    label = "Incompletos",
                    color = AppColors.Warning,
                    icon = { Icon(Icons.Filled.Warning, null, tint = AppColors.Warning) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))

            AppSearchField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = "Buscar por nombre, número o departamento…",
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SimpleDropdownField(
                    label = "Departamento",
                    value = state.departmentId.orEmpty(),
                    options = listOf("" to "Todos") + state.departments,
                    onSelect = { selected -> viewModel.setDepartment(selected.ifBlank { null }) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RecordStatusFilter.entries.forEach { filter ->
                    AppFilterChip(
                        label = filter.label,
                        selected = state.status == filter,
                        onClick = { viewModel.setStatus(filter) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${state.filtered.size} de ${state.total} · ${state.pendingFiltered.size} pendientes",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                    modifier = Modifier.weight(1f),
                )
                if (state.pendingFiltered.isNotEmpty()) {
                    AppFilterChip(
                        label = if (state.selectedPending.containsAll(state.pendingFiltered.map { it.user.id }.toSet()) && state.pendingFiltered.isNotEmpty()) {
                            "Quitar selección"
                        } else {
                            "Seleccionar pendientes"
                        },
                        selected = false,
                        onClick = viewModel::toggleSelectAll,
                    )
                }
            }
            if (state.selectedPending.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton(
                        text = "Avisar a ${state.selectedPending.size}",
                        onClick = { confirmBulk = true },
                        variant = PillButtonVariant.Primary,
                        loading = state.bulkSaving,
                        modifier = Modifier.weight(1f),
                    )
                    PillButton(
                        text = "Quitar",
                        onClick = viewModel::clearSelection,
                        variant = PillButtonVariant.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            state.actionMessage?.let { message ->
                Spacer(Modifier.height(8.dp))
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = AppColors.Success.copy(alpha = 0.10f),
                    borderColor = AppColors.Success.copy(alpha = 0.35f),
                    contentPadding = PaddingValues(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
                        AppFilterChip(label = "Ocultar", selected = false, onClick = viewModel::clearFeedback)
                    }
                }
            }
            state.actionError?.let { message ->
                Spacer(Modifier.height(8.dp))
                BentoErrorState(message = message, onRetry = null)
            }
            Spacer(Modifier.height(12.dp))
        }

        val loadError = state.error
        when {
            state.loading && state.rows.isEmpty() -> LoadingState(modifier = Modifier.weight(1f))
            loadError != null && state.rows.isEmpty() ->
                ErrorState(loadError, Modifier.weight(1f), onRetry = viewModel::load)
            state.filtered.isEmpty() ->
                EmptyState("No hay expedientes con esos filtros", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.filtered, key = { it.user.id }) { row ->
                    RecordRowCard(
                        row = row,
                        selected = row.user.id in state.selected,
                        expanded = state.expandedUserId == row.user.id,
                        documents = if (state.expandedUserId == row.user.id) state.documents else emptyList(),
                        documentsLoading = state.expandedUserId == row.user.id && state.documentsLoading,
                        downloadingDocumentId = state.downloadingDocumentId,
                        notifying = state.notifyingId == row.user.id,
                        onSelect = { viewModel.toggleSelected(row.user.id) },
                        onOpenProfile = { navigator.push(Screen.EmployeeProfile(row.user.id)) },
                        onNotify = { viewModel.notifyOne(row) },
                        onToggleDocuments = { viewModel.toggleDocuments(row.user.id) },
                        onDownload = { document -> viewModel.downloadDocument(row.user.id, document) },
                    )
                }
            }
        }
    }

    if (confirmBulk) {
        AppModal(
            title = "Avisar faltantes",
            icon = Icons.Filled.Warning,
            tone = AppModalTone.Warning,
            onDismiss = { confirmBulk = false },
            confirmLabel = "Enviar ${state.selectedPending.size} aviso(s)",
            saving = state.bulkSaving,
            onConfirm = {
                confirmBulk = false
                viewModel.notifySelected()
            },
        ) {
            Text(
                "Se avisará a ${state.selectedPending.size} persona(s) qué les falta en su expediente " +
                    "(notificación en la app y correo si tienen uno registrado). Las que ya estén completas se omiten.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
        }
    }
}

@Composable
private fun RecordRowCard(
    row: RecordBoardRow,
    selected: Boolean,
    expanded: Boolean,
    documents: List<EmployeeDocumentDto>,
    documentsLoading: Boolean,
    downloadingDocumentId: String?,
    notifying: Boolean,
    onSelect: () -> Unit,
    onOpenProfile: () -> Unit,
    onNotify: () -> Unit,
    onToggleDocuments: () -> Unit,
    onDownload: (EmployeeDocumentDto) -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BentoAvatar(name = row.user.name, color = statusColor(row.status), size = 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    row.user.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        row.user.employeeNumber?.let { "#$it" },
                        row.user.jobTitle,
                        row.user.department?.name,
                    ).joinToString(" · ").ifBlank { "Sin datos de puesto" },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusChip(statusLabel(row.status), toneOf(statusColor(row.status)))
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (row.missingRequiredCount > 0) {
                StatusChip("Faltan ${row.missingRequiredCount} obligatorio(s)", BentoTone.Rose)
            }
            if (row.missingOtherCount > 0) {
                StatusChip("${row.missingOtherCount} documento(s) más", BentoTone.Sun)
            }
            if (row.missingFieldsCount > 0) {
                StatusChip("${row.missingFieldsCount} dato(s) personal(es)", BentoTone.Lavender)
            }
            if (!row.pending) {
                StatusChip("Expediente completo", BentoTone.Mint)
            }
        }

        if (row.missingRequiredCount > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Falta: " + row.gaps?.missingRequired?.joinToString(", ") { it.name }.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PillButton(
                text = "Ver expediente",
                onClick = onOpenProfile,
                variant = PillButtonVariant.Ghost,
            )
            if (row.pending) {
                PillButton(
                    text = if (notifying) "Avisando…" else "Avisar faltantes",
                    onClick = onNotify,
                    variant = PillButtonVariant.Secondary,
                    enabled = !notifying,
                    loading = notifying,
                )
                AppFilterChip(
                    label = if (selected) "Seleccionado" else "Seleccionar",
                    selected = selected,
                    onClick = onSelect,
                )
            }
            AppFilterChip(
                label = if (expanded) "Ocultar documentos" else "Documentos",
                selected = expanded,
                onClick = onToggleDocuments,
            )
        }

        if (expanded) {
            Spacer(Modifier.height(8.dp))
            if (documentsLoading) {
                BentoState(message = "Cargando documentos…", loading = true)
            } else if (documents.isEmpty()) {
                BentoState(message = "Sin documentos subidos.")
            } else {
                documents.forEach { document ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                document.documentType?.name ?: document.originalName,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${document.originalName} · ${formatFileSize(document.sizeBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextFaint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        PillButton(
                            text = if (downloadingDocumentId == document.id) "Compartiendo…" else "Descargar",
                            onClick = { onDownload(document) },
                            variant = PillButtonVariant.Ghost,
                            enabled = downloadingDocumentId == null,
                            loading = downloadingDocumentId == document.id,
                        )
                    }
                }
            }
        }
    }
}

private fun statusLabel(status: RecordStatus): String = when (status) {
    RecordStatus.REQUIRED -> "Faltan obligatorios"
    RecordStatus.INCOMPLETE -> "Incompleto"
    RecordStatus.COMPLETE -> "Completo"
}

private fun statusColor(status: RecordStatus) = when (status) {
    RecordStatus.REQUIRED -> AppColors.Danger
    RecordStatus.INCOMPLETE -> AppColors.Warning
    RecordStatus.COMPLETE -> AppColors.Success
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
