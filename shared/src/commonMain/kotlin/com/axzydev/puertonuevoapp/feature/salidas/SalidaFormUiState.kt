package com.axzydev.puertonuevoapp.feature.salidas

data class SalidaFormUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val area: String = "Sistemas",
    val descripcion: String = "",
    val modelo: String = "",
    val marca: String = "",
    val proyecto: String = "",
    val cantidad: String = "1",
    val departamento: String = "",
    val usuario: String = "",
    val observaciones: String = "",
    val motivo: String = "",
    val saved: Boolean = false,
) {
    val isValid: Boolean get() = descripcion.isNotBlank() && departamento.isNotBlank() && usuario.isNotBlank()
}
