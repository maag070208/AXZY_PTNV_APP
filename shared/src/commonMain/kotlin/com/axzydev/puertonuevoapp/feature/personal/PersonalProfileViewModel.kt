package com.axzydev.puertonuevoapp.feature.personal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.personal.PersonalApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PersonalProfileViewModel(
    private val personId: String,
    private val personalApi: PersonalApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalProfileUiState())
    val uiState: StateFlow<PersonalProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                coroutineScope {
                    val profile = async { personalApi.get(personId) }
                    val documents = async { personalApi.documents(personId) }
                    val documentTypes = async { personalApi.documentTypes() }
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