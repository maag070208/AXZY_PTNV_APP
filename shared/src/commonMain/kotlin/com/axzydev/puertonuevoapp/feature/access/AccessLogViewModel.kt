package com.axzydev.puertonuevoapp.feature.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.access.AccessApi
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableSort
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Historial de portería. El guardia consulta sus escaneos del día
 * (`GET /access/me/today`, restringido a `GUARD`); un admin usa la tabla
 * server-side (`POST /access/query`).
 */
class AccessLogViewModel(
    private val accessApi: AccessApi,
    private val isGuard: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccessLogUiState())
    val uiState: StateFlow<AccessLogUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val state = _uiState.value
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val outcome = runCatching {
                if (isGuard) {
                    val response = accessApi.meToday()
                    response.data to response.total
                } else {
                    val response = accessApi.query(
                        TableRequest(
                            page = state.page,
                            limit = state.limit,
                            sort = TableSort(key = "occurredAt", direction = "desc"),
                        )
                    )
                    response.data to response.total
                }
            }
            outcome.fold(
                onSuccess = { (events, total) ->
                    _uiState.update { it.copy(loading = false, events = events, total = total) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(loading = false, error = networkMessage(e)) }
                },
            )
        }
    }

    fun previousPage() {
        if (isGuard || _uiState.value.page <= 1) return
        _uiState.update { it.copy(page = it.page - 1) }
        load()
    }

    fun nextPage() {
        if (isGuard || _uiState.value.page >= _uiState.value.totalPages) return
        _uiState.update { it.copy(page = it.page + 1) }
        load()
    }
}
