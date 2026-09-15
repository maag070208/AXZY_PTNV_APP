package com.axzydev.puertonuevoapp.feature.devices

import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypeDto

data class DeviceFormUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val types: List<DeviceTypeDto> = emptyList(),
    val typeId: String = "",
    val descripcion: String = "",
    val marca: String = "",
    val modelo: String = "",
    val area: String = "SISTEMAS",
    val numeroSerie: String = "",
    val nombreEquipo: String = "",
    val ip: String = "",
    val macAddress: String = "",
    val sistemaOp: String = "",
    val ram: String = "",
    val almacenamiento: String = "",
    val blocked: Boolean = false,
    val blockedCode: String = "",
    val saved: Boolean = false,
) {
    private val selectedType: DeviceTypeDto? get() = types.firstOrNull { it.id == typeId }

    fun showField(key: String): Boolean = when (key) {
        "numeroSerie" -> selectedType?.fieldConfig?.numeroSerie?.enabled ?: true
        "nombreEquipo" -> selectedType?.fieldConfig?.nombreEquipo?.enabled ?: true
        "ip" -> selectedType?.fieldConfig?.ip?.enabled ?: false
        "macAddress" -> selectedType?.fieldConfig?.macAddress?.enabled ?: false
        "sistemaOp" -> selectedType?.fieldConfig?.sistemaOp?.enabled ?: false
        "ram" -> selectedType?.fieldConfig?.ram?.enabled ?: false
        "almacenamiento" -> selectedType?.fieldConfig?.almacenamiento?.enabled ?: false
        else -> false
    }

    val showSpecs: Boolean get() = listOf("ip", "macAddress", "sistemaOp", "ram", "almacenamiento").any(::showField)
    val isValid: Boolean get() = typeId.isNotBlank() && descripcion.isNotBlank() && marca.isNotBlank() && modelo.isNotBlank()
}
