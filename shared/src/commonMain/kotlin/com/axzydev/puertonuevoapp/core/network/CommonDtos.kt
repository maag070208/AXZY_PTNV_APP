package com.axzydev.puertonuevoapp.core.network

import kotlinx.serialization.Serializable

@Serializable
data class DepartmentRefDto(
    val id: String,
    val name: String,
)

@Serializable
data class UserRefDto(
    val id: String,
    val name: String,
    val username: String,
    val puesto: String? = null,
    val numeroEmpleado: String? = null,
)

@Serializable
data class ApiErrorBody(
    val error: String? = null,
    val message: String? = null,
)
