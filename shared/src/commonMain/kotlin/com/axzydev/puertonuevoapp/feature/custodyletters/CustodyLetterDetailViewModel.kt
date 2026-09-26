package com.axzydev.puertonuevoapp.feature.custodyletters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.custodyletters.CustodyLettersApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CustodyLetterDetailViewModel(
    private val custodyLetterId: String,
    private val custodyLettersApi: CustodyLettersApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustodyLetterDetailUiState())
    val uiState: StateFlow<CustodyLetterDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { custodyLettersApi.get(custodyLetterId) }
            _uiState.update {
                it.copy(loading = false, error = result.exceptionOrNull()?.let(::networkMessage), custodyLetter = result.getOrNull())
            }
        }
    }

    fun requestDelete() = _uiState.update { it.copy(showDeleteConfirm = true) }
    fun dismissDelete() = _uiState.update { it.copy(showDeleteConfirm = false) }

    fun confirmDelete() {
        _uiState.update { it.copy(actionSaving = true) }
        viewModelScope.launch {
            val result = runCatching { custodyLettersApi.remove(custodyLetterId) }
            _uiState.update {
                it.copy(
                    actionSaving = false,
                    showDeleteConfirm = false,
                    deleted = result.isSuccess,
                    actionError = result.exceptionOrNull()?.let(::networkMessage) ?: it.actionError,
                )
            }
        }
    }

    fun openReturnModal() = _uiState.update { it.copy(showReturnModal = true, returnedBy = "", returnCondition = "") }
    fun dismissReturnModal() = _uiState.update { it.copy(showReturnModal = false) }
    fun onReturnedByChange(value: String) = _uiState.update { it.copy(returnedBy = value) }
    fun onReturnConditionChange(value: String) = _uiState.update { it.copy(returnCondition = value) }

    fun confirmReturn() {
        val state = _uiState.value
        if (state.returnedBy.isBlank() || state.returnCondition.isBlank()) return
        _uiState.update { it.copy(actionSaving = true) }
        viewModelScope.launch {
            val result = runCatching { custodyLettersApi.returnCustodyLetter(custodyLetterId, state.returnedBy.trim(), state.returnCondition.trim()) }
            _uiState.update {
                it.copy(
                    actionSaving = false,
                    showReturnModal = result.isFailure,
                    custodyLetter = result.getOrNull() ?: it.custodyLetter,
                    actionError = result.exceptionOrNull()?.let(::networkMessage) ?: it.actionError,
                )
            }
        }
    }

    fun requestUndoReturn() = _uiState.update { it.copy(showUndoConfirm = true) }
    fun dismissUndoReturn() = _uiState.update { it.copy(showUndoConfirm = false) }

    fun confirmUndoReturn() {
        _uiState.update { it.copy(actionSaving = true) }
        viewModelScope.launch {
            val result = runCatching { custodyLettersApi.undoReturn(custodyLetterId) }
            _uiState.update {
                it.copy(
                    actionSaving = false,
                    showUndoConfirm = false,
                    custodyLetter = result.getOrNull() ?: it.custodyLetter,
                    actionError = result.exceptionOrNull()?.let(::networkMessage) ?: it.actionError,
                )
            }
        }
    }

    fun dismissActionError() = _uiState.update { it.copy(actionError = null) }
}
