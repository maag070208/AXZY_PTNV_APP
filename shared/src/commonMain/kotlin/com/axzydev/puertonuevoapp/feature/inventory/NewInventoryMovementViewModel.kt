package com.axzydev.puertonuevoapp.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.devices.DevicesApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.inventory.InventoryApi
import com.axzydev.puertonuevoapp.core.network.inventory.MovementInput
import com.axzydev.puertonuevoapp.core.network.locations.LocationsApi
import com.axzydev.puertonuevoapp.core.util.conditionLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Registrar movimiento de inventario, incluyendo el flujo especial de
 * "malas condiciones" al hacer una devolución: da de baja el equipo (dos
 * movimientos: RETIREMENT + RETURN) o solo registra la devolución con la
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

    fun onTypeChange(value: String) = _uiState.update { it.copy(type = value, condition = "", poorConditionAction = "") }
    fun onLocationChange(value: String) = _uiState.update { it.copy(locationId = value) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }
    fun onLoanedToChange(value: String) = _uiState.update { it.copy(loanedTo = value) }
    fun onReturnDateChange(value: String) = _uiState.update { it.copy(returnDate = value) }
    fun onConditionChange(value: String) = _uiState.update { it.copy(condition = value, poorConditionAction = "") }
    fun onRetirementReasonChange(value: String) = _uiState.update { it.copy(retirementReason = value) }
    fun onPoorConditionActionChange(value: String) = _uiState.update { it.copy(poorConditionAction = value) }

    fun submit() {
        val state = _uiState.value
        if (!state.isValid || state.saving) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                when {
                    state.isPoorCondition && state.poorConditionAction == "RETIREMENT" -> {
                        inventoryApi.registerMovement(
                            MovementInput(
                                deviceId = state.deviceId,
                                type = "RETIREMENT",
                                notes = if (state.notes.isNotBlank()) "${state.notes} | Condición: ${conditionLabel(state.condition)}" else "Condición: ${conditionLabel(state.condition)}",
                                retirementReason = "Equipo devuelto en condiciones ${conditionLabel(state.condition).lowercase()}",
                            ),
                        )
                        inventoryApi.registerMovement(
                            MovementInput(deviceId = state.deviceId, type = "RETURN", notes = state.notes.ifBlank { null }, condition = state.condition),
                        )
                    }
                    state.isPoorCondition && state.poorConditionAction == "TICKET" -> {
                        inventoryApi.registerMovement(
                            MovementInput(deviceId = state.deviceId, type = "RETURN", notes = state.notes.ifBlank { null }, condition = state.condition),
                        )
                    }
                    else -> {
                        inventoryApi.registerMovement(
                            MovementInput(
                                deviceId = state.deviceId,
                                type = state.type,
                                locationId = if (state.requiresLocation) state.locationId else null,
                                notes = state.notes.trim().ifBlank { null },
                                loanedTo = if (state.requiresLoan) state.loanedTo.trim() else null,
                                expectedReturnDate = if (state.requiresLoan) state.returnDate.trim() else null,
                                condition = if (state.requiresReturn) state.condition else null,
                                retirementReason = if (state.type == "RETIREMENT") state.retirementReason.trim().ifBlank { null } else null,
                            ),
                        )
                    }
                }
            }
            _uiState.update { it.copy(saving = false, error = result.exceptionOrNull()?.let(::networkMessage), saved = result.isSuccess) }
        }
    }
}
