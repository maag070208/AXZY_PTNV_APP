package com.axzydev.puertonuevoapp.feature.hr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.hr.HrApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EmployeeProfileViewModel(
    private val personId: String,
    private val hrApi: HrApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmployeeProfileUiState())
    val uiState: StateFlow<EmployeeProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                coroutineScope {
                    val profile = async { hrApi.get(personId) }
                    val documents = async { hrApi.documents(personId) }
                    val documentTypes = async { hrApi.documentTypes() }
                    Triple(profile.await(), documents.await(), documentTypes.await())
                }
            }
            _uiState.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let(::networkMessage),
                    profile = result.getOrNull()?.first,
                    documents = result.getOrNull()?.second ?: it.documents,
                    documentTypes = result.getOrNull()?.third ?: it.documentTypes,
                )
            }
        }
    }
}