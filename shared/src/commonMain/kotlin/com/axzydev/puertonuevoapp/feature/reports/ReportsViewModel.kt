package com.axzydev.puertonuevoapp.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.reports.ReportsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Reportes: la web solo expone dos tabs (Asignados, Dispositivos); el
 * reporte de "entregas" no tiene pantalla propia ni en el web, así que no
 * se replica aquí. Exportar a PDF/CSV (client-side) queda fuera de alcance.
 */
class ReportsViewModel(
    private val reportsApi: ReportsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadAsignados()
        loadDevices()
    }

    fun onTabChange(tab: Int) = _uiState.update { it.copy(tab = tab) }

    fun loadAsignados() {
        _uiState.update { it.copy(asignadosLoading = true, asignadosError = null) }
        viewModelScope.launch {
            val result = runCatching { reportsApi.asignados() }
            _uiState.update {
                it.copy(
                    asignadosLoading = false,
                    asignadosError = result.exceptionOrNull()?.let(::networkMessage),
                    asignados = result.getOrDefault(it.asignados),
                )
            }
        }
    }

    fun loadDevices() {
        _uiState.update { it.copy(devicesLoading = true, devicesError = null) }
        viewModelScope.launch {
            val result = runCatching { reportsApi.devices() }
            _uiState.update {
                it.copy(
                    devicesLoading = false,
                    devicesError = result.exceptionOrNull()?.let(::networkMessage),
                    devices = result.getOrDefault(it.devices),
                )
            }
        }
    }
}
