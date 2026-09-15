package com.axzydev.puertonuevoapp.core.network.inventory

import com.axzydev.puertonuevoapp.core.network.common.UserRefDto
import com.axzydev.puertonuevoapp.core.network.devices.DeviceDto
import com.axzydev.puertonuevoapp.core.network.locations.LocationDto
import kotlinx.serialization.Serializable

@Serializable
data class InventoryMovementDto(
    val id: String,
    val deviceId: String,
    val device: DeviceDto? = null,
    val locationId: String? = null,
    val location: LocationDto? = null,
    val tipo: String,
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
    val locations: List<LocationDto> = emptyList(),
    val stats: InventoryStatsDto = InventoryStatsDto(),
)

@Serializable
data class MovementInput(
    val deviceId: String,
    val tipo: String,
    val locationId: String? = null,
    val notas: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val prestamoId: String? = null,
    val prestadoA: String? = null,
    val fechaRetornoEsperado: String? = null,
    val condicion: String? = null,
    val motivoBaja: String? = null,
    val cartaId: String? = null,
)

val movementTypeOptions: List<Pair<String, String>> = listOf(
    "ENTRADA" to "Entrada",
    "SALIDA" to "Salida",
    "TRASLADO" to "Traslado",
    "BAJA" to "Baja",
    "PRESTAMO" to "Asignado",
    "DEVOLUCION" to "Devolución",
)

val condicionOptions: List<Pair<String, String>> = listOf(
    "BUENO" to "Bueno",
    "ACEPTABLE" to "Aceptable",
    "MALO" to "Malo",
    "ROTO" to "Roto",
)
