package com.axzydev.puertonuevoapp.feature.inventory

import com.axzydev.puertonuevoapp.core.network.inventory.InventorySummaryDto

data class InventoryIndexUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val summary: InventorySummaryDto? = null,
)
