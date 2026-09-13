package com.axzydev.puertonuevoapp.feature.home

import com.axzydev.puertonuevoapp.core.network.devices.DeviceSummaryDto

/** Estado inmutable del home (resumen de inventario). */
data class HomeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val summary: DeviceSummaryDto? = null,
)