package com.axzydev.puertonuevoapp.feature.inventory

data class LocationFormUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val lugar: String = "",
    val descripcion: String = "",
    val saved: Boolean = false,
) {
    val isValid: Boolean get() = lugar.isNotBlank()
}
