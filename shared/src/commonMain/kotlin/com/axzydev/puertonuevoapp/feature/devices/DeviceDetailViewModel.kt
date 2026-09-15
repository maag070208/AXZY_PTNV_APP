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

class DeviceDetailViewModel(
    private val deviceId: String,
    private val devicesApi: DevicesApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceDetailUiState())
    val uiState: StateFlow<DeviceDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { devicesApi.get(deviceId) }
            _uiState.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let(::networkMessage),
                    device = result.getOrNull(),
                )
            }
        }
    }
}
