package com.axzydev.puertonuevoapp.feature.custodyletters

import com.axzydev.puertonuevoapp.core.network.custodyletters.CustodyLetterGeneratedDto
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypeDto

data class GenerateCustodyLetterUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val types: List<DeviceTypeDto> = emptyList(),
    val typeId: String = "",
    val generating: Boolean = false,
    val result: CustodyLetterGeneratedDto? = null,
)
