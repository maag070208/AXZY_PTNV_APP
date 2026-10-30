package com.axzydev.puertonuevoapp.feature.timeclock

import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import com.axzydev.puertonuevoapp.core.network.http.TableSort
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockApi
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockPunchDto
import com.axzydev.puertonuevoapp.core.ui.table.PaginatedTableViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Asistencia del reloj: tabla server-side de checadas (`POST /time-clock/query`)
 * con rango de fechas, búsqueda por nombre/número y filtros de método/reloj, más
 * el estado de sincronización de los relojes (`GET /time-clock/status`).
 *
 * El tablero (`GET /time-clock/summary`) exige `time_clocks.manage` o
 * `time_clock.write`, así que solo se pide cuando [canReadSummary]; con
 * `time_clock.view` la pantalla funciona igual, sin los KPIs del tablero.
 */
class TimeClockViewModel(
    private val timeClockApi: TimeClockApi,
    private val canReadSummary: Boolean,
) : PaginatedTableViewModel<TimeClockPunchDto>() {

    override val pageSize: Int = 25
    override val searchKey: String = "q"

    private val _range = MutableStateFlow(TimeClockRange())
    val range: StateFlow<TimeClockRange> = _range.asStateFlow()

    private val _overview = MutableStateFlow(TimeClockOverviewUiState())
    val overview: StateFlow<TimeClockOverviewUiState> = _overview.asStateFlow()

    init {
        loadOverview()
    }

    /** Estado de los relojes + tablero. Se pide al abrir y al refrescar. */
    fun loadOverview() {
        _overview.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val status = timeClockApi.status()
                val summary = if (canReadSummary) {
                    runCatching { timeClockApi.summary() }.getOrNull()
                } else {
                    null
                }
                _overview.value = TimeClockOverviewUiState(
                    loading = false,
                    status = status,
                    summary = summary,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _overview.update { it.copy(loading = false, error = networkMessage(e)) }
            }
        }
    }

    /** Pull-to-refresh / botón reintentar: vuelve a pedir el estado y la tabla. */
    fun reload() {
        loadOverview()
        refresh()
    }

    /** Aplica un preset de rango (Hoy/Ayer/Últimos 7) sin recargar si es Personalizado. */
    fun applyRange(mode: TimeClockRangeMode) {
        val next = when (mode) {
            TimeClockRangeMode.Today -> TimeClockRange(mode, todayDayKey(), todayDayKey())
            TimeClockRangeMode.Yesterday -> {
                val yesterday = todayDayKey(-1)
                TimeClockRange(mode, yesterday, yesterday)
            }
            TimeClockRangeMode.Last7 -> TimeClockRange(mode, todayDayKey(-6), todayDayKey())
            TimeClockRangeMode.Custom -> _range.value.copy(mode = mode)
        }
        _range.value = next
        if (next.isValid) refresh()
    }

    fun setCustomFrom(value: String) = setCustomRange(from = value.trim())

    fun setCustomTo(value: String) = setCustomRange(to = value.trim())

    private fun setCustomRange(from: String? = null, to: String? = null) {
        _range.update {
            it.copy(
                mode = TimeClockRangeMode.Custom,
                from = from ?: it.from,
                to = to ?: it.to,
            )
        }
        if (_range.value.isValid) refresh()
    }

    /** Limpia búsqueda y filtros y vuelve al rango de hoy. */
    fun clearFilters() {
        onFiltersChange(emptyMap())
        _range.value = TimeClockRange()
        refresh()
    }

    override suspend fun fetch(
        page: Int,
        limit: Int,
        filters: Map<String, String>,
    ): TableResponse<TimeClockPunchDto> = fetch(page, limit, filters, sort = null)

    override suspend fun fetch(
        page: Int,
        limit: Int,
        filters: Map<String, String>,
        sort: TableSort?,
    ): TableResponse<TimeClockPunchDto> {
        val range = _range.value
        val merged = filters.toMutableMap()
        merged["from"] = range.from
        merged["to"] = range.to
        return timeClockApi.punches(
            TableRequest(
                page = page,
                limit = limit,
                filters = merged,
                sort = sort,
            )
        )
    }
}
