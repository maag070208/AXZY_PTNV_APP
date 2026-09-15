package com.axzydev.puertonuevoapp.feature.inventory

import com.axzydev.puertonuevoapp.core.network.inventory.InventoryMovementDto
import com.axzydev.puertonuevoapp.core.network.locations.LocationDto

data class InventoryMovementsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val movements: List<InventoryMovementDto> = emptyList(),
    val locations: List<LocationDto> = emptyList(),
    val locationFilter: String = "",
    val startFilter: String = "",
    val endFilter: String = "",
)
