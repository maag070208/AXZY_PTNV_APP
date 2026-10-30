package com.axzydev.puertonuevoapp.feature.inventory

import com.axzydev.puertonuevoapp.core.network.inventory.InventoryAuditDto

/**
 * Estado de la pantalla del auditor de inventario: resultado de las 8 reglas,
 * refresco y resolución de los descuadres que se pueden arreglar desde aquí.
 */
data class InventoryAuditUiState(
    val loading: Boolean = true,
    /** Refresco en segundo plano: se conserva el resultado anterior mientras corre. */
    val refreshing: Boolean = false,
    val audit: InventoryAuditDto? = null,
    val error: String? = null,
    /** Hay una resolución de descuadre en vuelo. */
    val actionSaving: Boolean = false,
    val actionError: String? = null,
    /** Mensaje de la última resolución aplicada (se muestra como aviso). */
    val resolvedMessage: String? = null,
) {
    /** Reglas que hoy rompen, ordenadas como las manda el API. */
    val failing get() = audit?.checks.orEmpty().filter { it.count > 0 }

    /** Descuadres de movimientos: los únicos que se resuelven desde esta pantalla. */
    val resolvableRows get() = audit?.checks
        .orEmpty()
        .firstOrNull { it.key == "MOVEMENT_UNITS_MISMATCH" }
        ?.rows
        .orEmpty()
        .filter { !it.movementItemId.isNullOrBlank() }
}
