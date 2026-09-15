package com.axzydev.puertonuevoapp.feature.departments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentsApi
import com.axzydev.puertonuevoapp.core.network.departments.SubareaDto
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DepartmentDetailViewModel(
    private val departmentId: String,
    private val departmentsApi: DepartmentsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DepartmentDetailUiState())
    val uiState: StateFlow<DepartmentDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { departmentsApi.get(departmentId) }
            _uiState.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let(::networkMessage),
                    department = result.getOrNull(),
                )
            }
        }
    }

    fun onNewSubareaChange(value: String) = _uiState.update { it.copy(newSubarea = value) }

    fun addSubarea() {
        val name = _uiState.value.newSubarea.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            val result = runCatching { departmentsApi.addSubarea(departmentId, name) }
            if (result.isSuccess) {
                _uiState.update { it.copy(newSubarea = "") }
                load()
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.let(::networkMessage)) }
            }
        }
    }

    fun requestDeleteSubarea(subarea: SubareaDto) = _uiState.update { it.copy(subareaToDelete = subarea) }
    fun dismissDeleteSubarea() = _uiState.update { it.copy(subareaToDelete = null) }

    fun confirmDeleteSubarea() {
        val subarea = _uiState.value.subareaToDelete ?: return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = runCatching { departmentsApi.removeSubarea(subarea.id) }
            _uiState.update {
                it.copy(saving = false, subareaToDelete = null, error = result.exceptionOrNull()?.let(::networkMessage) ?: it.error)
            }
            if (result.isSuccess) load()
        }
    }

    fun requestDeleteDepartment() = _uiState.update { it.copy(showDeleteDept = true) }
    fun dismissDeleteDepartment() = _uiState.update { it.copy(showDeleteDept = false) }

    fun confirmDeleteDepartment() {
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = runCatching { departmentsApi.remove(departmentId) }
            _uiState.update {
                it.copy(
                    saving = false,
                    showDeleteDept = false,
                    deleted = result.isSuccess,
                    error = result.exceptionOrNull()?.let(::networkMessage) ?: it.error,
                )
            }
        }
    }
}
