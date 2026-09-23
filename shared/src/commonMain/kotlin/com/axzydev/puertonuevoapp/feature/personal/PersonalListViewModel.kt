package com.axzydev.puertonuevoapp.feature.employees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.users.UsersApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EmployeesListViewModel(
    private val usersApi: UsersApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmployeesListUiState())
    val uiState: StateFlow<EmployeesListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { usersApi.empleados().sortedBy { it.name } }
            _uiState.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let(::networkMessage),
                    employees = result.getOrDefault(it.employees),
                )
            }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }
    fun onDepartmentFilterChange(value: String) = _uiState.update { it.copy(departmentFilter = value) }
}
