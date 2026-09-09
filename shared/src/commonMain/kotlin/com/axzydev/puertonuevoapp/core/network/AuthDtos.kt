package com.axzydev.puertonuevoapp.core.network

import kotlinx.serialization.Serializable

@Serializable
data class AuthUserDto(
    val id: String,
    val username: String,
    val name: String,
    val role: String,
    val departmentId: String? = null,
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
