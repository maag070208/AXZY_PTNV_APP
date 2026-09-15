package com.axzydev.puertonuevoapp.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.locations.LocationDto
import com.axzydev.puertonuevoapp.core.network.locations.LocationsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocationsListViewModel(
    private val locationsApi: LocationsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationsListUiState())
    val uiState: StateFlow<LocationsListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { locationsApi.list(includeInactive = true) }
            _uiState.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let(::networkMessage),
                    locations = result.getOrDefault(it.locations),
                )
            }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }

    fun requestDelete(location: LocationDto) = _uiState.update { it.copy(deleteTarget = location) }
    fun dismissDelete() = _uiState.update { it.copy(deleteTarget = null) }

    fun confirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        _uiState.update { it.copy(actionSaving = true) }
        viewModelScope.launch {
            val result = runCatching { locationsApi.remove(target.id) }
            _uiState.update {
                it.copy(
                    actionSaving = false,
                    deleteTarget = null,
                    actionError = result.exceptionOrNull()?.let(::networkMessage),
                )
            }
            if (result.isSuccess) load()
        }
    }

    fun dismissActionError() = _uiState.update { it.copy(actionError = null) }
}
