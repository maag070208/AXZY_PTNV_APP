package com.axzydev.puertonuevoapp.core.network.reports

import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// Asignados (`POST /reports/assigned-devices`): la fila ES una unidad física.
// ---------------------------------------------------------------------------

@Serializable
data class AssignedDevicesResponseDto(
    val data: List<AssignedDeviceRowDto>,
    val total: Int = 0,
)

/** KPIs del conjunto filtrado completo (no de la página visible). */
@Serializable
data class AssignedDevicesStatsDto(
    val assigned: Int = 0,
    /** Promedio de días asignados; el API lo manda como número (puede traer decimales). */
    val averageDays: Double = 0.0,
    val over30: Int = 0,
)

/** Página de asignados: `paginatedTable` + `stats`. */
@Serializable
data class AssignedDevicesTableResponseDto(
    val data: List<AssignedDeviceRowDto> = emptyList(),
    val total: Int = 0,
    val stats: AssignedDevicesStatsDto = AssignedDevicesStatsDto(),
)

/** Universo filtrado para la exportación (tope del API: 2000 filas). */
@Serializable
data class AssignedDevicesExportResponseDto(
    val data: List<AssignedDeviceRowDto> = emptyList(),
    val total: Int = 0,
    val stats: AssignedDevicesStatsDto = AssignedDevicesStatsDto(),
    val truncated: Boolean = false,
)

@Serializable
data class AssignedDeviceRowDto(
    val deviceId: String,
    /** Unidad física (la fila es la pieza, no el modelo). */
    val unitId: String = "",
    val assetTag: String,
    val description: String,
    val brand: String,
    val model: String,
    val type: String,
    val custodian: String,
    val employeeNumber: String? = null,
    val department: String? = null,
    val date: String? = null,
    val daysAssigned: Int? = null,
    /** `CUSTODY_LETTER` (carta) o `MOVEMENT` (movimiento directo). */
    val source: String,
    val folio: String? = null,
)

// ---------------------------------------------------------------------------
// Dispositivos (`POST /reports/devices`): catálogo de unidades y su estado.
// ---------------------------------------------------------------------------

@Serializable
data class DevicesReportResponseDto(
    val data: List<DeviceReportRowDto>,
    val total: Int = 0,
)

/** KPIs del conjunto filtrado completo. */
@Serializable
data class DevicesStatsDto(
    val total: Int = 0,
    val available: Int = 0,
    val assigned: Int = 0,
    val retired: Int = 0,
    val over30: Int = 0,
    val averageDays: Double = 0.0,
)

@Serializable
data class DevicesTableResponseDto(
    val data: List<DeviceReportRowDto> = emptyList(),
    val total: Int = 0,
    val stats: DevicesStatsDto = DevicesStatsDto(),
)

@Serializable
data class DevicesExportResponseDto(
    val data: List<DeviceReportRowDto> = emptyList(),
    val total: Int = 0,
    val stats: DevicesStatsDto = DevicesStatsDto(),
    val truncated: Boolean = false,
)

@Serializable
data class DeviceReportRowDto(
    val deviceId: String,
    val unitId: String = "",
    val assetTag: String,
    val description: String,
    val brand: String,
    val model: String,
    val type: String,
    val serialNumber: String? = null,
    val hostname: String? = null,
    val ip: String? = null,
    val macAddress: String? = null,
    val area: String,
    val departmentName: String? = null,
    val status: String,
    val batchId: String? = null,
    val quantity: Int = 1,
    val custodian: String? = null,
    val employeeNumber: String? = null,
    val department: String? = null,
    val date: String? = null,
    val daysAssigned: Int? = null,
    val source: String? = null,
    val folio: String? = null,
)

// ---------------------------------------------------------------------------
// Resumen por periodo (`POST /reports/period-summary`)
// ---------------------------------------------------------------------------

/** Cuerpo del reporte de periodo. `date` es el día local de referencia `YYYY-MM-DD`. */
@Serializable
data class PeriodSummaryQueryDto(
    /** `DAY` | `WEEK` | `MONTH`. */
    val period: String = "DAY",
    val date: String,
    val timezone: String? = null,
    val typeId: String? = null,
    val departmentId: String? = null,
    val custodianId: String? = null,
    /** Tope del detalle de entregas que devuelve `period-summary/detail` (1..2000). */
    val detailLimit: Int? = null,
)

@Serializable
data class PeriodSummaryRangeDto(
    val start: String = "",
    val end: String = "",
    val timezone: String = "",
    val period: String = "DAY",
    val date: String = "",
)

@Serializable
data class PeriodSummaryFiltersDto(
    val typeId: String? = null,
    val departmentId: String? = null,
    val custodianId: String? = null,
)

@Serializable
data class PeriodSummaryTotalsDto(
    val delivered: Int = 0,
    val returned: Int = 0,
    val admitted: Int = 0,
    val net: Int = 0,
    val distinctRecipients: Int = 0,
    val distinctDevices: Int = 0,
    val distinctLoans: Int = 0,
    val distinctReturns: Int = 0,
    val distinctMovements: Int = 0,
)

/** Un día del periodo con sus tres series (entregado, devuelto, alta). */
@Serializable
data class PeriodBucketDto(
    val day: String = "",
    val delivered: Int = 0,
    val returned: Int = 0,
    val admitted: Int = 0,
    val net: Int = 0,
)

@Serializable
data class DeviceAggregateDto(
    val deviceId: String = "",
    val description: String = "",
    val typeId: String = "",
    val typeName: String = "",
    val delivered: Int = 0,
    val returned: Int = 0,
    val admitted: Int = 0,
    val net: Int = 0,
)

@Serializable
data class RecipientAggregateDto(
    val userId: String = "",
    val name: String = "",
    val employeeNumber: String? = null,
    val departmentName: String? = null,
    val loans: Int = 0,
    val delivered: Int = 0,
    val returned: Int = 0,
    val outstanding: Int = 0,
)

/** Resumen del periodo: totales, serie diaria y agregados por dispositivo/persona. */
@Serializable
data class PeriodSummaryDto(
    val range: PeriodSummaryRangeDto = PeriodSummaryRangeDto(),
    val filters: PeriodSummaryFiltersDto = PeriodSummaryFiltersDto(),
    val totals: PeriodSummaryTotalsDto = PeriodSummaryTotalsDto(),
    val byPeriod: List<PeriodBucketDto> = emptyList(),
    val byDevice: List<DeviceAggregateDto> = emptyList(),
    val recipients: List<RecipientAggregateDto> = emptyList(),
)

// ---------------------------------------------------------------------------
// Entregas del periodo (`period-summary/deliveries` y `period-summary/detail`)
// ---------------------------------------------------------------------------

@Serializable
data class DeliveryDetailResponseDto(
    val data: List<DeliveryRowDto> = emptyList(),
    val total: Int = 0,
    val truncated: Boolean = false,
)

/** Un renglón entregado (pieza) del periodo; alimenta la bitácora y la exportación. */
@Serializable
data class DeliveryRowDto(
    val id: String,
    val date: String = "",
    val number: String = "",
    val custodianName: String = "",
    val employeeNumber: String? = null,
    val departmentName: String? = null,
    val subareaName: String? = null,
    val deliveredBy: String = "",
    val deviceId: String = "",
    val description: String = "",
    val typeName: String = "",
    val brand: String = "",
    val model: String = "",
    val quantity: Int = 0,
    val returnedQuantity: Int = 0,
    val returnDate: String? = null,
    val returnNumber: String? = null,
    /** `ACTIVE` | `PARTIAL` | `RETURNED`. */
    val status: String = "ACTIVE",
)
