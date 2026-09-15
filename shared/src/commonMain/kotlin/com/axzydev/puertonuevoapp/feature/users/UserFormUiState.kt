package com.axzydev.puertonuevoapp.feature.users

import com.axzydev.puertonuevoapp.core.network.departments.DepartmentDto

data class UserFormUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val step: Int = 0,
    val departments: List<DepartmentDto> = emptyList(),
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val role: String = "EMPLEADO",
    val numeroEmpleado: String = "",
    val puesto: String = "",
    val departmentId: String = "",
    val subareaId: String = "",
    val saved: Boolean = false,
) {
    val selectedDepartment: DepartmentDto? get() = departments.firstOrNull { it.id == departmentId }
}

val userRoleOptions: List<Pair<String, String>> = listOf(
    "ADMIN" to "ADMIN",
    "GERENTE" to "GERENTE",
    "JEFE_DE_AREA" to "JEFE DE AREA",
    "EMPLEADO" to "EMPLEADO",
)
