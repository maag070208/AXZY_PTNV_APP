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
    val type: String,
    val notes: String? = null,
    val userId: String,
    val user: UserRefDto? = null,
    val loanId: String? = null,
    val loanedTo: String? = null,
    val expectedReturnDate: String? = null,
    val condition: String? = null,
    val retirementReason: String? = null,
    val createdAt: String,
)

@Serializable
data class StockLedgerDto(
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
    val type: String,
    val locationId: String? = null,
    val notes: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val loanId: String? = null,
    val loanedTo: String? = null,
    val expectedReturnDate: String? = null,
    val condition: String? = null,
    val retirementReason: String? = null,
    val custodyLetterId: String? = null,
)

val movementTypeOptions: List<Pair<String, String>> = listOf(
    "STOCK_IN" to "Entrada",
    "STOCK_OUT" to "Salida",
    "TRANSFER" to "Traslado",
    "RETIREMENT" to "Baja",
    "LOAN" to "Asignado",
    "RETURN" to "Devolución",
)

val conditionOptions: List<Pair<String, String>> = listOf(
    "GOOD" to "Bueno",
    "FAIR" to "Aceptable",
    "POOR" to "Malo",
    "BROKEN" to "Roto",
)
