package com.axzydev.puertonuevoapp.feature.inventory

import com.axzydev.puertonuevoapp.core.network.locations.LocationDto
import com.axzydev.puertonuevoapp.core.network.locations.SubLocationDto

data class LocationDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val location: LocationDto? = null,
    val newSubLocation: String = "",
    val subLocationToDelete: SubLocationDto? = null,
    val saving: Boolean = false,
)
