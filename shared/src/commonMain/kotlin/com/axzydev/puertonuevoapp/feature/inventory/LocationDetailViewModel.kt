package com.axzydev.puertonuevoapp.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.locations.LocationsApi
import com.axzydev.puertonuevoapp.core.network.locations.SublugarDto
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

    fun onNewSublugarChange(value: String) = _uiState.update { it.copy(newSublugar = value) }

    fun addSublugar() {
        val name = _uiState.value.newSublugar.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            val result = runCatching { locationsApi.addSublugar(locationId, name) }
            if (result.isSuccess) {
                _uiState.update { it.copy(newSublugar = "") }
                load()
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.let(::networkMessage)) }
            }
        }
    }

    fun requestDeleteSublugar(sublugar: SublugarDto) = _uiState.update { it.copy(sublugarToDelete = sublugar) }
    fun dismissDeleteSublugar() = _uiState.update { it.copy(sublugarToDelete = null) }

    fun confirmDeleteSublugar() {
        val sublugar = _uiState.value.sublugarToDelete ?: return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = runCatching { locationsApi.removeSublugar(sublugar.id) }
            _uiState.update {
                it.copy(saving = false, sublugarToDelete = null, error = result.exceptionOrNull()?.let(::networkMessage) ?: it.error)
            }
            if (result.isSuccess) load()
        }
    }
}
