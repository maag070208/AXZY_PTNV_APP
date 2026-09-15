package com.axzydev.puertonuevoapp.feature.cartas

import com.axzydev.puertonuevoapp.core.network.cartas.CartaGeneratedDto
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypeDto

data class GenerateCartaUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val types: List<DeviceTypeDto> = emptyList(),
    val typeId: String = "",
    val generating: Boolean = false,
    val result: CartaGeneratedDto? = null,
)
