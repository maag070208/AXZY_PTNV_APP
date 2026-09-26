package com.axzydev.puertonuevoapp.feature.materialoutputs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.materialoutputs.MaterialOutputDto
import com.axzydev.puertonuevoapp.core.network.materialoutputs.MaterialOutputsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MaterialOutputsListViewModel(
    private val materialOutputsApi: MaterialOutputsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MaterialOutputsListUiState())
    val uiState: StateFlow<MaterialOutputsListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { materialOutputsApi.list().data }
            _uiState.update {
                it.copy(loading = false, error = result.exceptionOrNull()?.let(::networkMessage), materialOutputs = result.getOrDefault(it.materialOutputs))
            }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }

    fun requestDelete(materialOutput: MaterialOutputDto) = _uiState.update { it.copy(deleteTarget = materialOutput) }
    fun dismissDelete() = _uiState.update { it.copy(deleteTarget = null) }

    fun confirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        _uiState.update { it.copy(actionSaving = true) }
        viewModelScope.launch {
            val result = runCatching { materialOutputsApi.remove(target.id) }
            _uiState.update {
                it.copy(actionSaving = false, deleteTarget = null, actionError = result.exceptionOrNull()?.let(::networkMessage))
            }
            if (result.isSuccess) load()
        }
    }

    fun dismissActionError() = _uiState.update { it.copy(actionError = null) }
}
