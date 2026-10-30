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
    /** Roles efectivos (principal + adicionales, `users.role` + `user_roles`). */
    val roles: List<String>? = null,
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
    /**
     * Contraseña provisional: el API exige cambiarla antes de usar el
     * sistema (`POST /auth/change-password`). `GET /auth/me` la vuelve a
     * traer tras el cambio (ya en false).
     */
    val mustChangePassword: Boolean = false,
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class LoginResponseDto(
    val token: String,
    /** Refresh token emitido por el API (`JWT_REFRESH_EXPIRES_IN`). */
    val refreshToken: String? = null,
    val user: AuthUserDto,
)

/**
 * Cuerpo de `POST /auth/refresh`. El endpoint NO devuelve usuario (a
 * diferencia del login): solo los dos tokens — el access nuevo y el refresh
 * rotado. El usuario se relee con `GET /auth/me`.
 */
@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class RefreshResponseDto(
    val token: String,
    val refreshToken: String? = null,
)

/** Cuerpo de `POST /auth/change-password` (responde 204 sin cuerpo). */
@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)
