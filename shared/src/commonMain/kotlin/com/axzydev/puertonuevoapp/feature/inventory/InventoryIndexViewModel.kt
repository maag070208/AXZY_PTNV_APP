package com.axzydev.puertonuevoapp.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.inventory.InventoryApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InventoryIndexViewModel(
    private val inventoryApi: InventoryApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryIndexUiState())
    val uiState: StateFlow<InventoryIndexUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { inventoryApi.summary() }
            _uiState.update {
                it.copy(loading = false, error = result.exceptionOrNull()?.let(::networkMessage), summary = result.getOrNull())
            }
        }
    }
}
