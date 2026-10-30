package com.axzydev.puertonuevoapp.feature.custodyletters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.custodyletters.CustodyLettersApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.inventory.InventoryApi
import com.axzydev.puertonuevoapp.core.network.inventory.ReturnCreateInput
import com.axzydev.puertonuevoapp.core.network.inventory.ReturnCreateItemInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Detalle de una carta responsiva (préstamo en el API) con devolución
 * **parcial por pieza y condición**.
 *
 * La carta se sigue leyendo con [CustodyLettersApi] (el modelo documentado de
 * la app) y el detalle del préstamo con [InventoryApi.loan] para tener los
 * renglones, sus piezas (`units[].returned`) y las devoluciones ya registradas.
 * Al devolver se manda `POST /inventory/returns` con `unitIds` (o `quantity`
 * cuando el renglón no tiene piezas ligadas) y la condición de cada renglón.
 *
 * **No se ofrece "deshacer devolución"**: el API no tiene endpoint para eso
 * (la implementación anterior de la app respondía 501 y aquí se elimina en
 * lugar de inventarlo).
 */
class CustodyLetterDetailViewModel(
    private val custodyLetterId: String,
    private val custodyLettersApi: CustodyLettersApi,
    private val inventoryApi: InventoryApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustodyLetterDetailUiState())
    val uiState: StateFlow<CustodyLetterDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val letter = runCatching { custodyLettersApi.get(custodyLetterId) }
            val loan = runCatching { inventoryApi.loan(custodyLetterId) }
            _uiState.update {
                it.copy(
                    loading = false,
                    custodyLetter = letter.getOrNull() ?: it.custodyLetter,
                    loan = loan.getOrNull() ?: it.loan,
                    error = (letter.exceptionOrNull() ?: loan.exceptionOrNull())?.let(::networkMessage),
                )
            }
        }
    }

    // -----------------------------------------------------------------------
    // Eliminar (cancelar) la carta
    // -----------------------------------------------------------------------

    fun requestDelete() = _uiState.update { it.copy(showDeleteConfirm = true) }

    fun dismissDelete() = _uiState.update { it.copy(showDeleteConfirm = false) }

    fun confirmDelete() {
        _uiState.update { it.copy(actionSaving = true) }
        viewModelScope.launch {
            val result = runCatching { custodyLettersApi.remove(custodyLetterId) }
            _uiState.update {
                it.copy(
                    actionSaving = false,
                    showDeleteConfirm = false,
                    deleted = result.isSuccess,
                    actionError = result.exceptionOrNull()?.let(::networkMessage) ?: it.actionError,
                )
            }
        }
    }

    // -----------------------------------------------------------------------
    // Devolución parcial por pieza
    // -----------------------------------------------------------------------

    /** Abre el modal de devolución con una fila por pieza pendiente (o por renglón sin piezas). */
    fun openReturn() {
        val loan = _uiState.value.loan ?: return
        val picks = loan.items.filter { it.pending > 0 }.flatMap { item ->
            val deviceName = item.device?.name.orEmpty()
            val pendingUnits = item.units
                .filter { !it.returned }
                .mapNotNull { link ->
                    val unit = link.deviceUnit ?: return@mapNotNull null
                    ReturnPickRow(
                        loanItemId = item.id,
                        deviceName = deviceName,
                        unitId = unit.id,
                        assetTag = unit.assetTag,
                        serialNumber = unit.serialNumber,
                        pending = item.pending,
                        selected = true,
                    )
                }
            if (pendingUnits.isNotEmpty()) {
                pendingUnits
            } else {
                // Préstamo sin piezas ligadas (datos migrados): se devuelve por cantidad.
                listOf(
                    ReturnPickRow(
                        loanItemId = item.id,
                        deviceName = deviceName,
                        pending = item.pending,
                        quantityText = item.pending.toString(),
                    )
                )
            }
        }
        _uiState.update {
            it.copy(
                showReturnModal = true,
                returnPicks = picks,
                returnNotes = "",
                validationError = null,
                successMessage = null,
            )
        }
    }

    fun dismissReturn() = _uiState.update { it.copy(showReturnModal = false, validationError = null) }

    fun togglePick(index: Int) = updatePick(index) { it.copy(selected = !it.selected) }

    fun setCondition(index: Int, condition: String) = updatePick(index) { it.copy(condition = condition) }

    fun setNotes(index: Int, notes: String) = updatePick(index) { it.copy(notes = notes) }

    /** Solo dígitos; el tope (piezas pendientes) se aplica al confirmar. */
    fun setQuantity(index: Int, value: String) = updatePick(index) {
        it.copy(quantityText = value.filter { ch -> ch.isDigit() }.take(4))
    }

    fun setReturnNotes(value: String) = _uiState.update { it.copy(returnNotes = value) }

    private fun updatePick(index: Int, transform: (ReturnPickRow) -> ReturnPickRow) {
        _uiState.update { state ->
            if (index !in state.returnPicks.indices) return@update state
            state.copy(
                returnPicks = state.returnPicks.mapIndexed { i, row -> if (i == index) transform(row) else row },
                validationError = null,
                successMessage = null,
            )
        }
    }

    /**
     * Registra la devolución con lo marcado: piezas exactas agrupadas por
     * condición/nota, o cantidad en los renglones sin piezas. ROTO/POOR exigen
     * explicar qué pasó (mínimo 3 caracteres), como en la web.
     */
    fun confirmReturn() {
        val state = _uiState.value
        val loan = state.loan ?: return
        val chosen = state.returnPicks.filter { it.included }
        if (chosen.isEmpty()) {
            _uiState.update { it.copy(validationError = "Marca al menos una pieza o cantidad a devolver.") }
            return
        }
        val missingNotes = chosen.any { it.needsNotes && it.notes.trim().length < 3 }
        if (missingNotes) {
            _uiState.update {
                it.copy(validationError = "Escribe qué pasó con las piezas malas o rotas (mínimo 3 caracteres).")
            }
            return
        }

        val items = buildList {
            chosen
                .filter { !it.isQuantityRow }
                .groupBy { Triple(it.loanItemId, it.condition, it.notes.trim()) }
                .forEach { (key, rows) ->
                    add(
                        ReturnCreateItemInput(
                            loanItemId = key.first,
                            unitIds = rows.mapNotNull { it.unitId },
                            quantity = rows.size,
                            condition = key.second,
                            notes = key.third.ifBlank { null },
                        )
                    )
                }
            chosen
                .filter { it.isQuantityRow }
                .forEach { row ->
                    add(
                        ReturnCreateItemInput(
                            loanItemId = row.loanItemId,
                            quantity = row.quantity.coerceAtMost(row.pending),
                            condition = row.condition,
                            notes = row.notes.trim().ifBlank { null },
                        )
                    )
                }
        }
        if (items.isEmpty()) {
            _uiState.update { it.copy(validationError = "No hay piezas por devolver.") }
            return
        }

        val pieces = state.pickedCount
        _uiState.update { it.copy(actionSaving = true, validationError = null) }
        viewModelScope.launch {
            val result = runCatching {
                inventoryApi.createReturn(
                    input = ReturnCreateInput(
                        loanId = loan.id,
                        custodianId = loan.custodianId,
                        notes = state.returnNotes.trim().ifBlank { null },
                        items = items,
                    ),
                    idempotencyKey = returnRequestKey(loan.id, chosen),
                )
            }
            _uiState.update { current ->
                result.fold(
                    onSuccess = {
                        current.copy(
                            actionSaving = false,
                            showReturnModal = false,
                            returnPicks = emptyList(),
                            successMessage = "Devolución registrada: $pieces pieza(s).",
                            actionError = null,
                        )
                    },
                    onFailure = { error -> current.copy(actionSaving = false, actionError = networkMessage(error)) },
                )
            }
            if (result.isSuccess) load()
        }
    }

    fun dismissActionError() = _uiState.update { it.copy(actionError = null) }

    fun clearSuccessMessage() = _uiState.update { it.copy(successMessage = null) }

    /**
     * Clave estable de la devolución (préstamo + piezas/cantidades + condición):
     * un doble envío no registra dos devoluciones.
     */
    private fun returnRequestKey(loanId: String, chosen: List<ReturnPickRow>): String {
        val signature = chosen
            .map { row ->
                if (row.isQuantityRow) {
                    "${row.loanItemId}:${row.quantity}:${row.condition}"
                } else {
                    "${row.unitId}:${row.condition}"
                }
            }
            .sorted()
            .joinToString("|")
        return "app-return-${("$loanId|$signature").hashCode().toUInt().toString(16)}"
    }
}
