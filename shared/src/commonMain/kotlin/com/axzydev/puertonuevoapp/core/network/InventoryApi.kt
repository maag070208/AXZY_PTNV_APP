package com.axzydev.puertonuevoapp.core.network

import io.ktor.http.encodeURLParameter
import kotlinx.serialization.Serializable

class InventoryApi(private val client: ApiClient) {
    suspend fun movements(
        deviceId: String? = null,
        locationId: String? = null,
        start: String? = null,
        end: String? = null,
    ): List<InventoryMovementDto> {
        val params = buildList {
            deviceId?.takeIf { it.isNotBlank() }?.let { add("deviceId=$it") }
            locationId?.takeIf { it.isNotBlank() }?.let { add("locationId=$it") }
            start?.takeIf { it.isNotBlank() }?.let { add("start=${it.encodeURLParameter()}") }
            end?.takeIf { it.isNotBlank() }?.let { add("end=${it.encodeURLParameter()}") }
        }
        val qs = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
        return client.get("/inventory/movements$qs")
    }

    suspend fun kardex(deviceId: String): KardexDto = client.get("/inventory/kardex/$deviceId")

    suspend fun registerMovement(input: RegisterMovementDto): InventoryMovementDto =
        client.post("/inventory/movements", input)

    suspend fun summary(): InventorySummaryDto = client.get("/inventory/summary")
}

@Serializable
data class InventoryMovementDto(
    val id: String,
    val deviceId: String,
    val device: DeviceDto? = null,
    val tipo: String,
    val locationId: String? = null,
    val location: LocationDto? = null,
    val notas: String? = null,
    val userId: String,
    val user: UserRefDto? = null,
    val prestamoId: String? = null,
    val prestadoA: String? = null,
    val fechaRetornoEsperado: String? = null,
    val condicion: String? = null,
    val motivoBaja: String? = null,
    val createdAt: String,
)

@Serializable
data class KardexDto(
    val device: DeviceDto,
    val movements: List<InventoryMovementDto> = emptyList(),
)

@Serializable
data class InventoryStatsDto(
    val totalDevices: Int = 0,
    val locatedDevices: Int = 0,
    val unlocatedDevices: Int = 0,
)

@Serializable
data class InventorySummaryDto(
    val locations: List<LocationDetailDto> = emptyList(),
    val stats: InventoryStatsDto = InventoryStatsDto(),
)

@Serializable
data class RegisterMovementDto(
    val deviceId: String,
    val tipo: String,
    val locationId: String? = null,
    val notas: String? = null,
    val prestamoId: String? = null,
    val prestadoA: String? = null,
    val fechaRetornoEsperado: String? = null,
    val condicion: String? = null,
    val motivoBaja: String? = null,
)
