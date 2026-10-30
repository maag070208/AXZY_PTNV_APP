package com.axzydev.puertonuevoapp.core.ui.table

import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import com.axzydev.puertonuevoapp.core.network.http.TableSort

/**
 * Tabla del estándar para endpoints que **no** tienen `POST /<recurso>/query` y
 * devuelven la lista completa (p. ej. `GET /inventory/movements`: el API no pagina
 * ni filtra por fecha, igual que en la web con `makeClientTableFetch`).
 *
 * La subclase describe los datos ([fetchAll]) y la semántica de cada filtro/orden
 * ([applyFilters], [applySort]); este motor trae la lista, la cachea entre páginas,
 * y expone el mismo [TableUiState] que [PaginatedTableViewModel] para que
 * `AppTableScreen` sea idéntico en ambos casos.
 *
 * Ojo: aquí el filtrado es **local**, así que el API no valida esos filtros. Úsalo
 * solo mientras el backend no ofrezca `/query`; cuando lo ofrezca, migra la
 * pantalla a [PaginatedTableViewModel] sin tocar la UI.
 */
abstract class ClientTableViewModel<T> : TableViewModelBase<T>() {

    private var cache: List<T> = emptyList()
    private var cacheFilters: Map<String, String>? = null
    private var cacheSort: TableSort? = null

    /** Descarga la lista completa (sin filtros ni orden: eso lo hace este motor). */
    protected abstract suspend fun fetchAll(): List<T>

    /**
     * Aplica los filtros de `TableUiState.filters` (incluye [searchKey] y, si la
     * pantalla expone `FilterControl.DateRange`, las claves `keyFrom`/`keyTo`).
     */
    protected abstract fun applyFilters(items: List<T>, filters: Map<String, String>): List<T>

    /** Orden local; por defecto conserva el orden en que llegó la lista. */
    protected open fun applySort(items: List<T>, sort: TableSort?): List<T> = items

    final override suspend fun loadPage(
        page: Int,
        limit: Int,
        filters: Map<String, String>,
        sort: TableSort?,
    ): TableResponse<T> {
        // La primera página (o un cambio de filtros/orden) siempre re-consulta la lista:
        // así el pull-to-refresh ve datos frescos. "Cargar más" reusa la caché.
        if (page <= 1 || cacheFilters != filters || cacheSort != sort) {
            cache = fetchAll()
            cacheFilters = filters
            cacheSort = sort
        }
        val filtered = applySort(applyFilters(cache, filters), sort)
        val from = ((page - 1) * limit).coerceIn(0, filtered.size)
        val to = (from + limit).coerceAtMost(filtered.size)
        return TableResponse(data = filtered.subList(from, to), total = filtered.size)
    }
}
