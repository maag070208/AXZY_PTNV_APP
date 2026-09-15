package com.axzydev.puertonuevoapp.feature.departments

import com.axzydev.puertonuevoapp.core.network.departments.DepartmentDto

data class DepartmentsListUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val departments: List<DepartmentDto> = emptyList(),
    val query: String = "",
    val showCreate: Boolean = false,
    val newName: String = "",
    val editTarget: DepartmentDto? = null,
    val editName: String = "",
    val deleteTarget: DepartmentDto? = null,
    val actionSaving: Boolean = false,
    val actionError: String? = null,
) {
    val filtered: List<DepartmentDto>
        get() = departments.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
}
