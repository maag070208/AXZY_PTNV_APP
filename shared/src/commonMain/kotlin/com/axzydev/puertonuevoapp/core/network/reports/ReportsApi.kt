package com.axzydev.puertonuevoapp.core.network.reports

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import io.ktor.http.encodeURLParameter

/**
 * Reportes del API (`/reports`, `/reports/period-summary*`).
 *
 * Las tres tablas de la web son server-side (`page`/`limit`/`filters`/`sort`):
 * `POST /reports/assigned-devices`, `POST /reports/devices` y la bitácora de
 * entregas `POST /reports/period-summary/deliveries`. Los filtros `start`/`end`
 * (`YYYY-MM-DD`) acotan el rango de fecha de las dos primeras.
 *
 * Exportaciones:
 * - `GET /reports/.csv` SÍ devuelve CSV del reporte de entregas (requiere
 *   `reports.export`): se comparte crudo con `shareFile`.
 * - `POST /reports/assigned-devices/export`, `POST /reports/devices/export` y
 *   `POST /reports/period-summary/detail` devuelven JSON con el universo
 *   filtrado (tope 2000); la app los convierte a CSV y los comparte con
 *   `shareCsv`. No existe exportación CSV para esas tres tablas.
 */
class ReportsApi(private val client: ApiClient) {

    // -----------------------------------------------------------------------
    // Asignados
    // -----------------------------------------------------------------------

    /** Listado simple (histórico); conservado por compatibilidad. */
    suspend fun assignedDevices(): List<AssignedDeviceRowDto> =
        client.get<AssignedDevicesResponseDto>("/reports/assigned-devices").data

    /** Tabla server-side de unidades asignadas + KPIs del conjunto filtrado. */
    suspend fun assigned(request: TableRequest): AssignedDevicesTableResponseDto =
        client.post<TableRequest, AssignedDevicesTableResponseDto>("/reports/assigned-devices", request)

    /** Universo filtrado (tope 2000) para exportar. */
    suspend fun assignedExport(request: TableRequest): AssignedDevicesExportResponseDto =
        client.post<TableRequest, AssignedDevicesExportResponseDto>("/reports/assigned-devices/export", request)

    // -----------------------------------------------------------------------
    // Dispositivos
    // -----------------------------------------------------------------------

    /** Listado simple (histórico); conservado por compatibilidad. */
    suspend fun devices(): List<DeviceReportRowDto> =
        client.get<DevicesReportResponseDto>("/reports/devices").data

    /** Tabla server-side del catálogo de unidades + KPIs del conjunto filtrado. */
    suspend fun devices(request: TableRequest): DevicesTableResponseDto =
        client.post<TableRequest, DevicesTableResponseDto>("/reports/devices", request)

    /** Universo filtrado (tope 2000) para exportar. */
    suspend fun devicesExport(request: TableRequest): DevicesExportResponseDto =
        client.post<TableRequest, DevicesExportResponseDto>("/reports/devices/export", request)

    // -----------------------------------------------------------------------
    // Resumen por periodo
    // -----------------------------------------------------------------------

    /** Resumen del periodo: totales, serie diaria y agregados por dispositivo y persona. */
    suspend fun periodSummary(query: PeriodSummaryQueryDto): PeriodSummaryDto =
        client.post<PeriodSummaryQueryDto, PeriodSummaryDto>("/reports/period-summary", query)

    /**
     * Bitácora de entregas del periodo, paginada en el servidor. Los filtros de
     * la ventana viajan dentro de `filters` (`period`, `date`, `timezone`), más
     * los propios de la tabla (`q`, `status`).
     */
    suspend fun periodDeliveries(request: TableRequest): TableResponse<DeliveryRowDto> =
        client.post<TableRequest, TableResponse<DeliveryRowDto>>("/reports/period-summary/deliveries", request)

    /** Universo de entregas del periodo para exportar (tope 2000, requiere `reports.export`). */
    suspend fun periodDetail(query: PeriodSummaryQueryDto): DeliveryDetailResponseDto =
        client.post<PeriodSummaryQueryDto, DeliveryDetailResponseDto>("/reports/period-summary/detail", query)

    // -----------------------------------------------------------------------
    // CSV real
    // -----------------------------------------------------------------------

    /**
     * CSV del reporte de entregas (`GET /reports/.csv`, requiere
     * `reports.export`). Filtros opcionales `start`/`end` (`YYYY-MM-DD`),
     * `department` y `employee`. Devuelve los bytes tal como los sirve el API
     * para compartirlos idénticos a la web.
     */
    suspend fun deliveryCsv(
        start: String? = null,
        end: String? = null,
        department: String? = null,
        employee: String? = null,
    ): ByteArray {
        val params = buildList {
            start?.takeIf { it.isNotBlank() }?.let { add("start=${it.encodeURLParameter()}") }
            end?.takeIf { it.isNotBlank() }?.let { add("end=${it.encodeURLParameter()}") }
            department?.takeIf { it.isNotBlank() }?.let { add("department=${it.encodeURLParameter()}") }
            employee?.takeIf { it.isNotBlank() }?.let { add("employee=${it.encodeURLParameter()}") }
        }
        val qs = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
        return client.getBytes("/reports/.csv$qs")
    }
}
