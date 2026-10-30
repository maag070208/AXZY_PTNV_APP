package com.axzydev.puertonuevoapp.feature.reports

import com.axzydev.puertonuevoapp.core.network.materialoutputs.MaterialOutputDto
import com.axzydev.puertonuevoapp.core.network.reports.AssignedDeviceRowDto
import com.axzydev.puertonuevoapp.core.network.reports.AssignedDevicesStatsDto
import com.axzydev.puertonuevoapp.core.network.reports.DeliveryRowDto
import com.axzydev.puertonuevoapp.core.network.reports.DeviceReportRowDto
import com.axzydev.puertonuevoapp.core.network.reports.DevicesStatsDto
import com.axzydev.puertonuevoapp.core.network.reports.PeriodSummaryDto

/** Pestañas del módulo de reportes (las tres de la web + resumen/bitácora del API). */
enum class ReportTab(val label: String) {
    Assigned("Asignados"),
    Devices("Dispositivos"),
    Outputs("Salidas"),
    Summary("Resumen"),
    Deliveries("Entregas"),
}

/** Rangos rápidos del filtro de periodo compartido por las tablas. */
enum class PeriodPreset(val label: String) {
    All("Todo"),
    Last7("7 días"),
    Last30("30 días"),
    Month("Este mes"),
    Custom("Personalizado"),
}

/** Rango de fechas local (`YYYY-MM-DD`) del filtro compartido; vacío = sin recorte. */
data class PeriodRange(
    val start: String = "",
    val end: String = "",
    val preset: PeriodPreset = PeriodPreset.All,
) {
    val isAll: Boolean get() = start.isBlank() && end.isBlank()

    /** Filtros `start`/`end` que entienden `assigned-devices`, `devices` y `material-outputs`. */
    fun filters(): Map<String, String> = buildMap {
        start.takeIf { it.isNotBlank() }?.let { put("start", it) }
        end.takeIf { it.isNotBlank() }?.let { put("end", it) }
    }

    val label: String
        get() = when {
            isAll -> "Todo el historial"
            start.isNotBlank() && end.isNotBlank() -> "$start a $end"
            start.isNotBlank() -> "Desde $start"
            else -> "Hasta $end"
        }
}

/** Granularidad del resumen por periodo (`PeriodSummaryQuery.period`). */
enum class SummaryPeriod(val api: String, val label: String) {
    Day("DAY", "Día"),
    Week("WEEK", "Semana"),
    Month("MONTH", "Mes"),
}

/** Estado común de una lista paginada server-side. */
data class ReportListState<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
) {
    val hasMore: Boolean get() = items.size < total
}

/** Estado del resumen por periodo (totales, serie diaria y agregados). */
data class ReportSummaryState(
    val loading: Boolean = false,
    val error: String? = null,
    val data: PeriodSummaryDto? = null,
)

/**
 * Estado de la pantalla de reportes: pestaña activa, filtro de periodo
 * compartido, cada tabla con su paginación y el resultado del resumen/bitácora
 * del periodo. `exporting`/`exportError`/`exportMessage` son de la exportación
 * CSV de la pestaña activa.
 */
data class ReportsUiState(
    val tab: ReportTab = ReportTab.Assigned,
    /** Filtro de periodo de las tablas (Asignados, Dispositivos, Salidas). */
    val period: PeriodRange = PeriodRange(),
    val assigned: ReportListState<AssignedDeviceRowDto> = ReportListState(),
    val assignedStats: AssignedDevicesStatsDto? = null,
    val devices: ReportListState<DeviceReportRowDto> = ReportListState(),
    val devicesStats: DevicesStatsDto? = null,
    val outputs: ReportListState<MaterialOutputDto> = ReportListState(),
    /** Ventana del resumen por periodo (Día/Semana/Mes + fecha ancla). */
    val summaryPeriod: SummaryPeriod = SummaryPeriod.Day,
    val summaryDate: String = "",
    val summary: ReportSummaryState = ReportSummaryState(),
    /** Bitácora de entregas del mismo periodo. */
    val deliveries: ReportListState<DeliveryRowDto> = ReportListState(),
    /** Pestañas ya cargadas al menos una vez (carga perezosa al entrar). */
    val loadedTabs: Set<ReportTab> = emptySet(),
    val exporting: Boolean = false,
    val exportError: String? = null,
    val exportMessage: String? = null,
) {
    /** Total de la pestaña activa (para el encabezado). */
    val activeTotal: Int
        get() = when (tab) {
            ReportTab.Assigned -> assigned.total
            ReportTab.Devices -> devices.total
            ReportTab.Outputs -> outputs.total
            ReportTab.Summary -> summary.data?.totals?.delivered ?: 0
            ReportTab.Deliveries -> deliveries.total
        }
}
