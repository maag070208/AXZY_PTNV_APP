package com.axzydev.puertonuevoapp.feature.timeclock

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.timeclock.RemovalCategoryDto
import com.axzydev.puertonuevoapp.core.network.timeclock.RosterPendingRemovalDto
import com.axzydev.puertonuevoapp.core.network.timeclock.RosterPersonDto
import com.axzydev.puertonuevoapp.core.network.timeclock.RosterReappearedDto
import com.axzydev.puertonuevoapp.core.network.timeclock.RosterRunDto
import com.axzydev.puertonuevoapp.core.network.timeclock.RosterRunResultDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockApi
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatDateTime
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import com.axzydev.puertonuevoapp.core.util.initials
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant

/**
 * Rol de turnos (`/hr/time-clock/roster` de la web): padrón del reloj con los
 * nuevos, las bajas pendientes (que esperan un motivo capturado por RH) y las
 * cuentas dadas de baja que reaparecieron en el reloj.
 *
 * - "Revisar padrón" corre `runRoster()` con `time_clock.sync`: vuelve a leer
 *   las personas de cada reloj que cuenta para asistencia (solo lectura de los
 *   equipos) y recalcula. Si un reloj bajó mucho su conteo, la corrida se marca
 *   sospechosa y no genera bajas.
 * - Las bajas NO se aplican solas: hay que elegir motivo del catálogo y
 *   escribirlo (`applyRemoval`, permisos `users.edit`/`hr.records`) o descartar
 *   la baja (`dismissRemoval`, `time_clock.link`).
 * - Los reaparecidos no se reactivan solos: la fila lleva a la cuenta para que
 *   RH decida.
 */
@Composable
fun TimeClockRosterScreen(
    viewModel: TimeClockRosterViewModel = viewModel {
        TimeClockRosterViewModel(TimeClockApi(AppContainer.apiClient))
    },
) {
    val state by viewModel.state.collectAsState()
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val navigator = LocalNavigator.current
    var confirmRemovalNumber by remember { mutableStateOf<String?>(null) }

    if (user == null || !user.can(PermissionKeys.TIME_CLOCK_VIEW)) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            BentoState(
                message = "No autorizado — se requiere el permiso Checador",
                tone = BentoTone.Rose,
                icon = Icons.Filled.Lock,
            )
        }
        return
    }

    val canRun = user.can(PermissionKeys.TIME_CLOCK_SYNC)
    val canLink = user.can(PermissionKeys.TIME_CLOCK_LINK)
    val canRemove = user.can(PermissionKeys.USERS_EDIT) || user.can(PermissionKeys.HR_RECORDS)
    val roster = state.roster

    Column(modifier = Modifier.fillMaxSize()) {
        RosterHeader(
            running = state.running,
            refreshing = state.refreshing,
            canRun = canRun,
            onRun = viewModel::runReview,
        )

        when {
            state.loading && roster == null -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                BentoState(
                    message = "Cargando padrón del reloj…",
                    loading = true,
                    modifier = Modifier.padding(24.dp),
                )
            }

            state.error != null && roster == null -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                BentoErrorState(
                    message = state.error ?: "No se pudo cargar el padrón",
                    onRetry = viewModel::retry,
                    modifier = Modifier.padding(24.dp),
                )
            }

            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.error?.let { message ->
                    item(key = "error") {
                        BentoErrorState(
                            message = message,
                            onRetry = viewModel::refresh,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                item(key = "last-run") {
                    LastRunCard(lastRun = roster?.lastRun, manualRun = state.lastManualRun)
                }

                val unlinked = roster?.unlinked.orEmpty()
                item(key = "unlinked-header") {
                    SectionHeader("Nuevos en el reloj", unlinked.size)
                }
                if (unlinked.isEmpty()) {
                    item(key = "unlinked-empty") {
                        EmptySection("Nadie nuevo: todas las personas del reloj tienen cuenta vinculada.")
                    }
                } else {
                    items(unlinked, key = { "unlinked-${it.employeeNumber}" }) { person ->
                        UnlinkedPersonCard(
                            person = person,
                            canLink = canLink,
                            onLink = { navigator.push(Screen.TimeClockEmployees) },
                        )
                    }
                }

                val pending = roster?.pendingRemovals.orEmpty()
                item(key = "pending-header") {
                    SectionHeader("Bajas pendientes", pending.size)
                }
                if (pending.isEmpty()) {
                    item(key = "pending-empty") {
                        EmptySection("Sin bajas pendientes: nadie ha dejado de checar el tiempo de gracia.")
                    }
                } else {
                    items(pending, key = { "pending-${it.employeeNumber}" }) { person ->
                        PendingRemovalCard(
                            person = person,
                            categories = state.categories,
                            draft = state.draftOf(person.employeeNumber),
                            canRemove = canRemove,
                            canLink = canLink,
                            busy = state.busyNumber == person.employeeNumber,
                            onCategory = { viewModel.setRemovalCategory(person.employeeNumber, it) },
                            onReason = { viewModel.setRemovalReason(person.employeeNumber, it) },
                            onApply = { confirmRemovalNumber = person.employeeNumber },
                            onDismiss = { viewModel.dismissRemoval(person.employeeNumber) },
                        )
                    }
                }

                val reappeared = roster?.reappeared.orEmpty()
                item(key = "reappeared-header") {
                    SectionHeader("Reaparecidos", reappeared.size)
                }
                if (reappeared.isEmpty()) {
                    item(key = "reappeared-empty") {
                        EmptySection("Nadie dado de baja ha vuelto a checar.")
                    }
                } else {
                    items(reappeared, key = { "reappeared-${it.employeeNumber}" }) { person ->
                        ReappearedCard(
                            person = person,
                            onOpenUser = { navigator.push(Screen.UserHistory(person.userId)) },
                        )
                    }
                }
            }
        }
    }

    confirmRemovalNumber?.let { number ->
        val pending = roster?.pendingRemovals?.firstOrNull { it.employeeNumber == number }
        val draft = state.draftOf(number)
        AppModal(
            title = "Aplicar la baja",
            icon = Icons.Filled.PersonOff,
            tone = AppModalTone.Danger,
            onDismiss = { if (state.busyNumber == null) confirmRemovalNumber = null },
            confirmLabel = "Aplicar baja",
            onConfirm = {
                viewModel.applyRemoval(number)
                confirmRemovalNumber = null
            },
            confirmEnabled = draft.isValid,
            saving = state.busyNumber == number,
        ) {
            Text(
                "Se dará de baja la cuenta de ${pending?.userName ?: "la persona vinculada"} " +
                    "del número $number, con el flujo de bajas de siempre.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Motivo: ${categoryName(state.categories, draft.categoryId)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
            )
            Text(
                draft.reason.trim(),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "La baja no se puede deshacer desde aquí; el vínculo del reloj se conserva como histórico.",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
            )
        }
    }
}

// ── Encabezado ───────────────────────────────────────────────────────────────

@Composable
private fun RosterHeader(
    running: Boolean,
    refreshing: Boolean,
    canRun: Boolean,
    onRun: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 14.dp, bottom = 10.dp),
    ) {
        Text(
            "El padrón se arma con los relojes que cuentan para asistencia. Nada se da de baja sin un motivo capturado.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        if (canRun) {
            Spacer(Modifier.height(10.dp))
            PillButton(
                text = if (running) "Revisando padrón…" else "Revisar padrón",
                onClick = onRun,
                loading = running,
                leadingIcon = Icons.Filled.Refresh,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (refreshing) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Actualizando…",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
            )
        }
    }
}

// ── Última corrida ───────────────────────────────────────────────────────────

@Composable
private fun LastRunCard(lastRun: RosterRunDto?, manualRun: RosterRunResultDto?) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = AppColors.Info,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Última corrida del padrón",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                )
                Text(
                    lastRun?.let { formatDateTime(it.finishedAt ?: it.startedAt) }
                        ?: "Sin corridas registradas",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                )
                if (lastRun != null) {
                    Text(
                        listOf(
                            "${lastRun.added} nuevos",
                            "${lastRun.removalPending} bajas pendientes",
                            "${lastRun.cancelled} canceladas",
                            "${lastRun.reappeared} reapariciones",
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextMuted,
                    )
                }
            }
            if (lastRun != null) {
                StatusChip(
                    label = when {
                        lastRun.suspicious -> "Sospechosa"
                        lastRun.ok -> "Correcta"
                        else -> "Sin lectura"
                    },
                    tone = when {
                        lastRun.suspicious -> BentoTone.Sun
                        lastRun.ok -> BentoTone.Mint
                        else -> BentoTone.Rose
                    },
                )
            }
        }

        if (lastRun != null && lastRun.perClock.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = AppColors.Outline.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            lastRun.perClock.forEach { clock ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        clock.serial,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (clock.ok) "${clock.count} personas" else (clock.error ?: "Sin lectura"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (clock.ok) AppColors.TextMuted else AppColors.Danger,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (manualRun != null) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = AppColors.Outline.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            Text(
                "Revisión manual: ${manualRun.added} nuevos · ${manualRun.removalPending} bajas pendientes · " +
                    "${manualRun.reappeared} reapariciones · ${manualRun.newCount} números leídos",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextMuted,
            )
        }
    }
}

// ── Secciones ────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        StatusChip(
            label = count.toString(),
            tone = if (count > 0) BentoTone.Sea else BentoTone.Plain,
        )
    }
}

@Composable
private fun EmptySection(message: String) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextFaint,
        )
    }
}

// ── Nuevos en el reloj ───────────────────────────────────────────────────────

@Composable
private fun UnlinkedPersonCard(
    person: RosterPersonDto,
    canLink: Boolean,
    onLink: () -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShape.card,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AppColors.EmeraldPrimary.copy(alpha = 0.12f), AppShape.pill),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initials(person.name),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.EmeraldPrimary,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    person.name.ifBlank { "Sin nombre en el reloj" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        append("#${person.employeeNumber}")
                        if (person.clocks.isNotEmpty()) append(" · ${person.clocks.joinToString(", ")}")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (canLink) {
                PillButton(
                    text = "Vincular",
                    onClick = onLink,
                    variant = PillButtonVariant.Secondary,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Aparece en el reloj y no tiene cuenta vinculada: sus checadas no forman jornadas hasta vincularlo.",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextMuted,
        )
    }
}

// ── Bajas pendientes ─────────────────────────────────────────────────────────

@Composable
private fun PendingRemovalCard(
    person: RosterPendingRemovalDto,
    categories: List<RemovalCategoryDto>,
    draft: RosterRemovalDraft,
    canRemove: Boolean,
    canLink: Boolean,
    busy: Boolean,
    onCategory: (String) -> Unit,
    onReason: (String) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShape.card,
        borderColor = AppColors.Warning.copy(alpha = 0.55f),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.PersonOff,
                contentDescription = null,
                tint = AppColors.Warning,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    person.userName.ifBlank { "Cuenta sin nombre" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "#${person.employeeNumber} · dejó de aparecer el ${formatShortDate(person.missingSince)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusChip(label = "Baja pendiente", tone = BentoTone.Sun)
        }

        if (canRemove) {
            Spacer(Modifier.height(12.dp))
            if (categories.isEmpty()) {
                Text(
                    "No hay motivos de baja activos en el catálogo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Danger,
                )
            } else {
                SimpleDropdownField(
                    label = "Motivo de la baja",
                    value = draft.categoryId,
                    options = categories.map { it.id to it.name },
                    onSelect = onCategory,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = draft.reason,
                    onValueChange = onReason,
                    label = { Text("Detalle del motivo") },
                    placeholder = { Text("Ej. renuncia presentada el 30 de octubre") },
                    minLines = 2,
                    maxLines = 3,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Mínimo $MinRemovalReasonLength caracteres. La baja aplica el flujo de bajas de siempre.",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                )
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Text(
                "Solo quien puede dar de baja cuentas (RH) captura el motivo y aplica la baja.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
        }

        if (canRemove || canLink) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (canLink) {
                    PillButton(
                        text = "Descartar",
                        onClick = onDismiss,
                        variant = PillButtonVariant.Ghost,
                        enabled = !busy,
                    )
                }
                if (canRemove) {
                    PillButton(
                        text = if (busy) "Aplicando…" else "Aplicar baja",
                        onClick = onApply,
                        variant = PillButtonVariant.Secondary,
                        enabled = draft.isValid && !busy,
                        loading = busy,
                        leadingIcon = Icons.Filled.PersonOff,
                    )
                }
            }
        }
    }
}

// ── Reaparecidos ─────────────────────────────────────────────────────────────

@Composable
private fun ReappearedCard(
    person: RosterReappearedDto,
    onOpenUser: () -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShape.card,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.PersonAdd,
                contentDescription = null,
                tint = AppColors.Info,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    person.userName.ifBlank { "Cuenta sin nombre" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "#${person.employeeNumber} · volvió a checar el ${formatShortDate(person.reappearedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PillButton(
                text = "Ver cuenta",
                onClick = onOpenUser,
                variant = PillButtonVariant.Ghost,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "La cuenta está dada de baja pero sigue checando. No se reactiva sola: RH decide si se reactiva o se corrige el reloj.",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextMuted,
        )
    }
}

private fun categoryName(categories: List<RemovalCategoryDto>, categoryId: String): String =
    categories.firstOrNull { it.id == categoryId }?.name ?: "Sin categoría"
