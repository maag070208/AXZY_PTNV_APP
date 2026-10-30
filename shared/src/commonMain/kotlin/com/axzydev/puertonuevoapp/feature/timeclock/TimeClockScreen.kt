package com.axzydev.puertonuevoapp.feature.timeclock

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockApi
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockPunchDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoAvatar
import com.axzydev.puertonuevoapp.core.ui.BentoMiniStat
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.ui.table.AppTableScreen
import com.axzydev.puertonuevoapp.core.ui.table.FilterControl
import com.axzydev.puertonuevoapp.core.ui.table.FilterOption
import com.axzydev.puertonuevoapp.core.ui.table.TableFilterSpec
import com.axzydev.puertonuevoapp.core.ui.table.TableSortSpec
import com.axzydev.puertonuevoapp.core.util.formatLocalTime
import com.axzydev.puertonuevoapp.core.util.relativeDayLabel

/** Orden por columna de la tabla de checadas (claves del API). */
private val punchSortSpecs = listOf(
    TableSortSpec("occurredAt", "Fecha"),
    TableSortSpec("name", "Persona"),
    TableSortSpec("method", "Método"),
    TableSortSpec("clock", "Reloj"),
)

private val methodFilterOptions = listOf(
    FilterOption("FACE", "Facial"),
    FilterOption("FINGERPRINT", "Huella"),
    FilterOption("CARD", "Tarjeta"),
    FilterOption("OTHER", "Otro"),
)

/**
 * Asistencia del reloj (`/hr/time-clock`): checadas de los Hikvision por rango
 * de fechas, con el estado de sincronización de cada reloj arriba.
 *
 * - Rango: pastillas Hoy / Ayer / Últimos 7 / Personalizado (dos campos
 *   `AAAA-MM-DD`), igual que la web.
 * - Tabla server-side (`POST /time-clock/query`) con `AppTableScreen`: búsqueda
 *   por nombre o número, filtros de método y reloj, orden por columna.
 * - KPIs de checadas del rango, relojes registrados y relojes con problemas;
 *   las checadas de hoy vienen de `/time-clock/summary` si la persona tiene
 *   `time_clocks.manage` o `time_clock.write`.
 *
 * Compuerta: `time_clock.view` (el menú ya la aplica; el API revalida).
 */
@Composable
fun TimeClockScreen() {
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canView = user?.can(PermissionKeys.TIME_CLOCK_VIEW) == true

    if (!canView) {
        Box(modifier = Modifier.fillMaxSize().background(AppBento.canvas).padding(16.dp)) {
            BentoErrorState(
                message = "No tienes permiso para ver la asistencia del reloj.",
                onRetry = null,
            )
        }
        return
    }

    val canReadSummary = user?.let {
        it.can(PermissionKeys.TIME_CLOCKS_MANAGE) || it.can(PermissionKeys.TIME_CLOCK_WRITE)
    } == true
    val viewModel: TimeClockViewModel = viewModel(key = "time-clock-attendance") {
        TimeClockViewModel(TimeClockApi(AppContainer.apiClient), canReadSummary)
    }
    val state by viewModel.uiState.collectAsState()
    val range by viewModel.range.collectAsState()
    val overview by viewModel.overview.collectAsState()

    val filterSpecs = remember(overview.status) {
        listOf(
            TableFilterSpec("method", "Método", FilterControl.Select(methodFilterOptions)),
            TableFilterSpec(
                "clock",
                "Reloj",
                FilterControl.Select(
                    overview.devices.map { device ->
                        FilterOption(device.clockSerial, device.name.ifBlank { device.clockSerial })
                    }
                ),
            ),
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBento.canvas)) {
        PunchKpis(
            punches = state.total,
            devices = overview.devices.size,
            failing = overview.failedDevices,
            punchesToday = overview.punchesToday,
        )

        overview.error?.let { message ->
            BentoErrorState(
                message = message,
                onRetry = viewModel::loadOverview,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        RangeControls(
            range = range,
            onModeChange = viewModel::applyRange,
            onFromChange = viewModel::setCustomFrom,
            onToChange = viewModel::setCustomTo,
        )

        AppTableScreen(
            state = state,
            heading = "Checadas",
            filterSpecs = filterSpecs,
            onSearchChange = viewModel::onSearchChange,
            onFiltersChange = viewModel::onFiltersChange,
            onRefresh = viewModel::reload,
            onLoadMore = viewModel::loadMore,
            onRetry = viewModel::retry,
            searchPlaceholder = "Buscar por nombre o número de empleado",
            emptyMessage = "No hay checadas en este rango",
            searchKey = "q",
            sortSpecs = punchSortSpecs,
            onSortChange = viewModel::onSortChange,
            itemKey = { it.id },
            modifier = Modifier.weight(1f),
        ) { punch ->
            PunchRow(punch)
        }
    }
}

/**
 * Franja de indicadores: checadas del rango, relojes registrados, relojes con
 * problemas y checadas de hoy. Cuatro mosaicos compactos en una fila, con el
 * mismo patrón que Tiempo extra (`BentoMiniStat` con tono y `weight(1f)`): así
 * entran los cuatro sin scroll horizontal ni recortes en el borde.
 */
@Composable
private fun PunchKpis(
    punches: Int,
    devices: Int,
    failing: Int,
    punchesToday: Int?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BentoMiniStat(
            label = "Checadas",
            value = punches.toString(),
            tone = BentoTone.Sea,
            labelMaxLines = 1,
            modifier = Modifier.weight(1f),
        )
        BentoMiniStat(
            label = "Relojes",
            value = devices.toString(),
            tone = BentoTone.Lavender,
            labelMaxLines = 1,
            modifier = Modifier.weight(1f),
        )
        BentoMiniStat(
            label = "Problemas",
            value = failing.toString(),
            tone = if (failing == 0) BentoTone.Mint else BentoTone.Rose,
            labelMaxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (punchesToday != null) {
            BentoMiniStat(
                label = "Hoy",
                value = punchesToday.toString(),
                tone = BentoTone.Sun,
                labelMaxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Pastillas del rango + (en Personalizado) los dos campos de fecha. */
@Composable
private fun RangeControls(
    range: TimeClockRange,
    onModeChange: (TimeClockRangeMode) -> Unit,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TimeClockRangeMode.entries.forEach { mode ->
                AppFilterChip(
                    label = mode.label,
                    selected = range.mode == mode,
                    onClick = { onModeChange(mode) },
                )
            }
        }

        if (range.mode == TimeClockRangeMode.Custom) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(
                    value = range.from,
                    onValueChange = onFromChange,
                    label = { Text("Desde (AAAA-MM-DD)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
                AppTextField(
                    value = range.to,
                    onValueChange = onToChange,
                    label = { Text("Hasta (AAAA-MM-DD)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f),
                )
            }
            if (!range.isValid) {
                Text(
                    text = "Escribe dos fechas válidas (AAAA-MM-DD), la de inicio antes de la de fin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/** Una checada: hora, persona, método y reloj. */
@Composable
private fun PunchRow(punch: TimeClockPunchDto) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.width(64.dp)) {
                Text(
                    text = formatLocalTime(punch.occurredAt),
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.TextPrimary,
                )
                Text(
                    text = relativeDayLabel(punch.occurredAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                )
            }
            Spacer(Modifier.width(6.dp))
            BentoAvatar(
                name = punch.name.ifBlank { punch.employeeNumber },
                color = AppColors.EmeraldPrimary,
                size = 34.dp,
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = punch.name.ifBlank { "Sin nombre en el reloj" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "#${punch.employeeNumber} · ${punch.clock ?: punch.clockSerial}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusChip(label = punchMethodLabel(punch.method), tone = punchMethodTone(punch.method))
        }
    }
}
