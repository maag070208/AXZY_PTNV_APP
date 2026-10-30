package com.axzydev.puertonuevoapp.core.ui.table

import com.axzydev.puertonuevoapp.core.network.http.TableSort

/**
 * Modelo de las tablas del estándar (`core/ui/table`).
 *
 * El contrato de red es `TableRequest`/`TableResponse` (ver `core/network/http/Table.kt`).
 * Estas piezas describen, en UI, qué filtros expone el modal, qué columnas se pueden
 * ordenar y el estado común de cualquier pantalla-tabla (lista + búsqueda + filtros +
 * orden + pull-to-refresh + "cargar más").
 */

/** Cómo se edita un filtro dentro del modal. */
sealed interface FilterControl {
    /** Campo de texto libre. */
    data object Text : FilterControl

    /** Desplegable con opciones predefinidas (catálogo). */
    data class Select(val options: List<FilterOption>) : FilterControl

    /**
     * Rango de fechas: el modal pide "Desde" y "Hasta" y el rango queda guardado en
     * `TableUiState.filters` bajo las claves [TableFilterSpec.fromKey]/[TableFilterSpec.toKey].
     * El adaptador de la pantalla decide cómo aplicarlo (local en `ClientTableViewModel`;
     * server-side requeriría un contrato de filtros con rangos, hoy `TableRequest.filters`
     * solo admite escalares).
     */
    data object DateRange : FilterControl
}

data class FilterOption(
    val value: String,
    val label: String,
)

/** Sufijos del par Desde/Hasta que guarda [FilterControl.DateRange]. */
const val DateRangeFromSuffix: String = "From"
const val DateRangeToSuffix: String = "To"

data class TableFilterSpec(
    /** Clave dentro de `TableRequest.filters`. */
    val key: String,
    val label: String,
    val control: FilterControl,
) {
    /** Clave de `TableUiState.filters` del extremo inferior de un [FilterControl.DateRange]. */
    val fromKey: String get() = key + DateRangeFromSuffix

    /** Clave de `TableUiState.filters` del extremo superior de un [FilterControl.DateRange]. */
    val toKey: String get() = key + DateRangeToSuffix
}

/** Columna ordenable de una tabla: [key] es la que viaja en `TableRequest.sort`. */
data class TableSortSpec(
    val key: String,
    val label: String,
)

/**
 * Estado común de una pantalla con tabla. La lista [items] nunca se filtra en cliente:
 * cada query re-pide datos al backend (o al adaptador de la pantalla).
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
    /** Orden vigente; `null` = el orden por defecto del backend. */
    val sort: TableSort? = null,
) {
    val hasMore: Boolean get() = items.size < total
}
