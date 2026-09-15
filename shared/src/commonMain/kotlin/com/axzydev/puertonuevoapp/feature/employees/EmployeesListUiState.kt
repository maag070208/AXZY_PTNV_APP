package com.axzydev.puertonuevoapp.feature.employees

import com.axzydev.puertonuevoapp.core.network.users.UserDto

data class EmployeesListUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val employees: List<UserDto> = emptyList(),
    val query: String = "",
    val departmentFilter: String = "",
) {
    val departmentOptions: List<String>
        get() = employees.mapNotNull { it.department?.name }.distinct().sorted()

    val filtered: List<UserDto>
        get() = employees.filter { e ->
            (departmentFilter.isBlank() || e.department?.name == departmentFilter) &&
                (query.isBlank() ||
                    e.name.contains(query, ignoreCase = true) ||
                    e.numeroEmpleado.orEmpty().contains(query, ignoreCase = true) ||
                    e.puesto.orEmpty().contains(query, ignoreCase = true))
        }
}
