package com.axzydev.puertonuevoapp.feature.devicetypes

import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceFieldConfigDto
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceFieldSettingDto

data class DeviceTypeFieldDef(val key: String, val label: String)

val deviceTypeFieldDefs = listOf(
    DeviceTypeFieldDef("serialNumber", "Número de serie"),
    DeviceTypeFieldDef("hostname", "Nombre de equipo"),
    DeviceTypeFieldDef("ip", "Dirección IP"),
    DeviceTypeFieldDef("macAddress", "MAC Address"),
    DeviceTypeFieldDef("operatingSystem", "Sistema operativo"),
    DeviceTypeFieldDef("ram", "RAM"),
    DeviceTypeFieldDef("storage", "Almacenamiento"),
)

fun DeviceFieldConfigDto.settingFor(key: String): DeviceFieldSettingDto = when (key) {
    "serialNumber" -> serialNumber
    "hostname" -> hostname
    "ip" -> ip
    "macAddress" -> macAddress
    "operatingSystem" -> operatingSystem
    "ram" -> ram
    "storage" -> storage
    else -> DeviceFieldSettingDto()
}

fun DeviceFieldConfigDto.withSetting(key: String, setting: DeviceFieldSettingDto): DeviceFieldConfigDto = when (key) {
    "serialNumber" -> copy(serialNumber = setting)
    "hostname" -> copy(hostname = setting)
    "ip" -> copy(ip = setting)
    "macAddress" -> copy(macAddress = setting)
    "operatingSystem" -> copy(operatingSystem = setting)
    "ram" -> copy(ram = setting)
    "storage" -> copy(storage = setting)
    else -> this
}

data class DeviceTypeFormUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val code: String = "",
    val name: String = "",
    val prefix: String = "",
    val active: Boolean = true,
    val fieldConfig: DeviceFieldConfigDto = DeviceFieldConfigDto(),
    val saved: Boolean = false,
) {
    val isValid: Boolean get() = code.isNotBlank() && name.isNotBlank() && prefix.isNotBlank()
}
