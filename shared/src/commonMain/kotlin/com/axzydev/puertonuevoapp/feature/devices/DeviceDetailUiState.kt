package com.axzydev.puertonuevoapp.feature.devices

import com.axzydev.puertonuevoapp.core.network.devices.DeviceDto

data class DeviceDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val device: DeviceDto? = null,
)
