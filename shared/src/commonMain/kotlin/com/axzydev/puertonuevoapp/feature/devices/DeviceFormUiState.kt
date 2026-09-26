package com.axzydev.puertonuevoapp.feature.devices

import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypeDto

data class DeviceFormUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val types: List<DeviceTypeDto> = emptyList(),
    val typeId: String = "",
    val description: String = "",
    val brand: String = "",
    val model: String = "",
    val area: String = "SISTEMAS",
    val serialNumber: String = "",
    val hostname: String = "",
    val ip: String = "",
    val macAddress: String = "",
    val operatingSystem: String = "",
    val ram: String = "",
    val storage: String = "",
    val blocked: Boolean = false,
    val blockedCode: String = "",
    val saved: Boolean = false,
) {
    private val selectedType: DeviceTypeDto? get() = types.firstOrNull { it.id == typeId }

    fun showField(key: String): Boolean = when (key) {
        "serialNumber" -> selectedType?.fieldConfig?.serialNumber?.enabled ?: true
        "hostname" -> selectedType?.fieldConfig?.hostname?.enabled ?: true
        "ip" -> selectedType?.fieldConfig?.ip?.enabled ?: false
        "macAddress" -> selectedType?.fieldConfig?.macAddress?.enabled ?: false
        "operatingSystem" -> selectedType?.fieldConfig?.operatingSystem?.enabled ?: false
        "ram" -> selectedType?.fieldConfig?.ram?.enabled ?: false
        "storage" -> selectedType?.fieldConfig?.storage?.enabled ?: false
        else -> false
    }

    val showSpecs: Boolean get() = listOf("ip", "macAddress", "operatingSystem", "ram", "storage").any(::showField)
    val isValid: Boolean get() = typeId.isNotBlank() && description.isNotBlank() && brand.isNotBlank() && model.isNotBlank()
}
