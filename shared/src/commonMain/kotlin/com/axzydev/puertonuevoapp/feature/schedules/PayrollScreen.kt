package com.axzydev.puertonuevoapp.feature.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.network.schedules.PeopleAttendanceRowDto
import com.axzydev.puertonuevoapp.core.network.schedules.WeeklyAttendanceDayDto
import com.axzydev.puertonuevoapp.core.network.schedules.WeeklyAttendanceRowDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.BentoMiniStat
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.RegisterTopBarAction
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextOverflow
import com.axzydev.puertonuevoapp.core.util.formatLocalTime

/**
 * Pre nómina (`payroll.view`): reporte semanal de asistencia de RH y
 * entradas/salidas por persona contra su horario.
 *
 * La primera pestaña junta la semana completa por persona (horas, extra
 * aprobado/pendiente, faltas e incompletos); la segunda muestra cada día del
 * periodo con la fuente seleccionada (checadas del reloj o bitácora de acceso).
 * El CSV que se comparte replica la vista vigente.
 */
@Composable
fun PayrollScreen() {
    val authState by AppContainer.authRepository.state.collectAsState()
    val canView = (authState as? AuthState.LoggedIn)?.user?.can(PermissionKeys.PAYROLL_VIEW) == true

    if (!canView) {
        BentoState(
            message = "No autorizado — requiere el permiso Pre nómina",
            modifier = Modifier.fillMaxSize().padding(16.dp),
            tone = BentoTone.Rose,
        )
        return
    }

    val viewModel: PayrollViewModel = viewModel(key = "payroll") {
        PayrollViewModel(SchedulesServices.api, AppContainer.departmentsApi)
    }
    val state by viewModel.uiState.collectAsState()
    var expandedPerson by remember { mutableStateOf("") }

    RegisterTopBarAction(Icons.Filled.Share, "Exportar CSV") { viewModel.exportCsv() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    "Pre nómina",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.TextPrimary,
                )
                Text(
                    "Asistencia semanal y entradas/salidas por persona, calculadas por el servidor.",
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
                AppFilterChip(
                    label = "Asistencia semanal",
                    selected = state.isWeekly,
                    onClick = { viewModel.changeTab(PAYROLL_TAB_WEEKLY) },
                )
                AppFilterChip(
                    label = "Entradas y salidas",
                    selected = !state.isWeekly,
                    onClick = { viewModel.changeTab(PAYROLL_TAB_PEOPLE) },
                )
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
                    state.rangeLabel,
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

        if (!state.isWeekly) {
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                ) {
                    AppFilterChip(
                        label = "Reloj",
                        selected = state.source == ATTENDANCE_SOURCE_CLOCK,
                        onClick = { viewModel.changeSource(ATTENDANCE_SOURCE_CLOCK) },
                    )
                    AppFilterChip(
                        label = "Acceso",
                        selected = state.source == ATTENDANCE_SOURCE_ACCESS,
                        onClick = { viewModel.changeSource(ATTENDANCE_SOURCE_ACCESS) },
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

        if (!state.isWeekly) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                ) {
                    ATTENDANCE_VIEWS.forEach { (value, label) ->
                        AppFilterChip(
                            label = label,
                            selected = state.view == value,
                            onClick = { viewModel.changeView(value) },
                        )
                    }
                }
            }
        }

        // ── KPIs ────────────────────────────────────────────────────────────
        item {
            if (state.isWeekly) {
                val summary = state.weekly?.summary
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    BentoMiniStat(
                        label = "Personas",
                        value = (summary?.people ?: 0).toString(),
                        modifier = Modifier.weight(1f),
                        tone = BentoTone.Sea,
                    )
                    BentoMiniStat(
                        label = "Trabajado",
                        value = hoursLabel(summary?.workedMin ?: 0),
                        modifier = Modifier.weight(1f),
                        tone = BentoTone.Mint,
                    )
                    BentoMiniStat(
                        label = "Extra",
                        value = hoursLabel(summary?.extraMin ?: 0),
                        modifier = Modifier.weight(1f),
                        tone = BentoTone.Lavender,
                    )
                    BentoMiniStat(
                        label = "Faltas",
                        value = (summary?.absences ?: 0).toString(),
                        modifier = Modifier.weight(1f),
                        tone = BentoTone.Sun,
                    )
                }
            } else {
                val summary = state.people?.summary
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    BentoMiniStat(
                        label = "Personas",
                        value = (summary?.people ?: 0).toString(),
                        modifier = Modifier.weight(1f),
                        tone = BentoTone.Sea,
                    )
                    BentoMiniStat(
                        label = "En sitio",
                        value = (summary?.onSite ?: 0).toString(),
                        modifier = Modifier.weight(1f),
                        tone = BentoTone.Mint,
                    )
                    BentoMiniStat(
                        label = "Retardos",
                        value = (summary?.lateDays ?: 0).toString(),
                        modifier = Modifier.weight(1f),
                        tone = BentoTone.Sun,
                    )
                    BentoMiniStat(
                        label = "Faltas",
                        value = (summary?.absences ?: 0).toString(),
                        modifier = Modifier.weight(1f),
                        tone = BentoTone.Rose,
                    )
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                SectionLabel(
                    if (state.isWeekly) {
                        "Semana · ${state.weekly?.rows?.size ?: 0} persona(s)"
                    } else {
                        "Periodo · ${state.people?.data?.size ?: 0} persona(s)"
                    },
                    modifier = Modifier.weight(1f),
                )
                PillButton(
                    text = "CSV",
                    onClick = viewModel::exportCsv,
                    variant = PillButtonVariant.Ghost,
                    leadingIcon = Icons.Filled.Share,
                )
            }
        }

        when {
            state.loading -> item {
                LoadingState(modifier = Modifier.padding(vertical = 24.dp))
            }

            state.error != null -> item {
                BentoState(
                    message = state.error ?: "No se pudo cargar la pre nómina",
                    tone = BentoTone.Rose,
                    actionLabel = "Reintentar",
                    onAction = viewModel::retry,
                )
            }

            state.isWeekly -> {
                val rows = state.weekly?.rows.orEmpty()
                if (rows.isEmpty()) {
                    item { BentoState("No hay personal con estos filtros.") }
                } else {
                    items(rows, key = { "weekly-${it.userId}" }) { row ->
                        WeeklyPersonCard(
                            row = row,
                            expanded = expandedPerson == row.userId,
                            onToggle = {
                                expandedPerson = if (expandedPerson == row.userId) "" else row.userId
                            },
                        )
                    }
                }
            }

            else -> {
                val rows = state.people?.data.orEmpty()
                if (rows.isEmpty()) {
                    item { BentoState("No hay personas con estos filtros.") }
                } else {
                    items(rows, key = { "people-${it.employeeId}" }) { row ->
                        PeopleAttendanceCard(
                            row = row,
                            expanded = expandedPerson == row.employeeId,
                            onToggle = {
                                expandedPerson = if (expandedPerson == row.employeeId) "" else row.employeeId
                            },
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun WeeklyPersonCard(
    row: WeeklyAttendanceRowDto,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle,
        borderColor = AppColors.Outline.copy(alpha = 0.45f),
        contentPadding = PaddingValues(14.dp),
    ) {
        Column {
            Text(
                row.name,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    row.employeeNumber?.takeIf { it.isNotBlank() }?.let { append("#$it · ") }
                    append(row.scheduleName ?: "Sin horario")
                    row.departmentName?.let { append(" · $it") }
                    if (!row.linked) append(" · número sin vincular")
                },
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
            row.days.forEach { day ->
                StatusChip(
                    label = attendanceDayStatusCode(day.status),
                    tone = attendanceStatusTone(day.status),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Trabajado ${hoursLabel(row.totals.workedMin)} · Extra ${hoursLabel(row.totals.extraMin)} · " +
                "Aprobado ${hoursLabel(row.totals.approvedExtraMin)} · Faltas ${row.totals.absences}",
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.TextMuted,
        )

        if (expanded) {
            Spacer(Modifier.height(8.dp))
            row.days.forEach { day -> WeeklyDayLine(day) }
        }
    }
}

@Composable
private fun WeeklyDayLine(day: WeeklyAttendanceDayDto) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                formatDateKey(day.date),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
                modifier = Modifier.weight(1f),
            )
            Text(
                buildString {
                    append(day.entryAt?.let { formatLocalTime(it) } ?: "—")
                    append(" → ")
                    append(day.exitAt?.let { formatLocalTime(it) } ?: "—")
                },
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
                modifier = Modifier.weight(1.4f),
            )
            StatusChip(label = attendanceDayStatusLabel(day.status), tone = attendanceStatusTone(day.status))
        }
        Text(
            buildString {
                append("Trabajado ${hoursLabel(day.workedMin)}")
                if (day.extraMin > 0) append(" · Extra ${hoursLabel(day.extraMin)}")
                if (day.lateMin > 0) append(" · Retardo ${day.lateMin} min")
                if (day.missingMin > 0) append(" · Faltó ${hoursLabel(day.missingMin)}")
                day.approval?.let { append(" · ${overtimeStatusLabel(it)}") }
            },
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextFaint,
            modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
        )
    }
}

@Composable
private fun PeopleAttendanceCard(
    row: PeopleAttendanceRowDto,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle,
        borderColor = AppColors.Outline.copy(alpha = 0.45f),
        contentPadding = PaddingValues(14.dp),
    ) {
        Column {
            Text(
                row.employeeName,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    row.employeeNumber?.takeIf { it.isNotBlank() }?.let { append("#$it · ") }
                    append(row.departmentName ?: "Sin departamento")
                    if (!row.linked) append(" · sin vincular")
                },
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
            row.days.forEach { day ->
                StatusChip(
                    label = attendanceDayStatusCode(day.status),
                    tone = attendanceStatusTone(day.status),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Trabajado ${hoursLabel(row.workedMinutes)} · Retardos ${row.lateDays} · " +
                "Faltas ${row.absences} · Sin salida ${row.withoutExit} · Sin entrada ${row.withoutEntry}",
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.TextMuted,
        )

        if (expanded) {
            Spacer(Modifier.height(8.dp))
            row.days.forEach { day ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        formatDateKey(day.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        buildString {
                            append(day.entryAt?.let { formatLocalTime(it) } ?: "—")
                            append(" → ")
                            append(day.exitAt?.let { formatLocalTime(it) } ?: "—")
                            if (day.onSite) append(" · en sitio")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextFaint,
                        modifier = Modifier.weight(1.6f),
                    )
                    StatusChip(label = attendanceDayStatusLabel(day.status), tone = attendanceStatusTone(day.status))
                }
            }
        }
    }
}

private fun attendanceStatusTone(status: String): BentoTone = when (status) {
    "WORKED", "ATTENDED" -> BentoTone.Mint
    "OVERTIME", "REST_WORKED" -> BentoTone.Sea
    "LATE" -> BentoTone.Sun
    "ABSENCE" -> BentoTone.Rose
    "INCOMPLETE" -> BentoTone.Lavender
    else -> BentoTone.Plain
}
