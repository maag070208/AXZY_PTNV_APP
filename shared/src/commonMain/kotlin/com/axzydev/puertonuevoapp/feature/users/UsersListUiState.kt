package com.axzydev.puertonuevoapp.feature.users

import com.axzydev.puertonuevoapp.core.network.users.UserDto

data class UsersListUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val users: List<UserDto> = emptyList(),
    val query: String = "",
    val roleFilter: String? = null,
    val selectedForDelete: UserDto? = null,
    val selectedForPassword: UserDto? = null,
    val newPassword: String = "",
    val actionSaving: Boolean = false,
    val actionError: String? = null,
) {
    val filtered: List<UserDto>
        get() = users.filter { user ->
            (roleFilter == null || user.role == roleFilter) &&
                (query.isBlank() || user.name.contains(query, true) || user.username.contains(query, true) || user.numeroEmpleado.orEmpty().contains(query, true))
        }
}

val userRoleFilters: List<Pair<String?, String>> = listOf(
    null to "Todos",
    "ADMIN" to "Admin",
    "GERENTE" to "Gerente",
    "JEFE_DE_AREA" to "Jefe",
    "EMPLEADO" to "Empleado",
)
