package com.axzydev.puertonuevoapp.feature.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.schedules.ScheduleDto
import com.axzydev.puertonuevoapp.core.network.schedules.ScheduleUpdateInput
import com.axzydev.puertonuevoapp.core.network.schedules.SchedulesApi
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Listado y acciones del catálogo de horarios (`schedules.view` para ver,
 * `schedules.manage` para crear/editar/dar de baja — lo decide el API y la
 * pantalla esconde lo que el usuario no puede hacer).
 *
 * El listado trae también los inactivos (`includeInactive=true`), porque la
 * pantalla los muestra con su filtro propio, igual que la web.
 */
class SchedulesViewModel(
    private val api: SchedulesApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SchedulesUiState())
    val uiState: StateFlow<SchedulesUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { api.schedules(includeInactive = true) }
                .onSuccess { rows ->
                    _uiState.update { it.copy(loading = false, schedules = rows, error = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(loading = false, error = networkMessage(error)) }
                }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }

    fun onFilterChange(filter: SchedulesFilter) = _uiState.update { it.copy(filter = filter) }

    /** Activo → pide confirmación de baja; inactivo → reactiva de una vez. */
    fun onToggleActive(schedule: ScheduleDto) {
        if (schedule.active) {
            _uiState.update { it.copy(pendingDeactivate = schedule) }
        } else {
            updateActive(schedule, active = true)
        }
    }

    fun dismissDeactivate() = _uiState.update { it.copy(pendingDeactivate = null) }

    fun confirmDeactivate() {
        val schedule = _uiState.value.pendingDeactivate ?: return
        _uiState.update { it.copy(pendingDeactivate = null) }
        updateActive(schedule, active = false)
    }

    private fun updateActive(schedule: ScheduleDto, active: Boolean) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            runCatching { api.updateSchedule(schedule.id, ScheduleUpdateInput(active = active)) }
                .onSuccess {
                    AppSnackbar.show(if (active) "Horario activado" else "Horario desactivado")
                    _uiState.update { it.copy(busy = false) }
                    load()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(busy = false) }
                    AppSnackbar.showError(networkMessage(error))
                }
        }
    }

    fun requestDelete(schedule: ScheduleDto) = _uiState.update { it.copy(pendingDelete = schedule) }

    fun dismissDelete() = _uiState.update { it.copy(pendingDelete = null) }

    /**
     * `DELETE /schedules/:id`: el API hace baja lógica si el horario está activo
     * o tiene gente asignada, y borrado físico si ya estaba inactivo y vacío.
     */
    fun confirmDelete() {
        val schedule = _uiState.value.pendingDelete ?: return
        if (_uiState.value.busy) return
        _uiState.update { it.copy(pendingDelete = null, busy = true) }
        viewModelScope.launch {
            runCatching { api.removeSchedule(schedule.id) }
                .onSuccess { result ->
                    AppSnackbar.show(
                        if (result.soft) "Horario desactivado (tenía personas o estaba activo)"
                        else "Horario eliminado"
                    )
                    _uiState.update { it.copy(busy = false) }
                    load()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(busy = false) }
                    AppSnackbar.showError(networkMessage(error))
                }
        }
    }
}
