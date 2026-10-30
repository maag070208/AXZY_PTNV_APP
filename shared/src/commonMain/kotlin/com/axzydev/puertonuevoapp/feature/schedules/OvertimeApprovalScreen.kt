package com.axzydev.puertonuevoapp.feature.schedules

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.network.schedules.OvertimeDayRowDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.util.formatIsoUtc
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import com.axzydev.puertonuevoapp.core.util.parseIsoToEpochMillis
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoMiniStat
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.RegisterTopBarAction
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import androidx.compose.ui.text.style.TextOverflow

/**
 * Tiempo extra (`overtime.view`; aprobar/rechazar exige `overtime.approve`).
 *
 * Se elige el periodo (día/semana/quincena/mes) y se navega con las flechas; se
 * filtra por departamento, persona y estatus. Los días pendientes se marcan y
 * se aprueban o rechazan de un jalón; el rechazo lleva un motivo opcional.
 * Quien no puede aprobar ve el modo solo lectura (y el API le devuelve solo lo
 * aprobado). El export es un CSV por persona.
 */
@Composable
fun OvertimeApprovalScreen() {
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canView = user?.can(PermissionKeys.OVERTIME_VIEW) == true
    val canApprove = user?.can(PermissionKeys.OVERTIME_APPROVE) == true

    if (!canView) {
        BentoState(
            message = "No autorizado — requiere el permiso Tiempo extra",
            modifier = Modifier.fillMaxSize().padding(16.dp),
            tone = BentoTone.Rose,
        )
        return
    }

    val viewModel: OvertimeApprovalViewModel = viewModel(key = "overtime-approval-$canApprove") {
        OvertimeApprovalViewModel(SchedulesServices.api, AppContainer.departmentsApi, canApprove)
    }
    val state by viewModel.uiState.collectAsState()

    RegisterTopBarAction(Icons.Filled.Share, "Exportar CSV") { viewModel.exportCsv() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    "Tiempo extra",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.TextPrimary,
                )
                Text(
                    if (canApprove) {
                        "Revisa los días calculados y aprueba o rechaza lo que corresponda."
                    } else {
                        "Consulta de lo aprobado (tu perfil no puede decidir sobre tiempo extra)."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            ) {
                REPORT_PERIODS.forEach { (value, label) ->
                    AppFilterChip(
                        label = label,
                        selected = state.period == value,
                        onClick = { viewModel.changePeriod(value) },
                    )
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { viewModel.shiftPeriod(forward = false) }) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "Periodo anterior",
                        tint = AppColors.TextMuted,
                    )
                }
                Text(
                    overtimePeriodLabel(state),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = { viewModel.shiftPeriod(forward = true) }) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "Periodo siguiente",
                        tint = AppColors.TextMuted,
                    )
                }
            }
        }

        item {
            SimpleDropdownField(
                label = "Departamento",
                value = state.departmentId,
                options = buildList {
                    add("" to "Todos los departamentos")
                    state.departments.forEach { add(it.id to it.name) }
                },
                onSelect = viewModel::onDepartmentChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            AppSearchField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Buscar persona o número…",
            )
        }

        if (canApprove) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                ) {
                    OVERTIME_STATUS_TABS.forEach { (value, label) ->
                        AppFilterChip(
                            label = label,
                            selected = state.statusTab == value,
                            onClick = { viewModel.changeStatusTab(value) },
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                BentoMiniStat(
                    label = "Días pendientes",
                    value = (state.summary?.pendingDays ?: 0).toString(),
                    modifier = Modifier.weight(1f),
                    tone = BentoTone.Sun,
                )
                BentoMiniStat(
                    label = "Min. pendientes",
                    value = hoursLabel(state.summary?.pendingMinutes ?: 0),
                    modifier = Modifier.weight(1f),
                    tone = BentoTone.Lavender,
                )
                BentoMiniStat(
                    label = "Min. aprobados",
                    value = hoursLabel(state.summary?.approvedMinutes ?: 0),
                    modifier = Modifier.weight(1f),
                    tone = BentoTone.Mint,
                )
                BentoMiniStat(
                    label = "Personas",
                    value = (state.summary?.peopleWithPending ?: 0).toString(),
                    modifier = Modifier.weight(1f),
                    tone = BentoTone.Sea,
                )
            }
        }

        if (canApprove) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    SectionLabel(
                        if (state.selected.isEmpty()) "Selecciona días pendientes"
                        else "${state.selected.size} día(s) seleccionado(s)",
                        modifier = Modifier.weight(1f),
                    )
                    if (state.selected.isNotEmpty()) {
                        TextButton(onClick = viewModel::clearSelection) {
                            Text("Limpiar", style = MaterialTheme.typography.labelMedium)
                        }
                    } else if (state.selectablePending.isNotEmpty()) {
                        TextButton(onClick = viewModel::selectAllVisible) {
                            Text("Todos los visibles", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            state.selected.takeIf { it.isNotEmpty() }?.let {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        PillButton(
                            text = "Aprobar (${state.selected.size})",
                            onClick = viewModel::approve,
                            variant = PillButtonVariant.Primary,
                            loading = state.saving,
                            leadingIcon = Icons.Filled.CheckCircle,
                            modifier = Modifier.weight(1f),
                        )
                        PillButton(
                            text = "Rechazar",
                            onClick = viewModel::openReject,
                            variant = PillButtonVariant.Secondary,
                            enabled = !state.saving,
                            leadingIcon = Icons.Filled.Cancel,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        when {
            state.loading -> item {
                LoadingState(modifier = Modifier.padding(vertical = 24.dp))
            }

            state.error != null -> item {
                BentoState(
                    message = state.error ?: "No se pudo cargar el tiempo extra",
                    tone = BentoTone.Rose,
                    actionLabel = "Reintentar",
                    onAction = viewModel::retry,
                )
            }

            state.rows.isEmpty() -> item {
                BentoState(
                    message = if (canApprove) {
                        "No hay días de tiempo extra con estos filtros."
                    } else {
                        "No hay tiempo extra aprobado en este periodo."
                    },
                )
            }

            else -> {
                items(state.rows, key = { overtimeDayKey(it) }) { row ->
                    OvertimeDayCard(
                        row = row,
                        selected = state.selected.containsKey(overtimeDayKey(row)),
                        canSelect = canApprove && row.status == "PENDING",
                        onToggle = { viewModel.toggleRow(row) },
                    )
                }
                if (state.hasMore) {
                    item {
                        PillButton(
                            text = "Cargar más (${state.rows.size} de ${state.total})",
                            onClick = viewModel::loadMore,
                            variant = PillButtonVariant.Ghost,
                            loading = state.loadingMore,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    if (state.rejectOpen) {
        AppModal(
            title = "Rechazar ${state.selected.size} día(s)",
            icon = Icons.Filled.Cancel,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissReject,
            confirmLabel = "Rechazar",
            saving = state.saving,
            onConfirm = viewModel::confirmReject,
        ) {
            AppTextField(
                value = state.rejectNote,
                onValueChange = viewModel::onRejectNoteChange,
                label = { Text("Motivo (opcional)") },
                placeholder = { Text("Ej. no autorizado por el jefe de área") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}


/**
 * Rango del periodo en texto humano. El API manda `summary.range.start/end`
 * como instantes ISO (medianoche de la zona del reporte, p. ej.
 * `2026-10-28T06:00:00.000Z`), así que solo se usa la fecha (sin horas): de ahí
 * salía el ISO crudo que se veía en pantalla.
 *
 * Un solo día → "Hoy"/"Ayer"/"28 oct" (día ancla); un rango → "28 oct – 4 nov"
 * (con el año solo si cruza de año).
 */
private fun overtimePeriodLabel(state: OvertimeApprovalUiState): String {
    val range = state.summary?.range
    val startKey = range?.start?.take(10)?.takeIf { it.length == 10 }
    // El API manda `end` como instante EXCLUSIVO (medianoche del día siguiente,
    // igual que `prevCivilDay` de los PDF de la web): el último día incluido es
    // el anterior.
    val endKey = range?.end?.let { previousDayKey(it) ?: it.take(10) }?.takeIf { it.length == 10 }
    if (startKey == null || endKey == null) return anchorDayLabel(state.dateKey)
    if (startKey == endKey) return anchorDayLabel(startKey)

    val start = shortMonthDayLabel(startKey) ?: formatShortDate(startKey)
    val end = shortMonthDayLabel(endKey) ?: formatShortDate(endKey)
    return if (startKey.take(4) == endKey.take(4)) {
        "$start – $end"
    } else {
        "$start ${startKey.take(4)} – $end ${endKey.take(4)}"
    }
}

/**
 * Clave `YYYY-MM-DD` del día anterior a un instante ISO de medianoche local
 * (el `range.end` exclusivo del API). Restar 24 h deja la fecha correcta en la
 * zona del reporte; si no se puede interpretar, `null`.
 */
private fun previousDayKey(iso: String): String? {
    val millis = parseIsoToEpochMillis(iso) ?: return null
    return formatIsoUtc(millis - 86_400_000L).take(10)
}

/** "Hoy"/"Ayer" si es el día ancla local; si no, "28 oct". */
private fun anchorDayLabel(dateKey: String): String {
    val today = todayDateKey()
    return when (dateKey) {
        today -> "Hoy"
        shiftDateKey(today, -1) -> "Ayer"
        else -> shortMonthDayLabel(dateKey) ?: formatShortDate(dateKey)
    }
}

/** "28 oct" de una clave `YYYY-MM-DD`; `null` si no se puede leer. */
private fun shortMonthDayLabel(dateKey: String): String? {
    val parts = dateKey.split("-")
    if (parts.size != 3) return null
    val day = parts[2].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val monthName = MONTH_SHORT.getOrNull(month - 1) ?: return null
    return "$day $monthName"
}

@Composable
private fun OvertimeDayCard(
    row: OvertimeDayRowDto,
    selected: Boolean,
    canSelect: Boolean,
    onToggle: () -> Unit,
) {
    val statusTone = when (row.status) {
        "APPROVED" -> BentoTone.Mint
        "REJECTED" -> BentoTone.Rose
        else -> BentoTone.Sun
    }
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (canSelect) onToggle else null,
        borderColor = if (selected) AppColors.EmeraldPrimary else AppColors.Outline.copy(alpha = 0.45f),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (canSelect) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (selected) AppColors.EmeraldPrimary else AppColors.Outline,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    row.employeeName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        append(formatDateKey(row.date))
                        row.shift?.let { append(" · $it") }
                        if (row.restDay) append(" · descanso")
                        if (row.withoutSchedule) append(" · sin horario")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                )
            }
            StatusChip(label = overtimeStatusLabel(row.status), tone = statusTone)
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                "Extra ${hoursLabel(row.extraMin)}",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.EmeraldPrimary,
            )
            Text(
                "Trabajado ${hoursLabel(row.workedMin)}",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextMuted,
            )
            Text(
                "Programado ${hoursLabel(row.scheduledMin)}",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextFaint,
            )
            if (row.status == "APPROVED") {
                Text(
                    "Aprobado ${hoursLabel(row.approvedExtraMin)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.Success,
                )
            }
        }
        if (!row.note.isNullOrBlank()) {
            Text(
                "Nota: ${row.note}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
