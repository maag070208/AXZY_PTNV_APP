package com.axzydev.puertonuevoapp.core.session

data class SessionUser(
    val id: String,
    val username: String,
    val name: String,
    val role: String,
    val departmentId: String?,
) {
    val isAdmin: Boolean get() = role == "ADMIN"

    /** Matriz de autorización — API_DOCUMENTATION.md §1.3. */
    val canManageCatalogs: Boolean get() = role == "ADMIN"
    val canDeleteCartas: Boolean get() = role in setOf("ADMIN", "GERENTE", "JEFE_DE_AREA")
    val canRegisterMovement: Boolean get() = role in setOf("ADMIN", "GERENTE", "JEFE_DE_AREA")
    val canGenerateCartas: Boolean get() = role in setOf("ADMIN", "GERENTE")
    val canCreateTicket: Boolean get() = role != "EMPLEADO"
    val canDeleteTicket: Boolean get() = role == "ADMIN"
    val canSeeAdminTasks: Boolean get() = role == "ADMIN" || role == "GERENTE"
    val canSeeAudit: Boolean get() = role == "ADMIN"
}

sealed interface AuthState {
    data object Loading : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val user: SessionUser) : AuthState
}
