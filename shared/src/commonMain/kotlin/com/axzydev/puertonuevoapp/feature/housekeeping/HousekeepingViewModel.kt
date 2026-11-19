package com.axzydev.puertonuevoapp.feature.housekeeping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.housekeeping.HousekeepingApi
import com.axzydev.puertonuevoapp.core.network.housekeeping.HousekeepingReasonDto
import com.axzydev.puertonuevoapp.core.network.housekeeping.HousekeepingTaskDto
import com.axzydev.puertonuevoapp.core.network.housekeeping.InspectInput
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Rechazo de una inspección: motivo (obligatorio en el API) y notas. */
data class RejectDialogState(
    val task: HousekeepingTaskDto,
    val reasonId: String? = null,
    val notes: String = "",
    val submitting: Boolean = false,
) {
    val canSubmit: Boolean get() = !submitting && reasonId != null
}

data class HousekeepingUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
    /** Camarista: pendientes y en curso. */
    val mine: List<HousekeepingTaskDto> = emptyList(),
    /** Supervisora: `GET /hotel/tasks?pendingInspection=true`. */
    val toInspect: List<HousekeepingTaskDto> = emptyList(),
    val reasons: List<HousekeepingReasonDto> = emptyList(),
    val busyId: String? = null,
    val reject: RejectDialogState? = null,
)

/**
 * Limpieza de habitaciones. Camarista (`housekeeping.operate`): Empezar -> Terminé.
 * Supervisora (`housekeeping.inspect`): Aprobar / Rechazar lo terminado.
 */
class HousekeepingViewModel(
    private val api: HousekeepingApi,
    private val userId: String,
    private val canOperate: Boolean,
    private val canInspect: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HousekeepingUiState())
    val uiState: StateFlow<HousekeepingUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        fetch(silent = false)
    }

    fun refresh(silent: Boolean = false) {
        if (!silent) _uiState.update { it.copy(refreshing = true) }
        fetch(silent)
    }

    private fun fetch(silent: Boolean) {
        viewModelScope.launch {
            runCatching {
                val mine = if (canOperate) {
                    api.tasks().filter { it.assignedToId == userId && (it.status == "PENDING" || it.status == "IN_PROGRESS") }
                        .sortedBy { it.room?.number?.filter(Char::isDigit)?.toIntOrNull() ?: Int.MAX_VALUE }
                } else {
                    emptyList()
                }
                val toInspect = if (canInspect) api.tasks(pendingInspection = true) else emptyList()
                mine to toInspect
            }.onSuccess { (mine, toInspect) ->
                _uiState.update { it.copy(loading = false, refreshing = false, loaded = true, error = null, mine = mine, toInspect = toInspect) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(loading = false, refreshing = false, error = if (silent && it.loaded) it.error else networkMessage(e))
                }
            }
        }
    }

    fun start(task: HousekeepingTaskDto) = act(task.id, "Limpieza iniciada") { api.start(task.id) }

    fun finish(task: HousekeepingTaskDto) = act(task.id, "Habitación terminada") { api.finish(task.id) }

    fun approve(task: HousekeepingTaskDto) =
        act(task.id, "Habitación aprobada") { api.inspect(task.id, InspectInput(result = "APPROVED")) }

    fun openReject(task: HousekeepingTaskDto) {
        _uiState.update { it.copy(reject = RejectDialogState(task)) }
        if (_uiState.value.reasons.isNotEmpty()) return
        viewModelScope.launch {
            runCatching { api.inspectionReasons() }
                .onSuccess { all -> _uiState.update { s -> s.copy(reasons = all.filter { it.active }) } }
                .onFailure { e -> AppSnackbar.show(networkMessage(e), isError = true) }
        }
    }

    fun closeReject() {
        if (_uiState.value.reject?.submitting != true) _uiState.update { it.copy(reject = null) }
    }

    fun setRejectReason(id: String) = _uiState.update { s -> s.copy(reject = s.reject?.copy(reasonId = id)) }

    fun setRejectNotes(value: String) = _uiState.update { s -> s.copy(reject = s.reject?.copy(notes = value)) }

    fun submitReject() {
        val dialog = _uiState.value.reject ?: return
        if (!dialog.canSubmit) return
        _uiState.update { it.copy(reject = dialog.copy(submitting = true)) }
        viewModelScope.launch {
            val input = InspectInput(result = "REJECTED", reasonId = dialog.reasonId, notes = dialog.notes.trim().ifBlank { null })
            runCatching { api.inspect(dialog.task.id, input) }
                .onSuccess { AppSnackbar.show("Habitación rechazada") }
                .onFailure { e -> AppSnackbar.show(networkMessage(e), isError = true) }
            _uiState.update { it.copy(reject = null) }
            refresh(silent = true)
        }
    }

    private fun act(id: String, successMessage: String, call: suspend () -> Unit) {
        if (_uiState.value.busyId != null) return
        _uiState.update { it.copy(busyId = id) }
        viewModelScope.launch {
            runCatching { call() }
                .onSuccess { AppSnackbar.show(successMessage) }
                .onFailure { e -> AppSnackbar.show(networkMessage(e), isError = true) }
            _uiState.update { it.copy(busyId = null) }
            refresh(silent = true)
        }
    }
}
