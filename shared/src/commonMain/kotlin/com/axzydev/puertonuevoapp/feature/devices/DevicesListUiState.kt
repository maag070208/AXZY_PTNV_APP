package com.axzydev.puertonuevoapp.feature.devices

import com.axzydev.puertonuevoapp.core.network.devices.DeviceDto

data class DevicesListUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val devices: List<DeviceDto> = emptyList(),
    val query: String = "",
    val estadoFilter: String? = null,
) {
    val filtered: List<DeviceDto>
        get() = devices
            .filter { estadoFilter == null || it.estado == estadoFilter }
            .filter {
                query.isBlank() ||
                    it.controlActivos.contains(query, ignoreCase = true) ||
                    it.descripcion.contains(query, ignoreCase = true) ||
                    it.marca.contains(query, ignoreCase = true) ||
                    it.modelo.contains(query, ignoreCase = true)
            }
            .sortedBy { it.controlActivos }
}

val deviceEstadoFilters: List<Pair<String?, String>> = listOf(
    null to "Todos",
    "DISPONIBLE" to "Disponibles",
    "ASIGNADO" to "Asignados",
    "BAJA" to "Baja",
)
