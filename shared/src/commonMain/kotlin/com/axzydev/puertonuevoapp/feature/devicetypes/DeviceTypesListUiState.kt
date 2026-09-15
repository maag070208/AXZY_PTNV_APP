package com.axzydev.puertonuevoapp.feature.devicetypes

import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypeDto

data class DeviceTypesListUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val types: List<DeviceTypeDto> = emptyList(),
)
