package com.axzydev.puertonuevoapp.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.devices.DevicesApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.inventory.InventoryApi
import com.axzydev.puertonuevoapp.core.network.inventory.MovementInput
import com.axzydev.puertonuevoapp.core.network.inventory.UnitIdentityInput
import com.axzydev.puertonuevoapp.core.network.locations.LocationsApi
import com.axzydev.puertonuevoapp.core.util.conditionLabel
import com.axzydev.puertonuevoapp.core.util.newClientEventId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Registrar movimiento de inventario. Cubre:
 * - **Entrada (`STOCK_IN`)**: cantidad 1..5000 piezas NUEVAS e identidades
 *   opcionales por pieza (`MovementInput.quantity/units`), sin ubicación
 *   obligatoria, con `Idempotency-Key` (un doble toque o reintento no duplica
 *   la entrada). Es el mismo flujo que la web y que "Agregar unidades".
 * - El resto de tipos de una pieza, incluido el flujo especial de "malas
 *   condiciones" al devolver: da de baja el equipo (RETIREMENT + RETURN) o solo
 *   registra la devolución con la condición (paridad con NewMovementPage web).
 */
class NewInventoryMovementViewModel(
    private val fixedDeviceId: String?,
    private val inventoryApi: InventoryApi,
    private val devicesApi: DevicesApi,
    private val locationsApi: LocationsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewInventoryMovementUiState())
    val uiState: StateFlow<NewInventoryMovementUiState> = _uiState.asStateFlow()

    /** Id de la próxima fila de identificación (clave estable de la fila, no de la base). */
    private var nextIdentityRowId = 1L

    /**
     * Clave de idempotencia: base aleatoria por pantalla + huella del contenido.
     * Reintentar el MISMO envío manda la misma clave y el API no duplica la
     * entrada; si el contenido cambia, es otra petición. (Mismo patrón que
     * `DeviceStockInViewModel`.)
     */
    private val requestBase: String = newClientEventId().replace("-", "")
    private var lastFingerprint: String? = null
    private var lastRequestKey: String? = null

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

    // --- Entrada (STOCK_IN) -------------------------------------------------

    /** Cambia la cantidad de piezas nuevas (solo dígitos, tope 5000). */
    fun onQuantityChange(raw: String) {
        val digits = raw.filter { it.isDigit() }.take(5)
        val quantity = (digits.toIntOrNull() ?: 0).coerceAtMost(MAX_STOCK_IN_QUANTITY)
        val text = if (digits.isEmpty()) "" else quantity.toString()
        _uiState.update { state ->
            // Bajar la cantidad recorta identificaciones sobrantes; subirla no borra nada.
            val rows = if (digits.isNotEmpty() && quantity in 1 until state.identityRows.size) {
                state.identityRows.take(quantity)
            } else {
                state.identityRows
            }
            state.copy(quantityText = text, identityRows = rows)
        }
    }

    fun onEntryReasonChange(value: String) = _uiState.update { it.copy(entryReason = value) }

    /** Agrega una identificación opcional para la siguiente pieza (máximo: la cantidad). */
    fun addIdentityRow() {
        _uiState.update { state ->
            if (!state.canAddIdentity) state
            else state.copy(identityRows = state.identityRows + StockInIdentityRow(id = nextIdentityRowId++))
        }
    }

    fun removeIdentityRow(rowId: Long) = _uiState.update { state ->
        state.copy(identityRows = state.identityRows.filterNot { it.id == rowId })
    }

    fun onIdentityChange(rowId: Long, field: StockInIdentityField, value: String) = _uiState.update { state ->
        state.copy(identityRows = state.identityRows.map { row -> if (row.id == rowId) row.withIdentity(field, value) else row })
    }

    // --- Envío --------------------------------------------------------------

    fun submit() {
        val state = _uiState.value
        if (!state.isValid || state.saving) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                when {
                    state.isEntry -> registerEntry(state)

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

    /** `STOCK_IN` con cantidad e identidades opcionales; el `Idempotency-Key` evita duplicar. */
    private suspend fun registerEntry(state: NewInventoryMovementUiState) {
        val identities = if (state.usesUnitIdentity) {
            state.identityRows.take(state.quantity).map { it.toInput() }
        } else {
            emptyList()
        }
        inventoryApi.registerMovement(
            input = MovementInput(
                deviceId = state.deviceId,
                type = "STOCK_IN",
                quantity = state.quantity,
                units = identities,
                // La API guarda este campo como `reason` del movimiento: es el "motivo" de la entrada.
                retirementReason = state.entryReason.trim().ifBlank { null },
                notes = state.notes.trim().ifBlank { null },
            ),
            idempotencyKey = entryRequestKey(state, identities),
        )
    }

    /**
     * Misma clave para el mismo contenido (doble toque / reintento) y clave nueva
     * si algo cambia. La huella es FNV-1a de 32 bits en `[0-9a-f]`, formato que
     * valida el API (`[A-Za-z0-9_-]{8,100}`).
     */
    private fun entryRequestKey(state: NewInventoryMovementUiState, identities: List<UnitIdentityInput>): String {
        val payload = buildString {
            append(state.deviceId).append('|').append(state.quantity).append('|')
            append(state.entryReason.trim()).append('|').append(state.notes.trim())
            identities.forEach { identity ->
                append('|').append(identity.serialNumber ?: "")
                append('|').append(identity.macAddress ?: "")
                append('|').append(identity.ip ?: "")
                append('|').append(identity.hostname ?: "")
            }
        }
        val fingerprint = fingerprintOf(payload)
        if (fingerprint != lastFingerprint) {
            lastFingerprint = fingerprint
            lastRequestKey = "$requestBase-$fingerprint"
        }
        return lastRequestKey ?: "$requestBase-$fingerprint"
    }
}

/** Huella corta y estable del contenido (FNV-1a de 32 bits), solo `[0-9a-f]`. */
private fun fingerprintOf(text: String): String {
    var hash = 0x811c9dc5u
    for (char in text) {
        hash = hash xor char.code.toUInt()
        hash *= 0x01000193u
    }
    return hash.toString(16).padStart(8, '0')
}
