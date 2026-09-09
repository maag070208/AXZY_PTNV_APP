package com.axzydev.puertonuevoapp.core.session

data class SessionUser(
    val id: String,
    val username: String,
    val name: String,
    val role: String,
    val departmentId: String?,
) {
    val isAdmin: Boolean get() = role == "ADMIN"
}

sealed interface AuthState {
    data object Loading : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val user: SessionUser) : AuthState
}
