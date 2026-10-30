package com.axzydev.puertonuevoapp.feature.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentsApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.schedules.OvertimeApprovalInput
import com.axzydev.puertonuevoapp.core.network.schedules.OvertimeDayRowDto
import com.axzydev.puertonuevoapp.core.network.schedules.OvertimeDecisionItem
import com.axzydev.puertonuevoapp.core.network.schedules.OvertimeFilters
import com.axzydev.puertonuevoapp.core.network.schedules.OvertimeQuery
import com.axzydev.puertonuevoapp.core.network.schedules.SchedulesApi
import com.axzydev.puertonuevoapp.core.platform.shareCsv
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Aprobación de tiempo extra.
 *
 * El periodo (`DAY`/`WEEK`/`FORTNIGHT`/`MONTH`) y la fecha de referencia los
 * resuelve el servidor en la zona horaria configurada; la app solo navega el
 * periodo y filtra por departamento/persona y estatus. Aprobar/rechazar manda
 * los días seleccionados (`persona + fecha`) y el API recalcula y guarda el
 * snapshot (`overtime.approve`).
 */
class OvertimeApprovalViewModel(
    private val api: SchedulesApi,
    private val departmentsApi: DepartmentsApi,
    private val canApprove: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OvertimeApprovalUiState(canApprove = canApprove, statusTab = if (canApprove) "PENDING" else "APPROVED")
    )
    val uiState: StateFlow<OvertimeApprovalUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadDepartments()
        load(reset = true)
    }

    private fun loadDepartments() {
        viewModelScope.launch {
            runCatching { departmentsApi.list() }
                .onSuccess { rows -> _uiState.update { it.copy(departments = rows) } }
        }
    }

    /** Filtros base (sin estatus): los mismos que se usan para recalcular al decidir. */
    private fun baseFilters(state: OvertimeApprovalUiState): OvertimeFilters = OvertimeFilters(
        period = state.period,
        date = state.dateKey,
        departmentId = state.departmentId.takeIf { it.isNotBlank() },
        q = state.query.trim().takeIf { it.isNotBlank() },
    )

    fun retry() = load(reset = true)

    fun load(reset: Boolean) {
        val state = _uiState.value
        if (!reset && (state.loading || state.loadingMore || !state.hasMore)) return
        val page = if (reset) 1 else state.page + 1
        _uiState.update {
            it.copy(
                loading = reset,
                loadingMore = !reset,
                error = null,
                page = page,
                rows = if (reset) emptyList() else it.rows,
            )
        }
        viewModelScope.launch {
            val filters = baseFilters(state).copy(status = state.effectiveTab)
            runCatching {
                api.overtime(OvertimeQuery(page = page, limit = PAGE_SIZE, filters = filters))
            }
                .onSuccess { response ->
                    _uiState.update { current ->
                        val merged = if (reset) response.data else current.rows + response.data
                        current.copy(
                            loading = false,
                            loadingMore = false,
                            rows = merged,
                            total = response.total,
                            page = page,
                            summary = response.summary,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(loading = false, loadingMore = false, error = networkMessage(error))
                    }
                }
        }
    }

    fun loadMore() = load(reset = false)

    fun changePeriod(period: String) {
        _uiState.update {
            it.copy(period = period, dateKey = todayDateKey(), selected = emptyMap())
        }
        load(reset = true)
    }

    /** Mueve el periodo de referencia un paso atrás o adelante. */
    fun shiftPeriod(forward: Boolean) {
        val state = _uiState.value
        val days = when (state.period) {
            "DAY" -> 1
            "WEEK" -> 7
            "FORTNIGHT" -> 15
            "MONTH" -> 30
            else -> 1
        }
        val next = shiftDateKey(state.dateKey, if (forward) days else -days)
        _uiState.update { it.copy(dateKey = next, selected = emptyMap()) }
        load(reset = true)
    }

    fun changeStatusTab(tab: String) {
        if (!_uiState.value.canApprove) return
        _uiState.update { it.copy(statusTab = tab, selected = emptyMap()) }
        load(reset = true)
    }

    fun onDepartmentChange(departmentId: String) {
        _uiState.update { it.copy(departmentId = departmentId, selected = emptyMap()) }
        load(reset = true)
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            load(reset = true)
        }
    }

    fun toggleRow(row: OvertimeDayRowDto) {
        if (!_uiState.value.canApprove || row.status != "PENDING") return
        val key = overtimeDayKey(row)
        _uiState.update { state ->
            val next = state.selected.toMutableMap()
            if (next.containsKey(key)) next.remove(key)
            else next[key] = OvertimeDecisionItem(userId = row.userId, date = row.date)
            state.copy(selected = next)
        }
    }

    fun selectAllVisible() {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                selected = state.selectablePending.associate { row ->
                    overtimeDayKey(row) to OvertimeDecisionItem(userId = row.userId, date = row.date)
                }
            )
        }
    }

    fun clearSelection() = _uiState.update { it.copy(selected = emptyMap()) }

    fun approve() = decide("APPROVED", note = null)

    fun openReject() {
        if (_uiState.value.selected.isEmpty() || !_uiState.value.canApprove) return
        _uiState.update { it.copy(rejectOpen = true, rejectNote = "") }
    }

    fun dismissReject() = _uiState.update { it.copy(rejectOpen = false) }

    fun onRejectNoteChange(value: String) = _uiState.update { it.copy(rejectNote = value) }

    fun confirmReject() {
        val note = _uiState.value.rejectNote.trim().takeIf { it.isNotEmpty() }
        _uiState.update { it.copy(rejectOpen = false) }
        decide("REJECTED", note)
    }

    private fun decide(status: String, note: String?) {
        val state = _uiState.value
        if (!state.canApprove || state.selected.isEmpty() || state.saving) return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            runCatching {
                api.decideOvertime(
                    OvertimeApprovalInput(
                        filters = baseFilters(state),
                        items = state.selected.values.toList(),
                        status = status,
                        note = note,
                    )
                )
            }
                .onSuccess { result ->
                    AppSnackbar.show(
                        if (status == "APPROVED") {
                            "${result.updated} día(s) aprobado(s)" +
                                if (result.skipped > 0) " · ${result.skipped} omitido(s)" else ""
                        } else {
                            "${result.updated} día(s) rechazado(s)" +
                                if (result.skipped > 0) " · ${result.skipped} omitido(s)" else ""
                        }
                    )
                    _uiState.update { it.copy(saving = false, selected = emptyMap()) }
                    load(reset = true)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(saving = false) }
                    AppSnackbar.showError(networkMessage(error))
                }
        }
    }

    /**
     * Exporta el universo aprobado del periodo a CSV (persona-level, como el
     * export de la web). No hay endpoint de PDF: la app comparte el CSV.
     */
    fun exportCsv() {
        val state = _uiState.value
        viewModelScope.launch {
            runCatching {
                api.overtimeExport(
                    OvertimeQuery(page = 1, limit = 100, filters = baseFilters(state))
                )
            }
                .onSuccess { export ->
                    val csv = buildString {
                        appendLine(
                            "Empleado,Numero,Departamento,Trabajados_min,Programados_min," +
                                "Extra_min,Aprobado_min,Pendiente_min,Rechazado_min,Dias_extra"
                        )
                        export.data.forEach { row ->
                            appendCsvRow(
                                row.employeeName,
                                row.employeeNumber ?: "",
                                row.departmentName ?: "",
                                row.workedMin.toString(),
                                row.scheduledMin.toString(),
                                row.extraMin.toString(),
                                row.approvedMin.toString(),
                                row.pendingMin.toString(),
                                row.rejectedMin.toString(),
                                row.daysWithExtra.toString(),
                            )
                        }
                    }
                    shareCsv("tiempo-extra-${state.dateKey}", csv)
                    AppSnackbar.show("CSV generado (${export.data.size} persona(s))")
                }
                .onFailure { error ->
                    AppSnackbar.showError(networkMessage(error))
                }
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
