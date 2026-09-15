package com.axzydev.puertonuevoapp.feature.departments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentDto
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentUpdateInput
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentsApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DepartmentsListViewModel(
    private val departmentsApi: DepartmentsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DepartmentsListUiState())
    val uiState: StateFlow<DepartmentsListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { departmentsApi.list(includeInactive = true).sortedBy { it.name } }
            _uiState.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let(::networkMessage),
                    departments = result.getOrDefault(it.departments),
                )
            }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }

    fun openCreate() = _uiState.update { it.copy(showCreate = true, newName = "") }
    fun dismissCreate() = _uiState.update { it.copy(showCreate = false) }
    fun onNewNameChange(value: String) = _uiState.update { it.copy(newName = value) }

    fun submitCreate() {
        val name = _uiState.value.newName.trim()
        if (name.isBlank()) return
        _uiState.update { it.copy(actionSaving = true) }
        viewModelScope.launch {
            val result = runCatching { departmentsApi.create(name) }
            _uiState.update {
                it.copy(
                    actionSaving = false,
                    showCreate = false,
                    actionError = result.exceptionOrNull()?.let(::networkMessage),
                )
            }
            if (result.isSuccess) load()
        }
    }

    fun openEdit(department: DepartmentDto) = _uiState.update { it.copy(editTarget = department, editName = department.name) }
    fun dismissEdit() = _uiState.update { it.copy(editTarget = null) }
    fun onEditNameChange(value: String) = _uiState.update { it.copy(editName = value) }

    fun submitEdit() {
        val target = _uiState.value.editTarget ?: return
        val name = _uiState.value.editName.trim()
        if (name.isBlank()) return
        _uiState.update { it.copy(actionSaving = true) }
        viewModelScope.launch {
            val result = runCatching { departmentsApi.update(target.id, DepartmentUpdateInput(name = name)) }
            _uiState.update {
                it.copy(
                    actionSaving = false,
                    editTarget = null,
                    actionError = result.exceptionOrNull()?.let(::networkMessage),
                )
            }
            if (result.isSuccess) load()
        }
    }

    fun requestDelete(department: DepartmentDto) = _uiState.update { it.copy(deleteTarget = department) }
    fun dismissDelete() = _uiState.update { it.copy(deleteTarget = null) }

    fun confirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        _uiState.update { it.copy(actionSaving = true) }
        viewModelScope.launch {
            val result = runCatching { departmentsApi.remove(target.id) }
            _uiState.update {
                it.copy(
                    actionSaving = false,
                    deleteTarget = null,
                    actionError = result.exceptionOrNull()?.let(::networkMessage),
                )
            }
            if (result.isSuccess) load()
        }
    }

    fun dismissActionError() = _uiState.update { it.copy(actionError = null) }
}
