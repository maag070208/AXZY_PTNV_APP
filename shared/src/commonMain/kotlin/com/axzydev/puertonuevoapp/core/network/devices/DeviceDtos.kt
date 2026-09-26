package com.axzydev.puertonuevoapp.core.network.devices

import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypeDto
import kotlinx.serialization.Serializable

@Serializable
data class DeviceLocationRefDto(
    val id: String,
    val name: String? = null,
    val description: String? = null,
)

@Serializable
data class DeviceHistoryEntryDto(
    val id: String,
    val deviceId: String,
    val type: String,
    val detail: String? = null,
    val author: com.axzydev.puertonuevoapp.core.network.common.UserRefDto? = null,
    val createdAt: String,
)

@Serializable
data class CustodyLetterSignerRefDto(
    val id: String,
    val name: String,
    val username: String,
)

@Serializable
data class DeviceCustodyLetterRefDto(
    val id: String,
    val consecutive: String,
    val date: String,
    val employeeNumber: String,
    val department: String,
    val deliveryBy: String,
    val returnDate: String? = null,
    val custodian: CustodyLetterSignerRefDto? = null,
    val supervisor: CustodyLetterSignerRefDto? = null,
    val location: DeviceLocationRefDto? = null,
)

@Serializable
data class DeviceCustodyLetterItemDto(
    val id: String,
    val custodyLetter: DeviceCustodyLetterRefDto,
)

@Serializable
data class DeviceBatchCountDto(
    val available: Int = 0,
    val assigned: Int = 0,
    val retirement: Int = 0,
)

@Serializable
data class DeviceDto(
    val id: String,
    val typeId: String,
    val type: DeviceTypeDto? = null,
    val assetTag: String,
    val description: String,
    val brand: String,
    val model: String,
    val serialNumber: String? = null,
    val hostname: String? = null,
    val area: String,
    val status: String,
    val locationId: String? = null,
    val location: DeviceLocationRefDto? = null,
    val custodyLetterItems: List<DeviceCustodyLetterItemDto> = emptyList(),
    val ip: String? = null,
    val macAddress: String? = null,
    val operatingSystem: String? = null,
    val ram: String? = null,
    val storage: String? = null,
    val batchId: String? = null,
    val batchSize: Int? = null,
    val batchCount: DeviceBatchCountDto? = null,
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
data class DeviceCreateInput(
    val typeId: String,
    val description: String,
    val brand: String,
    val model: String,
    val serialNumber: String? = null,
    val hostname: String? = null,
    val area: String? = null,
    val status: String? = null,
    val locationId: String? = null,
    val ip: String? = null,
    val macAddress: String? = null,
    val operatingSystem: String? = null,
    val ram: String? = null,
    val storage: String? = null,
)

@Serializable
data class DeviceUpdateInput(
    val typeId: String? = null,
    val description: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val hostname: String? = null,
    val area: String? = null,
    val status: String? = null,
    val locationId: String? = null,
    val ip: String? = null,
    val macAddress: String? = null,
    val operatingSystem: String? = null,
    val ram: String? = null,
    val storage: String? = null,
)

@Serializable
data class DeviceBatchUnitInput(
    val serialNumber: String? = null,
    val hostname: String? = null,
    val ip: String? = null,
    val macAddress: String? = null,
)

@Serializable
data class DeviceBatchInput(
    val typeId: String,
    val description: String,
    val brand: String,
    val model: String,
    val area: String? = null,
    val status: String? = null,
    val locationId: String? = null,
    val operatingSystem: String? = null,
    val ram: String? = null,
    val storage: String? = null,
    val units: List<DeviceBatchUnitInput>,
)

@Serializable
data class DeviceBatchUnitUpdateInput(
    val id: String,
    val serialNumber: String? = null,
    val hostname: String? = null,
    val ip: String? = null,
    val macAddress: String? = null,
    val area: String? = null,
)

@Serializable
data class DeviceBatchUpdateInput(
    val typeId: String? = null,
    val description: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val operatingSystem: String? = null,
    val ram: String? = null,
    val storage: String? = null,
    val units: List<DeviceBatchUnitUpdateInput>? = null,
)

@Serializable
data class AddUnitsInput(val quantity: Int)

@Serializable
data class AddUnitsResponseDto(
    val batchId: String,
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
data class DeviceAvailabilityCustodyLetterDto(
    val consecutive: String,
    val date: String,
    val employeeNumber: String,
    val department: String,
    val deliveryBy: String,
    val custodian: String? = null,
    val supervisor: String? = null,
    val locationName: String? = null,
)

@Serializable
data class DeviceAvailabilityRowDto(
    val id: String,
    val assetTag: String,
    val description: String,
    val brand: String,
    val model: String,
    val status: String,
    val area: String,
    val location: String? = null,
    val custodyLetter: DeviceAvailabilityCustodyLetterDto? = null,
)

@Serializable
data class DeviceAvailabilityGroupDto(
    val typeId: String,
    val code: String,
    val name: String,
    val total: Int,
    val available: Int,
    val assigned: Int,
    val devices: List<DeviceAvailabilityRowDto>,
)
