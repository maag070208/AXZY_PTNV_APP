package com.axzydev.puertonuevoapp.feature.schedules

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.schedules.ScheduleDayDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppActionCard
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.BentoIconButton
import com.axzydev.puertonuevoapp.core.ui.BentoMiniStat
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.BentoWeekBars
import com.axzydev.puertonuevoapp.core.ui.BentoWeekDay
import com.axzydev.puertonuevoapp.core.ui.RegisterTopBarAction
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import androidx.compose.ui.text.style.TextOverflow
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.StatusChip

/**
 * Horarios y turnos (`schedules.view`): listado con búsqueda y filtro por
 * estado, resumen semanal de cada horario, personas asignadas y las acciones
 * de editar, activar/desactivar y dar de baja (`schedules.manage`).
 *
 * Desde aquí se abren las otras pantallas del módulo: asignación masiva
 * (`schedules.manage`), tiempo extra (`overtime.view`) y pre nómina
 * (`payroll.view`).
 */
@Composable
fun SchedulesScreen() {
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canView = user?.can(PermissionKeys.SCHEDULES_VIEW) == true
    val canManage = user?.can(PermissionKeys.SCHEDULES_MANAGE) == true
    val canOvertime = user?.can(PermissionKeys.OVERTIME_VIEW) == true
    val canPayroll = user?.can(PermissionKeys.PAYROLL_VIEW) == true

    if (!canView) {
        BentoState(
            message = "No autorizado — requiere el permiso Horarios",
            modifier = Modifier.fillMaxSize().padding(16.dp),
            tone = BentoTone.Rose,
        )
        return
    }

    val viewModel: SchedulesViewModel = viewModel(key = "schedules-list") {
        SchedulesViewModel(SchedulesServices.api)
    }
    val state by viewModel.uiState.collectAsState()

    // Recarga al entrar: al volver del formulario, la lista refleja el alta/edición.
    LaunchedEffect(Unit) { viewModel.load() }

    if (canManage) {
        RegisterTopBarAction(Icons.Filled.Add, "Nuevo horario") {
            navigator.push(Screen.ScheduleForm())
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppBento.canvas),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    "Horarios y turnos",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.TextPrimary,
                )
                Text(
                    "Define la semana de cada turno y asígnalo a varias personas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                )
            }
        }

        if (canManage || canOvertime || canPayroll) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (canManage) {
                        AppActionCard(
                            label = "Asignar",
                            icon = Icons.Filled.Groups,
                            onClick = { navigator.push(Screen.ScheduleAssign) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (canOvertime) {
                        AppActionCard(
                            label = "Tiempo extra",
                            icon = Icons.Filled.Timer,
                            onClick = { navigator.push(Screen.OvertimeApproval) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (canPayroll) {
                        AppActionCard(
                            label = "Pre nómina",
                            icon = Icons.Filled.Schedule,
                            onClick = { navigator.push(Screen.Payroll) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item {
            AppSearchField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Buscar horario…",
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AppFilterChip(
                    label = "Todos",
                    selected = state.filter == SchedulesFilter.ALL,
                    onClick = { viewModel.onFilterChange(SchedulesFilter.ALL) },
                )
                AppFilterChip(
                    label = "Activos",
                    selected = state.filter == SchedulesFilter.ACTIVE,
                    onClick = { viewModel.onFilterChange(SchedulesFilter.ACTIVE) },
                )
                AppFilterChip(
                    label = "Inactivos",
                    selected = state.filter == SchedulesFilter.INACTIVE,
                    onClick = { viewModel.onFilterChange(SchedulesFilter.INACTIVE) },
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                BentoMiniStat(
                    label = "Horarios",
                    value = state.schedules.size.toString(),
                    modifier = Modifier.weight(1f),
                    tone = BentoTone.Sea,
                )
                BentoMiniStat(
                    label = "Activos",
                    value = state.activeCount.toString(),
                    modifier = Modifier.weight(1f),
                    tone = BentoTone.Mint,
                )
                BentoMiniStat(
                    label = "Asignaciones",
                    value = state.assignedTotal.toString(),
                    modifier = Modifier.weight(1f),
                    tone = BentoTone.Lavender,
                )
            }
        }

        when {
            state.loading -> item {
                BentoState("Cargando horarios…", loading = true)
            }

            state.error != null -> item {
                BentoState(
                    message = state.error ?: "No se pudieron cargar los horarios",
                    tone = BentoTone.Rose,
                    actionLabel = "Reintentar",
                    onAction = viewModel::load,
                )
            }

            state.visible.isEmpty() -> item {
                BentoState(
                    message = if (state.query.isBlank()) {
                        "Aún no hay horarios. Crea el primero con el botón +."
                    } else {
                        "No hay horarios que coincidan con “${state.query}”."
                    },
                )
            }

            else -> items(state.visible, key = { it.id }) { schedule ->
                ScheduleCard(
                    name = schedule.name,
                    active = schedule.active,
                    days = schedule.days,
                    mealBreakMin = schedule.mealBreakMin,
                    assigned = schedule.assigned ?: 0,
                    busy = state.busy,
                    canManage = canManage,
                    onEdit = { navigator.push(Screen.ScheduleForm(schedule.id)) },
                    onToggleActive = { viewModel.onToggleActive(schedule) },
                    onDelete = { viewModel.requestDelete(schedule) },
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    state.pendingDeactivate?.let { schedule ->
        AppModal(
            title = "Desactivar horario",
            icon = Icons.Filled.PlayArrow,
            tone = AppModalTone.Warning,
            onDismiss = viewModel::dismissDeactivate,
            confirmLabel = "Desactivar",
            saving = state.busy,
            onConfirm = viewModel::confirmDeactivate,
        ) {
            Text(
                "¿Desactivar “${schedule.name}”? Dejará de asignarse a nuevas personas; " +
                    "las asignaciones vigentes se conservan.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
        }
    }

    state.pendingDelete?.let { schedule ->
        AppModal(
            title = "Eliminar horario",
            icon = Icons.Filled.DeleteOutline,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissDelete,
            confirmLabel = "Eliminar",
            saving = state.busy,
            onConfirm = viewModel::confirmDelete,
        ) {
            Text(
                "¿Eliminar “${schedule.name}”? Si está activo o tiene personas asignadas se " +
                    "desactivará; si ya estaba inactivo y vacío se borra definitivamente.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
        }
    }
}

@Composable
private fun ScheduleCard(
    name: String,
    active: Boolean,
    days: List<ScheduleDayDto>,
    mealBreakMin: Int,
    assigned: Int,
    busy: Boolean,
    canManage: Boolean,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = AppColors.Outline.copy(alpha = 0.6f),
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.size(8.dp))
                    StatusChip(
                        label = if (active) "Activo" else "Inactivo",
                        tone = if (active) BentoTone.Mint else BentoTone.Plain,
                    )
                }
                Text(
                    "${daySummary(days)} · ${hoursLabel(weeklyMinutes(days, mealBreakMin))} a la semana",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "$assigned persona(s) asignada(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextFaint,
                )
            }
            if (canManage) {
                BentoIconButton(
                    icon = Icons.Filled.Edit,
                    contentDescription = "Editar horario",
                    onClick = onEdit,
                )
                Spacer(Modifier.size(6.dp))
                BentoIconButton(
                    icon = Icons.Filled.DeleteOutline,
                    contentDescription = "Eliminar horario",
                    onClick = onDelete,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        SectionLabel("Semana")
        Spacer(Modifier.height(6.dp))
        BentoWeekBars(
            days = days.sortedBy { it.weekday }.map { day ->
                BentoWeekDay(
                    label = WEEKDAY_SHORT.getOrElse(day.weekday - 1) { "?" },
                    value = dayMinutes(day, mealBreakMin),
                )
            },
            color = AppColors.EmeraldPrimary.copy(alpha = 0.55f),
            todayColor = AppColors.EmeraldPrimary,
            contentColor = AppColors.TextFaint,
            barHeight = 30.dp,
        )

        if (canManage) {
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                PillButton(
                    text = if (active) "Desactivar" else "Activar",
                    onClick = onToggleActive,
                    variant = PillButtonVariant.Ghost,
                    enabled = !busy,
                    leadingIcon = if (active) Icons.Filled.Schedule else Icons.Filled.CheckCircle,
                )
            }
        }
    }
}
