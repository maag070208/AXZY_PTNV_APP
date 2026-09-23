package com.axzydev.puertonuevoapp.core.network.users

import com.axzydev.puertonuevoapp.core.network.common.DepartmentRefDto
import kotlinx.serialization.Serializable

@Serializable
data class SubareaRefDto(
    val id: String,
    val name: String,
)

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
data class UserCreateInput(
    val username: String,
    val password: String,
    val name: String,
    val role: String,
    val email: String? = null,
    val puesto: String? = null,
    val numeroEmpleado: String? = null,
    val empresa: String? = null,
    val departmentId: String? = null,
    val subareaId: String? = null,
)

@Serializable
data class UserUpdateInput(
    val username: String? = null,
    val name: String? = null,
    val role: String? = null,
    val email: String? = null,
    val active: Boolean? = null,
    val puesto: String? = null,
    val numeroEmpleado: String? = null,
    val empresa: String? = null,
    val departmentId: String? = null,
    val subareaId: String? = null,
)

@Serializable
data class UserPasswordInput(val password: String)
