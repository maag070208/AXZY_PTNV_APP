package com.axzydev.puertonuevoapp.core.ui.table

import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import com.axzydev.puertonuevoapp.core.network.http.TableSort

/**
 * Tabla del estándar para endpoints que **paginan en el servidor**
 * (`POST /<recurso>/query` con `TableRequest`/`TableResponse`): la subclase traduce
 * `page`/`limit`/`filters`/`sort` a su `TableRequest` y el API filtra, ordena y pagina.
 *
 * Cuando el módulo tenga orden por columna, implementa la variante de cuatro
 * argumentos de [fetch] (las subclases viejas que solo implementan la de tres
 * siguen compilando; el orden se ignora hasta que migren).
 */
abstract class PaginatedTableViewModel<T> : TableViewModelBase<T>() {

    /** Página server-side. [filters] ya viene sin claves vacías. */
    protected abstract suspend fun fetch(
        page: Int,
        limit: Int,
        filters: Map<String, String>,
    ): TableResponse<T>

    /**
     * Igual que [fetch] pero con el orden elegido en la UI. Por defecto delega en
     * [fetch] (sin orden) para no romper los VMs existentes.
     */
    protected open suspend fun fetch(
        page: Int,
        limit: Int,
        filters: Map<String, String>,
        sort: TableSort?,
    ): TableResponse<T> = fetch(page, limit, filters)

    final override suspend fun loadPage(
        page: Int,
        limit: Int,
        filters: Map<String, String>,
        sort: TableSort?,
    ): TableResponse<T> = fetch(page, limit, filters, sort)
}
