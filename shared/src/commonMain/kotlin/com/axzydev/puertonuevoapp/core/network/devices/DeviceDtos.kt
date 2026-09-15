package com.axzydev.puertonuevoapp.core.network.devices

import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypeDto
import kotlinx.serialization.Serializable

@Serializable
data class DeviceLocationRefDto(
    val id: String,
    val lugar: String? = null,
    val descripcion: String? = null,
)

@Serializable
data class DeviceHistoryEntryDto(
    val id: String,
    val deviceId: String,
    val type: String,
    val detail: String? = null,
    val autor: com.axzydev.puertonuevoapp.core.network.common.UserRefDto? = null,
    val createdAt: String,
)

@Serializable
data class CartaFirmanteRefDto(
    val id: String,
    val name: String,
    val username: String,
)

@Serializable
data class DeviceCartaRefDto(
    val id: String,
    val consecutive: String,
    val fecha: String,
    val numeroEmpleado: String,
    val departamento: String,
    val deliveryBy: String,
    val returnDate: String? = null,
    val responsable: CartaFirmanteRefDto? = null,
    val encargado: CartaFirmanteRefDto? = null,
    val ubicacion: DeviceLocationRefDto? = null,
)

@Serializable
data class DeviceCartaItemDto(
    val id: String,
    val carta: DeviceCartaRefDto,
)

@Serializable
data class DeviceLoteCountDto(
    val disponible: Int = 0,
    val asignado: Int = 0,
    val baja: Int = 0,
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
    val location: DeviceLocationRefDto? = null,
    val cartaItems: List<DeviceCartaItemDto> = emptyList(),
    val ip: String? = null,
    val macAddress: String? = null,
    val sistemaOp: String? = null,
    val ram: String? = null,
    val almacenamiento: String? = null,
    val loteId: String? = null,
    val loteSize: Int? = null,
    val loteCount: DeviceLoteCountDto? = null,
    val history: List<DeviceHistoryEntryDto> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class DeviceListResponseDto(
    val data: List<DeviceDto>,
    val total: Int = 0,
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
data class DeviceCreateInput(
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
data class DeviceUpdateInput(
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

@Serializable
data class DeviceBatchUnitInput(
    val numeroSerie: String? = null,
    val nombreEquipo: String? = null,
    val ip: String? = null,
    val macAddress: String? = null,
)

@Serializable
data class DeviceBatchInput(
    val typeId: String,
    val descripcion: String,
    val marca: String,
    val modelo: String,
    val area: String? = null,
    val estado: String? = null,
    val locationId: String? = null,
    val sistemaOp: String? = null,
    val ram: String? = null,
    val almacenamiento: String? = null,
    val units: List<DeviceBatchUnitInput>,
)

@Serializable
data class DeviceLoteUnitUpdateInput(
    val id: String,
    val numeroSerie: String? = null,
    val nombreEquipo: String? = null,
    val ip: String? = null,
    val macAddress: String? = null,
    val area: String? = null,
)

@Serializable
data class DeviceLoteUpdateInput(
    val typeId: String? = null,
    val descripcion: String? = null,
    val marca: String? = null,
    val modelo: String? = null,
    val sistemaOp: String? = null,
    val ram: String? = null,
    val almacenamiento: String? = null,
    val units: List<DeviceLoteUnitUpdateInput>? = null,
)

@Serializable
data class AddUnitsInput(val cantidad: Int)

@Serializable
data class AddUnitsResponseDto(
    val loteId: String,
    val data: List<DeviceDto>,
    val total: Int = 0,
)

@Serializable
data class DeviceRemoveResponseDto(
    val soft: Boolean = false,
    val forced: Boolean = false,
    val data: DeviceDto? = null,
)

@Serializable
data class DeviceAvailabilityCartaDto(
    val consecutive: String,
    val fecha: String,
    val numeroEmpleado: String,
    val departamento: String,
    val deliveryBy: String,
    val responsable: String? = null,
    val encargado: String? = null,
    val lugar: String? = null,
)

@Serializable
data class DeviceAvailabilityRowDto(
    val id: String,
    val controlActivos: String,
    val descripcion: String,
    val marca: String,
    val modelo: String,
    val estado: String,
    val area: String,
    val ubicacion: String? = null,
    val carta: DeviceAvailabilityCartaDto? = null,
)

@Serializable
data class DeviceAvailabilityGroupDto(
    val typeId: String,
    val code: String,
    val name: String,
    val total: Int,
    val disponible: Int,
    val asignado: Int,
    val devices: List<DeviceAvailabilityRowDto>,
)
