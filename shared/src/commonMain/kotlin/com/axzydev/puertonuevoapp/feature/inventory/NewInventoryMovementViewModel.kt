package com.axzydev.puertonuevoapp.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.devices.DevicesApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.inventory.InventoryApi
import com.axzydev.puertonuevoapp.core.network.inventory.MovementInput
import com.axzydev.puertonuevoapp.core.network.locations.LocationsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Registrar movimiento de inventario, incluyendo el flujo especial de
 * "malas condiciones" al hacer una devolución: da de baja el equipo (dos
 * movimientos: BAJA + DEVOLUCION) o solo registra la devolución con la
 * condición (paridad con NewInventoryMovementPage del web).
 */
class NewInventoryMovementViewModel(
    private val fixedDeviceId: String?,
    private val inventoryApi: InventoryApi,
    private val devicesApi: DevicesApi,
    private val locationsApi: LocationsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewInventoryMovementUiState())
    val uiState: StateFlow<NewInventoryMovementUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val result = runCatching {
                val devices = devicesApi.list().data
                val locations = locationsApi.list()
                devices to locations
            }
            result.fold(
                onSuccess = { (devices, locations) ->
                    _uiState.update {
                        val preselected = fixedDeviceId?.let { id -> devices.firstOrNull { it.id == id } }
                        it.copy(
                            loading = false,
                            devices = devices,
                            locations = locations,
                            deviceId = preselected?.id ?: it.deviceId,
                            locationId = preselected?.locationId ?: it.locationId,
                        )
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(loading = false, error = networkMessage(e)) } },
            )
        }
    }

    fun onDeviceChange(id: String) = _uiState.update { state ->
        state.copy(deviceId = id, locationId = state.devices.firstOrNull { it.id == id }?.locationId ?: "")
    }

    fun onTipoChange(value: String) = _uiState.update { it.copy(tipo = value, condicion = "", accionMalasCondiciones = "") }
    fun onLocationChange(value: String) = _uiState.update { it.copy(locationId = value) }
    fun onNotasChange(value: String) = _uiState.update { it.copy(notas = value) }
    fun onPrestadoAChange(value: String) = _uiState.update { it.copy(prestadoA = value) }
    fun onFechaRetornoChange(value: String) = _uiState.update { it.copy(fechaRetorno = value) }
    fun onCondicionChange(value: String) = _uiState.update { it.copy(condicion = value, accionMalasCondiciones = "") }
    fun onMotivoBajaChange(value: String) = _uiState.update { it.copy(motivoBaja = value) }
    fun onAccionMalasCondicionesChange(value: String) = _uiState.update { it.copy(accionMalasCondiciones = value) }

    fun submit() {
        val state = _uiState.value
        if (!state.isValid || state.saving) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                when {
                    state.isMalasCondiciones && state.accionMalasCondiciones == "BAJA" -> {
                        inventoryApi.registerMovement(
                            MovementInput(
                                deviceId = state.deviceId,
                                tipo = "BAJA",
                                notas = if (state.notas.isNotBlank()) "${state.notas} | Condición: ${state.condicion}" else "Condición: ${state.condicion}",
                                motivoBaja = "Equipo devuelto en condiciones ${state.condicion.lowercase()}",
                            ),
                        )
                        inventoryApi.registerMovement(
                            MovementInput(deviceId = state.deviceId, tipo = "DEVOLUCION", notas = state.notas.ifBlank { null }, condicion = state.condicion),
                        )
                    }
                    state.isMalasCondiciones && state.accionMalasCondiciones == "TICKET" -> {
                        inventoryApi.registerMovement(
                            MovementInput(deviceId = state.deviceId, tipo = "DEVOLUCION", notas = state.notas.ifBlank { null }, condicion = state.condicion),
                        )
                    }
                    else -> {
                        inventoryApi.registerMovement(
                            MovementInput(
                                deviceId = state.deviceId,
                                tipo = state.tipo,
                                locationId = if (state.requiresLocation) state.locationId else null,
                                notas = state.notas.trim().ifBlank { null },
                                prestadoA = if (state.requiresPrestamo) state.prestadoA.trim() else null,
                                fechaRetornoEsperado = if (state.requiresPrestamo) state.fechaRetorno.trim() else null,
                                condicion = if (state.requiresDevolucion) state.condicion else null,
                                motivoBaja = if (state.tipo == "BAJA") state.motivoBaja.trim().ifBlank { null } else null,
                            ),
                        )
                    }
                }
            }
            _uiState.update { it.copy(saving = false, error = result.exceptionOrNull()?.let(::networkMessage), saved = result.isSuccess) }
        }
    }
}
