package com.axzydev.puertonuevoapp.feature.materialoutputs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.materialoutputs.MaterialOutputInput
import com.axzydev.puertonuevoapp.core.network.materialoutputs.MaterialOutputsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MaterialOutputFormViewModel(
    private val materialOutputId: String?,
    private val materialOutputsApi: MaterialOutputsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MaterialOutputFormUiState(loading = materialOutputId != null))
    val uiState: StateFlow<MaterialOutputFormUiState> = _uiState.asStateFlow()

    init {
        if (materialOutputId != null) load(materialOutputId)
    }

    private fun load(id: String) {
        viewModelScope.launch {
            val result = runCatching { materialOutputsApi.get(id) }
            result.fold(
                onSuccess = { s ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            area = s.area,
                            description = s.description,
                            model = s.model ?: "",
                            brand = s.brand ?: "",
                            project = s.project ?: "",
                            quantity = s.quantity.toString(),
                            departmentName = s.departmentName,
                            userName = s.userName,
                            notes = s.notes ?: "",
                            reason = s.reason ?: "",
                        )
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(loading = false, error = networkMessage(e)) } },
            )
        }
    }

    fun onAreaChange(value: String) = _uiState.update { it.copy(area = value) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun onModelChange(value: String) = _uiState.update { it.copy(model = value) }
    fun onBrandChange(value: String) = _uiState.update { it.copy(brand = value) }
    fun onProjectChange(value: String) = _uiState.update { it.copy(project = value) }
    fun onQuantityChange(value: String) = _uiState.update { it.copy(quantity = value.filter { c -> c.isDigit() }) }
    fun onDepartmentNameChange(value: String) = _uiState.update { it.copy(departmentName = value) }
    fun onUserNameChange(value: String) = _uiState.update { it.copy(userName = value) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }
    fun onReasonChange(value: String) = _uiState.update { it.copy(reason = value) }

    fun submit() {
        val state = _uiState.value
        if (!state.isValid || state.saving) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val input = MaterialOutputInput(
                description = state.description.trim(),
                model = state.model.trim().ifBlank { null },
                brand = state.brand.trim().ifBlank { null },
                project = state.project.trim().ifBlank { null },
                quantity = state.quantity.toIntOrNull()?.takeIf { it > 0 } ?: 1,
                departmentName = state.departmentName.trim(),
                userName = state.userName.trim(),
                notes = state.notes.trim().ifBlank { null },
                area = state.area.trim().ifBlank { null },
                reason = state.reason.ifBlank { null },
            )
            val result = runCatching {
                if (materialOutputId == null) materialOutputsApi.create(input) else materialOutputsApi.update(materialOutputId, input)
            }
            _uiState.update { it.copy(saving = false, error = result.exceptionOrNull()?.let(::networkMessage), saved = result.isSuccess) }
        }
    }
}
