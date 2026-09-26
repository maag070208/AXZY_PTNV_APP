package com.axzydev.puertonuevoapp.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.locations.LocationsApi
import com.axzydev.puertonuevoapp.core.network.locations.SubLocationDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocationDetailViewModel(
    private val locationId: String,
    private val locationsApi: LocationsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationDetailUiState())
    val uiState: StateFlow<LocationDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { locationsApi.get(locationId) }
            _uiState.update {
                it.copy(loading = false, error = result.exceptionOrNull()?.let(::networkMessage), location = result.getOrNull())
            }
        }
    }

    fun onNewSubLocationChange(value: String) = _uiState.update { it.copy(newSubLocation = value) }

    fun addSubLocation() {
        val name = _uiState.value.newSubLocation.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            val result = runCatching { locationsApi.addSubLocation(locationId, name) }
            if (result.isSuccess) {
                _uiState.update { it.copy(newSubLocation = "") }
                load()
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.let(::networkMessage)) }
            }
        }
    }

    fun requestDeleteSubLocation(subLocation: SubLocationDto) = _uiState.update { it.copy(subLocationToDelete = subLocation) }
    fun dismissDeleteSubLocation() = _uiState.update { it.copy(subLocationToDelete = null) }

    fun confirmDeleteSubLocation() {
        val subLocation = _uiState.value.subLocationToDelete ?: return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = runCatching { locationsApi.removeSubLocation(subLocation.id) }
            _uiState.update {
                it.copy(saving = false, subLocationToDelete = null, error = result.exceptionOrNull()?.let(::networkMessage) ?: it.error)
            }
            if (result.isSuccess) load()
        }
    }
}
