package com.axzydev.puertonuevoapp.feature.custodyletters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.custodyletters.CustodyLetterDto
import com.axzydev.puertonuevoapp.core.network.custodyletters.CustodyLettersApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Cartas responsivas: la visibilidad ya viene filtrada por rol desde el
 * backend (EMPLOYEE ve solo las suyas, AREA_HEAD solo las de su
 * departamento), así que aquí no se vuelve a filtrar por dueño.
 */
class CustodyLettersListViewModel(
    private val custodyLettersApi: CustodyLettersApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustodyLettersListUiState())
    val uiState: StateFlow<CustodyLettersListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { custodyLettersApi.list().data }
            _uiState.update {
                it.copy(loading = false, error = result.exceptionOrNull()?.let(::networkMessage), custodyLetters = result.getOrDefault(it.custodyLetters))
            }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }

    fun requestDelete(custodyLetter: CustodyLetterDto) = _uiState.update { it.copy(deleteTarget = custodyLetter) }
    fun dismissDelete() = _uiState.update { it.copy(deleteTarget = null) }

    fun confirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        _uiState.update { it.copy(actionSaving = true) }
        viewModelScope.launch {
            val result = runCatching { custodyLettersApi.remove(target.id) }
            _uiState.update {
                it.copy(actionSaving = false, deleteTarget = null, actionError = result.exceptionOrNull()?.let(::networkMessage))
            }
            if (result.isSuccess) load()
        }
    }

    fun dismissActionError() = _uiState.update { it.copy(actionError = null) }
}
