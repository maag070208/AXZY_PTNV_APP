package com.axzydev.puertonuevoapp.feature.inventory

data class LocationFormUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val name: String = "",
    val description: String = "",
    val saved: Boolean = false,
) {
    val isValid: Boolean get() = name.isNotBlank()
}
