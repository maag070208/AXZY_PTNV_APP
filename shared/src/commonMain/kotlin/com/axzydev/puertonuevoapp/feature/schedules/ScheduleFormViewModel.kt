package com.axzydev.puertonuevoapp.feature.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.schedules.ScheduleCreateInput
import com.axzydev.puertonuevoapp.core.network.schedules.ScheduleUpdateInput
import com.axzydev.puertonuevoapp.core.network.schedules.SchedulesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Alta y edición de un horario. `scheduleId == null` = alta.
 *
 * El API no tiene `GET /schedules/:id`: el detalle se toma del listado
 * (`GET /schedules?includeInactive=true`), el mismo que usa la web. Al guardar,
 * `saved = true` y la pantalla vuelve al listado.
 */
class ScheduleFormViewModel(
    private val scheduleId: String?,
    private val api: SchedulesApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ScheduleFormUiState(creating = scheduleId == null, loading = scheduleId != null)
    )
    val uiState: StateFlow<ScheduleFormUiState> = _uiState.asStateFlow()

    init {
        if (scheduleId != null) load()
    }

    private fun load() {
        viewModelScope.launch {
            runCatching { api.schedules(includeInactive = true).firstOrNull { it.id == scheduleId } }
                .onSuccess { schedule ->
                    if (schedule == null) {
                        _uiState.update { it.copy(loading = false, notFound = true) }
                        return@onSuccess
                    }
                    _uiState.update {
                        it.copy(
                            loading = false,
                            notFound = false,
                            name = schedule.name,
                            entryToleranceText = schedule.entryToleranceMin.toString(),
                            exitToleranceText = schedule.exitToleranceMin.toString(),
                            mealBreakText = schedule.mealBreakMin.toString(),
                            minOvertimeText = schedule.minOvertimeMin.toString(),
                            crossesMidnight = schedule.crossesMidnight,
                            days = (1..7).map { weekday ->
                                val day = schedule.days.firstOrNull { it.weekday == weekday }
                                if (day == null) {
                                    ScheduleDayForm(weekday = weekday, restDay = true)
                                } else {
                                    ScheduleDayForm(
                                        weekday = weekday,
                                        restDay = day.restDay,
                                        startTime = day.startTime ?: "08:00",
                                        endTime = day.endTime ?: "16:00",
                                        split = day.splitStartTime != null && day.splitEndTime != null,
                                        splitStartTime = day.splitStartTime ?: "14:00",
                                        splitEndTime = day.splitEndTime ?: "15:00",
                                    )
                                }
                            },
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(loading = false, error = networkMessage(error)) }
                }
        }
    }

    /** La pantalla ya navegó de regreso: limpia la bandera para una reapertura. */
    fun consumeSaved() = _uiState.update { it.copy(saved = false) }

    fun clearError() = _uiState.update { it.copy(error = null) }

    fun retry() {
        if (scheduleId == null) return
        _uiState.update { it.copy(loading = true, error = null, notFound = false) }
        load()
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, validationError = null) }

    fun onEntryToleranceChange(value: String) =
        _uiState.update { it.copy(entryToleranceText = value.filter { c -> c.isDigit() }, validationError = null) }

    fun onExitToleranceChange(value: String) =
        _uiState.update { it.copy(exitToleranceText = value.filter { c -> c.isDigit() }, validationError = null) }

    fun onMealBreakChange(value: String) =
        _uiState.update { it.copy(mealBreakText = value.filter { c -> c.isDigit() }, validationError = null) }

    fun onMinOvertimeChange(value: String) =
        _uiState.update { it.copy(minOvertimeText = value.filter { c -> c.isDigit() }, validationError = null) }

    fun toggleRest(weekday: Int) = updateDay(weekday) { day ->
        day.copy(restDay = !day.restDay)
    }

    fun toggleSplit(weekday: Int) = updateDay(weekday) { day ->
        day.copy(split = !day.split)
    }

    fun onStartChange(weekday: Int, value: String) = updateDay(weekday) { it.copy(startTime = value) }

    fun onEndChange(weekday: Int, value: String) = updateDay(weekday) { it.copy(endTime = value) }

    fun onSplitStartChange(weekday: Int, value: String) =
        updateDay(weekday) { it.copy(splitStartTime = value) }

    fun onSplitEndChange(weekday: Int, value: String) =
        updateDay(weekday) { it.copy(splitEndTime = value) }

    /** Copia la jornada del primer día laborable a todos los laborables (atajo). */
    fun copyFirstWorkingDayToAll() {
        val source = _uiState.value.days.firstOrNull { !it.restDay } ?: return
        _uiState.update { state ->
            state.copy(
                days = state.days.map { day ->
                    if (day.restDay) day else day.copy(
                        startTime = source.startTime,
                        endTime = source.endTime,
                        split = source.split,
                        splitStartTime = source.splitStartTime,
                        splitEndTime = source.splitEndTime,
                    )
                },
                validationError = null,
            )
        }
    }

    private fun updateDay(weekday: Int, transform: (ScheduleDayForm) -> ScheduleDayForm) {
        _uiState.update { state ->
            state.copy(
                days = state.days.map { if (it.weekday == weekday) transform(it) else it },
                validationError = null,
            )
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.saving || state.saved || state.loading) return
        val error = validate(state)
        if (error != null) {
            _uiState.update { it.copy(validationError = error) }
            return
        }
        val entryTolerance = state.entryToleranceText.toIntOrNull() ?: 10
        val exitTolerance = state.exitToleranceText.toIntOrNull() ?: 10
        val mealBreak = state.mealBreakText.toIntOrNull() ?: 0
        val minOvertime = state.minOvertimeText.toIntOrNull() ?: 60
        val days = state.dtoDays
        val crossesMidnight = state.crossesMidnightDetected

        _uiState.update { it.copy(saving = true, validationError = null) }
        viewModelScope.launch {
            val id = scheduleId
            val result = runCatching {
                if (id == null) {
                    api.createSchedule(
                        ScheduleCreateInput(
                            name = state.name.trim(),
                            entryToleranceMin = entryTolerance,
                            exitToleranceMin = exitTolerance,
                            mealBreakMin = mealBreak,
                            minOvertimeMin = minOvertime,
                            crossesMidnight = crossesMidnight,
                            days = days,
                        )
                    )
                } else {
                    api.updateSchedule(
                        id,
                        ScheduleUpdateInput(
                            name = state.name.trim(),
                            entryToleranceMin = entryTolerance,
                            exitToleranceMin = exitTolerance,
                            mealBreakMin = mealBreak,
                            minOvertimeMin = minOvertime,
                            crossesMidnight = crossesMidnight,
                            days = days,
                        ),
                    )
                }
            }
            result
                .onSuccess {
                    _uiState.update { it.copy(saving = false, saved = true) }
                }
                .onFailure { failure ->
                    _uiState.update {
                        it.copy(saving = false, error = networkMessage(failure))
                    }
                }
        }
    }

    private fun validate(state: ScheduleFormUiState): String? {
        if (state.name.isBlank()) return "Ponle un nombre al horario."
        if (state.name.trim().length > 80) return "El nombre no puede pasar de 80 caracteres."
        if (state.workedDays == 0) return "Marca al menos un día laboral."
        val invalidDay = state.days.firstOrNull { !it.validTimes }
        if (invalidDay != null) {
            return "${WEEKDAY_NAMES[invalidDay.weekday - 1]}: captura horas válidas (HH:mm) y distintas."
        }
        val entry = state.entryToleranceText.toIntOrNull()
        val exit = state.exitToleranceText.toIntOrNull()
        val meal = state.mealBreakText.toIntOrNull()
        val overtime = state.minOvertimeText.toIntOrNull()
        if (entry == null || entry > 240) return "La tolerancia de entrada va de 0 a 240 minutos."
        if (exit == null || exit > 240) return "La tolerancia de salida va de 0 a 240 minutos."
        if (meal == null || meal > 240) return "El tiempo de comida va de 0 a 240 minutos."
        if (overtime == null || overtime > 1440) return "El mínimo de tiempo extra va de 0 a 1440 minutos."
        return null
    }
}
