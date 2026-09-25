package com.axzydev.puertonuevoapp.core.network.auth

import com.axzydev.puertonuevoapp.core.network.common.DepartmentRefDto
import kotlinx.serialization.Serializable

@Serializable
data class AuthUserDto(
    val id: String,
    val username: String,
    val email: String? = null,
    val name: String,
    val role: String,
    val departmentId: String? = null,
    // Datos de la credencial: solo `GET /auth/me` los trae (el login no).
    val numeroEmpleado: String? = null,
    val puesto: String? = null,
    val department: DepartmentRefDto? = null,
    /** Ruta de la foto (`/personal/:id/foto/raw`) o null si no tiene. */
    val fotoUrl: String? = null,
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class LoginResponseDto(
    val token: String,
    val user: AuthUserDto,
)
