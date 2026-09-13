package com.axzydev.puertonuevoapp.core.network

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val email: String? = null,
    val name: String,
    val role: String,
    val active: Boolean = true,
    val puesto: String? = null,
    val area: String? = null,
    val numeroEmpleado: String? = null,
    val empresa: String? = null,
    val departmentId: String? = null,
    val department: DepartmentRefDto? = null,
    val subareaId: String? = null,
    val subarea: SubareaRefDto? = null,
)

@Serializable
data class SubareaRefDto(
    val id: String,
    val name: String,
)

@Serializable
data class UserCreateRequest(
    val username: String,
    val email: String? = null,
    val password: String,
    val name: String,
    val role: String,
    val puesto: String? = null,
    val numeroEmpleado: String? = null,
    val empresa: String? = null,
    val departmentId: String? = null,
    val subareaId: String? = null,
)

@Serializable
data class UserUpdateRequest(
    val username: String? = null,
    val email: String? = null,
    val name: String? = null,
    val role: String? = null,
    val active: Boolean? = null,
    val puesto: String? = null,
    val numeroEmpleado: String? = null,
    val empresa: String? = null,
    val departmentId: String? = null,
    val subareaId: String? = null,
)

@Serializable
data class UserActiveRequest(val active: Boolean)

@Serializable
data class ChangePasswordRequest(val password: String)

@Serializable
data class UserDeleteResponse(
    val soft: Boolean = false,
    val forced: Boolean = false,
    val data: UserDto? = null,
)

@Serializable
data class UserHistoryEntry(
    val id: String,
    val type: String,
    val title: String,
    val detail: String,
    val timestamp: String,
    val refId: String? = null,
)
