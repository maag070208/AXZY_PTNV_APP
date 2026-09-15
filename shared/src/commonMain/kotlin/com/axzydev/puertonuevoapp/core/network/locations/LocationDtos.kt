package com.axzydev.puertonuevoapp.core.network.locations

import com.axzydev.puertonuevoapp.core.network.common.UserRefDto
import com.axzydev.puertonuevoapp.core.network.devices.DeviceDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SublugarDto(
    val id: String,
    val locationId: String,
    val name: String,
    val numero: String? = null,
    val active: Boolean = true,
    val createdAt: String = "",
)

@Serializable
data class LocationCountDto(
    val devices: Int = 0,
    val cartas: Int = 0,
)

@Serializable
data class LocationCartaItemDto(
    val id: String,
    val controlActivos: String,
    val descripcion: String,
)

@Serializable
data class LocationCartaDto(
    val id: String,
    val consecutive: String,
    val fecha: String,
    val returnDate: String? = null,
    val returnCondition: String? = null,
    val responsable: UserRefDto? = null,
    val encargado: UserRefDto? = null,
    val items: List<LocationCartaItemDto> = emptyList(),
)

@Serializable
data class LocationDto(
    val id: String,
    val lugar: String,
    val active: Boolean = true,
    val descripcion: String? = null,
    val departmentId: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val sublugares: List<SublugarDto> = emptyList(),
    @SerialName("_count") val count: LocationCountDto? = null,
    val devices: List<DeviceDto> = emptyList(),
    val cartas: List<LocationCartaDto> = emptyList(),
)

@Serializable
data class LocationCreateInput(
    val lugar: String,
    val descripcion: String? = null,
)

@Serializable
data class LocationUpdateInput(
    val lugar: String? = null,
    val descripcion: String? = null,
    val active: Boolean? = null,
)

@Serializable
data class LocationDeleteResultDto(
    val soft: Boolean = false,
    val data: LocationDto? = null,
)

@Serializable
data class SublugarCreateInput(val name: String)

@Serializable
data class SublugarDeleteResultDto(
    val soft: Boolean = false,
    val data: SublugarDto? = null,
)
