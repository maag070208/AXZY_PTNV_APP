package com.axzydev.puertonuevoapp.feature.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.devices.DevicesApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DevicesListViewModel(
    private val devicesApi: DevicesApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevicesListUiState())
    val uiState: StateFlow<DevicesListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { devicesApi.list().data }
            _uiState.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let(::networkMessage),
                    devices = result.getOrDefault(it.devices),
                )
            }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }
    fun onEstadoFilterChange(value: String?) = _uiState.update { it.copy(estadoFilter = value) }
}
