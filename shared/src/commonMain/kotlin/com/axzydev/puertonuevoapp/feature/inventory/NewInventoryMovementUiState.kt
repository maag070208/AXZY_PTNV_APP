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
    val tipo: String = "",
    val locationId: String = "",
    val notas: String = "",
    val prestadoA: String = "",
    val fechaRetorno: String = "",
    val condicion: String = "",
    val motivoBaja: String = "",
    val accionMalasCondiciones: String = "",
    val saved: Boolean = false,
) {
    val selectedDevice: DeviceDto? get() = devices.firstOrNull { it.id == deviceId }
    val requiresLocation: Boolean get() = tipo == "ENTRADA" || tipo == "TRASLADO"
    val requiresPrestamo: Boolean get() = tipo == "PRESTAMO"
    val requiresDevolucion: Boolean get() = tipo == "DEVOLUCION"
    val isMalasCondiciones: Boolean get() = condicion == "MALO" || condicion == "ROTO"

    val isValid: Boolean
        get() = deviceId.isNotBlank() && tipo.isNotBlank() &&
            (!requiresLocation || locationId.isNotBlank()) &&
            (!requiresPrestamo || (prestadoA.isNotBlank() && fechaRetorno.isNotBlank())) &&
            (!requiresDevolucion || condicion.isNotBlank()) &&
            (!isMalasCondiciones || accionMalasCondiciones.isNotBlank())
}

val movementTipoOptions: List<Pair<String, String>> = listOf(
    "ENTRADA" to "Entrada (alta en inventario)",
    "SALIDA" to "Salida (retirar de ubicación)",
    "TRASLADO" to "Traslado (mover a otra ubicación)",
    "BAJA" to "Baja (dar de baja el dispositivo)",
    "PRESTAMO" to "Asignado (equipo entregado a alguien)",
    "DEVOLUCION" to "Devolución (equipo regresado de asignación)",
)

val movementCondicionCodes: List<String> = listOf("BUENO", "ACEPTABLE", "MALO", "ROTO")
