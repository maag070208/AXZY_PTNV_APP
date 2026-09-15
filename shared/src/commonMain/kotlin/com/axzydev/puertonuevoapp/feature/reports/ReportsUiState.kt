package com.axzydev.puertonuevoapp.feature.reports

import com.axzydev.puertonuevoapp.core.network.reports.AsignadoRowDto
import com.axzydev.puertonuevoapp.core.network.reports.DeviceReportRowDto

data class ReportsUiState(
    val tab: Int = 0,
    val asignadosLoading: Boolean = true,
    val asignadosError: String? = null,
    val asignados: List<AsignadoRowDto> = emptyList(),
    val devicesLoading: Boolean = true,
    val devicesError: String? = null,
    val devices: List<DeviceReportRowDto> = emptyList(),
) {
    val promedioDias: Int get() = if (asignados.isEmpty()) 0 else asignados.sumOf { it.diasAsignado ?: 0 } / asignados.size
    val masDe30: Int get() = asignados.count { (it.diasAsignado ?: 0) > 30 }

    val disponibles: Int get() = devices.count { it.estado == "DISPONIBLE" }
    val asignadosCount: Int get() = devices.count { it.estado == "ASIGNADO" }
    val bajas: Int get() = devices.count { it.estado == "BAJA" }
}
