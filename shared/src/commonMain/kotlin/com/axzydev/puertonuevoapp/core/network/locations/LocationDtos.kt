package com.axzydev.puertonuevoapp.core.network.locations

import com.axzydev.puertonuevoapp.core.network.common.UserRefDto
import com.axzydev.puertonuevoapp.core.network.devices.DeviceDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubLocationDto(
    val id: String,
    val locationId: String,
    val name: String,
    val number: String? = null,
    val active: Boolean = true,
    val createdAt: String = "",
)

@Serializable
data class LocationCountDto(
    val devices: Int = 0,
    val custodyLetters: Int = 0,
)

@Serializable
data class LocationCustodyLetterItemDto(
    val id: String,
    val assetTag: String,
    val description: String,
)

@Serializable
data class LocationCustodyLetterDto(
    val id: String,
    val consecutive: String,
    val date: String,
    val returnDate: String? = null,
    val returnCondition: String? = null,
    val custodian: UserRefDto? = null,
    val supervisor: UserRefDto? = null,
    val items: List<LocationCustodyLetterItemDto> = emptyList(),
)

@Serializable
data class LocationDto(
    val id: String,
    val name: String,
    val active: Boolean = true,
    val description: String? = null,
    val departmentId: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val subLocations: List<SubLocationDto> = emptyList(),
    @SerialName("_count") val count: LocationCountDto? = null,
    val devices: List<DeviceDto> = emptyList(),
    val custodyLetters: List<LocationCustodyLetterDto> = emptyList(),
)

@Serializable
data class LocationCreateInput(
    val name: String,
    val description: String? = null,
)

@Serializable
data class LocationUpdateInput(
    val name: String? = null,
    val description: String? = null,
    val active: Boolean? = null,
)

@Serializable
data class LocationDeleteResultDto(
    val soft: Boolean = false,
    val data: LocationDto? = null,
)

@Serializable
data class SubLocationCreateInput(val name: String)

@Serializable
data class SubLocationDeleteResultDto(
    val soft: Boolean = false,
    val data: SubLocationDto? = null,
)
