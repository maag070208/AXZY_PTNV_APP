package com.axzydev.puertonuevoapp.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.locations.LocationCreateInput
import com.axzydev.puertonuevoapp.core.network.locations.LocationUpdateInput
import com.axzydev.puertonuevoapp.core.network.locations.LocationsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocationFormViewModel(
    private val locationId: String?,
    private val locationsApi: LocationsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationFormUiState(loading = locationId != null))
    val uiState: StateFlow<LocationFormUiState> = _uiState.asStateFlow()

    init {
        if (locationId != null) load(locationId)
    }

    private fun load(id: String) {
        viewModelScope.launch {
            val result = runCatching { locationsApi.get(id) }
            result.fold(
                onSuccess = { loc -> _uiState.update { it.copy(loading = false, name = loc.name, description = loc.description ?: "") } },
                onFailure = { e -> _uiState.update { it.copy(loading = false, error = networkMessage(e)) } },
            )
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value.uppercase()) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }

    fun submit() {
        val state = _uiState.value
        if (!state.isValid || state.saving) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                if (locationId == null) {
                    locationsApi.create(LocationCreateInput(name = state.name.trim(), description = state.description.trim().ifBlank { null }))
                } else {
                    locationsApi.update(locationId, LocationUpdateInput(name = state.name.trim(), description = state.description.trim().ifBlank { null }))
                }
            }
            _uiState.update { it.copy(saving = false, error = result.exceptionOrNull()?.let(::networkMessage), saved = result.isSuccess) }
        }
    }
}
