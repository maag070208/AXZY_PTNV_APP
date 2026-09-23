package com.axzydev.puertonuevoapp.feature.home

import com.axzydev.puertonuevoapp.core.network.dashboard.DashboardSummaryDto
import com.axzydev.puertonuevoapp.core.network.devices.DeviceSummaryDto

/** Estado inmutable del home (panel administrativo + resumen de inventario). */
data class HomeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val dashboard: DashboardSummaryDto? = null,
    val summary: DeviceSummaryDto? = null,
)
