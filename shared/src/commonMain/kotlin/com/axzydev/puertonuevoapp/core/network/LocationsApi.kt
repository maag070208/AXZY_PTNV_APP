package com.axzydev.puertonuevoapp.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class LocationsApi(private val client: ApiClient) {
    suspend fun list(): List<LocationDto> = client.get("/locations")

    suspend fun get(id: String): LocationDetailDto = client.get("/locations/$id")

    suspend fun create(input: LocationCreateDto): LocationDto = client.post("/locations", input)

    suspend fun update(id: String, input: LocationCreateDto): LocationDto = client.put("/locations/$id", input)

    // El backend responde { success: true } (no relocaliza el LocationDto) —
    // ver DeleteSuccessResponse en CommonDtos.kt.
    suspend fun remove(id: String): DeleteSuccessResponse = client.delete("/locations/$id")
}

@Serializable
data class LocationCountDto(
    val devices: Int = 0,
)

@Serializable
data class LocationDto(
    val id: String,
    val lugar: String? = null,
    val subLugar: String? = null,
    val numero: String? = null,
    val descripcion: String? = null,
    @SerialName("_count") val count: LocationCountDto? = null,
)

@Serializable
data class LocationDetailDto(
    val id: String,
    val lugar: String? = null,
    val subLugar: String? = null,
    val numero: String? = null,
    val descripcion: String? = null,
    @SerialName("_count") val count: LocationCountDto? = null,
    val devices: List<DeviceDto> = emptyList(),
)

@Serializable
data class LocationCreateDto(
    val lugar: String? = null,
    val subLugar: String? = null,
    val numero: String? = null,
    val descripcion: String? = null,
)
