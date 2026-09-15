package com.axzydev.puertonuevoapp.feature.devicetypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypeCreateInput
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypeUpdateInput
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypesApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeviceTypeFormViewModel(
    private val typeId: String?,
    private val deviceTypesApi: DeviceTypesApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceTypeFormUiState(loading = typeId != null))
    val uiState: StateFlow<DeviceTypeFormUiState> = _uiState.asStateFlow()

    init {
        if (typeId != null) load(typeId)
    }

    private fun load(id: String) {
        viewModelScope.launch {
            val result = runCatching { deviceTypesApi.get(id) }
            result.fold(
                onSuccess = { t ->
                    _uiState.update {
                        it.copy(loading = false, code = t.code, name = t.name, prefix = t.prefix, active = t.active, fieldConfig = t.fieldConfig)
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(loading = false, error = networkMessage(e)) } },
            )
        }
    }

    fun onCodeChange(value: String) = _uiState.update { it.copy(code = value.uppercase()) }
    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onPrefixChange(value: String) = _uiState.update { it.copy(prefix = value.uppercase()) }
    fun onActiveChange(value: Boolean) = _uiState.update { it.copy(active = value) }

    fun onFieldEnabledChange(key: String, enabled: Boolean) = _uiState.update { state ->
        val current = state.fieldConfig.settingFor(key)
        state.copy(fieldConfig = state.fieldConfig.withSetting(key, current.copy(enabled = enabled, required = enabled && current.required)))
    }

    fun onFieldRequiredChange(key: String, required: Boolean) = _uiState.update { state ->
        val current = state.fieldConfig.settingFor(key)
        state.copy(fieldConfig = state.fieldConfig.withSetting(key, current.copy(required = required)))
    }

    fun submit() {
        val state = _uiState.value
        if (!state.isValid || state.saving) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                if (typeId == null) {
                    deviceTypesApi.create(
                        DeviceTypeCreateInput(code = state.code.trim(), name = state.name.trim(), prefix = state.prefix.trim(), fieldConfig = state.fieldConfig),
                    )
                } else {
                    deviceTypesApi.update(
                        typeId,
                        DeviceTypeUpdateInput(name = state.name.trim(), active = state.active, fieldConfig = state.fieldConfig),
                    )
                }
            }
            _uiState.update {
                it.copy(saving = false, error = result.exceptionOrNull()?.let(::networkMessage), saved = result.isSuccess)
            }
        }
    }
}
