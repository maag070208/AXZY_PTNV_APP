package com.axzydev.puertonuevoapp.feature.materialoutputs

data class MaterialOutputFormUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val area: String = "Sistemas",
    val description: String = "",
    val model: String = "",
    val brand: String = "",
    val project: String = "",
    val quantity: String = "1",
    val departmentName: String = "",
    val userName: String = "",
    val notes: String = "",
    val reason: String = "",
    val saved: Boolean = false,
) {
    val isValid: Boolean get() = description.isNotBlank() && departmentName.isNotBlank() && userName.isNotBlank()
}
