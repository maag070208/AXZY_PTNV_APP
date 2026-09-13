package com.axzydev.puertonuevoapp.core.network

import kotlinx.serialization.Serializable

/**
 * Reportes: la propia web solo expone /reportes con dos tabs
 * (Asignados, Dispositivos — ReportesPage.tsx) — el reporte "entregas"
 * de report.service.ts (getReport/getReportTable/csv) no tiene ninguna
 * pantalla que lo consuma en el web, así que se deja fuera aquí también.
 * La exportación a PDF de cada tab (@react-pdf/renderer, 100% client-side)
 * tampoco tiene endpoint en el backend y queda fuera del alcance móvil.
 */
class ReportsApi(private val client: ApiClient) {
    suspend fun asignados(): List<AsignadoRowDto> =
        client.get<AsignadosResponseDto>("/reports/asignados").data

    suspend fun devices(): List<DeviceReportRowDto> =
        client.get<DevicesReportResponseDto>("/reports/devices").data
}

@Serializable
data class AsignadosResponseDto(
    val data: List<AsignadoRowDto>,
    val total: Int,
)

@Serializable
data class DevicesReportResponseDto(
    val data: List<DeviceReportRowDto>,
    val total: Int,
)

@Serializable
data class AsignadoRowDto(
    val deviceId: String,
    val controlActivos: String,
    val descripcion: String,
    val marca: String,
    val modelo: String,
    val tipo: String,
    val responsable: String,
    val numeroEmpleado: String? = null,
    val departamento: String? = null,
    val fecha: String? = null,
    val diasAsignado: Int? = null,
    val origen: String,
    val folio: String? = null,
)

@Serializable
data class DeviceReportRowDto(
    val deviceId: String,
    val controlActivos: String,
    val descripcion: String,
    val marca: String,
    val modelo: String,
    val tipo: String,
    val numeroSerie: String? = null,
    val nombreEquipo: String? = null,
    val ip: String? = null,
    val macAddress: String? = null,
    val area: String,
    val location: String? = null,
    val estado: String,
    val loteId: String? = null,
    val cantidad: Int = 1,
    val responsable: String? = null,
    val numeroEmpleado: String? = null,
    val departamento: String? = null,
    val fecha: String? = null,
    val diasAsignado: Int? = null,
    val origen: String? = null,
    val folio: String? = null,
)
