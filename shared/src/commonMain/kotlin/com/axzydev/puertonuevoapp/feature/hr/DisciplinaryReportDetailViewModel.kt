package com.axzydev.puertonuevoapp.feature.hr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.hr.HrApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Detalle de un acta administrativa (`GET /hr/disciplinary-reports/:id`) con
 * eliminación (`DELETE /hr/disciplinary-reports/:id`, permiso
 * `hr.disciplinary_reports`).
 *
 * **PDF:** el acta se imprime desde la web (render client-side); el API no
 * expone un endpoint de PDF, así que aquí solo se muestra la vista en pantalla
 * y no se ofrece descarga (no se inventa la ruta).
 */
class DisciplinaryReportDetailViewModel(
    private val reportId: String,
    private val hrApi: HrApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DisciplinaryReportDetailUiState())
    val uiState: StateFlow<DisciplinaryReportDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { hrApi.disciplinaryReport(reportId) }
            _uiState.update {
                it.copy(
                    loading = false,
                    report = result.getOrNull() ?: it.report,
                    error = result.exceptionOrNull()?.let(::networkMessage),
                )
            }
        }
    }

    fun requestDelete() = _uiState.update { it.copy(showDeleteConfirm = true, deleteError = null) }

    fun dismissDelete() = _uiState.update { it.copy(showDeleteConfirm = false) }

    fun confirmDelete() {
        if (_uiState.value.deleting) return
        _uiState.update { it.copy(deleting = true, deleteError = null) }
        viewModelScope.launch {
            val result = runCatching { hrApi.removeDisciplinaryReport(reportId) }
            _uiState.update { current ->
                result.fold(
                    onSuccess = { current.copy(deleting = false, showDeleteConfirm = false, deleted = true) },
                    onFailure = { error ->
                        current.copy(deleting = false, showDeleteConfirm = false, deleteError = networkMessage(error))
                    },
                )
            }
        }
    }

    fun dismissDeleteError() = _uiState.update { it.copy(deleteError = null) }
}
