package com.axzydev.puertonuevoapp.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableSort
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.materialoutputs.MaterialOutputsApi
import com.axzydev.puertonuevoapp.core.network.reports.AssignedDeviceRowDto
import com.axzydev.puertonuevoapp.core.network.reports.DeliveryRowDto
import com.axzydev.puertonuevoapp.core.network.reports.DeviceReportRowDto
import com.axzydev.puertonuevoapp.core.network.reports.PeriodSummaryQueryDto
import com.axzydev.puertonuevoapp.core.network.reports.ReportsApi
import com.axzydev.puertonuevoapp.core.platform.CSV_MIME_TYPE
import com.axzydev.puertonuevoapp.core.platform.shareCsv
import com.axzydev.puertonuevoapp.core.platform.shareFile
import com.axzydev.puertonuevoapp.core.util.currentTimeMillis
import com.axzydev.puertonuevoapp.core.util.deviceStatusLabel
import com.axzydev.puertonuevoapp.core.util.formatIsoUtc
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import com.axzydev.puertonuevoapp.core.util.localOffsetMillis
import com.axzydev.puertonuevoapp.core.util.parseIsoToEpochMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Filas por página de las tablas (server-side). */
private const val PageSize = 20

/** Tope que aplica el API a los universos de exportación. */
private const val ExportLimit = 2000

/**
 * Reportes: pestañas Asignados / Dispositivos / Salidas de material / Resumen
 * por periodo / Bitácora de entregas.
 *
 * Las tablas son server-side (`page`/`limit`/`filters`/`sort`); el filtro de
 * periodo (`start`/`end`, `YYYY-MM-DD`) se comparte entre las tres primeras. El
 * resumen y la bitácora usan su propia ventana (`period` DAY/WEEK/MONTH + día
 * ancla), que es la que entiende `POST /reports/period-summary`.
 *
 * Exportaciones (solo con `reports.export`; la UI oculta el botón si falta):
 * - Entregas: CSV real del API (`GET /reports/.csv`) → `shareFile` crudo.
 * - Asignados / Dispositivos / Resumen: el API devuelve el universo filtrado en
 *   JSON (tope 2000) → la app lo convierte a CSV → `shareCsv`.
 * - Salidas: `GET /material-outputs` filtrado por el periodo → CSV en la app.
 */
class ReportsViewModel(
    private val reportsApi: ReportsApi,
    private val materialOutputsApi: MaterialOutputsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState(summaryDate = todayLocalIso()))
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    // Token por tabla: al cambiar el periodo con una carga en vuelo, la
    // respuesta vieja no debe pisar la nueva (misma sección, otra ventana).
    private var assignedToken = 0
    private var devicesToken = 0
    private var outputsToken = 0
    private var deliveriesToken = 0

    init {
        _uiState.update { it.copy(loadedTabs = it.loadedTabs + ReportTab.Assigned) }
        loadAssigned(reset = true)
    }

    // -----------------------------------------------------------------------
    // Pestañas
    // -----------------------------------------------------------------------

    fun setTab(tab: ReportTab) {
        _uiState.update { it.copy(tab = tab) }
        ensureLoaded(tab)
    }

    /** Carga perezosa: la primera vez que se entra a una pestaña se piden sus datos. */
    private fun ensureLoaded(tab: ReportTab) {
        if (tab in _uiState.value.loadedTabs) return
        _uiState.update { it.copy(loadedTabs = it.loadedTabs + tab) }
        when (tab) {
            ReportTab.Assigned -> loadAssigned(reset = true)
            ReportTab.Devices -> loadDevices(reset = true)
            ReportTab.Outputs -> loadOutputs(reset = true)
            ReportTab.Summary -> loadSummary()
            ReportTab.Deliveries -> loadDeliveries(reset = true)
        }
    }

    // -----------------------------------------------------------------------
    // Filtro de periodo compartido (tablas)
    // -----------------------------------------------------------------------

    fun onPreset(preset: PeriodPreset) {
        if (preset == PeriodPreset.Custom) return
        _uiState.update { it.copy(period = presetRange(preset)) }
        reloadLoadedTableTabs()
    }

    /**
     * Aplica un rango manual. Devuelve el mensaje de error si alguna fecha no es
     * `YYYY-MM-DD` válida (`null` = aplicado). Vacío = sin recorte por ese lado.
     */
    fun applyCustomRange(start: String, end: String): String? {
        val s = start.trim()
        val e = end.trim()
        if (s.isNotBlank() && epochDayOf(s) == null) return "La fecha inicial debe ser AAAA-MM-DD."
        if (e.isNotBlank() && epochDayOf(e) == null) return "La fecha final debe ser AAAA-MM-DD."
        if (s.isNotBlank() && e.isNotBlank() && s > e) return "La fecha inicial no puede ser posterior a la final."
        _uiState.update { it.copy(period = PeriodRange(start = s, end = e, preset = PeriodPreset.Custom)) }
        reloadLoadedTableTabs()
        return null
    }

    fun clearPeriod() {
        _uiState.update { it.copy(period = PeriodRange()) }
        reloadLoadedTableTabs()
    }

    private fun reloadLoadedTableTabs() {
        val loaded = _uiState.value.loadedTabs
        if (ReportTab.Assigned in loaded) loadAssigned(reset = true)
        if (ReportTab.Devices in loaded) loadDevices(reset = true)
        if (ReportTab.Outputs in loaded) loadOutputs(reset = true)
    }

    // -----------------------------------------------------------------------
    // Ventana del resumen / bitácora
    // -----------------------------------------------------------------------

    fun setSummaryPeriod(period: SummaryPeriod) {
        if (_uiState.value.summaryPeriod == period) return
        _uiState.update { it.copy(summaryPeriod = period) }
        if (ReportTab.Summary in _uiState.value.loadedTabs) loadSummary()
        if (ReportTab.Deliveries in _uiState.value.loadedTabs) loadDeliveries(reset = true)
    }

    /** Fecha ancla del resumen; solo se consulta cuando es una fecha completa válida. */
    fun setSummaryDate(value: String) {
        val cleaned = value.filter { it.isDigit() || it == '-' }.take(10)
        _uiState.update { it.copy(summaryDate = cleaned) }
        if (epochDayOf(cleaned) == null) return
        if (ReportTab.Summary in _uiState.value.loadedTabs) loadSummary()
        if (ReportTab.Deliveries in _uiState.value.loadedTabs) loadDeliveries(reset = true)
    }

    // -----------------------------------------------------------------------
    // Cargas
    // -----------------------------------------------------------------------

    fun loadAssigned(reset: Boolean) {
        val current = _uiState.value.assigned
        if (!reset && (current.loading || current.loadingMore)) return
        val page = if (reset) 1 else current.page + 1
        val token = ++assignedToken
        _uiState.update {
            it.copy(assigned = it.assigned.copy(loading = reset, loadingMore = !reset, error = null))
        }
        viewModelScope.launch {
            val result = runCatching { reportsApi.assigned(tableRequest(page, sortKey = "assetTag")) }
            _uiState.update { state ->
                if (token != assignedToken) return@update state
                result.fold(
                    onSuccess = { response ->
                        state.copy(
                            assigned = state.assigned.copy(
                                items = if (reset) response.data else state.assigned.items + response.data,
                                total = response.total,
                                page = page,
                                loading = false,
                                loadingMore = false,
                                error = null,
                            ),
                            assignedStats = response.stats,
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            assigned = state.assigned.copy(
                                loading = false,
                                loadingMore = false,
                                error = networkMessage(error),
                            ),
                        )
                    },
                )
            }
        }
    }

    fun loadDevices(reset: Boolean) {
        val current = _uiState.value.devices
        if (!reset && (current.loading || current.loadingMore)) return
        val page = if (reset) 1 else current.page + 1
        val token = ++devicesToken
        _uiState.update {
            it.copy(devices = it.devices.copy(loading = reset, loadingMore = !reset, error = null))
        }
        viewModelScope.launch {
            val result = runCatching { reportsApi.devices(tableRequest(page, sortKey = "assetTag")) }
            _uiState.update { state ->
                if (token != devicesToken) return@update state
                result.fold(
                    onSuccess = { response ->
                        state.copy(
                            devices = state.devices.copy(
                                items = if (reset) response.data else state.devices.items + response.data,
                                total = response.total,
                                page = page,
                                loading = false,
                                loadingMore = false,
                                error = null,
                            ),
                            devicesStats = response.stats,
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            devices = state.devices.copy(
                                loading = false,
                                loadingMore = false,
                                error = networkMessage(error),
                            ),
                        )
                    },
                )
            }
        }
    }

    fun loadOutputs(reset: Boolean) {
        val current = _uiState.value.outputs
        if (!reset && (current.loading || current.loadingMore)) return
        val page = if (reset) 1 else current.page + 1
        val token = ++outputsToken
        _uiState.update {
            it.copy(outputs = it.outputs.copy(loading = reset, loadingMore = !reset, error = null))
        }
        viewModelScope.launch {
            val request = TableRequest(
                page = page,
                limit = PageSize,
                filters = _uiState.value.period.filters(),
                sort = TableSort(key = "date", direction = "desc"),
            )
            val result = runCatching { materialOutputsApi.query(request) }
            _uiState.update { state ->
                if (token != outputsToken) return@update state
                result.fold(
                    onSuccess = { response ->
                        state.copy(
                            outputs = state.outputs.copy(
                                items = if (reset) response.data else state.outputs.items + response.data,
                                total = response.total,
                                page = page,
                                loading = false,
                                loadingMore = false,
                                error = null,
                            ),
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            outputs = state.outputs.copy(
                                loading = false,
                                loadingMore = false,
                                error = networkMessage(error),
                            ),
                        )
                    },
                )
            }
        }
    }

    fun loadSummary() {
        val state = _uiState.value
        if (state.summary.loading) return
        _uiState.update { it.copy(summary = it.summary.copy(loading = true, error = null)) }
        viewModelScope.launch {
            val result = runCatching { reportsApi.periodSummary(summaryQuery()) }
            _uiState.update { current ->
                result.fold(
                    onSuccess = { summary ->
                        current.copy(summary = current.summary.copy(loading = false, error = null, data = summary))
                    },
                    onFailure = { error ->
                        current.copy(summary = current.summary.copy(loading = false, error = networkMessage(error)))
                    },
                )
            }
        }
    }

    fun loadDeliveries(reset: Boolean) {
        val current = _uiState.value.deliveries
        if (!reset && (current.loading || current.loadingMore)) return
        val page = if (reset) 1 else current.page + 1
        val token = ++deliveriesToken
        _uiState.update {
            it.copy(deliveries = it.deliveries.copy(loading = reset, loadingMore = !reset, error = null))
        }
        viewModelScope.launch {
            val request = TableRequest(
                page = page,
                limit = PageSize,
                filters = deliveryFilters(),
                sort = TableSort(key = "date", direction = "desc"),
            )
            val result = runCatching { reportsApi.periodDeliveries(request) }
            _uiState.update { state ->
                if (token != deliveriesToken) return@update state
                result.fold(
                    onSuccess = { response ->
                        state.copy(
                            deliveries = state.deliveries.copy(
                                items = if (reset) response.data else state.deliveries.items + response.data,
                                total = response.total,
                                page = page,
                                loading = false,
                                loadingMore = false,
                                error = null,
                            ),
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            deliveries = state.deliveries.copy(
                                loading = false,
                                loadingMore = false,
                                error = networkMessage(error),
                            ),
                        )
                    },
                )
            }
        }
    }

    fun refreshActive() = when (_uiState.value.tab) {
        ReportTab.Assigned -> loadAssigned(reset = true)
        ReportTab.Devices -> loadDevices(reset = true)
        ReportTab.Outputs -> loadOutputs(reset = true)
        ReportTab.Summary -> loadSummary()
        ReportTab.Deliveries -> loadDeliveries(reset = true)
    }

    fun loadMoreActive() {
        val state = _uiState.value
        when (state.tab) {
            ReportTab.Assigned -> if (state.assigned.hasMore) loadAssigned(reset = false)
            ReportTab.Devices -> if (state.devices.hasMore) loadDevices(reset = false)
            ReportTab.Outputs -> if (state.outputs.hasMore) loadOutputs(reset = false)
            ReportTab.Deliveries -> if (state.deliveries.hasMore) loadDeliveries(reset = false)
            ReportTab.Summary -> Unit
        }
    }

    // -----------------------------------------------------------------------
    // Exportación CSV
    // -----------------------------------------------------------------------

    fun exportCsv() {
        val state = _uiState.value
        if (state.exporting) return
        _uiState.update { it.copy(exporting = true, exportError = null, exportMessage = null) }
        viewModelScope.launch {
            val result = runCatching {
                when (state.tab) {
                    ReportTab.Assigned -> exportAssigned()
                    ReportTab.Devices -> exportDevices()
                    ReportTab.Outputs -> exportOutputs()
                    ReportTab.Summary -> exportSummary()
                    ReportTab.Deliveries -> exportDeliveriesCsv()
                }
            }
            _uiState.update { current ->
                result.fold(
                    onSuccess = { message ->
                        current.copy(exporting = false, exportError = null, exportMessage = message)
                    },
                    onFailure = { error ->
                        current.copy(exporting = false, exportError = networkMessage(error))
                    },
                )
            }
        }
    }

    fun clearExportFeedback() = _uiState.update { it.copy(exportError = null, exportMessage = null) }

    /** Asignados: el API devuelve el universo filtrado (tope 2000) en JSON. */
    private suspend fun exportAssigned(): String {
        val response = reportsApi.assignedExport(
            TableRequest(page = 1, limit = ExportLimit, filters = _uiState.value.period.filters(), sort = TableSort("assetTag")),
        )
        val rows = response.data.map { row ->
            listOf(
                row.assetTag,
                row.description,
                row.brand,
                row.model,
                row.type,
                row.custodian,
                row.employeeNumber,
                row.department,
                formatShortDate(row.date),
                row.daysAssigned?.toString(),
                sourceLabel(row.source),
                row.folio,
            )
        }
        val headers = listOf(
            "Activo fijo", "Descripción", "Marca", "Modelo", "Tipo", "Responsable",
            "No. empleado", "Departamento", "Fecha", "Días asignados", "Origen", "Folio",
        )
        return shareCsvReport("reporte-asignados", headers, rows, response.truncated)
    }

    /** Dispositivos: mismo caso que Asignados (universo JSON → CSV en la app). */
    private suspend fun exportDevices(): String {
        val response = reportsApi.devicesExport(
            TableRequest(page = 1, limit = ExportLimit, filters = _uiState.value.period.filters(), sort = TableSort("assetTag")),
        )
        val rows = response.data.map { row ->
            listOf(
                row.assetTag,
                row.description,
                row.brand,
                row.model,
                row.type,
                row.serialNumber,
                row.hostname,
                row.ip,
                row.macAddress,
                row.area,
                deviceStatusLabel(row.status),
                row.custodian,
                row.employeeNumber,
                row.department,
                formatShortDate(row.date),
                row.daysAssigned?.toString(),
            )
        }
        val headers = listOf(
            "Activo fijo", "Descripción", "Marca", "Modelo", "Tipo", "Serie", "Hostname",
            "IP", "MAC", "Área", "Estado", "Responsable", "No. empleado", "Departamento",
            "Fecha", "Días asignados",
        )
        return shareCsvReport("reporte-dispositivos", headers, rows, response.truncated)
    }

    /** Salidas: `GET /material-outputs` filtrado por el periodo (sin tope propio conocido). */
    private suspend fun exportOutputs(): String {
        val period = _uiState.value.period
        val response = materialOutputsApi.list(
            start = period.start.takeIf { it.isNotBlank() },
            end = period.end.takeIf { it.isNotBlank() },
        )
        val rows = response.data.map { row ->
            listOf(
                formatShortDate(row.date),
                row.description,
                row.brand,
                row.model,
                row.quantity.toString(),
                row.departmentName,
                row.userName,
                outputReasonLabel(row.reason),
                row.project,
                row.notes,
            )
        }
        val headers = listOf(
            "Fecha", "Descripción", "Marca", "Modelo", "Cantidad", "Departamento",
            "Usuario", "Motivo", "Proyecto", "Notas",
        )
        return shareCsvReport("reporte-salidas", headers, rows, truncated = false)
    }

    /** Resumen: `period-summary/detail` trae el universo de entregas del periodo. */
    private suspend fun exportSummary(): String {
        val response = reportsApi.periodDetail(summaryQuery().copy(detailLimit = ExportLimit))
        return shareCsvReport("reporte-periodo", DeliveryCsvHeaders, response.data.map(::deliveryCsvRow), response.truncated)
    }

    /** Entregas: CSV real del API (`GET /reports/.csv`), idéntico al de la web. */
    private suspend fun exportDeliveriesCsv(): String {
        val (start, end) = summaryLocalRange()
        val bytes = reportsApi.deliveryCsv(start = start, end = end)
        val name = "reporte-entregas-${todayLocalIso()}.csv"
        shareFile(name, CSV_MIME_TYPE, bytes)
        return "CSV de entregas compartido ($name)."
    }

    // -----------------------------------------------------------------------
    // Helpers internos
    // -----------------------------------------------------------------------

    private fun tableRequest(page: Int, sortKey: String): TableRequest = TableRequest(
        page = page,
        limit = PageSize,
        filters = _uiState.value.period.filters(),
        sort = TableSort(key = sortKey),
    )

    private fun summaryQuery(): PeriodSummaryQueryDto = PeriodSummaryQueryDto(
        period = _uiState.value.summaryPeriod.api,
        date = effectiveSummaryDate(),
    )

    private fun deliveryFilters(): Map<String, String> = mapOf(
        "period" to _uiState.value.summaryPeriod.api,
        "date" to effectiveSummaryDate(),
    )

    private fun effectiveSummaryDate(): String =
        _uiState.value.summaryDate.takeIf { epochDayOf(it) != null } ?: todayLocalIso()

    /** Comparte la tabla ya convertida a CSV y arma el mensaje de resultado. */
    private fun shareCsvReport(
        prefix: String,
        header: List<String>,
        rows: List<List<String?>>,
        truncated: Boolean,
    ): String {
        val name = "$prefix-${todayLocalIso()}.csv"
        shareCsv(name, buildCsv(header, rows))
        return buildString {
            append("CSV de la pestaña compartido ($name): ")
            append(rows.size)
            append(" fila(s)")
            if (truncated) append(". El API limita la exportación a $ExportLimit filas.")
        }
    }

    /** Rango local (`YYYY-MM-DD`) de la ventana del resumen, para el CSV real. */
    private fun summaryLocalRange(): Pair<String, String> {
        val date = effectiveSummaryDate()
        val day = epochDayOf(date) ?: epochDayOf(todayLocalIso()) ?: 0L
        return when (_uiState.value.summaryPeriod) {
            SummaryPeriod.Day -> date to date
            SummaryPeriod.Week -> {
                val weekday = ((day + 3) % 7 + 7) % 7
                val monday = day - weekday
                isoOfEpochDay(monday) to isoOfEpochDay(monday + 6)
            }
            SummaryPeriod.Month -> date.take(7) + "-01" to isoOfEpochDay(lastEpochDayOfMonth(date))
        }
    }

    private fun presetRange(preset: PeriodPreset): PeriodRange {
        val today = todayLocalIso()
        val todayDay = epochDayOf(today) ?: return PeriodRange(preset = preset)
        return when (preset) {
            PeriodPreset.All -> PeriodRange()
            PeriodPreset.Last7 -> PeriodRange(isoOfEpochDay(todayDay - 6), today, preset)
            PeriodPreset.Last30 -> PeriodRange(isoOfEpochDay(todayDay - 29), today, preset)
            PeriodPreset.Month -> PeriodRange(today.take(7) + "-01", today, preset)
            PeriodPreset.Custom -> PeriodRange(preset = preset)
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers de fecha (sin kotlinx-datetime), CSV y etiquetas
// ---------------------------------------------------------------------------

/** Día local de hoy `YYYY-MM-DD` (el backend trabaja con claves de día). */
private fun todayLocalIso(): String {
    val now = currentTimeMillis()
    return formatIsoUtc(now + localOffsetMillis(now)).take(10)
}

/** Día-época de un `YYYY-MM-DD`; `null` si no es una fecha válida. */
private fun epochDayOf(iso: String): Long? {
    if (iso.length != 10) return null
    val millis = parseIsoToEpochMillis(iso + "T00:00:00.000Z") ?: return null
    return millis / 86_400_000L
}

private fun isoOfEpochDay(day: Long): String = formatIsoUtc(day * 86_400_000L).take(10)

/** Último día-época del mes de `YYYY-MM-DD` (sirve para el rango local del mes). */
private fun lastEpochDayOfMonth(iso: String): Long {
    val year = iso.take(4).toIntOrNull() ?: 1970
    val month = iso.substring(5, 7).toIntOrNull() ?: 1
    val nextMonthFirst = if (month == 12) {
        "${year + 1}-01-01"
    } else {
        "$year-${(month + 1).toString().padStart(2, '0')}-01"
    }
    return (epochDayOf(nextMonthFirst) ?: 0L) - 1
}

private val DeliveryCsvHeaders = listOf(
    "Fecha", "Folio", "Responsable", "No. empleado", "Departamento", "Subárea",
    "Entregó", "Dispositivo", "Tipo", "Marca", "Modelo", "Cantidad",
    "Devuelto", "Fecha devolución", "Estado",
)

private fun deliveryCsvRow(row: DeliveryRowDto): List<String?> = listOf(
    formatShortDate(row.date),
    row.number,
    row.custodianName,
    row.employeeNumber,
    row.departmentName,
    row.subareaName,
    row.deliveredBy,
    row.description,
    row.typeName,
    row.brand,
    row.model,
    row.quantity.toString(),
    row.returnedQuantity.toString(),
    formatShortDate(row.returnDate),
    deliveryStatusLabel(row.status),
)

internal fun deliveryStatusLabel(status: String): String = when (status) {
    "ACTIVE" -> "Activo"
    "PARTIAL" -> "Parcial"
    "RETURNED" -> "Devuelto"
    else -> status
}

private fun sourceLabel(source: String): String = when (source) {
    "CUSTODY_LETTER" -> "Carta responsiva"
    "MOVEMENT" -> "Movimiento"
    else -> source
}

internal fun outputReasonLabel(reason: String?): String = when (reason) {
    "DAMAGED" -> "Dañado"
    "OBSOLETE" -> "Obsoleto"
    "LOST" -> "Extravío"
    "OTHER" -> "Otro"
    else -> "Sin especificar"
}

/**
 * CSV con BOM UTF-8 (para que Excel respete los acentos) y las comillas RFC 4180
 * cuando el valor trae coma, comilla o salto de línea.
 */
private fun buildCsv(header: List<String>, rows: List<List<String?>>): String = buildString {
    append('\uFEFF')
    append(header.joinToString(",") { csvCell(it) })
    append("\r\n")
    rows.forEach { row ->
        append(row.joinToString(",") { csvCell(it.orEmpty()) })
        append("\r\n")
    }
}

private fun csvCell(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        '"' + value.replace("\"", "\"\"") + '"'
    } else {
        value
    }
