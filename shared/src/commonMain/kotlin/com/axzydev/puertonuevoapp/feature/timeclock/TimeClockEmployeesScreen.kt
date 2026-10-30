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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.network.timeclock.EmployeeClockStatusDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockApi
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockEmployeeDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockEmployeesSummaryDto
import com.axzydev.puertonuevoapp.core.network.users.UserDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.BentoMiniStat
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.ui.table.AppTableScreen
import com.axzydev.puertonuevoapp.core.ui.table.FilterControl
import com.axzydev.puertonuevoapp.core.ui.table.FilterOption
import com.axzydev.puertonuevoapp.core.ui.table.TableFilterSpec
import com.axzydev.puertonuevoapp.core.ui.table.TableSortSpec
import com.axzydev.puertonuevoapp.core.util.formatDateTime
import com.axzydev.puertonuevoapp.core.util.initials

/**
 * Padrón del reloj (`/hr/time-clock/employees` de la web): números vistos en
 * los relojes con su vínculo a una cuenta, estado de vínculo, sugerencias de
 * vinculación y el estado de alta en cada reloj.
 *
 * - Tabla server-side (`AppTableScreen` + `POST /time-clock/employees/query`):
 *   buscador por número o nombre, filtros de número/nombre/estado y orden.
 * - Mosaicos de resumen (total, vinculados, sin vincular, sugerencias) que
 *   filtran la tabla, como las tarjetas de la web.
 * - Acciones con `time_clock.link` (el API lo revalida): vincular/cambiar con
 *   selector de cuenta, aceptar sugerencia, desvincular y "vincular
 *   sugerencias seguras" (solo `HIGH`).
 * - Estado en los relojes (`employeeClockStatus`) con `time_clocks.manage` o
 *   `time_clock.write`.
 */
@Composable
fun TimeClockEmployeesScreen(
    viewModel: TimeClockEmployeesViewModel = viewModel {
        val user = (AppContainer.authRepository.state.value as? AuthState.LoggedIn)?.user
        TimeClockEmployeesViewModel(
            timeClockApi = TimeClockApi(AppContainer.apiClient),
            usersApi = AppContainer.usersApi,
            canLink = user?.can(PermissionKeys.TIME_CLOCK_LINK) == true,
        )
    },
) {
    val table by viewModel.uiState.collectAsState()
    val screen by viewModel.screen.collectAsState()
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user

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

    val canLink = user.can(PermissionKeys.TIME_CLOCK_LINK)
    val canSeeClockStatus = user.can(PermissionKeys.TIME_CLOCK_WRITE) ||
        user.can(PermissionKeys.TIME_CLOCKS_MANAGE)
    val status = table.filters[TimeClockEmployeeStatusKey].orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        EmployeesHeader(
            summary = screen.summary,
            status = status,
            canLink = canLink,
            linkingSuggested = screen.linkingSuggested,
            onSelectStatus = viewModel::selectStatus,
            onLinkSuggested = viewModel::linkSuggested,
        )

        AppTableScreen(
            state = table,
            heading = "Empleados del reloj",
            filterSpecs = employeeFilterSpecs,
            sortSpecs = employeeSortSpecs,
            onSortChange = viewModel::onSortChange,
            onSearchChange = viewModel::onSearchChange,
            onFiltersChange = viewModel::onFiltersChange,
            onRefresh = viewModel::refresh,
            onLoadMore = viewModel::loadMore,
            onRetry = viewModel::retry,
            searchPlaceholder = "Buscar por número o nombre",
            emptyMessage = "No hay empleados del reloj con estos filtros",
            searchKey = "q",
            itemKey = { it.employeeNumber },
            modifier = Modifier.weight(1f),
        ) { row ->
            EmployeeLinkCard(
                row = row,
                canLink = canLink,
                canSeeClockStatus = canSeeClockStatus,
                saving = screen.savingNumber == row.employeeNumber,
                onLink = { viewModel.openLink(row) },
                onAcceptSuggestion = { viewModel.acceptSuggestion(row) },
                onUnlink = { viewModel.unlink(row) },
                onClockStatus = { viewModel.openClockStatus(row) },
            )
        }
    }

    screen.linkingTarget?.let { target ->
        LinkEmployeeDialog(
            target = target,
            users = screen.users,
            loadingUsers = screen.loadingUsers,
            selectedUserId = screen.selectedUserId,
            saving = screen.savingLink,
            onSelectUser = viewModel::setSelectedUserId,
            onDismiss = viewModel::dismissLink,
            onConfirm = viewModel::confirmLink,
        )
    }

    screen.clockStatusTarget?.let { target ->
        ClockStatusDialog(
            target = target,
            loading = screen.loadingClockStatus,
            status = screen.clockStatus,
            onDismiss = viewModel::dismissClockStatus,
        )
    }
}

// ── Encabezado: mosaicos y vínculo masivo ────────────────────────────────────

@Composable
private fun EmployeesHeader(
    summary: TimeClockEmployeesSummaryDto,
    status: String,
    canLink: Boolean,
    linkingSuggested: Boolean,
    onSelectStatus: (String) -> Unit,
    onLinkSuggested: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BentoMiniStat(
                label = "Empleados",
                value = summary.total.toString(),
                tone = BentoTone.Sea,
                highlight = status.isEmpty(),
                onClick = { onSelectStatus("") },
                modifier = Modifier.weight(1f),
            )
            BentoMiniStat(
                label = "Vinculados",
                value = summary.linkedCount.toString(),
                tone = BentoTone.Mint,
                highlight = status == "LINKED",
                onClick = { onSelectStatus("LINKED") },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BentoMiniStat(
                label = "Sin vincular",
                value = summary.withoutLink.toString(),
                tone = if (summary.withoutLink > 0) BentoTone.Sun else BentoTone.Plain,
                highlight = status == "UNLINKED",
                onClick = { onSelectStatus("UNLINKED") },
                modifier = Modifier.weight(1f),
            )
            BentoMiniStat(
                label = "Sugerencias",
                value = summary.registrationSuggestions.toString(),
                tone = if (summary.registrationSuggestions > 0) BentoTone.Lavender else BentoTone.Plain,
                highlight = status == "SUGGESTED",
                onClick = { onSelectStatus("SUGGESTED") },
                modifier = Modifier.weight(1f),
            )
        }
        if (canLink && summary.registrationSuggestions > 0) {
            Spacer(Modifier.height(10.dp))
            PillButton(
                text = "Vincular ${summary.registrationSuggestions} sugerencias",
                onClick = onLinkSuggested,
                loading = linkingSuggested,
                leadingIcon = Icons.Filled.CheckCircle,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(6.dp))
    }
}

// ── Tarjeta de un número del reloj ───────────────────────────────────────────

@Composable
private fun EmployeeLinkCard(
    row: TimeClockEmployeeDto,
    canLink: Boolean,
    canSeeClockStatus: Boolean,
    saving: Boolean,
    onLink: () -> Unit,
    onAcceptSuggestion: () -> Unit,
    onUnlink: () -> Unit,
    onClockStatus: () -> Unit,
) {
    val status = row.linkStatus()
    val tone = when (status) {
        "LINKED" -> BentoTone.Mint
        "SUGGESTED" -> BentoTone.Sun
        else -> BentoTone.Rose
    }
    val colors = AppBento.tone(tone)

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShape.card,
        elevation = 2.dp,
        borderColor = AppColors.Outline.copy(alpha = 0.5f),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).background(colors.container, AppShape.pill),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initials(row.name),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.content,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    row.name.ifBlank { "Sin nombre en el reloj" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "#${row.employeeNumber} · ${row.punches} checada(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    row.lastPunch.takeIf { it.isNotBlank() }
                        ?.let { "Última checada: ${formatDateTime(it)}" }
                        ?: "Sin checadas registradas",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusChip(label = statusLabel(status), tone = tone)
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = colors.content.copy(alpha = 0.10f))
        Spacer(Modifier.height(8.dp))

        when {
            row.link != null -> {
                val link = row.link
                Text(
                    "Cuenta: ${link.name}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        link.employeeNumber?.takeIf { it.isNotBlank() }?.let { "#$it" },
                        if (!link.active) "Cuenta dada de baja" else null,
                    ).joinToString(" · ").ifBlank { "Sin número de empleado" },
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            row.suggestion != null -> {
                val suggestion = row.suggestion
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(
                        label = if (suggestion.confidence == "HIGH") "Sugerencia alta" else "Sugerencia media",
                        tone = if (suggestion.confidence == "HIGH") BentoTone.Mint else BentoTone.Sun,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        suggestion.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    listOfNotNull(
                        suggestion.employeeNumber?.takeIf { it.isNotBlank() }?.let { "#$it" },
                        if (!suggestion.active) "Cuenta dada de baja" else null,
                        if (suggestion.confidence == "HIGH") "Coincide nombre y número" else "Solo coincide el nombre",
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                )
            }

            else -> Text(
                "Sin cuenta candidata. Al vincularlo, sus checadas forman jornadas.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
            )
        }

        val showActions = canLink || (canSeeClockStatus && row.link != null)
        if (showActions) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (canLink) {
                    PillButton(
                        text = if (row.link != null) "Cambiar cuenta" else "Vincular",
                        onClick = onLink,
                        variant = if (row.link != null) PillButtonVariant.Secondary else PillButtonVariant.Primary,
                        enabled = !saving,
                        leadingIcon = Icons.Filled.Link,
                    )
                    if (row.suggestion != null) {
                        PillButton(
                            text = "Aceptar sugerencia",
                            onClick = onAcceptSuggestion,
                            variant = PillButtonVariant.Secondary,
                            enabled = !saving,
                            leadingIcon = Icons.Filled.CheckCircle,
                        )
                    }
                    if (row.link != null) {
                        PillButton(
                            text = "Desvincular",
                            onClick = onUnlink,
                            variant = PillButtonVariant.Ghost,
                            enabled = !saving,
                        )
                    }
                }
                if (canSeeClockStatus && row.link != null) {
                    PillButton(
                        text = "Estado en relojes",
                        onClick = onClockStatus,
                        variant = PillButtonVariant.Ghost,
                        enabled = !saving,
                        leadingIcon = Icons.Filled.Devices,
                    )
                }
            }
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    "LINKED" -> "Vinculado"
    "SUGGESTED" -> "Sugerencia"
    else -> "Sin vincular"
}

// ── Diálogos ─────────────────────────────────────────────────────────────────

@Composable
private fun LinkEmployeeDialog(
    target: TimeClockEmployeeDto,
    users: List<UserDto>,
    loadingUsers: Boolean,
    selectedUserId: String,
    saving: Boolean,
    onSelectUser: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AppModal(
        title = "Vincular número del reloj",
        icon = Icons.Filled.Link,
        onDismiss = onDismiss,
        confirmLabel = if (target.link != null) "Cambiar vínculo" else "Vincular",
        onConfirm = onConfirm,
        confirmEnabled = selectedUserId.isNotBlank() && !loadingUsers,
        saving = saving,
    ) {
        Text(
            "#${target.employeeNumber} · ${target.name.ifBlank { "Sin nombre en el reloj" }}",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextMuted,
        )
        Spacer(Modifier.height(14.dp))
        if (loadingUsers) {
            BentoState(message = "Cargando cuentas…", loading = true)
        } else {
            SimpleDropdownField(
                label = "Cuenta",
                value = selectedUserId,
                options = users.map { user ->
                    user.id to buildString {
                        append(user.name)
                        user.employeeNumber?.takeIf { it.isNotBlank() }?.let { append(" · #$it") }
                        if (!user.active) append(" (baja)")
                    }
                },
                onSelect = onSelectUser,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "El vínculo es explícito: el número del reloj puede no coincidir con el número de nómina (el reloj antepone el área).",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
            )
        }
    }
}

@Composable
private fun ClockStatusDialog(
    target: TimeClockEmployeeDto,
    loading: Boolean,
    status: EmployeeClockStatusDto?,
    onDismiss: () -> Unit,
) {
    AppModal(
        title = "Estado en los relojes",
        icon = Icons.Filled.Devices,
        onDismiss = onDismiss,
    ) {
        Text(
            "${target.link?.name ?: target.name} · #${status?.employeeNumber ?: target.employeeNumber}",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextMuted,
        )
        Spacer(Modifier.height(14.dp))
        when {
            loading -> BentoState(message = "Consultando los relojes…", loading = true)
            status == null || status.clocks.isEmpty() -> BentoState(
                message = "No hay relojes registrados para consultar.",
                icon = Icons.Filled.Devices,
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                status.clocks.forEach { clock ->
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    clock.name.ifBlank { clock.serial },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    listOfNotNull(
                                        clock.serial.takeIf { it.isNotBlank() },
                                        if (clock.countsAttendance) "Cuenta para asistencia" else "No cuenta para asistencia",
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColors.TextFaint,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                StatusChip(
                                    label = if (clock.connection == "ONLINE") "En línea" else "Sin respuesta",
                                    tone = if (clock.connection == "ONLINE") BentoTone.Mint else BentoTone.Rose,
                                )
                                Spacer(Modifier.height(4.dp))
                                StatusChip(
                                    label = when (clock.state) {
                                        "DADA_DE_ALTA" -> "Dada de alta"
                                        "SIN_ALTA" -> "Sin alta"
                                        else -> "Sin lectura"
                                    },
                                    tone = when (clock.state) {
                                        "DADA_DE_ALTA" -> BentoTone.Mint
                                        "SIN_ALTA" -> BentoTone.Sun
                                        else -> BentoTone.Plain
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Filtros y orden de la tabla ──────────────────────────────────────────────

private val employeeStatusOptions: List<FilterOption> = listOf(
    FilterOption("LINKED", "Vinculados"),
    FilterOption("SUGGESTED", "Con sugerencia"),
    FilterOption("UNLINKED", "Sin vincular"),
)

private val employeeFilterSpecs: List<TableFilterSpec> = listOf(
    TableFilterSpec("employeeNumber", "Número en el reloj", FilterControl.Text),
    TableFilterSpec("name", "Nombre en el reloj", FilterControl.Text),
    TableFilterSpec(TimeClockEmployeeStatusKey, "Estado", FilterControl.Select(employeeStatusOptions)),
)

private val employeeSortSpecs: List<TableSortSpec> = listOf(
    TableSortSpec("employeeNumber", "Número"),
    TableSortSpec("name", "Nombre"),
    TableSortSpec("punches", "Checadas"),
    TableSortSpec("lastPunch", "Última checada"),
    TableSortSpec("link", "Cuenta vinculada"),
)
