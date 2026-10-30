package com.axzydev.puertonuevoapp.feature.timeclock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.schedules.PeopleAttendanceFilters
import com.axzydev.puertonuevoapp.core.network.schedules.PeopleAttendanceQuery
import com.axzydev.puertonuevoapp.core.network.schedules.SchedulesApi
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockApi
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockReportFilters
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockReportQuery
import com.axzydev.puertonuevoapp.core.platform.shareCsv
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Tamaño de página del reporte por persona (el contrato de tablas del API lo acepta). */
private const val PAGE_SIZE = 25

/** Filas máximas que trae el export del API (`/time-clock/report/export`). */
private const val EXPORT_LIMIT = 5000

/** Espera del buscador antes de consultar. */
private const val SEARCH_DEBOUNCE_MS = 350L

/**
 * Entradas y salidas por persona a partir de las checadas del reloj.
 *
 * - Tabla: `POST /schedules/attendance/time-clock` (una fila por persona con
 *   cada día del periodo contra su horario, igual que la web; permiso
 *   `time_clock.view`).
 * - CSV: `POST /time-clock/report/export` trae TODAS las sesiones del periodo
 *   con los mismos filtros y se arma el archivo en la app; [shareCsv] abre la
 *   hoja de compartir del sistema (la web lo descarga).
 */
class TimeClockEntriesExitsViewModel(
    private val schedulesApi: SchedulesApi,
    private val timeClockApi: TimeClockApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimeClockEntriesExitsUiState())
    val uiState: StateFlow<TimeClockEntriesExitsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var searchJob: Job? = null

    init {
        load(reset = true)
    }

    // ── Filtros ────────────────────────────────────────────────────────────

    /** Cambia el periodo (`Día/Semana/Quincena/Mes`) y vuelve a la fecha de hoy. */
    fun setPeriod(period: AttendancePeriod) {
        if (_uiState.value.period == period) return
        _uiState.update { it.copy(period = period, date = todayDayKey()) }
        load(reset = true)
    }

    /** Mueve el periodo de referencia hacia atrás ([step] = -1) o adelante (+1). */
    fun shiftPeriod(step: Int) {
        _uiState.update { it.copy(date = shiftPeriodDayKey(it.date, it.period, step)) }
        load(reset = true)
    }

    /** Vuelve al periodo que contiene hoy. */
    fun goToday() {
        _uiState.update { it.copy(date = todayDayKey()) }
        load(reset = true)
    }

    /** Vista rápida del reporte (incidencias, en sitio, sin registros...). */
    fun setView(view: AttendanceView) {
        if (_uiState.value.view == view) return
        _uiState.update { it.copy(view = view) }
        load(reset = true)
    }

    /** Incluye personas inactivas sin registros en el universo del reporte. */
    fun toggleIncludeInactive() {
        _uiState.update { it.copy(includeInactive = !it.includeInactive) }
        load(reset = true)
    }

    /** Búsqueda por nombre o número, con debounce. */
    fun onSearchChange(value: String) {
        _uiState.update { it.copy(q = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            load(reset = true)
        }
    }

    // ── Carga ──────────────────────────────────────────────────────────────

    fun refresh() = load(reset = true, refreshing = true)

    fun retry() = load(reset = true)

    fun loadMore() {
        val current = _uiState.value
        if (current.loading || current.loadingMore || current.refreshing || !current.hasMore) return
        load(reset = false)
    }

    private fun load(reset: Boolean, refreshing: Boolean = false) {
        loadJob?.cancel()
        val snapshot = _uiState.value
        if (!reset && !snapshot.hasMore) return
        val page = if (reset) 1 else snapshot.page + 1

        _uiState.update {
            it.copy(
                loading = reset,
                loadingMore = !reset,
                refreshing = refreshing,
                error = null,
            )
        }

        loadJob = viewModelScope.launch {
            try {
                val response = schedulesApi.peopleAttendanceFromClock(queryFor(snapshot, page))
                _uiState.update { state ->
                    state.copy(
                        rows = if (reset) response.data else state.rows + response.data,
                        summary = response.summary,
                        total = response.total,
                        page = page,
                        loading = false,
                        loadingMore = false,
                        refreshing = false,
                        error = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        loadingMore = false,
                        refreshing = false,
                        error = networkMessage(e),
                    )
                }
            }
        }
    }

    private fun queryFor(state: TimeClockEntriesExitsUiState, page: Int): PeopleAttendanceQuery =
        PeopleAttendanceQuery(
            page = page,
            limit = PAGE_SIZE,
            filters = PeopleAttendanceFilters(
                period = state.period.apiValue,
                date = state.date,
                departmentId = null,
                q = state.q.trim().ifBlank { null },
                includeInactive = state.includeInactive,
                view = state.view.apiValue,
            ),
        )

    // ── Exportar CSV ───────────────────────────────────────────────────────

    /**
     * Exporta a CSV todas las sesiones del periodo con los filtros vigentes y
     * abre la hoja de compartir. El archivo se arma aquí (el API devuelve JSON);
     * mismo detalle y columnas que el CSV de la web.
     */
    fun exportCsv() {
        val snapshot = _uiState.value
        if (snapshot.exporting) return
        _uiState.update { it.copy(exporting = true, exportError = null) }
        viewModelScope.launch {
            try {
                val response = timeClockApi.reportExport(
                    TimeClockReportQuery(
                        page = 1,
                        limit = EXPORT_LIMIT,
                        filters = TimeClockReportFilters(
                            period = snapshot.period.apiValue,
                            date = snapshot.date,
                            q = snapshot.q.trim().ifBlank { null },
                            includeInactive = snapshot.includeInactive,
                        ),
                    )
                )
                val fileName = "entradas-salidas-reloj-" +
                    "${snapshot.period.apiValue.lowercase()}-${snapshot.date}.csv"
                shareCsv(fileName, buildReportCsv(response.data))
                _uiState.update { it.copy(exporting = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(exporting = false, exportError = networkMessage(e))
                }
            }
        }
    }
}
