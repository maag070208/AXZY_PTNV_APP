package com.axzydev.puertonuevoapp.feature.timeclock

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.network.schedules.PeopleAttendanceDayDto
import com.axzydev.puertonuevoapp.core.network.schedules.PeopleAttendanceRowDto
import com.axzydev.puertonuevoapp.core.network.schedules.SchedulesApi
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockApi
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.BentoAvatar
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoMiniStat
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.RegisterTopBarAction
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatMinutesAsHours

/** Días que se muestran como tira en cada persona (los del periodo, en orden). */
private const val MAX_STRIP_DAYS = 31

/**
 * Entradas y salidas por persona (`/hr/time-clock/entries-exits`), paridad con
 * la pantalla de la web:
 *
 * - `POST /schedules/attendance/time-clock`: una fila por persona con cada día
 *   del periodo contra su horario (tira de días, incidencias y resumen).
 * - Periodo Día/Semana/Quincena/Mes con navegación ← → y atajo "Hoy".
 * - Vistas rápidas: todos, incidencias, en sitio y sin registros.
 * - Exportar CSV: `POST /time-clock/report/export` trae todas las sesiones con
 *   los filtros vigentes, se arma el archivo y se comparte con el sistema
 *   (`shareCsv`), igual que el CSV de la web.
 *
 * Compuerta: `time_clock.view` (el menú ya la aplica; el API revalida).
 */
@Composable
fun TimeClockEntriesExitsScreen() {
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canView = user?.can(PermissionKeys.TIME_CLOCK_VIEW) == true

    if (!canView) {
        Box(modifier = Modifier.fillMaxSize().background(AppBento.canvas).padding(16.dp)) {
            BentoErrorState(
                message = "No tienes permiso para ver las entradas y salidas del reloj.",
                onRetry = null,
            )
        }
        return
    }

    val viewModel: TimeClockEntriesExitsViewModel =
        viewModel(key = "time-clock-entries-exits") {
            TimeClockEntriesExitsViewModel(
                schedulesApi = SchedulesApi(AppContainer.apiClient),
                timeClockApi = TimeClockApi(AppContainer.apiClient),
            )
        }
    val state by viewModel.uiState.collectAsState()
    val today = remember { todayDayKey() }

    RegisterTopBarAction(Icons.Filled.Share, "Exportar CSV") { viewModel.exportCsv() }

    Column(modifier = Modifier.fillMaxSize().background(AppBento.canvas)) {
        AttendanceKpis(state)
        ReportFiltersCard(
            state = state,
            today = today,
            onPeriodChange = viewModel::setPeriod,
            onShiftPeriod = viewModel::shiftPeriod,
            onToday = viewModel::goToday,
            onViewChange = viewModel::setView,
            onSearchChange = viewModel::onSearchChange,
            onToggleInactive = viewModel::toggleIncludeInactive,
            onExport = viewModel::exportCsv,
        )

        val loadError = state.error
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.loading && state.rows.isEmpty() -> BentoState(
                    message = "Cargando entradas y salidas…",
                    loading = true,
                    modifier = Modifier.padding(24.dp),
                )
                loadError != null && state.rows.isEmpty() -> BentoErrorState(
                    message = loadError,
                    onRetry = viewModel::retry,
                    modifier = Modifier.padding(24.dp),
                )
                state.rows.isEmpty() -> BentoState(
                    message = "Sin personas para este periodo y filtros.",
                    modifier = Modifier.padding(24.dp),
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 12.dp,
                        end = 16.dp,
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (state.refreshing) {
                        item(key = "refreshing") {
                            Text(
                                "Actualizando…",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextFaint,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    items(state.rows.size, key = { index -> "person-$index" }) { index ->
                        val row = state.rows[index]
                        PersonAttendanceCard(
                            row = row,
                            period = state.period,
                            today = today,
                            days = state.rangeDays,
                        )
                    }
                    if (state.hasMore) {
                        item(key = "more") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                PillButton(
                                    text = "Cargar más",
                                    onClick = viewModel::loadMore,
                                    loading = state.loadingMore,
                                    variant = PillButtonVariant.Secondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Franja de indicadores: compacta, con tono por significado y neutra cuando está en cero. */
@Composable
private fun AttendanceKpis(state: TimeClockEntriesExitsUiState) {
    val summary = state.summary ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BentoMiniStat(
            label = "Personas",
            value = summary.people.toString(),
            tone = BentoTone.Sea,
            labelMaxLines = 1,
            modifier = Modifier.width(106.dp),
        )
        BentoMiniStat(
            label = "Con registro",
            value = summary.withRecords.toString(),
            tone = if (summary.withRecords == 0) BentoTone.Plain else BentoTone.Mint,
            labelMaxLines = 1,
            modifier = Modifier.width(106.dp),
        )
        BentoMiniStat(
            label = "En sitio",
            value = summary.onSite.toString(),
            tone = if (summary.onSite == 0) BentoTone.Plain else BentoTone.Lavender,
            labelMaxLines = 1,
            modifier = Modifier.width(106.dp),
        )
        BentoMiniStat(
            label = "Horas",
            value = formatMinutesAsHours(summary.workedMinutes),
            tone = if (summary.workedMinutes == 0) BentoTone.Plain else BentoTone.Sun,
            labelMaxLines = 1,
            modifier = Modifier.width(106.dp),
        )
        BentoMiniStat(
            label = "Incidencias",
            value = state.incidents.toString(),
            tone = if (state.incidents == 0) BentoTone.Plain else BentoTone.Rose,
            labelMaxLines = 1,
            modifier = Modifier.width(106.dp),
        )
    }
}

/** Periodo, navegación, vistas rápidas, búsqueda y exportación. */
@Composable
private fun ReportFiltersCard(
    state: TimeClockEntriesExitsUiState,
    today: String,
    onPeriodChange: (AttendancePeriod) -> Unit,
    onShiftPeriod: (Int) -> Unit,
    onToday: () -> Unit,
    onViewChange: (AttendanceView) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleInactive: () -> Unit,
    onExport: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AttendancePeriod.entries.forEach { period ->
                AppFilterChip(
                    label = period.label,
                    selected = state.period == period,
                    onClick = { onPeriodChange(period) },
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onShiftPeriod(-1) }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Periodo anterior", tint = AppColors.TextMuted)
            }
            Text(
                text = attendanceRangeLabel(state.period, state.rangeDays, state.date),
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onShiftPeriod(1) }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Periodo siguiente", tint = AppColors.TextMuted)
            }
            AppFilterChip(
                label = "Hoy",
                selected = state.date == today,
                onClick = onToday,
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AttendanceView.entries.forEach { view ->
                AppFilterChip(
                    label = view.label,
                    selected = state.view == view,
                    onClick = { onViewChange(view) },
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        AppSearchField(
            value = state.q,
            onValueChange = onSearchChange,
            placeholder = "Buscar por nombre, número o departamento",
        )

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AppFilterChip(
                label = "Incluir inactivos",
                selected = state.includeInactive,
                onClick = onToggleInactive,
            )
            Spacer(Modifier.weight(1f))
            PillButton(
                text = "Exportar CSV",
                onClick = onExport,
                loading = state.exporting,
                variant = PillButtonVariant.Secondary,
            )
        }

        if (state.exportError != null) {
            Text(
                text = state.exportError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(10.dp))
        AttendanceLegend()
    }
}

/** Leyenda discreta de la tira de días: punto de color + etiqueta. */
@Composable
private fun AttendanceLegend() {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf("ATTENDED", "LATE", "ABSENCE", "REST", "NO_INFO").forEach { status ->
            val colors = AppBento.tone(attendanceDayTone(status))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(colors.content, CircleShape))
                Spacer(Modifier.width(5.dp))
                Text(
                    attendanceDayLabel(status),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                )
            }
        }
    }
}

/** Una persona del reporte: cabecera, tira de días (o detalle del día) y contadores. */
@Composable
private fun PersonAttendanceCard(
    row: PeopleAttendanceRowDto,
    period: AttendancePeriod,
    today: String,
    days: List<String>,
) {
    val focus = focusDay(row, period, today)
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BentoAvatar(name = row.displayName(), color = AppColors.EmeraldPrimary, size = 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = row.displayName(),
                        style = MaterialTheme.typography.titleSmall,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (row.onSite) {
                        Spacer(Modifier.width(6.dp))
                        StatusChip(label = "En sitio", tone = BentoTone.Sea)
                    }
                    if (!row.active) {
                        Spacer(Modifier.width(6.dp))
                        StatusChip(label = "Inactivo", tone = BentoTone.Plain)
                    }
                }
                Text(
                    text = listOfNotNull(
                        row.employeeNumber?.let { "#$it" },
                        row.departmentName?.takeIf { it.isNotBlank() },
                        row.jobTitle?.takeIf { it.isNotBlank() },
                    ).joinToString(" · ").ifBlank { "Sin expediente vinculado" },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (period == AttendancePeriod.Day) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(
                    label = attendanceDayLabel(focus?.status ?: "NO_INFO"),
                    tone = attendanceDayTone(focus?.status ?: "NO_INFO"),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = row.timesLabel(focus),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = workedTimeLabel(focus?.workedMinutes ?: 0),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextMuted,
                )
            }
        } else {
            DayStrip(days = days.ifEmpty { row.days.map { it.date } }, row = row)
            focus?.let { day ->
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (day.date == today) "Hoy" else shortDayLabel(day.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextMuted,
                    )
                    Spacer(Modifier.width(6.dp))
                    StatusChip(label = attendanceDayLabel(day.status), tone = attendanceDayTone(day.status))
                    if (day.entryAt != null || day.exitAt != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${row.timesLabel(day)} · ${workedTimeLabel(day.workedMinutes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                val extra = buildList {
                    if (day.lateMinutes > 0) add("retardo ${day.lateMinutes} min")
                    day.incident?.let { add(incidentLabel(it)) }
                }
                if (extra.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        extra.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextFaint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        val counters = buildList {
            if (row.lateDays > 0) add("${row.lateDays} retardo(s)" to BentoTone.Sun)
            if (row.absences > 0) add("${row.absences} falta(s)" to BentoTone.Rose)
            if (row.withoutExit > 0) add("${row.withoutExit} sin salida" to BentoTone.Sun)
            if (row.withoutEntry > 0) add("${row.withoutEntry} sin entrada" to BentoTone.Rose)
        }
        if (counters.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                counters.forEach { (label, tone) -> StatusChip(label = label, tone = tone) }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = "Trabajado en el periodo: ${formatMinutesAsHours(row.workedMinutes)}",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextFaint,
        )
    }
}

/** Tira de días del periodo: letra y día del mes, teñidos por el estado del día. */
@Composable
private fun DayStrip(days: List<String>, row: PeopleAttendanceRowDto) {
    val byDate = remember(row.days) { row.days.associateBy { it.date } }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        days.take(MAX_STRIP_DAYS).forEach { day ->
            val state = byDate[day]
            val colors = AppBento.tone(attendanceDayTone(state?.status ?: "NO_INFO"))
            Column(
                modifier = Modifier
                    .width(38.dp)
                    .clip(AppShape.row)
                    .background(colors.container, AppShape.row)
                    .padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    weekdayLetter(day),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.content.copy(alpha = 0.7f),
                )
                Text(
                    dayKeyDayNumber(day),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.content,
                )
            }
        }
    }
}

/** Día del mes de una clave `YYYY-MM-DD` ("03"). */
private fun dayKeyDayNumber(dayKey: String): String =
    if (dayKey.length >= 10) dayKey.substring(8, 10) else dayKey

/** Día que se detalla en el pie de la tarjeta: hoy si está en el periodo; si no, el último con datos. */
private fun focusDay(
    row: PeopleAttendanceRowDto,
    period: AttendancePeriod,
    today: String,
): PeopleAttendanceDayDto? {
    if (row.days.isEmpty()) return null
    if (period == AttendancePeriod.Day) return row.days.first()
    row.days.firstOrNull { it.date == today }?.let { if (it.hasData()) return it }
    return row.days.lastOrNull { it.hasData() } ?: row.days.lastOrNull()
}

/** ¿El día tiene alguna marca del reloj? */
private fun PeopleAttendanceDayDto.hasData(): Boolean =
    entryAt != null || exitAt != null || status == "ATTENDED" || status == "LATE"
