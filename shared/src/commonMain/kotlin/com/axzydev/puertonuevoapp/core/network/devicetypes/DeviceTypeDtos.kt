package com.axzydev.puertonuevoapp.core.network.devicetypes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceFieldSettingDto(
    val enabled: Boolean = true,
    val required: Boolean = false,
)

@Serializable
data class DeviceFieldConfigDto(
    val numeroSerie: DeviceFieldSettingDto = DeviceFieldSettingDto(),
    val nombreEquipo: DeviceFieldSettingDto = DeviceFieldSettingDto(),
    val ip: DeviceFieldSettingDto = DeviceFieldSettingDto(enabled = false),
    val macAddress: DeviceFieldSettingDto = DeviceFieldSettingDto(enabled = false),
    val sistemaOp: DeviceFieldSettingDto = DeviceFieldSettingDto(enabled = false),
    val ram: DeviceFieldSettingDto = DeviceFieldSettingDto(enabled = false),
    val almacenamiento: DeviceFieldSettingDto = DeviceFieldSettingDto(enabled = false),
)

@Serializable
data class DeviceTypeCountDto(val devices: Int = 0)

@Serializable
data class DeviceTypeDto(
    val id: String,
    val code: String,
    val name: String,
    val prefix: String,
    val contador: Int = 0,
    val active: Boolean = true,
    val fieldConfig: DeviceFieldConfigDto = DeviceFieldConfigDto(),
    @SerialName("_count") val count: DeviceTypeCountDto? = null,
)

@Serializable
data class DeviceTypeCreateInput(
    val code: String,
    val name: String,
    val prefix: String,
    val fieldConfig: DeviceFieldConfigDto? = null,
)

@Serializable
data class DeviceTypeUpdateInput(
    val name: String? = null,
    val active: Boolean? = null,
    val fieldConfig: DeviceFieldConfigDto? = null,
)

@Serializable
data class NextFolioDto(val siguiente: String)
