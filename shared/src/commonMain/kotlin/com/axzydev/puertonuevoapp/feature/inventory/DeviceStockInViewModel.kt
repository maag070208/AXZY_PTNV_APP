package com.axzydev.puertonuevoapp.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.devices.DevicesApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.inventory.InventoryApi
import com.axzydev.puertonuevoapp.core.network.inventory.MovementInput
import com.axzydev.puertonuevoapp.core.util.newClientEventId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * "Agregar unidades" a un dispositivo que ya existe: registra un `STOCK_IN` con
 * N piezas NUEVAS y, opcionalmente, la identificación de cada una en el orden
 * en que se crean (serie/MAC/IP/hostname según el tipo). Es el mismo flujo de la
 * web («Agregar unidades» → Movimientos → Entrada con cantidad).
 */
class DeviceStockInViewModel(
    private val deviceId: String,
    private val devicesApi: DevicesApi,
    private val inventoryApi: InventoryApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceStockInUiState())
    val uiState: StateFlow<DeviceStockInUiState> = _uiState.asStateFlow()

    /** Los renglones de identidad reciben un id nuevo cuando aparecen por cantidad. */
    private var nextRowId = 1L

    /**
     * Clave de idempotencia: base aleatoria por pantalla + huella del contenido.
     * Reintentar el MISMO envío (doble toque, corte de red) manda la misma clave
     * y el API no duplica la entrada; si el contenido cambia, es otra petición.
     */
    private val requestBase: String = newClientEventId().replace("-", "")
    private var lastFingerprint: String? = null
    private var lastRequestKey: String? = null

    /** La pantalla salió de composición: al volver hay que limpiar el resultado y refrescar. */
    private var resetOnReturnArmed = false

    init {
        load()
    }

    /** Al volver a la pantalla, limpia el éxito anterior y recarga dispositivo/existencias. */
    fun onEnterScreen() {
        if (!resetOnReturnArmed) return
        resetOnReturnArmed = false
        _uiState.update { it.copy(result = null, submitError = null, saving = false) }
        load()
    }

    /** La pantalla salió de composición (botón atrás o "Volver al dispositivo"). */
    fun onLeaveScreen() {
        resetOnReturnArmed = true
    }

    fun load() {
        _uiState.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            val result = runCatching {
                val device = devicesApi.get(deviceId)
                // Las existencias son informativas: si fallan, la entrada se puede registrar igual.
                val stock = runCatching { inventoryApi.stock(deviceId) }.getOrNull()
                device to stock
            }
            result.fold(
                onSuccess = { (device, stock) ->
                    _uiState.update { it.copy(loading = false, device = device, stock = stock) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(loading = false, loadError = networkMessage(e)) }
                },
            )
        }
    }

    /**
     * Cambia la cantidad de piezas. Los renglones ya capturados se conservan
     * (bajar la cantidad recorta el final; subirla agrega renglones vacíos).
     */
    fun onQuantityChange(raw: String) {
        val digits = raw.filter { it.isDigit() }.take(5)
        val quantity = (digits.toIntOrNull() ?: 0).coerceAtMost(MAX_STOCK_IN_QUANTITY)
        val text = if (digits.isEmpty()) "" else quantity.toString()
        val rows = resizedRows(_uiState.value.rows, quantity)
        _uiState.update {
            it.copy(
                quantityText = text,
                rows = rows,
                expandedRowIds = if (it.expandedRowIds.isEmpty()) {
                    rows.firstOrNull()?.let { row -> setOf(row.id) } ?: emptySet()
                } else {
                    it.expandedRowIds
                },
            )
        }
    }

    fun onReasonChange(value: String) = _uiState.update { it.copy(reason = value) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }

    fun onIdentityChange(rowId: Long, field: StockInIdentityField, value: String) = _uiState.update {
        it.copy(rows = it.rows.map { row -> if (row.id == rowId) row.withIdentity(field, value) else row })
    }

    fun toggleRow(rowId: Long) = _uiState.update {
        it.copy(
            expandedRowIds = if (rowId in it.expandedRowIds) {
                it.expandedRowIds - rowId
            } else {
                it.expandedRowIds + rowId
            },
        )
    }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit || state.result != null) return

        val quantity = state.quantity
        // Sin campos de identidad en el tipo no se mandan miles de objetos vacíos.
        val identities = if (state.usesUnitIdentity) state.rows.take(quantity).map { it.toInput() } else emptyList()
        val reason = state.reason.trim().ifBlank { null }
        val notes = state.notes.trim().ifBlank { null }

        // Huella del contenido: mismo envío → misma clave; contenido distinto → clave nueva.
        val payload = buildString {
            append(deviceId).append('|').append(quantity).append('|')
            append(reason ?: "").append('|').append(notes ?: "")
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
        val requestKey = lastRequestKey

        _uiState.update { it.copy(saving = true, submitError = null) }
        viewModelScope.launch {
            val result = runCatching {
                inventoryApi.registerMovement(
                    input = MovementInput(
                        deviceId = deviceId,
                        type = "STOCK_IN",
                        quantity = quantity,
                        units = identities,
                        // La API guarda este campo como `reason` del movimiento: es el "motivo" de la entrada.
                        retirementReason = reason,
                        notes = notes,
                    ),
                    idempotencyKey = requestKey,
                )
            }
            result.fold(
                onSuccess = {
                    _uiState.update { current ->
                        current.copy(
                            saving = false,
                            result = StockInResult(
                                quantity = quantity,
                                firstFolio = current.firstFolio,
                                lastFolio = current.lastFolio,
                                reason = reason ?: "",
                            ),
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(saving = false, submitError = networkMessage(e)) }
                },
            )
        }
    }

    /**
     * Ajusta la lista de renglones a [quantity] CONSERVANDO lo ya capturado: los
     * existentes mantienen su id y sus datos; los nuevos nacen vacíos con un id
     * propio.
     */
    private fun resizedRows(current: List<StockInIdentityRow>, quantity: Int): List<StockInIdentityRow> {
        if (quantity <= 0) return emptyList()
        return List(quantity) { index -> current.getOrNull(index) ?: StockInIdentityRow(id = nextRowId++) }
    }
}

/**
 * Huella corta y estable del contenido (FNV-1a de 32 bits, la misma idea que
 * `useRequestKey` de la web). El resultado solo usa `[0-9a-f]`, así que la clave
 * final cumple el formato que valida el API (`[A-Za-z0-9_-]{8,100}`).
 */
private fun fingerprintOf(text: String): String {
    var hash = 0x811c9dc5u
    for (char in text) {
        hash = hash xor char.code.toUInt()
        hash *= 0x01000193u
    }
    return hash.toString(16).padStart(8, '0')
}
