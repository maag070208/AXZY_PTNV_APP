package com.axzydev.puertonuevoapp.core.ui.table

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
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

private enum class LoadMode { Initial, Refresh, More }

/**
 * ViewModel base de pantallas-tabla server-side. Orquesta la carga con el
 * contrato `TableRequest`/`TableResponse`: primer load, pull-to-refresh,
 * "cargar más" (paginación infinita) y re-query al cambiar búsqueda/filtros.
 *
 * La búsqueda ([searchKey]) y el modal de filtros comparten el mismo mapa
 * `filters`; el texto de la caja de búsqueda se guarda bajo `searchKey`.
 */
abstract class PaginatedTableViewModel<T> : ViewModel() {

    /** Tamaño de página (por defecto 10). */
    protected open val pageSize: Int = 10

    /** Clave de filtro que alimenta la caja de búsqueda, o `null` para ocultarla. */
    protected open val searchKey: String? = null

    private val _uiState = MutableStateFlow(TableUiState<T>())
    val uiState: StateFlow<TableUiState<T>> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var searchJob: Job? = null

    protected abstract suspend fun fetch(page: Int, limit: Int, filters: Map<String, String>): TableResponse<T>

    init {
        // `yield` antes del primer load: `fetch` es open/virtual y la subclase
        // aún no terminó su construcción si corriéramos síncrono aquí.
        viewModelScope.launch {
            kotlinx.coroutines.yield()
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
            val result = try {
                Result.success(fetch(page, pageSize, filters))
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