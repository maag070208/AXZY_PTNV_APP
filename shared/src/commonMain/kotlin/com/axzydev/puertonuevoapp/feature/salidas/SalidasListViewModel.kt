package com.axzydev.puertonuevoapp.feature.salidas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.salidas.SalidaDto
import com.axzydev.puertonuevoapp.core.network.salidas.SalidasApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SalidasListViewModel(
    private val salidasApi: SalidasApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalidasListUiState())
    val uiState: StateFlow<SalidasListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { salidasApi.list().data }
            _uiState.update {
                it.copy(loading = false, error = result.exceptionOrNull()?.let(::networkMessage), salidas = result.getOrDefault(it.salidas))
            }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }

    fun requestDelete(salida: SalidaDto) = _uiState.update { it.copy(deleteTarget = salida) }
    fun dismissDelete() = _uiState.update { it.copy(deleteTarget = null) }

    fun confirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        _uiState.update { it.copy(actionSaving = true) }
        viewModelScope.launch {
            val result = runCatching { salidasApi.remove(target.id) }
            _uiState.update {
                it.copy(actionSaving = false, deleteTarget = null, actionError = result.exceptionOrNull()?.let(::networkMessage))
            }
            if (result.isSuccess) load()
        }
    }

    fun dismissActionError() = _uiState.update { it.copy(actionError = null) }
}
