package com.axzydev.puertonuevoapp.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.inventory.InventoryApi
import com.axzydev.puertonuevoapp.core.network.locations.LocationsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InventoryMovementsViewModel(
    private val inventoryApi: InventoryApi,
    private val locationsApi: LocationsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryMovementsUiState())
    val uiState: StateFlow<InventoryMovementsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val locations = runCatching { locationsApi.list() }.getOrDefault(emptyList())
            _uiState.update { it.copy(locations = locations) }
            load()
        }
    }

    fun load() {
        val state = _uiState.value
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                inventoryApi.movements(
                    locationId = state.locationFilter.ifBlank { null },
                    start = state.startFilter.ifBlank { null },
                    end = state.endFilter.ifBlank { null },
                )
            }
            _uiState.update {
                it.copy(loading = false, error = result.exceptionOrNull()?.let(::networkMessage), movements = result.getOrDefault(it.movements))
            }
        }
    }

    fun onLocationFilterChange(value: String) {
        _uiState.update { it.copy(locationFilter = value) }
        load()
    }

    fun onStartFilterChange(value: String) = _uiState.update { it.copy(startFilter = value) }
    fun onEndFilterChange(value: String) = _uiState.update { it.copy(endFilter = value) }
}
