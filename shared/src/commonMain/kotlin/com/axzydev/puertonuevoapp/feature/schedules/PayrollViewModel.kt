package com.axzydev.puertonuevoapp.feature.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentsApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.schedules.PeopleAttendanceFilters
import com.axzydev.puertonuevoapp.core.network.schedules.PeopleAttendanceQuery
import com.axzydev.puertonuevoapp.core.network.schedules.SchedulesApi
import com.axzydev.puertonuevoapp.core.network.schedules.WeeklyAttendanceQuery
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
 * Pre nómina (`payroll.view`): reporte semanal de asistencia de RH y
 * entradas/salidas por persona.
 *
 * El cálculo es del servidor (la misma regla de jornada que aprueban los jefes
 * de tiempo extra); la app solo elige periodo, departamento y persona, y
 * comparte el CSV que arma con las filas visibles. No hay PDF en el API.
 */
class PayrollViewModel(
    private val api: SchedulesApi,
    private val departmentsApi: DepartmentsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PayrollUiState())
    val uiState: StateFlow<PayrollUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadDepartments()
        load()
    }

    private fun loadDepartments() {
        viewModelScope.launch {
            runCatching { departmentsApi.list() }
                .onSuccess { rows -> _uiState.update { it.copy(departments = rows) } }
        }
    }

    fun retry() = load()

    fun load() {
        val state = _uiState.value
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            if (state.isWeekly) {
                runCatching {
                    api.weeklyAttendance(
                        WeeklyAttendanceQuery(
                            date = state.dateKey,
                            departmentId = state.departmentId.takeIf { it.isNotBlank() },
                            q = state.query.trim().takeIf { it.isNotBlank() },
                        )
                    )
                }
                    .onSuccess { report ->
                        _uiState.update { it.copy(loading = false, weekly = report, error = null) }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(loading = false, error = networkMessage(error)) }
                    }
            } else {
                val query = PeopleAttendanceQuery(
                    page = 1,
                    limit = 100,
                    filters = PeopleAttendanceFilters(
                        period = state.period,
                        date = state.dateKey,
                        departmentId = state.departmentId.takeIf { it.isNotBlank() },
                        q = state.query.trim().takeIf { it.isNotBlank() },
                        view = state.view,
                    ),
                )
                runCatching {
                    if (state.source == ATTENDANCE_SOURCE_CLOCK) {
                        api.peopleAttendanceFromClock(query)
                    } else {
                        api.peopleAttendanceFromAccess(query)
                    }
                }
                    .onSuccess { report ->
                        _uiState.update { it.copy(loading = false, people = report, error = null) }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(loading = false, error = networkMessage(error)) }
                    }
            }
        }
    }

    fun changeTab(tab: String) {
        if (_uiState.value.tab == tab) return
        _uiState.update {
            it.copy(
                tab = tab,
                loading = true,
                dateKey = todayDateKey(),
                view = "ALL",
                people = null,
            )
        }
        load()
    }

    fun changeSource(source: String) {
        if (_uiState.value.source == source) return
        _uiState.update { it.copy(source = source, loading = true, people = null) }
        load()
    }

    fun changePeriod(period: String) {
        _uiState.update { it.copy(period = period, dateKey = todayDateKey()) }
        load()
    }

    fun changeView(view: String) {
        _uiState.update { it.copy(view = view) }
        load()
    }

    /** Mueve el periodo de referencia (semana ±7 días; en la otra vista, según el periodo). */
    fun shiftPeriod(forward: Boolean) {
        val state = _uiState.value
        val days = if (state.isWeekly) {
            7
        } else {
            when (state.period) {
                "DAY" -> 1
                "WEEK" -> 7
                "FORTNIGHT" -> 15
                "MONTH" -> 30
                else -> 1
            }
        }
        _uiState.update { it.copy(dateKey = shiftDateKey(state.dateKey, if (forward) days else -days)) }
        load()
    }

    fun onDepartmentChange(departmentId: String) {
        _uiState.update { it.copy(departmentId = departmentId) }
        load()
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            load()
        }
    }

    /** CSV de la vista vigente (persona por renglón), compartido con la hoja del sistema. */
    fun exportCsv() {
        val state = _uiState.value
        if (state.isWeekly) {
            val report = state.weekly ?: return
            val csv = buildString {
                appendLine(
                    "Empleado,Numero,Departamento,Horario,Trabajados_min,Programados_min," +
                        "Extra_min,Aprobado_min,Pendiente_min,Faltas,Dias_incompletos"
                )
                report.rows.forEach { row ->
                    appendCsvRow(
                        row.name,
                        row.employeeNumber ?: "",
                        row.departmentName ?: "",
                        row.scheduleName ?: "Sin horario",
                        row.totals.workedMin.toString(),
                        row.totals.scheduledMin.toString(),
                        row.totals.extraMin.toString(),
                        row.totals.approvedExtraMin.toString(),
                        row.totals.pendingExtraMin.toString(),
                        row.totals.absences.toString(),
                        row.totals.incompleteDays.toString(),
                    )
                }
            }
            shareCsv("prenomina-${report.range.start}", csv)
            AppSnackbar.show("CSV generado (${report.rows.size} persona(s))")
        } else {
            val report = state.people ?: return
            val csv = buildString {
                appendLine(
                    "Empleado,Numero,Departamento,Trabajados_min,Retardos_min,Faltas," +
                        "Dias_con_retardo,Incompletos,En_sitio"
                )
                report.data.forEach { row ->
                    appendCsvRow(
                        row.employeeName,
                        row.employeeNumber ?: "",
                        row.departmentName ?: "",
                        row.workedMinutes.toString(),
                        row.days.sumOf { it.lateMinutes }.toString(),
                        row.absences.toString(),
                        row.lateDays.toString(),
                        row.withoutEntry.toString(),
                        if (row.onSite) "Sí" else "No",
                    )
                }
            }
            shareCsv("entradas-salidas-${report.summary.range.start}", csv)
            AppSnackbar.show("CSV generado (${report.data.size} persona(s))")
        }
    }
}
