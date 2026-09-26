package com.axzydev.puertonuevoapp.feature.devices

import com.axzydev.puertonuevoapp.core.network.devices.DeviceDto

data class DevicesListUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val devices: List<DeviceDto> = emptyList(),
    val query: String = "",
    val statusFilter: String? = null,
) {
    val filtered: List<DeviceDto>
        get() = devices
            .filter { statusFilter == null || it.status == statusFilter }
            .filter {
                query.isBlank() ||
                    it.assetTag.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.brand.contains(query, ignoreCase = true) ||
                    it.model.contains(query, ignoreCase = true)
            }
            .sortedBy { it.assetTag }
}

val deviceStatusFilters: List<Pair<String?, String>> = listOf(
    null to "Todos",
    "AVAILABLE" to "Disponibles",
    "ASSIGNED" to "Asignados",
    "RETIRED" to "Baja",
)
