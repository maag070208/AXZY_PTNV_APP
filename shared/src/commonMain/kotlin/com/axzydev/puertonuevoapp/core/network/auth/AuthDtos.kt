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
    val employeeNumber: String? = null,
    val jobTitle: String? = null,
    val department: DepartmentRefDto? = null,
    /** Ruta de la foto (`/hr/:id/photo/raw`) o null si no tiene. */
    val photoUrl: String? = null,
    /**
     * Permisos efectivos (clave → alcance NONE/OWN/AREA/ALL), solo los
     * distintos de NONE. Solo `GET /auth/me` los trae (el login no).
     */
    val permissions: Map<String, String>? = null,
    /** Idioma del sistema (`sys_config.LANGUAGE`: es | en). */
    val language: String? = null,
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
