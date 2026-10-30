package com.axzydev.puertonuevoapp.feature.hr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.hr.DisciplinaryReportDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoAvatar
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.RegisterTopBarAction
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.ui.table.AppTableScreen
import com.axzydev.puertonuevoapp.core.ui.table.FilterControl
import com.axzydev.puertonuevoapp.core.ui.table.FilterOption
import com.axzydev.puertonuevoapp.core.ui.table.TableFilterSpec
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import com.axzydev.puertonuevoapp.core.theme.BentoTone

/**
 * Actas administrativas (`/employees/disciplinary-reports`): tabla con filtros
 * de texto, motivo y rango de fecha; alta en formulario modal y navegación al
 * detalle. Requiere `hr.disciplinary_reports`.
 */
@Composable
fun DisciplinaryReportsScreen(
    viewModel: DisciplinaryReportsViewModel = viewModel { DisciplinaryReportsViewModel(AppContainer.hrApi) },
) {
    val state by viewModel.uiState.collectAsState()
    val form by viewModel.formState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canView = user?.can(PermissionKeys.HR_DISCIPLINARY_REPORTS) == true

    if (canView) {
        RegisterTopBarAction(Icons.Filled.Add, "Nueva acta") { viewModel.openForm() }
    }

    if (!canView) {
        Column(modifier = Modifier.fillMaxSize().background(AppBento.canvas).padding(16.dp)) {
            BentoState(message = "Necesitas el permiso de actas administrativas (hr.disciplinary_reports).")
        }
        return
    }

    LaunchedEffect(form.createdId) {
        form.createdId?.let { id ->
            viewModel.consumeCreated()
            navigator.push(Screen.DisciplinaryReportDetail(id))
        }
    }

    AppTableScreen(
        state = state,
        heading = "Actas administrativas",
        filterSpecs = listOf(
            TableFilterSpec(
                key = "reason",
                label = "Motivo",
                control = FilterControl.Select(
                    disciplinaryReasonOptions.map { (value, label) -> FilterOption(value, label) },
                ),
            ),
            TableFilterSpec(key = "from", label = "Desde (AAAA-MM-DD)", control = FilterControl.Text),
            TableFilterSpec(key = "to", label = "Hasta (AAAA-MM-DD)", control = FilterControl.Text),
        ),
        onSearchChange = viewModel::onSearchChange,
        onFiltersChange = viewModel::onFiltersChange,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onRetry = viewModel::retry,
        searchPlaceholder = "Buscar por empleado, número o descripción…",
        emptyMessage = "Todavía no hay actas registradas",
        itemKey = { it.id },
        modifier = Modifier.background(AppBento.canvas),
        searchKey = "q",
    ) { report ->
        DisciplinaryReportCard(
            report = report,
            onClick = { navigator.push(Screen.DisciplinaryReportDetail(report.id)) },
        )
    }

    if (form.visible) {
        DisciplinaryFormModal(
            form = form,
            onDismiss = viewModel::closeForm,
            onPersonQuery = viewModel::setPersonQuery,
            onSelectPerson = viewModel::selectPerson,
            onClearPerson = viewModel::clearPerson,
            onReason = viewModel::setReason,
            onDate = viewModel::setIncidentDate,
            onDescription = viewModel::setDescription,
            onSanction = viewModel::setSanction,
            onSave = viewModel::save,
            onDismissError = viewModel::clearFormError,
        )
    }
}

@Composable
private fun DisciplinaryReportCard(report: DisciplinaryReportDto, onClick: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BentoAvatar(name = report.user.name, color = AppColors.Danger, size = 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    report.user.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        report.user.employeeNumber?.takeIf { it.isNotBlank() }?.let { "#$it" },
                        report.user.jobTitle?.takeIf { it.isNotBlank() },
                        report.user.department?.name?.takeIf { it.isNotBlank() },
                    ).joinToString(" · ").ifBlank { "Sin datos de puesto" },
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatShortDate(report.incidentDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                )
                Spacer(Modifier.height(4.dp))
                StatusChip(disciplinaryReasonLabel(report.reason), reasonColor(report.reason))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            report.description,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        report.sanction?.takeIf { it.isNotBlank() }?.let { sanction ->
            Spacer(Modifier.height(4.dp))
            Text(
                "Sanción: $sanction",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Levantada por ${report.createdBy.name}",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Text("Ver acta", style = MaterialTheme.typography.labelMedium, color = AppColors.EmeraldPrimary)
        }
    }
}

@Composable
private fun DisciplinaryFormModal(
    form: DisciplinaryFormState,
    onDismiss: () -> Unit,
    onPersonQuery: (String) -> Unit,
    onSelectPerson: (com.axzydev.puertonuevoapp.core.network.users.UserDto) -> Unit,
    onClearPerson: () -> Unit,
    onReason: (String) -> Unit,
    onDate: (String) -> Unit,
    onDescription: (String) -> Unit,
    onSanction: (String) -> Unit,
    onSave: () -> Unit,
    onDismissError: () -> Unit,
) {
    AppModal(
        title = "Nueva acta administrativa",
        icon = Icons.Filled.Add,
        onDismiss = onDismiss,
        confirmLabel = "Guardar acta",
        confirmEnabled = form.canSave,
        saving = form.saving,
        onConfirm = onSave,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
        ) {
            Text(
                "Solo se asientan hechos. La persona, el motivo, la fecha y la descripción son obligatorios.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
            Spacer(Modifier.height(10.dp))

            val selected = form.selected
            if (selected != null) {
                AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BentoAvatar(name = selected.name, color = AppColors.EmeraldPrimary, size = 32.dp)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(selected.name, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                            Text(
                                listOfNotNull(selected.employeeNumber?.let { "#$it" }, selected.department?.name)
                                    .joinToString(" · ")
                                    .ifBlank { "—" },
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextFaint,
                            )
                        }
                        AppFilterChip(label = "Cambiar", selected = false, onClick = onClearPerson)
                    }
                }
            } else {
                AppSearchField(
                    value = form.personQuery,
                    onValueChange = onPersonQuery,
                    placeholder = "Busca por nombre o número de empleado…",
                )
                if (form.peopleLoading) {
                    Text("Buscando…", style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
                } else if (form.people.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    form.people.forEach { person ->
                        AppCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            contentPadding = PaddingValues(8.dp),
                            onClick = { onSelectPerson(person) },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = AppColors.TextFaint)
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        person.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        listOfNotNull(person.employeeNumber?.let { "#$it" }, person.department?.name)
                                            .joinToString(" · ")
                                            .ifBlank { "—" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppColors.TextFaint,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            SimpleDropdownField(
                label = "Motivo",
                value = form.reason,
                options = listOf("" to "Selecciona el motivo") + disciplinaryReasonOptions,
                onSelect = onReason,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = form.incidentDate,
                onValueChange = onDate,
                label = { Text("Fecha del incidente (AAAA-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = form.description,
                onValueChange = onDescription,
                label = { Text("Descripción del incidente") },
                placeholder = { Text("Describe el hecho de forma clara y objetiva") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = form.sanction,
                onValueChange = onSanction,
                label = { Text("Sanción / medida aplicada (opcional)") },
                placeholder = { Text("Llamada de atención, suspensión, etc.") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            form.error?.let { message ->
                Spacer(Modifier.height(8.dp))
                BentoErrorState(message = message, onRetry = onDismissError)
            }
        }
    }
}

internal fun reasonColor(reason: String) = when (reason) {
    "ABSENCE", "INTOXICATION" -> AppColors.Danger
    "TARDINESS", "MISCONDUCT", "NONCOMPLIANCE" -> AppColors.Warning
    else -> AppColors.Slate
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
