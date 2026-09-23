package com.axzydev.puertonuevoapp.core.ui.table

/**
 * Modelo de las tablas server-side genéricas.
 *
 * El contrato de red es `TableRequest`/`TableResponse` (ver `core/network/http/Table.kt`).
 * Estas piezas describen, en UI, qué filtros expone el modal y el estado común
 * de paginación de cualquier pantalla-tabla (lista + búsqueda + filtros +
 * pull-to-refresh + "cargar más").
 */

/** Cómo se edita un filtro dentro del modal. */
sealed interface FilterControl {
    /** Campo de texto libre. */
    data object Text : FilterControl

    /** Desplegable con opciones predefinidas. */
    data class Select(val options: List<FilterOption>) : FilterControl
}

data class FilterOption(
    val value: String,
    val label: String,
)

data class TableFilterSpec(
    /** Clave dentro de `TableRequest.filters`. */
    val key: String,
    val label: String,
    val control: FilterControl,
)

/**
 * Estado común de una pantalla con tabla server-side. La lista No. [items]
 * nunca se filtra en cliente: cada query re-pide al backend.
 */
data class TableUiState<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    val filters: Map<String, String> = emptyMap(),
) {
    val hasMore: Boolean get() = items.size < total
}