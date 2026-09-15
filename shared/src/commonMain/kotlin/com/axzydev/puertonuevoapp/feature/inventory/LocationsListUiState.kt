package com.axzydev.puertonuevoapp.feature.inventory

import com.axzydev.puertonuevoapp.core.network.locations.LocationDto

data class LocationsListUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val locations: List<LocationDto> = emptyList(),
    val query: String = "",
    val deleteTarget: LocationDto? = null,
    val actionSaving: Boolean = false,
    val actionError: String? = null,
) {
    val filtered: List<LocationDto>
        get() = locations.filter { query.isBlank() || it.lugar.contains(query, ignoreCase = true) }
}
