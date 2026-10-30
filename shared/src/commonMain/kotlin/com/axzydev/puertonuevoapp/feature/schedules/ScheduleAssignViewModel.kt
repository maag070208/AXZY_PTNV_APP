package com.axzydev.puertonuevoapp.feature.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentsApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.schedules.ScheduleAssignmentInput
import com.axzydev.puertonuevoapp.core.network.schedules.ScheduleRemoveAssignmentsInput
import com.axzydev.puertonuevoapp.core.network.schedules.SchedulesApi
import com.axzydev.puertonuevoapp.core.network.users.UsersApi
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Asignación masiva de horarios (`schedules.manage`).
 *
 * La pantalla carga el catálogo de horarios activos, el directorio de personas
 * (`GET /users/employees`, cualquier sesión), los departamentos y las
 * asignaciones vigentes del horario elegido. Con eso arma las dos listas:
 * personas sin el horario (para asignar desde una fecha) y personas que ya lo
 * tienen (para quitar la vigencia).
 */
class ScheduleAssignViewModel(
    private val api: SchedulesApi,
    private val usersApi: UsersApi,
    private val departmentsApi: DepartmentsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleAssignUiState())
    val uiState: StateFlow<ScheduleAssignUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val schedules = api.schedules()
                val employees = usersApi.employees()
                val departments = departmentsApi.list()
                Triple(schedules, employees, departments)
            }
                .onSuccess { (schedules, employees, departments) ->
                    val keep = _uiState.value.scheduleId
                        .takeIf { id -> schedules.any { it.id == id } }
                        ?: schedules.firstOrNull()?.id
                        ?: ""
                    _uiState.update {
                        it.copy(
                            loading = false,
                            schedules = schedules,
                            employees = employees,
                            departments = departments,
                            scheduleId = keep,
                            error = null,
                        )
                    }
                    if (keep.isNotBlank()) loadAssignees(keep)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(loading = false, error = networkMessage(error)) }
                }
        }
    }

    fun retry() = load()

    fun onScheduleChange(scheduleId: String) {
        _uiState.update {
            it.copy(
                scheduleId = scheduleId,
                selectedToAssign = emptySet(),
                selectedToRemove = emptySet(),
                assigned = emptyList(),
            )
        }
        loadAssignees(scheduleId)
    }

    fun onDepartmentChange(departmentId: String) = _uiState.update {
        it.copy(departmentId = departmentId, selectedToAssign = emptySet(), selectedToRemove = emptySet())
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }

    fun onFromDateChange(value: String) = _uiState.update {
        it.copy(fromDate = value.take(10), validationError = null)
    }

    fun toggleAssign(userId: String) = _uiState.update { state ->
        val next = state.selectedToAssign.toMutableSet()
        if (!next.add(userId)) next.remove(userId)
        state.copy(selectedToAssign = next, validationError = null)
    }

    fun toggleRemove(userId: String) = _uiState.update { state ->
        val next = state.selectedToRemove.toMutableSet()
        if (!next.add(userId)) next.remove(userId)
        state.copy(selectedToRemove = next, validationError = null)
    }

    fun selectAllAvailable() = _uiState.update { state ->
        state.copy(selectedToAssign = state.available.map { it.id }.toSet(), validationError = null)
    }

    fun selectAllAssigned() = _uiState.update { state ->
        state.copy(selectedToRemove = state.assignedPeople.map { it.id }.toSet(), validationError = null)
    }

    fun clearAssignSelection() = _uiState.update { it.copy(selectedToAssign = emptySet()) }

    fun clearRemoveSelection() = _uiState.update { it.copy(selectedToRemove = emptySet()) }

    private fun loadAssignees(scheduleId: String) {
        if (scheduleId.isBlank()) return
        _uiState.update { it.copy(assignedLoading = true) }
        viewModelScope.launch {
            runCatching { api.assignees(scheduleId) }
                .onSuccess { rows ->
                    _uiState.update { it.copy(assignedLoading = false, assigned = rows) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(assignedLoading = false) }
                    AppSnackbar.showError(networkMessage(error))
                }
        }
    }

    fun assign() {
        val state = _uiState.value
        if (!state.canAssign) return
        if (!isValidDateKey(state.fromDate)) {
            _uiState.update { it.copy(validationError = "La fecha “Desde” debe ser AAAA-MM-DD.") }
            return
        }
        _uiState.update { it.copy(saving = true, validationError = null) }
        viewModelScope.launch {
            runCatching {
                api.assign(
                    ScheduleAssignmentInput(
                        scheduleId = state.scheduleId,
                        userIds = state.selectedToAssign.toList(),
                        from = state.fromDate,
                    )
                )
            }
                .onSuccess { result ->
                    AppSnackbar.show("${result.assigned} persona(s) asignada(s) desde ${formatDateKey(result.from)}")
                    _uiState.update { it.copy(saving = false, selectedToAssign = emptySet()) }
                    loadAssignees(state.scheduleId)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(saving = false) }
                    AppSnackbar.showError(networkMessage(error))
                }
        }
    }

    fun remove() {
        val state = _uiState.value
        if (!state.canRemove) return
        _uiState.update { it.copy(saving = true, validationError = null) }
        viewModelScope.launch {
            runCatching {
                api.removeAssignments(
                    ScheduleRemoveAssignmentsInput(
                        scheduleId = state.scheduleId,
                        userIds = state.selectedToRemove.toList(),
                    )
                )
            }
                .onSuccess { result ->
                    AppSnackbar.show("${result.removed} asignación(es) cerrada(s)")
                    _uiState.update { it.copy(saving = false, selectedToRemove = emptySet()) }
                    loadAssignees(state.scheduleId)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(saving = false) }
                    AppSnackbar.showError(networkMessage(error))
                }
        }
    }
}

private fun isValidDateKey(value: String): Boolean =
    Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(value)
