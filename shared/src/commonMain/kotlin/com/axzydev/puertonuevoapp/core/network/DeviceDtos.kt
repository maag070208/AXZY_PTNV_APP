package com.axzydev.puertonuevoapp.core.network

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
data class DeviceTypeCountDto(
    val devices: Int = 0,
)

@Serializable
data class DeviceTypeDto(
    val id: String,
    val code: String,
    val name: String,
    val prefix: String,
    val contador: Int = 0,
    val cartaContador: Int = 0,
    val active: Boolean = true,
    val fieldConfig: DeviceFieldConfigDto = DeviceFieldConfigDto(),
    @SerialName("_count") val count: DeviceTypeCountDto? = null,
)

@Serializable
data class DeviceTypeCreateDto(
    val code: String,
    val name: String,
    val prefix: String,
    val fieldConfig: DeviceFieldConfigDto? = null,
)

@Serializable
data class DeviceTypeUpdateDto(
    val name: String? = null,
    val prefix: String? = null,
    val active: Boolean? = null,
    val fieldConfig: DeviceFieldConfigDto? = null,
)

@Serializable
data class LocationRefDto(
    val id: String,
    val lugar: String? = null,
    val subLugar: String? = null,
    val numero: String? = null,
    val descripcion: String? = null,
)

@Serializable
data class DeviceHistoryEntryDto(
    val id: String,
    val deviceId: String,
    val type: String,
    val detail: String? = null,
    val autor: UserRefDto? = null,
    val createdAt: String,
)

@Serializable
data class DeviceDto(
    val id: String,
    val typeId: String,
    val type: DeviceTypeDto? = null,
    val controlActivos: String,
    val descripcion: String,
    val marca: String,
    val modelo: String,
    val numeroSerie: String? = null,
    val nombreEquipo: String? = null,
    val area: String,
    val estado: String,
    val locationId: String? = null,
    val location: LocationRefDto? = null,
    val ip: String? = null,
    val macAddress: String? = null,
    val sistemaOp: String? = null,
    val ram: String? = null,
    val almacenamiento: String? = null,
    val loteSize: Int? = null,
    val history: List<DeviceHistoryEntryDto> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class DeviceListResponseDto(
    val data: List<DeviceDto>,
    val total: Int,
)

@Serializable
data class DeviceSummaryDto(
    val total: Int,
    val disponible: Int,
    val asignado: Int,
    val baja: Int,
    val tipos: Int,
)


@Serializable
data class DeviceCreateDto(
    val typeId: String,
    val descripcion: String,
    val marca: String,
    val modelo: String,
    val numeroSerie: String? = null,
    val nombreEquipo: String? = null,
    val area: String? = null,
    val estado: String? = null,
    val locationId: String? = null,
    val ip: String? = null,
    val macAddress: String? = null,
    val sistemaOp: String? = null,
    val ram: String? = null,
    val almacenamiento: String? = null,
)

@Serializable
data class DeviceUpdateDto(
    val typeId: String? = null,
    val descripcion: String? = null,
    val marca: String? = null,
    val modelo: String? = null,
    val numeroSerie: String? = null,
    val nombreEquipo: String? = null,
    val area: String? = null,
    val estado: String? = null,
    val locationId: String? = null,
    val ip: String? = null,
    val macAddress: String? = null,
    val sistemaOp: String? = null,
    val ram: String? = null,
    val almacenamiento: String? = null,
)
