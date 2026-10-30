package com.axzydev.puertonuevoapp.feature.inventory

import com.axzydev.puertonuevoapp.core.network.inventory.LoanReturnDto

/**
 * Estado de la bandeja de devoluciones. La lista va paginada por
 * `PaginatedTableViewModel` (la API devuelve la bitácora completa y el
 * adaptador pagina/ordena); este estado guarda el detalle abierto.
 */
data class InventoryReturnsUiState(
    /** Devolución abierta en el detalle (null = cerrado). */
    val selected: LoanReturnDto? = null,
) {
    val detailOpen: Boolean get() = selected != null
}
