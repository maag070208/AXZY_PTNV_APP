package com.axzydev.puertonuevoapp.core.network

import kotlinx.serialization.Serializable

@Serializable
data class DeviceTypeDto(
    val id: String,
    val code: String,
    val name: String,
    val prefix: String,
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
