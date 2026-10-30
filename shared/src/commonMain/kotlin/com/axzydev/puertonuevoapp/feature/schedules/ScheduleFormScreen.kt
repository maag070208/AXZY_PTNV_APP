package com.axzydev.puertonuevoapp.feature.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoMiniStat
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.SectionLabel

/**
 * Alta/edición de un horario (`schedules.manage`).
 *
 * Rejilla de siete días: cada uno se marca laboral o de descanso; los
 * laborables llevan entrada/salida y, si la jornada se parte, un segundo tramo.
 * Arriba van las tolerancias, el tiempo de comida y el mínimo de tiempo extra;
 * abajo, el resumen semanal calculado en vivo (días laborables y horas), como
 * en la web.
 */
@Composable
fun ScheduleFormScreen(scheduleId: String?) {
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val canManage = (authState as? AuthState.LoggedIn)?.user?.can(PermissionKeys.SCHEDULES_MANAGE) == true

    if (!canManage) {
        BentoState(
            message = "No autorizado — requiere el permiso Administrar horarios",
            modifier = Modifier.fillMaxSize().padding(16.dp),
            tone = BentoTone.Rose,
        )
        return
    }

    val viewModel: ScheduleFormViewModel = viewModel(key = "schedule-form-${scheduleId ?: "new"}") {
        ScheduleFormViewModel(scheduleId, SchedulesServices.api)
    }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) {
            navigator.pop()
            viewModel.consumeSaved()
        }
    }

    when {
        state.loading -> LoadingState(modifier = Modifier.fillMaxSize())

        state.notFound -> BentoState(
            message = "No se encontró el horario. Puede que lo hayan eliminado.",
            modifier = Modifier.fillMaxSize().padding(16.dp),
            tone = BentoTone.Rose,
            actionLabel = "Volver",
            onAction = { navigator.pop() },
        )

        // Si falló la carga del detalle no se pinta el formulario vacío: guardar
        // pisaría el horario real con los valores por defecto.
        state.error != null && !state.creating && state.name.isBlank() -> BentoState(
            message = state.error ?: "No se pudo cargar el horario",
            modifier = Modifier.fillMaxSize().padding(16.dp),
            tone = BentoTone.Rose,
            actionLabel = "Reintentar",
            onAction = viewModel::retry,
        )

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column {
                    Text(
                        if (state.creating) "Nuevo horario" else "Editar horario",
                        style = MaterialTheme.typography.titleLarge,
                        color = AppColors.TextPrimary,
                    )
                    Text(
                        "Define los días, las horas y las tolerancias del turno.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                    )
                }
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                    SectionLabel("Datos del horario")
                    AppTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = { Text("Nombre del horario") },
                        placeholder = { Text("Ej. RECEPCIÓN TURNO MAÑANA") },
                        singleLine = true,
                        isError = state.validationError != null && state.name.isBlank(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(14.dp))
                    SectionLabel("Tolerancias y tiempos")
                    Text(
                        "Minutos de holgura para la entrada y la salida, la comida y el mínimo para contar tiempo extra.",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextFaint,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        AppTextField(
                            value = state.entryToleranceText,
                            onValueChange = viewModel::onEntryToleranceChange,
                            label = { Text("Tolerancia entrada") },
                            placeholder = { Text("min") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f),
                        )
                        AppTextField(
                            value = state.exitToleranceText,
                            onValueChange = viewModel::onExitToleranceChange,
                            label = { Text("Tolerancia salida") },
                            placeholder = { Text("min") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        AppTextField(
                            value = state.mealBreakText,
                            onValueChange = viewModel::onMealBreakChange,
                            label = { Text("Comida") },
                            placeholder = { Text("min") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f),
                        )
                        AppTextField(
                            value = state.minOvertimeText,
                            onValueChange = viewModel::onMinOvertimeChange,
                            label = { Text("Mínimo tiempo extra") },
                            placeholder = { Text("min") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel("Semana", modifier = Modifier.weight(1f))
                    TextButton(onClick = viewModel::copyFirstWorkingDayToAll) {
                        Text("Copiar a todos", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            items(state.days, key = { it.weekday }) { day ->
                ScheduleDayEditor(
                    day = day,
                    mealBreakMin = state.mealBreakText.toIntOrNull() ?: 0,
                    onToggleRest = { viewModel.toggleRest(day.weekday) },
                    onToggleSplit = { viewModel.toggleSplit(day.weekday) },
                    onStartChange = { viewModel.onStartChange(day.weekday, it) },
                    onEndChange = { viewModel.onEndChange(day.weekday, it) },
                    onSplitStartChange = { viewModel.onSplitStartChange(day.weekday, it) },
                    onSplitEndChange = { viewModel.onSplitEndChange(day.weekday, it) },
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    BentoMiniStat(
                        label = "Días laborables",
                        value = state.workedDays.toString(),
                        modifier = Modifier.weight(1f),
                        tone = BentoTone.Sea,
                    )
                    BentoMiniStat(
                        label = "Horas / semana",
                        value = hoursLabel(state.weeklyMinutesTotal),
                        modifier = Modifier.weight(1f),
                        tone = BentoTone.Mint,
                    )
                    BentoMiniStat(
                        label = "Cruza medianoche",
                        value = if (state.crossesMidnightDetected) "Sí" else "No",
                        modifier = Modifier.weight(1f),
                        tone = if (state.crossesMidnightDetected) BentoTone.Sun else BentoTone.Plain,
                    )
                }
            }

            state.validationError?.let { message ->
                item {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Danger,
                    )
                }
            }

            state.error?.let { message ->
                item {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Danger,
                    )
                }
            }

            item {
                PillButton(
                    text = if (state.creating) "Crear horario" else "Guardar cambios",
                    onClick = viewModel::save,
                    variant = PillButtonVariant.Primary,
                    loading = state.saving,
                    leadingIcon = Icons.Filled.CheckCircle,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ScheduleDayEditor(
    day: ScheduleDayForm,
    mealBreakMin: Int,
    onToggleRest: () -> Unit,
    onToggleSplit: () -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onSplitStartChange: (String) -> Unit,
    onSplitEndChange: (String) -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = AppColors.Outline.copy(alpha = 0.55f),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Filled.Schedule,
                contentDescription = null,
                tint = AppColors.TextFaint,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    WEEKDAY_NAMES[day.weekday - 1],
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.TextPrimary,
                )
                Text(
                    if (day.restDay) "Descanso" else {
                        val minutes = dayMinutes(day.toDto(), mealBreakMin)
                        "${hoursLabel(minutes)} de trabajo"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                )
            }
            AppFilterChip(
                label = if (day.restDay) "Descanso" else "Laboral",
                selected = !day.restDay,
                onClick = onToggleRest,
            )
        }

        if (!day.restDay) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AppTextField(
                    value = day.startTime,
                    onValueChange = onStartChange,
                    label = { Text("Entrada") },
                    placeholder = { Text("HH:mm") },
                    singleLine = true,
                    isError = !isValidTime(day.startTime),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
                AppTextField(
                    value = day.endTime,
                    onValueChange = onEndChange,
                    label = { Text("Salida") },
                    placeholder = { Text("HH:mm") },
                    singleLine = true,
                    isError = !isValidTime(day.endTime) || day.endTime == day.startTime,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(6.dp))
            if (day.split) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    AppTextField(
                        value = day.splitStartTime,
                        onValueChange = onSplitStartChange,
                        label = { Text("Descanso desde") },
                        placeholder = { Text("HH:mm") },
                        singleLine = true,
                        isError = !isValidTime(day.splitStartTime),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                    )
                    AppTextField(
                        value = day.splitEndTime,
                        onValueChange = onSplitEndChange,
                        label = { Text("Descanso hasta") },
                        placeholder = { Text("HH:mm") },
                        singleLine = true,
                        isError = !isValidTime(day.splitEndTime) || day.splitEndTime == day.splitStartTime,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            TextButton(onClick = onToggleSplit) {
                Text(
                    if (day.split) "Quitar jornada partida" else "Agregar jornada partida",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
