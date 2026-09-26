package com.axzydev.puertonuevoapp.core.network.reports

import com.axzydev.puertonuevoapp.core.network.http.ApiClient

/**
 * Reportes: la web solo expone dos tabs (Asignados, Dispositivos —
 * ReportsPage.tsx); el reporte "entregas" de report.service.ts
 * (getReport/query/csv) no tiene pantalla que lo consuma ni en el web, así
 * que se deja fuera aquí. La exportación a PDF/CSV (client-side) también
 * queda fuera del alcance móvil.
 */
class ReportsApi(private val client: ApiClient) {
    suspend fun assignedDevices(): List<AssignedDeviceRowDto> = client.get<AssignedDevicesResponseDto>("/reports/assigned-devices").data

    suspend fun devices(): List<DeviceReportRowDto> = client.get<DevicesReportResponseDto>("/reports/devices").data
}
