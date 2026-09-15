package com.axzydev.puertonuevoapp.feature.cartas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.cartas.CartasApi
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypesApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Generar carta responsiva por tipo de dispositivo: el backend elige
 * automáticamente el siguiente dispositivo DISPONIBLE de ese tipo y crea la
 * carta de forma transaccional (paridad con GenerarCartasPage.tsx).
 */
class GenerateCartaViewModel(
    private val cartasApi: CartasApi,
    private val deviceTypesApi: DeviceTypesApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenerateCartaUiState())
    val uiState: StateFlow<GenerateCartaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val result = runCatching { deviceTypesApi.list().filter { it.active } }
            _uiState.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let(::networkMessage),
                    types = result.getOrDefault(emptyList()),
                    typeId = result.getOrNull()?.firstOrNull()?.id ?: "",
                )
            }
        }
    }

    fun onTypeChange(value: String) = _uiState.update { it.copy(typeId = value) }

    fun generate() {
        val typeId = _uiState.value.typeId
        if (typeId.isBlank() || _uiState.value.generating) return
        _uiState.update { it.copy(generating = true, error = null, result = null) }
        viewModelScope.launch {
            val result = runCatching { cartasApi.generate(typeId) }
            _uiState.update {
                it.copy(generating = false, error = result.exceptionOrNull()?.let(::networkMessage), result = result.getOrNull())
            }
        }
    }
}
