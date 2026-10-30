package com.axzydev.puertonuevoapp.feature.custodyletters

import com.axzydev.puertonuevoapp.core.network.custodyletters.CustodyLetterDto
import com.axzydev.puertonuevoapp.core.network.inventory.LoanDetailDto

/** Condiciones válidas del API para una pieza devuelta (`Condition`). */
val returnConditionChoices: List<Pair<String, String>> = listOf(
    "GOOD" to "Bueno",
    "FAIR" to "Aceptable",
    "POOR" to "Malo",
    "BROKEN" to "Roto",
)

/**
 * Una pieza (o una cantidad de un renglón sin piezas identificadas) que se
 * puede devolver. La web decide pieza por pieza; aquí igual, y cuando el
 * préstamo no tiene piezas ligadas se cae a la cantidad del renglón (el API
 * acepta las dos formas).
 */
data class ReturnPickRow(
    val loanItemId: String,
    val deviceName: String,
    /** Pieza física exacta (null = renglón sin piezas: se devuelve por cantidad). */
    val unitId: String? = null,
    val assetTag: String = "",
    val serialNumber: String? = null,
    /** Piezas pendientes del renglón (tope del fallback por cantidad). */
    val pending: Int = 0,
    val selected: Boolean = false,
    val condition: String = "GOOD",
    val notes: String = "",
    /** Cantidad a devolver cuando la fila es por cantidad. */
    val quantityText: String = "",
) {
    /** Un renglón sin piezas identificadas se devuelve por cantidad. */
    val isQuantityRow: Boolean get() = unitId == null

    val quantity: Int get() = quantityText.toIntOrNull() ?: 0

    /** ¿Esta fila entra en la devolución? */
    val included: Boolean get() = if (isQuantityRow) quantity > 0 else selected

    /** MALO y ROTO exigen decir qué pasó (mínimo 3 caracteres), igual que la web. */
    val needsNotes: Boolean get() = condition == "POOR" || condition == "BROKEN"

    val label: String get() = if (isQuantityRow) deviceName else assetTag
}

/**
 * Estado del detalle de una carta responsiva (préstamo del API) con devolución
 * parcial por pieza y condición.
 *
 * No hay acción "deshacer devolución": el API no expone endpoint para eso (la
 * app respondía 501), así que no se ofrece en la UI. Registrar una devolución
 * nueva sí está soportado (`POST /inventory/returns`, `loans.edit`).
 */
data class CustodyLetterDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val custodyLetter: CustodyLetterDto? = null,
    /** Detalle del préstamo con sus renglones, piezas y devoluciones. */
    val loan: LoanDetailDto? = null,
    val actionSaving: Boolean = false,
    val actionError: String? = null,
    val deleted: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val showReturnModal: Boolean = false,
    val returnPicks: List<ReturnPickRow> = emptyList(),
    /** Nota general de la devolución (opcional). */
    val returnNotes: String = "",
    val validationError: String? = null,
    /** Aviso de la última devolución registrada. */
    val successMessage: String? = null,
) {
    /** Renglones del préstamo que todavía tienen piezas por devolver. */
    val pendingItems get() = loan?.items.orEmpty().filter { it.pending > 0 }

    val hasPending: Boolean get() = pendingItems.isNotEmpty()

    /** Piezas (o cantidades) que se van a devolver. */
    val pickedCount: Int
        get() = returnPicks.sumOf { row ->
            when {
                !row.included -> 0
                row.isQuantityRow -> row.quantity.coerceAtMost(row.pending)
                else -> 1
            }
        }

    /** Devoluciones ya registradas del préstamo (de la más reciente a la más antigua). */
    val returns get() = loan?.returns.orEmpty()
}
