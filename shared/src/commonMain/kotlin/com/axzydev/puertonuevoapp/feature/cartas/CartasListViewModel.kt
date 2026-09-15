package com.axzydev.puertonuevoapp.feature.cartas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.cartas.CartaDto
import com.axzydev.puertonuevoapp.core.network.cartas.CartasApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Cartas responsivas: la visibilidad ya viene filtrada por rol desde el
 * backend (EMPLEADO ve solo las suyas, JEFE_DE_AREA solo las de su
 * departamento), así que aquí no se vuelve a filtrar por dueño.
 */
class CartasListViewModel(
    private val cartasApi: CartasApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartasListUiState())
    val uiState: StateFlow<CartasListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { cartasApi.list().data }
            _uiState.update {
                it.copy(loading = false, error = result.exceptionOrNull()?.let(::networkMessage), cartas = result.getOrDefault(it.cartas))
            }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }

    fun requestDelete(carta: CartaDto) = _uiState.update { it.copy(deleteTarget = carta) }
    fun dismissDelete() = _uiState.update { it.copy(deleteTarget = null) }

    fun confirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        _uiState.update { it.copy(actionSaving = true) }
        viewModelScope.launch {
            val result = runCatching { cartasApi.remove(target.id) }
            _uiState.update {
                it.copy(actionSaving = false, deleteTarget = null, actionError = result.exceptionOrNull()?.let(::networkMessage))
            }
            if (result.isSuccess) load()
        }
    }

    fun dismissActionError() = _uiState.update { it.copy(actionError = null) }
}
