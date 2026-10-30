package com.axzydev.puertonuevoapp.feature.departments

import com.axzydev.puertonuevoapp.core.network.departments.DepartmentDto

/** Filtro por estado del departamento (todos / activos / inactivos). */
enum class DepartmentStatusFilter(val label: String) {
    All("Todos"),
    Active("Activos"),
    Inactive("Inactivos"),
}

data class DepartmentsListUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val departments: List<DepartmentDto> = emptyList(),
    val query: String = "",
    val status: DepartmentStatusFilter = DepartmentStatusFilter.All,
    val showCreate: Boolean = false,
    val newName: String = "",
    val editTarget: DepartmentDto? = null,
    val editName: String = "",
    val deleteTarget: DepartmentDto? = null,
    val actionSaving: Boolean = false,
    val actionError: String? = null,
) {
    val filtered: List<DepartmentDto>
        get() = departments.filter { department ->
            (query.isBlank() || department.name.contains(query, ignoreCase = true)) &&
                when (status) {
                    DepartmentStatusFilter.All -> true
                    DepartmentStatusFilter.Active -> department.active
                    DepartmentStatusFilter.Inactive -> !department.active
                }
        }

    val total: Int get() = departments.size

    /** Personas activas adscritas, sumando el conteo de cada departamento. */
    val peopleCount: Int get() = departments.sumOf { it.count?.users ?: 0 }

    val inactiveCount: Int get() = departments.count { !it.active }
}
