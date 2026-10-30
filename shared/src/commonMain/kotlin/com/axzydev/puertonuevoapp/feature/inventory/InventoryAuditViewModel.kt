package com.axzydev.puertonuevoapp.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.inventory.InventoryApi
import com.axzydev.puertonuevoapp.core.network.inventory.InventoryAuditRowDto
import com.axzydev.puertonuevoapp.core.network.inventory.ResolveMismatchInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Auditor de consistencia del inventario (`GET /inventory/audit`): corre las 8
 * reglas (unidades, préstamos y kardex) y resuelve los descuadres de
 * movimientos desde `POST /inventory/audit/mismatches/resolve`.
 */
class InventoryAuditViewModel(
    private val inventoryApi: InventoryApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryAuditUiState())
    val uiState: StateFlow<InventoryAuditUiState> = _uiState.asStateFlow()

    init {
        run()
    }

    /** Corre el auditor (también sirve de refresco: conserva el resultado anterior). */
    fun run() {
        val firstLoad = _uiState.value.audit == null
        _uiState.update {
            it.copy(loading = firstLoad, refreshing = !firstLoad, error = null, actionError = null)
        }
        viewModelScope.launch {
            val result = runCatching { inventoryApi.audit() }
            _uiState.update {
                result.fold(
                    onSuccess = { audit -> it.copy(loading = false, refreshing = false, audit = audit, error = null) },
                    onFailure = { error -> it.copy(loading = false, refreshing = false, error = networkMessage(error)) },
                )
            }
        }
    }

    /**
     * Aplica una salida del descuadre:
     * - `link`: liga las piezas que ya existen en el dispositivo (no mueve existencias).
     * - `quantity`: deja la cantidad del renglón en lo que hay (MUEVE el kardex).
     * - `review`: solo deja constancia de que se revisó.
     */
    fun resolve(row: InventoryAuditRowDto, mode: String) {
        val movementItemId = row.movementItemId ?: return
        if (_uiState.value.actionSaving) return
        _uiState.update { it.copy(actionSaving = true, actionError = null, resolvedMessage = null) }
        viewModelScope.launch {
            val result = runCatching {
                inventoryApi.resolveMismatch(ResolveMismatchInput(movementItemId = movementItemId, mode = mode))
            }
            _uiState.update { current ->
                result.fold(
                    onSuccess = {
                        current.copy(
                            actionSaving = false,
                            actionError = null,
                            resolvedMessage = when (mode) {
                                "link" -> "Se registraron las piezas que faltaban en el movimiento."
                                "quantity" -> "El movimiento quedó con ${row.linkedAfter ?: row.linked ?: row.quantity ?: 0} piezas registradas."
                                else -> "Quedó anotado que el movimiento se revisó."
                            },
                        )
                    },
                    onFailure = { error -> current.copy(actionSaving = false, actionError = networkMessage(error)) },
                )
            }
            if (result.isSuccess) run()
        }
    }

    fun dismissActionError() = _uiState.update { it.copy(actionError = null) }

    fun clearResolvedMessage() = _uiState.update { it.copy(resolvedMessage = null) }
}
