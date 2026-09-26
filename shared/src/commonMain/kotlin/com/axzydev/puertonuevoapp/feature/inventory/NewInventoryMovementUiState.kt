package com.axzydev.puertonuevoapp.feature.inventory

import com.axzydev.puertonuevoapp.core.network.devices.DeviceDto
import com.axzydev.puertonuevoapp.core.network.locations.LocationDto

data class NewInventoryMovementUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val devices: List<DeviceDto> = emptyList(),
    val locations: List<LocationDto> = emptyList(),
    val deviceId: String = "",
    val type: String = "",
    val locationId: String = "",
    val notes: String = "",
    val loanedTo: String = "",
    val returnDate: String = "",
    val condition: String = "",
    val retirementReason: String = "",
    val poorConditionAction: String = "",
    val saved: Boolean = false,
) {
    val selectedDevice: DeviceDto? get() = devices.firstOrNull { it.id == deviceId }
    val requiresLocation: Boolean get() = type == "STOCK_IN" || type == "TRANSFER"
    val requiresLoan: Boolean get() = type == "LOAN"
    val requiresReturn: Boolean get() = type == "RETURN"
    val isPoorCondition: Boolean get() = condition == "POOR" || condition == "BROKEN"

    val isValid: Boolean
        get() = deviceId.isNotBlank() && type.isNotBlank() &&
            (!requiresLocation || locationId.isNotBlank()) &&
            (!requiresLoan || (loanedTo.isNotBlank() && returnDate.isNotBlank())) &&
            (!requiresReturn || condition.isNotBlank()) &&
            (!isPoorCondition || poorConditionAction.isNotBlank())
}

val movementTypeFormOptions: List<Pair<String, String>> = listOf(
    "STOCK_IN" to "Entrada (alta en inventario)",
    "STOCK_OUT" to "Salida (retirar de ubicación)",
    "TRANSFER" to "Traslado (mover a otra ubicación)",
    "RETIREMENT" to "Baja (dar de baja el dispositivo)",
    "LOAN" to "Asignado (equipo entregado a alguien)",
    "RETURN" to "Devolución (equipo regresado de asignación)",
)

val movementConditionCodes: List<String> = listOf("GOOD", "FAIR", "POOR", "BROKEN")
