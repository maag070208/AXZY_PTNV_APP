package com.axzydev.puertonuevoapp.feature.reports

import com.axzydev.puertonuevoapp.core.network.reports.AssignedDeviceRowDto
import com.axzydev.puertonuevoapp.core.network.reports.DeviceReportRowDto

data class ReportsUiState(
    val tab: Int = 0,
    val assignedDevicesLoading: Boolean = true,
    val assignedDevicesError: String? = null,
    val assignedDevices: List<AssignedDeviceRowDto> = emptyList(),
    val devicesLoading: Boolean = true,
    val devicesError: String? = null,
    val devices: List<DeviceReportRowDto> = emptyList(),
) {
    val avgDays: Int get() = if (assignedDevices.isEmpty()) 0 else assignedDevices.sumOf { it.daysAssigned ?: 0 } / assignedDevices.size
    val over30Days: Int get() = assignedDevices.count { (it.daysAssigned ?: 0) > 30 }

    val available: Int get() = devices.count { it.status == "AVAILABLE" }
    val assignedDevicesCount: Int get() = devices.count { it.status == "ASSIGNED" }
    val retired: Int get() = devices.count { it.status == "RETIRED" }
}
