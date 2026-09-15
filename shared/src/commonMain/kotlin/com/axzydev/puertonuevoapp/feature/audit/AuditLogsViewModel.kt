package com.axzydev.puertonuevoapp.feature.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.audit.AuditApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Auditoría del sistema. El backend restringe este módulo a ADMIN
 * (audit.routes.ts: authorize(["ADMIN"])); se replica igual aquí.
 */
class AuditLogsViewModel(
    private val auditApi: AuditApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuditLogsUiState())
    val uiState: StateFlow<AuditLogsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val state = _uiState.value
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                auditApi.list(
                    action = state.filterAction.ifBlank { null },
                    start = state.filterStart.ifBlank { null },
                    end = state.filterEnd.ifBlank { null },
                    page = state.page,
                    limit = state.limit,
                )
            }
            _uiState.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let(::networkMessage),
                    logs = result.getOrNull()?.data ?: it.logs,
                    total = result.getOrNull()?.total ?: it.total,
                )
            }
        }
    }

    fun onFilterActionChange(value: String) {
        _uiState.update { it.copy(filterAction = value, page = 1) }
        load()
    }

    fun onFilterStartChange(value: String) = _uiState.update { it.copy(filterStart = value) }
    fun onFilterEndChange(value: String) = _uiState.update { it.copy(filterEnd = value) }

    fun applyFilters() {
        _uiState.update { it.copy(page = 1) }
        load()
    }

    fun previousPage() {
        if (_uiState.value.page <= 1) return
        _uiState.update { it.copy(page = it.page - 1) }
        load()
    }

    fun nextPage() {
        if (_uiState.value.page >= _uiState.value.totalPages) return
        _uiState.update { it.copy(page = it.page + 1) }
        load()
    }
}
