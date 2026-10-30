package com.axzydev.puertonuevoapp.feature.timeclock

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockApi
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockCameraDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockConfigDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockDeviceDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockImportDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockInspectOverviewDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockProgressDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoIconButton
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.BentoTabItem
import com.axzydev.puertonuevoapp.core.ui.BentoTabs
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatDateTime
import androidx.compose.ui.semantics.Role

/** A partir de esta diferencia con el servidor se avisa que el reloj no está en hora. */
private const val DriftNoticeSeconds = 60

/** Días del período para leer eventos del equipo (mismo selector que la web). */
private val InspectDayOptions = listOf(1, 2, 7, 30)

/**
 * Relojes checadores (solo ADMIN, `time_clocks.manage`): lista con su
 * sincronización, alta/edición/baja del registro del sistema y el detalle de
 * cada reloj con su configuración leída en vivo, su timeline y la inspección de
 * lo que tiene el equipo (personas, tarjetas, eventos y cámara).
 *
 * **Solo lectura hacia el equipo.** Nada de esta pantalla modifica el reloj:
 * dar de alta, editar o dar de baja cambia únicamente cómo el sistema se conecta
 * y usa sus checadas (TIME_CLOCK.md §1 y §4).
 */
@Composable
fun TimeClocksScreen(
    viewModel: TimeClocksViewModel = viewModel {
        // El API de relojes aún no está cableado en `AppContainer`; se construye
        // sobre el mismo `ApiClient` mientras se agrega ahí (una línea).
        TimeClocksViewModel(TimeClockApi(AppContainer.apiClient))
    },
) {
    val state by viewModel.uiState.collectAsState()
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canManage = user?.can(PermissionKeys.TIME_CLOCKS_MANAGE) == true

    if (!canManage) {
        Box(modifier = Modifier.fillMaxSize().background(AppBento.canvas).padding(16.dp)) {
            BentoState(
                message = "Necesitas el permiso de administración de relojes (time_clocks.manage) para ver esta pantalla.",
                tone = BentoTone.Rose,
                icon = Icons.Filled.Lock,
            )
        }
        return
    }

    val selected = state.selected
    Box(modifier = Modifier.fillMaxSize().background(AppBento.canvas)) {
        if (selected == null) {
            ClocksList(state = state, viewModel = viewModel)
        } else {
            ClockDetail(state = state, clock = selected, viewModel = viewModel)
        }
    }

    if (state.registerOpen) RegisterClockDialog(state = state, viewModel = viewModel)
    if (state.editSerial != null) EditClockDialog(state = state, viewModel = viewModel)
    state.retireTarget?.let { target -> RetireClockDialog(target = target, state = state, viewModel = viewModel) }
    if (state.importOpen) ImportDialog(state = state, viewModel = viewModel)
    state.photo?.let { photo -> PhotoDialog(photo = photo, onDismiss = viewModel::closePhoto) }
}

// ─────────────────────────────────────────────────────────────────────────────
// Lista de relojes
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ClocksList(state: TimeClocksUiState, viewModel: TimeClocksViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ReadOnlyCard(state = state, viewModel = viewModel) }

        if (!state.configured) {
            item {
                BentoState(
                    message = "La API no tiene configurado el usuario de los relojes (CHECADOR_USER / TIME_CLOCK_USER): no se puede conectar a ninguno.",
                    tone = BentoTone.Sun,
                    icon = Icons.Filled.Warning,
                )
            }
        }

        state.inProgress?.let { progress ->
            item { SyncProgressCard(progress = progress, syncing = state.syncing) }
        }

        state.importJob?.let { job ->
            item { ImportJobCard(job = job) }
        }

        if (state.error != null && state.devices.isEmpty()) {
            item { BentoErrorState(message = state.error, onRetry = { viewModel.load() }) }
        }

        if (state.loading && state.devices.isEmpty()) {
            item { BentoState(message = "Leyendo los relojes…", loading = true) }
        }

        if (!state.loading && state.devices.isEmpty() && state.error == null) {
            item {
                BentoState(
                    message = "Todavía no hay relojes dados de alta.",
                    tone = BentoTone.Sea,
                    icon = Icons.Filled.Router,
                    actionLabel = "Dar de alta un reloj",
                    onAction = viewModel::openRegister,
                )
            }
        }

        items(state.devices, key = { it.clockSerial }) { device ->
            ClockCard(device = device, onClick = { viewModel.openClock(device.clockSerial) })
        }

        item {
            Text(
                text = "El sistema solo LEE de los relojes: sus checadas y su configuración. Nunca les cambia nada.",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

/** Tarjeta de cabecera: explica el modo solo-lectura y concentra las acciones globales. */
@Composable
private fun ReadOnlyCard(state: TimeClocksUiState, viewModel: TimeClocksViewModel) {
    val tone = AppBento.tone(BentoTone.Sea)
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = null,
        containerColor = tone.container,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = tone.content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Solo lectura", style = MaterialTheme.typography.titleSmall, color = tone.content)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "El sistema se conecta a los relojes para leer sus checadas y su configuración; nunca les cambia nada. " +
                "De cada reloj solo se leen las checadas (rostro, huella o tarjeta), no las aperturas de puerta.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Se conecta con el usuario y la contraseña configurados en la API (los mismos para todos los relojes).",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextFaint,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PillButton(
                text = if (state.syncing) "Sincronizando…" else "Sincronizar todo",
                onClick = viewModel::sync,
                variant = PillButtonVariant.Ink,
                enabled = state.configured && !state.syncing,
                loading = state.syncing,
                leadingIcon = Icons.Filled.Sync,
            )
            PillButton(
                text = "Importar por fechas",
                onClick = viewModel::openImport,
                variant = PillButtonVariant.Secondary,
                enabled = state.configured,
                leadingIcon = Icons.Filled.CalendarMonth,
            )
            PillButton(
                text = "Dar de alta un reloj",
                onClick = viewModel::openRegister,
                variant = PillButtonVariant.Secondary,
                enabled = state.configured,
                leadingIcon = Icons.Filled.Add,
            )
            if (state.devices.isNotEmpty()) {
                PillButton(
                    text = "Actualizar",
                    onClick = { viewModel.refresh() },
                    variant = PillButtonVariant.Ghost,
                    leadingIcon = Icons.Filled.Refresh,
                )
            }
        }
    }
}

/** Tarjeta de un reloj: nombre, estado, si cuenta para asistencia y su sincronización. */
@Composable
private fun ClockCard(device: TimeClockDeviceDto, onClick: () -> Unit) {
    val status = clockCardStatus(device)
    val colors = AppBento.tone(clockStatusTone(status))
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        borderColor = null,
        containerColor = colors.container,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.name.ifBlank { device.clockSerial },
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(device.clockSerial, style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Ver detalles",
                tint = AppColors.TextFaint,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StatusChip(label = status.label, tone = clockStatusTone(status))
            StatusChip(
                label = if (device.countsAttendance) "Cuenta para entradas/salidas" else "No cuenta para entradas/salidas",
                tone = if (device.countsAttendance) BentoTone.Lavender else BentoTone.Plain,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            device.url,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        clockStatusText(device)?.let { text ->
            Spacer(Modifier.height(6.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = when (status) {
                    TimeClockCardStatus.Error, TimeClockCardStatus.Paused -> AppColors.Danger
                    else -> AppColors.TextMuted
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MiniStat("Checadas", device.punches.toString(), Modifier.weight(1f))
            MiniStat("Última sincronización", formatDateTime(device.syncedAt), Modifier.weight(1f))
            MiniStat("Último intento", formatDateTime(device.lastRun?.finishedAt), Modifier.weight(1f))
        }
    }
}

/** Texto del estado de la tarjeta (avance de la corrida, error del último intento…). */
private fun clockStatusText(device: TimeClockDeviceDto): String? = when (clockCardStatus(device)) {
    TimeClockCardStatus.Running -> device.inProgress?.let { progress ->
        if (progress.total != null) {
            "${progress.readCount} de ${progress.total} eventos revisados · ${progress.newCount} checadas nuevas"
        } else {
            "${progress.newCount} checadas nuevas"
        }
    }

    TimeClockCardStatus.Paused -> "Rechazó la contraseña: sincronización en pausa."
    TimeClockCardStatus.Error -> device.lastRun?.error ?: "El último intento no conectó."
    TimeClockCardStatus.Pending -> "Su primera sincronización aún no termina."
    TimeClockCardStatus.Ok -> null
}

/** Barra de avance de la corrida global de sincronización. */
@Composable
private fun SyncProgressCard(progress: TimeClockProgressDto, syncing: Boolean) {
    val tone = AppBento.tone(BentoTone.Sea)
    AppCard(modifier = Modifier.fillMaxWidth(), borderColor = null, containerColor = tone.container) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Sync, contentDescription = null, tint = tone.content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (syncing) "Sincronización en curso" else "Sincronizando",
                style = MaterialTheme.typography.titleSmall,
                color = tone.content,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(syncProgressText(progress), style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
    }
}

private fun syncProgressText(progress: TimeClockProgressDto): String {
    val detail = if (progress.total != null) {
        "${progress.readCount} de ${progress.total} eventos revisados"
    } else {
        "${progress.readCount} eventos revisados"
    }
    val remaining = progress.remaining?.let { " · faltan $it" } ?: ""
    return "$detail · ${progress.newCount} checadas nuevas$remaining"
}

/** Avance de la importación manual por fechas (`status.importJob`). */
@Composable
private fun ImportJobCard(job: TimeClockImportDto) {
    val running = job.finishedAt == null
    val tone = when {
        running -> BentoTone.Sea
        job.error != null -> BentoTone.Rose
        else -> BentoTone.Mint
    }
    val colors = AppBento.tone(tone)
    AppCard(modifier = Modifier.fillMaxWidth(), borderColor = null, containerColor = colors.container) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = colors.content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    running -> "Importando checadas por fechas"
                    job.error != null -> "Importación con error"
                    else -> "Importación terminada"
                },
                style = MaterialTheme.typography.titleSmall,
                color = colors.content,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(importJobText(job), style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
        job.error?.let { error ->
            Spacer(Modifier.height(4.dp))
            Text(error, style = MaterialTheme.typography.labelSmall, color = AppColors.Danger)
        }
    }
}

private fun importJobText(job: TimeClockImportDto): String {
    val range = "${job.from} – ${job.to}"
    if (job.finishedAt == null && job.total == null) return "$range · conectando con los relojes…"
    if (job.total != null) {
        return "$range · ${job.readCount} de ${job.total} checadas leídas · ${job.newCount} nuevas"
    }
    return "$range · ${job.newCount} checadas nuevas de ${job.readCount} leídas"
}

// ─────────────────────────────────────────────────────────────────────────────
// Detalle de un reloj (configuración en vivo, timeline e inspección)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ClockDetail(state: TimeClocksUiState, clock: TimeClockDeviceDto, viewModel: TimeClocksViewModel) {
    val status = clockCardStatus(clock)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PillButton(
                text = "Volver",
                onClick = viewModel::closeClock,
                variant = PillButtonVariant.Ghost,
                leadingIcon = Icons.AutoMirrored.Filled.ArrowBack,
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    clock.name.ifBlank { clock.clockSerial },
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    clock.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StatusChip(label = status.label, tone = clockStatusTone(status))
            StatusChip(
                label = if (clock.countsAttendance) "Cuenta para entradas/salidas" else "No cuenta para entradas/salidas",
                tone = if (clock.countsAttendance) BentoTone.Lavender else BentoTone.Plain,
            )
            if (clock.writeEnabled) StatusChip(label = "Escritura habilitada", tone = BentoTone.Sun)
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PillButton(
                text = if (state.syncing) "Sincronizando…" else "Sincronizar",
                onClick = viewModel::sync,
                variant = PillButtonVariant.Ink,
                enabled = state.configured && !state.syncing,
                loading = state.syncing,
                leadingIcon = Icons.Filled.Sync,
            )
            PillButton(
                text = "Volver a leer",
                onClick = {
                    viewModel.loadConfig(clock.clockSerial)
                    viewModel.loadHistory(clock.clockSerial)
                    viewModel.refreshInspect()
                },
                variant = PillButtonVariant.Secondary,
                loading = state.config.loading,
                leadingIcon = Icons.Filled.Refresh,
            )
            PillButton(
                text = "Editar",
                onClick = { viewModel.openEdit(clock) },
                variant = PillButtonVariant.Secondary,
                leadingIcon = Icons.Filled.Edit,
            )
            PillButton(
                text = "Dar de baja",
                onClick = { viewModel.askRetire(clock.clockSerial) },
                variant = PillButtonVariant.Ghost,
                leadingIcon = Icons.Filled.Delete,
            )
        }

        clock.lastRun?.error?.let { error ->
            BentoState(
                message = "El último intento de sincronización falló: $error",
                tone = BentoTone.Rose,
                icon = Icons.Filled.Warning,
            )
        }

        SyncSection(clock = clock)
        HistorySection(history = state.history, onRetry = { viewModel.loadHistory(clock.clockSerial) })
        ConfigSection(config = state.config, onRefresh = { viewModel.loadConfig(clock.clockSerial) })
        InspectSection(
            inspect = state.inspect,
            onTab = viewModel::selectInspectTab,
            onDays = viewModel::setInspectDays,
            onRefresh = viewModel::refreshInspect,
            onPerson = viewModel::openPhoto,
        )
        Spacer(Modifier.height(4.dp))
    }
}

/** Datos de sincronización del registro del sistema. */
@Composable
private fun SyncSection(clock: TimeClockDeviceDto) {
    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("Sincronización")
        InfoRow("Checadas guardadas", clock.punches.toString())
        InfoRow("Última checada", formatDateTime(clock.lastPunch))
        InfoRow("Última sincronización", formatDateTime(clock.syncedAt))
        InfoRow("Consecutivo leído", clock.lastSerialNo.toString())
        clock.lastRun?.let { run ->
            InfoRow("Último intento", formatDateTime(run.finishedAt))
            InfoRow("Eventos revisados", run.readCount.toString())
            InfoRow("Checadas nuevas", run.newCount.toString())
        }
        clock.inProgress?.let { progress ->
            Spacer(Modifier.height(6.dp))
            Text(syncProgressText(progress), style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
        }
        if (clock.pausedByCredentials) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Un reloj rechazó la contraseña y se pausó su sincronización. Actualiza la contraseña en la API y reiníciala.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Danger,
            )
        }
    }
}

/** Timeline de los últimos intentos de sincronización del reloj. */
@Composable
private fun HistorySection(history: TimeClocksHistoryUi, onRetry: () -> Unit) {
    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("Historial de sincronización")
        val data = history.data
        when {
            history.loading && data == null -> BentoState(message = "Leyendo el historial…", loading = true)
            history.error != null && data == null -> BentoState(
                message = history.error,
                tone = BentoTone.Rose,
                actionLabel = "Reintentar",
                onAction = onRetry,
            )

            data == null -> Text(
                "Sin intentos registrados.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )

            else -> {
                val summary = data.summary
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MiniStat("Intentos", summary.attempts.toString(), Modifier.weight(1f))
                    MiniStat("Conectados", summary.okCount.toString(), Modifier.weight(1f))
                    MiniStat("Fallidos", summary.failCount.toString(), Modifier.weight(1f))
                    MiniStat("Fallos seguidos", summary.failuresInARow.toString(), Modifier.weight(1f))
                }
                summary.lastError?.let { error ->
                    Spacer(Modifier.height(6.dp))
                    Text("Último error: $error", style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
                }
                if (data.events.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    BentoState(message = "Sin intentos registrados.")
                } else {
                    Spacer(Modifier.height(6.dp))
                    data.events.take(8).forEach { event ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    formatDateTime(event.startedAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextPrimary,
                                )
                                Text(
                                    "${event.trigger} · ${event.readCount} leídas · ${event.newCount} nuevas",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColors.TextFaint,
                                )
                                event.error?.let { error ->
                                    Text(error, style = MaterialTheme.typography.labelSmall, color = AppColors.Danger)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            StatusChip(
                                label = if (event.ok) "Conectó" else "Falló",
                                tone = if (event.ok) BentoTone.Mint else BentoTone.Rose,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Configuración leída en vivo del reloj (solo lectura). */
@Composable
private fun ConfigSection(config: TimeClocksConfigUi, onRefresh: () -> Unit) {
    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Configuración del reloj", modifier = Modifier.weight(1f))
            if (config.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = AppColors.TextFaint,
                )
            } else {
                BentoIconButton(
                    icon = Icons.Filled.Refresh,
                    contentDescription = "Actualizar configuración",
                    onClick = onRefresh,
                )
            }
        }
        Text(
            "Leída en vivo del reloj, sin cambiarle nada.",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextFaint,
        )
        Spacer(Modifier.height(8.dp))
        when {
            config.loading && config.data == null -> BentoState(message = "Leyendo del reloj…", loading = true)
            config.error != null && config.data == null -> BentoState(
                message = config.error,
                tone = BentoTone.Rose,
                actionLabel = "Reintentar",
                onAction = onRefresh,
            )

            config.data == null -> Text(
                "Abre la configuración con «Actualizar».",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )

            else -> {
                config.error?.let { error ->
                    Text(error, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
                    Spacer(Modifier.height(6.dp))
                }
                ClockSettings(settings = config.data)
            }
        }
    }
}

@Composable
private fun ClockSettings(settings: TimeClockConfigDto) {
    InfoRow("Nombre en el reloj", settings.device.name ?: "—")
    InfoRow("Modelo", settings.device.model ?: "—")
    InfoRow("Firmware", settings.device.firmware ?: "—")
    InfoRow("MAC", settings.device.mac ?: "—")
    InfoRow("Número de serie", settings.clockSerial)
    val hour = settings.hour
    if (hour != null) {
        val drift = hour.driftSeconds
        val inHour = kotlin.math.abs(drift) < DriftNoticeSeconds
        InfoRow("Hora del reloj", clockTime(hour.localTime))
        InfoRow("Ajuste de hora", hour.mode?.let { if (it == "NTP") "Automático (NTP)" else "Manual" } ?: "—")
        InfoRow("Zona", hour.zone ?: "—")
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(
                label = when {
                    inHour -> "En hora"
                    drift > 0 -> "Va ${driftText(drift)} adelantado"
                    else -> "Va ${driftText(drift)} atrasado"
                },
                tone = if (inHour) BentoTone.Mint else BentoTone.Sun,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (inHour) "${kotlin.math.abs(drift)} s de diferencia con el servidor" else "Ajusta la hora en el propio reloj.",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextMuted,
            )
        }
    }
    settings.people?.let { people ->
        Spacer(Modifier.height(4.dp))
        InfoRow("Personas registradas", people.total.toString())
        Text(
            "${people.withFace} con rostro · ${people.withFingerprint} con huella · ${people.withCard} con tarjeta",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextFaint,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Inspección de lo que tiene el reloj (solo lectura)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InspectSection(
    inspect: TimeClocksInspectUi,
    onTab: (TimeClockInspectTab) -> Unit,
    onDays: (Int) -> Unit,
    onRefresh: () -> Unit,
    onPerson: (String) -> Unit,
) {
    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Lo que tiene el reloj", modifier = Modifier.weight(1f))
            if (inspect.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = AppColors.TextFaint,
                )
            } else {
                BentoIconButton(
                    icon = Icons.Filled.Refresh,
                    contentDescription = "Actualizar inspección",
                    onClick = onRefresh,
                )
            }
        }
        Text(
            "Personas, tarjetas y eventos que el reloj tiene guardados. Solo lectura: no cambia nada en el equipo.",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextFaint,
        )
        Spacer(Modifier.height(10.dp))
        BentoTabs(
            items = TimeClockInspectTab.entries.map { BentoTabItem(it.label) },
            selectedIndex = inspect.tab.ordinal,
            onSelect = { index -> TimeClockInspectTab.entries.getOrNull(index)?.let(onTab) },
        )
        if (inspect.tab == TimeClockInspectTab.Events) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                InspectDayOptions.forEach { days ->
                    AppFilterChip(
                        label = if (days == 1) "Último día" else "Últimos $days días",
                        selected = inspect.days == days,
                        onClick = { onDays(days) },
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        when {
            inspect.loading && inspect.tabHasNoData() -> BentoState(message = "Leyendo del reloj…", loading = true)
            inspect.error != null -> BentoState(
                message = inspect.error,
                tone = BentoTone.Rose,
                actionLabel = "Reintentar",
                onAction = onRefresh,
            )

            else -> when (inspect.tab) {
                TimeClockInspectTab.Overview -> inspect.overview?.let { InspectOverviewBody(it) } ?: EmptyInspectHint()
                TimeClockInspectTab.Users -> inspect.users?.let { users ->
                    InspectTable(
                        caption = "${users.shown} de ${users.total} personas",
                        empty = "El reloj no tiene personas dadas de alta.",
                        headers = listOf("Número", "Nombre"),
                        rows = users.people.map { person -> listOf(person.employeeNumber, person.name) },
                        onRow = users.people.map { person -> { onPerson(person.employeeNumber) } },
                    )
                } ?: EmptyInspectHint()

                TimeClockInspectTab.Cards -> inspect.cards?.let { cards ->
                    InspectTable(
                        caption = "${cards.shown} de ${cards.total} tarjetas",
                        empty = "El reloj no tiene tarjetas.",
                        headers = listOf("Tarjeta", "Número de empleado", "Tipo"),
                        rows = cards.cards.map { card -> listOf(card.cardNo, card.employeeNumber, card.cardType ?: "—") },
                    )
                } ?: EmptyInspectHint()

                TimeClockInspectTab.Events -> inspect.events?.let { events ->
                    InspectTable(
                        caption = "${events.shown} de ${events.total} eventos",
                        empty = "No hay eventos en este período.",
                        headers = listOf("Fecha y hora", "Tipo", "Número", "Nombre"),
                        rows = events.events.map { event ->
                            listOf(
                                formatDateTime(event.time),
                                eventMinorLabel(event.minor),
                                event.employeeNumber ?: "—",
                                event.name ?: "—",
                            )
                        },
                    )
                } ?: EmptyInspectHint()

                TimeClockInspectTab.Camera -> inspect.camera?.let { camera -> InspectCameraBody(camera) } ?: EmptyInspectHint()
            }
        }
    }
}

private fun TimeClocksInspectUi.tabHasNoData(): Boolean = when (tab) {
    TimeClockInspectTab.Overview -> overview == null
    TimeClockInspectTab.Users -> users == null
    TimeClockInspectTab.Cards -> cards == null
    TimeClockInspectTab.Events -> events == null
    TimeClockInspectTab.Camera -> camera == null
}

@Composable
private fun EmptyInspectHint() {
    Text(
        "Pulsa «Actualizar» para leer esta sección del reloj.",
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.TextMuted,
    )
}

@Composable
private fun InspectOverviewBody(overview: TimeClockInspectOverviewDto) {
    InfoRow("Nombre", overview.device.deviceName ?: "—")
    InfoRow("Modelo", overview.device.model ?: "—")
    InfoRow("Serie", overview.device.serialNumber)
    InfoRow("Firmware", overview.device.firmwareVersion ?: "—")
    InfoRow("MAC", overview.device.macAddress ?: "—")
    InfoRow("Dirección", overview.clock.url)
    Spacer(Modifier.height(6.dp))
    InfoRow("Hora del reloj", clockTime(overview.time.localTime))
    InfoRow("Modo", overview.time.timeMode ?: "—")
    InfoRow("Zona", overview.time.timeZone ?: "—")
    Spacer(Modifier.height(6.dp))
    InfoRow("Personas", overview.counts.userNumber.toString())
    InfoRow("Con rostro", overview.counts.bindFaceUserNumber.toString())
    InfoRow("Con huella", overview.counts.bindFingerprintUserNumber.toString())
    InfoRow("Con tarjeta", overview.counts.bindCardUserNumber.toString())
}

@Composable
private fun InspectCameraBody(camera: TimeClockCameraDto) {
    val playback = camera.playback
    when {
        playback == null -> Text(
            "El reloj no reporta información de su cámara.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )

        playback.mode == "http" -> {
            playback.url?.let { url -> InfoRow("Stream", url) }
            Text(
                "El reloj expone su video por HTTP; ábrelo fuera de la app.",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
            )
        }

        else -> {
            Text(
                "Este reloj transmite por RTSP y la app no reproduce ese protocolo. " +
                    "Para verlo hace falta un servidor de medios (por ejemplo MediaMTX) que lo convierta.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
            playback.rtspPath?.let { path ->
                Spacer(Modifier.height(4.dp))
                Text(path, style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
            }
            playback.reason?.let { reason ->
                Spacer(Modifier.height(4.dp))
                Text(reason, style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
            }
        }
    }
    if (camera.inputs.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        camera.inputs.forEach { input ->
            InfoRow(input.name ?: "Canal ${input.id}", if (input.enabled) "Activo" else "Inactivo")
        }
    }
    if (camera.streams.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text("Canales de video", style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
        camera.streams.forEach { stream ->
            val resolution = if (stream.width != null && stream.height != null) "${stream.width}×${stream.height}" else "—"
            InfoRow(
                stream.name ?: "Canal ${stream.id}",
                listOfNotNull(stream.transport, resolution, if (stream.enabled) "Activo" else "Inactivo").joinToString(" · "),
            )
        }
    } else {
        Spacer(Modifier.height(6.dp))
        Text(
            "El reloj no reporta canales de video.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
    }
}

/** Tabla simple para las secciones de inspección (sin paginar; el API topa `shown`). */
@Composable
private fun InspectTable(
    caption: String,
    empty: String,
    headers: List<String>,
    rows: List<List<String>>,
    onRow: List<(() -> Unit)?> = emptyList(),
) {
    Text(caption, style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
    Spacer(Modifier.height(6.dp))
    if (rows.isEmpty()) {
        Text(empty, style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            headers.forEach { header ->
                Text(
                    header.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        rows.forEachIndexed { index, row ->
            val action = onRow.getOrNull(index)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .then(
                        if (action != null) {
                            Modifier.clickable(role = Role.Button, onClick = action)
                        } else {
                            Modifier
                        }
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.forEach { cell ->
                    Text(
                        cell,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (action != null) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.Face,
                        contentDescription = "Ver foto",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Modales
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RegisterClockDialog(state: TimeClocksUiState, viewModel: TimeClocksViewModel) {
    AppModal(
        title = "Dar de alta un reloj",
        icon = Icons.Filled.Add,
        onDismiss = viewModel::closeRegister,
        confirmLabel = if (state.registering) "Conectando…" else "Dar de alta",
        onConfirm = viewModel::register,
        confirmEnabled = state.registerUrl.isNotBlank(),
        saving = state.registering,
    ) {
        AppTextField(
            value = state.registerUrl,
            onValueChange = viewModel::setRegisterUrl,
            label = { Text("Dirección del reloj") },
            placeholder = { Text("https://192.168.1.132") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "La IP o la dirección que abres en el navegador para entrar al reloj.",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextFaint,
        )
        Spacer(Modifier.height(12.dp))
        AppTextField(
            value = state.registerName,
            onValueChange = viewModel::setRegisterName,
            label = { Text("Nombre (opcional)") },
            placeholder = { Text("p. ej. Entrada de personal") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Si lo dejas vacío se usa el nombre configurado en el reloj.",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextFaint,
        )
        Spacer(Modifier.height(12.dp))
        AppFilterChip(
            label = "Cuenta para entradas/salidas",
            selected = state.registerCountsAttendance,
            onClick = { viewModel.setRegisterCountsAttendance(!state.registerCountsAttendance) },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Si cuenta, sus checadas arman las jornadas junto con las de los demás relojes que cuentan.",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextFaint,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "El sistema se conecta para confirmar la dirección y empieza a traer sus checadas. No modifica el reloj.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        state.registerError?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(error, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
        }
    }
}

@Composable
private fun EditClockDialog(state: TimeClocksUiState, viewModel: TimeClocksViewModel) {
    AppModal(
        title = "Editar ${state.editName.ifBlank { state.editSerial.orEmpty() }}",
        icon = Icons.Filled.Edit,
        onDismiss = viewModel::closeEdit,
        confirmLabel = "Guardar",
        onConfirm = viewModel::saveEdit,
        confirmEnabled = state.editName.isNotBlank(),
        saving = state.savingEdit,
    ) {
        AppTextField(
            value = state.editName,
            onValueChange = viewModel::setEditName,
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        AppFilterChip(
            label = "Cuenta para entradas/salidas",
            selected = state.editCountsAttendance,
            onClick = { viewModel.setEditCountsAttendance(!state.editCountsAttendance) },
        )
        Spacer(Modifier.height(12.dp))
        AppFilterChip(
            label = "Permitir escribir en este reloj",
            selected = state.editWriteEnabled,
            onClick = { viewModel.setEditWriteEnabled(!state.editWriteEnabled) },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Si está activo, el sistema puede dar de alta, dar de baja y registrar huellas en este reloj. " +
                "Apagado, el reloj es de solo lectura para el sistema.",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextFaint,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Solo cambia cómo el sistema usa las checadas de este reloj. No modifica el reloj.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        state.editError?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(error, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
        }
    }
}

@Composable
private fun RetireClockDialog(
    target: TimeClockDeviceDto,
    state: TimeClocksUiState,
    viewModel: TimeClocksViewModel,
) {
    AppModal(
        title = "Dar de baja ${target.name.ifBlank { target.clockSerial }}",
        icon = Icons.Filled.Delete,
        tone = AppModalTone.Danger,
        onDismiss = viewModel::cancelRetire,
        confirmLabel = "Dar de baja",
        onConfirm = viewModel::confirmRetire,
        saving = state.retiring,
    ) {
        Text(
            "Se deja de sincronizar. Sus checadas se quedan y, si lo vuelves a dar de alta, sigue desde donde iba. " +
                "No modifica el reloj.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextMuted,
        )
    }
}

@Composable
private fun ImportDialog(state: TimeClocksUiState, viewModel: TimeClocksViewModel) {
    val valid = isImportDay(state.importFrom.trim()) && isImportDay(state.importTo.trim())
    AppModal(
        title = "Importar checadas por fechas",
        icon = Icons.Filled.CalendarMonth,
        onDismiss = viewModel::closeImport,
        confirmLabel = if (state.importing) "Importando…" else "Importar",
        onConfirm = viewModel::startImport,
        confirmEnabled = valid,
        saving = state.importing,
    ) {
        Text(
            "Trae de TODOS los relojes las checadas del rango de fechas (días locales, inclusive). " +
                "Puede tardar varios minutos y corre en segundo plano; puedes seguir usando la app.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextField(
                value = state.importFrom,
                onValueChange = viewModel::setImportFrom,
                label = { Text("Desde") },
                placeholder = { Text("AAAA-MM-DD") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            AppTextField(
                value = state.importTo,
                onValueChange = viewModel::setImportTo,
                label = { Text("Hasta") },
                placeholder = { Text("AAAA-MM-DD") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Formato AAAA-MM-DD; se incluyen ambos días.",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextFaint,
        )
        state.importError?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(error, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
        }
    }
}

@Composable
private fun PhotoDialog(photo: TimeClocksPhotoUi, onDismiss: () -> Unit) {
    AppModal(
        title = "Foto de ${photo.employeeNumber}",
        icon = Icons.Filled.Face,
        onDismiss = onDismiss,
    ) {
        when {
            photo.loading -> BentoState(message = "Cargando la foto del reloj…", loading = true)
            photo.error != null -> BentoState(message = photo.error, tone = BentoTone.Rose)
            photo.bytes == null -> BentoState(
                message = "El reloj no tiene foto para esta persona.",
                tone = BentoTone.Sun,
                icon = Icons.Filled.Warning,
            )

            else -> {
                val bitmap: ImageBitmap? = remember(photo.bytes) {
                    runCatching { photo.bytes?.decodeToImageBitmap() }.getOrNull()
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Foto de ${photo.employeeNumber}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                    )
                } else {
                    BentoState(message = "No se pudo mostrar la foto.", tone = BentoTone.Rose)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Utilidades de presentación
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            value.ifBlank { "—" },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Tono de la insignia de cada estado (misma semántica que la web). */
private fun clockStatusTone(status: TimeClockCardStatus): BentoTone = when (status) {
    TimeClockCardStatus.Ok -> BentoTone.Mint
    TimeClockCardStatus.Running -> BentoTone.Sea
    TimeClockCardStatus.Paused -> BentoTone.Rose
    TimeClockCardStatus.Error -> BentoTone.Rose
    TimeClockCardStatus.Pending -> BentoTone.Plain
}

/** `2026-09-24T13:51:43-07:00` → `24/09/2026 13:51:43 (UTC-07:00)`, como lo marca el reloj. */
private fun clockTime(iso: String): String {
    if (iso.length < 19) return formatDateTime(iso)
    val date = "${iso.substring(8, 10)}/${iso.substring(5, 7)}/${iso.substring(0, 4)}"
    val time = iso.substring(11, 19)
    val offset = if (iso.length > 19) iso.substring(19) else ""
    return if (offset.isBlank()) "$date $time" else "$date $time (UTC$offset)"
}

/** Segundos → "45 s", "3 min 20 s", "2 h 5 min". */
private fun driftText(seconds: Int): String {
    val value = kotlin.math.abs(seconds)
    if (value < 60) return "$value s"
    val minutes = value / 60
    if (minutes < 60) return if (value % 60 == 0) "$minutes min" else "$minutes min ${value % 60} s"
    return "${minutes / 60} h ${minutes % 60} min"
}

/** Texto del código de evento del reloj (mismo mapeo que la web). */
private fun eventMinorLabel(minor: Int): String = when (minor) {
    1 -> "Tarjeta"
    38 -> "Huella"
    75 -> "Rostro"
    104 -> "Intento fallido"
    else -> "Tipo $minor"
}
