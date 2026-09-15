package com.axzydev.puertonuevoapp.core.network.common

import kotlinx.serialization.Serializable

/** Referencia liviana a un departamento, embebida en otros DTOs. */
@Serializable
data class DepartmentRefDto(
    val id: String? = null,
    val name: String,
)

/** Referencia liviana a un usuario, embebida en otros DTOs (autor, firmante…). */
@Serializable
data class UserRefDto(
    val id: String,
    val name: String,
    val username: String,
    val puesto: String? = null,
    val numeroEmpleado: String? = null,
)

// Varios DELETE (ej. `/locations/:id`) responden `{ success: true }` en vez
// del recurso borrado — decodificar el DTO del recurso ahí revienta con
// MissingFieldException.
@Serializable
data class DeleteSuccessResponse(
    val success: Boolean,
)
