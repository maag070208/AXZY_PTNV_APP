package com.axzydev.puertonuevoapp.feature.devicetypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypesApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeviceTypesListViewModel(
    private val deviceTypesApi: DeviceTypesApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceTypesListUiState())
    val uiState: StateFlow<DeviceTypesListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { deviceTypesApi.list(includeInactive = true).sortedBy { it.name } }
            _uiState.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let(::networkMessage),
                    types = result.getOrDefault(it.types),
                )
            }
        }
    }
}
