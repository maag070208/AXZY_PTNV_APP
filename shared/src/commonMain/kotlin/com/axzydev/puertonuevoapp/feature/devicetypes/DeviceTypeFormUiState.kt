package com.axzydev.puertonuevoapp.feature.devicetypes

import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceFieldConfigDto
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceFieldSettingDto

data class DeviceTypeFieldDef(val key: String, val label: String)

val deviceTypeFieldDefs = listOf(
    DeviceTypeFieldDef("numeroSerie", "Número de serie"),
    DeviceTypeFieldDef("nombreEquipo", "Nombre de equipo"),
    DeviceTypeFieldDef("ip", "Dirección IP"),
    DeviceTypeFieldDef("macAddress", "MAC Address"),
    DeviceTypeFieldDef("sistemaOp", "Sistema operativo"),
    DeviceTypeFieldDef("ram", "RAM"),
    DeviceTypeFieldDef("almacenamiento", "Almacenamiento"),
)

fun DeviceFieldConfigDto.settingFor(key: String): DeviceFieldSettingDto = when (key) {
    "numeroSerie" -> numeroSerie
    "nombreEquipo" -> nombreEquipo
    "ip" -> ip
    "macAddress" -> macAddress
    "sistemaOp" -> sistemaOp
    "ram" -> ram
    "almacenamiento" -> almacenamiento
    else -> DeviceFieldSettingDto()
}

fun DeviceFieldConfigDto.withSetting(key: String, setting: DeviceFieldSettingDto): DeviceFieldConfigDto = when (key) {
    "numeroSerie" -> copy(numeroSerie = setting)
    "nombreEquipo" -> copy(nombreEquipo = setting)
    "ip" -> copy(ip = setting)
    "macAddress" -> copy(macAddress = setting)
    "sistemaOp" -> copy(sistemaOp = setting)
    "ram" -> copy(ram = setting)
    "almacenamiento" -> copy(almacenamiento = setting)
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
