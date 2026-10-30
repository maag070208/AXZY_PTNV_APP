package com.axzydev.puertonuevoapp.core.ui.table

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import com.axzydev.puertonuevoapp.core.network.http.TableSort
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

private enum class LoadMode { Initial, Refresh, More }

/**
 * Motor común del estándar de tablas (`AppTableScreen` + `core/ui/table`):
 * primer load, pull-to-refresh, paginación infinita, búsqueda con debounce,
 * filtros por columna y orden. La pantalla solo aporta [loadPage].
 *
 * Hay dos implementaciones según lo que ofrezca el API:
 * - [PaginatedTableViewModel]: endpoints con `POST /<recurso>/query`
 *   (`TableRequest`/`TableResponse`), paginación y filtros server-side.
 * - [ClientTableViewModel]: endpoints que devuelven la lista completa (sin `/query`).
 */
abstract class TableViewModelBase<T> : ViewModel() {

    /** Tamaño de página (por defecto 10). */
    protected open val pageSize: Int = 10

    /** Clave de filtro que alimenta la caja de búsqueda, o `null` para ocultarla. */
    protected open val searchKey: String? = null

    private val _uiState = MutableStateFlow(TableUiState<T>())
    val uiState: StateFlow<TableUiState<T>> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var searchJob: Job? = null

    /** Obtiene una página según los filtros y el orden vigentes. */
    protected abstract suspend fun loadPage(
        page: Int,
        limit: Int,
        filters: Map<String, String>,
        sort: TableSort?,
    ): TableResponse<T>

    init {
        // `yield` antes del primer load: [loadPage] es open/virtual y la subclase
        // aún no terminó su construcción si corriéramos síncrono aquí.
        viewModelScope.launch {
            yield()
            startLoad(page = 1, mode = LoadMode.Initial)
        }
    }

    /** Pull-to-refresh: re-pide la primera página sin spinner completo. */
    fun refresh() = startLoad(page = 1, mode = LoadMode.Refresh)

    /** Reintenta el load inicial tras un error. */
    fun retry() = startLoad(page = 1, mode = LoadMode.Initial)

    /** Página siguiente (paginación infinita). No-op si no hay más o ya hay una en vuelo. */
    fun loadMore() {
        val s = _uiState.value
        if (s.loading || s.loadingMore || s.refreshing || !s.hasMore) return
        startLoad(page = s.page + 1, mode = LoadMode.More)
    }

    /** Texto de la caja de búsqueda → filtro `searchKey` con debounce. */
    fun onSearchChange(value: String) {
        val key = searchKey ?: return
        setFilter(key, value)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            startLoad(page = 1, mode = LoadMode.Initial)
        }
    }

    /** Resultado del modal de filtros (reemplaza el mapa completo). */
    fun onFiltersChange(filters: Map<String, String>) {
        _uiState.update { it.copy(filters = filters.filterValues { v -> v.isNotBlank() }) }
        searchJob?.cancel()
        startLoad(page = 1, mode = LoadMode.Initial)
    }

    /** Orden por columna elegido en el modal (o `null` para el orden por defecto). */
    fun onSortChange(sort: TableSort?) {
        if (_uiState.value.sort == sort) return
        _uiState.update { it.copy(sort = sort) }
        searchJob?.cancel()
        startLoad(page = 1, mode = LoadMode.Initial)
    }

    /** Atajo para un control de un toque: mismo campo alterna asc/desc, otro campo arranca asc. */
    fun toggleSort(key: String) {
        val current = _uiState.value.sort
        val next = if (current?.key == key) {
            TableSort(key, if (current.direction == "asc") "desc" else "asc")
        } else {
            TableSort(key, "asc")
        }
        onSortChange(next)
    }

    private fun setFilter(key: String, value: String) {
        _uiState.update { s ->
            val next = if (value.isBlank()) s.filters - key else s.filters + (key to value)
            s.copy(filters = next)
        }
    }

    private fun startLoad(page: Int, mode: LoadMode) {
        if (mode != LoadMode.More) searchJob?.cancel()
        loadJob?.cancel()

        _uiState.update {
            it.copy(
                loading = mode == LoadMode.Initial,
                refreshing = mode == LoadMode.Refresh,
                loadingMore = mode == LoadMode.More,
                error = null,
            )
        }

        loadJob = viewModelScope.launch {
            val filters = _uiState.value.filters
            val sort = _uiState.value.sort
            val result = try {
                Result.success(loadPage(page, pageSize, filters, sort))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Result.failure(e)
            }

            result.fold(
                onSuccess = { response ->
                    _uiState.update { s ->
                        val items = if (mode == LoadMode.More) s.items + response.data else response.data
                        s.copy(
                            items = items,
                            total = response.total,
                            page = page,
                            loading = false,
                            loadingMore = false,
                            refreshing = false,
                            error = null,
                        )
                    }
                },
                onFailure = { e ->
                    val message = networkMessage(e)
                    _uiState.update { s ->
                        if (mode == LoadMode.More && s.items.isNotEmpty()) {
                            s.copy(loading = false, loadingMore = false, refreshing = false)
                        } else {
                            s.copy(loading = false, loadingMore = false, refreshing = false, error = message)
                        }
                    }
                    if (mode == LoadMode.More) AppSnackbar.showError(message)
                },
            )
        }
    }
}
