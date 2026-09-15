package com.axzydev.puertonuevoapp.feature.inventory

import com.axzydev.puertonuevoapp.core.network.locations.LocationDto
import com.axzydev.puertonuevoapp.core.network.locations.SublugarDto

data class LocationDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val location: LocationDto? = null,
    val newSublugar: String = "",
    val sublugarToDelete: SublugarDto? = null,
    val saving: Boolean = false,
)
