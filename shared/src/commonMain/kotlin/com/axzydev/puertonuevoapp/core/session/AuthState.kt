package com.axzydev.puertonuevoapp.core.session

data class SessionUser(
    val id: String,
    val username: String,
    val name: String,
    val role: String,
    val departmentId: String?,
) {
    val isAdmin: Boolean get() = role == "ADMIN"
    val isManager: Boolean get() = role == "MANAGER"
    val isAreaHead: Boolean get() = role == "AREA_HEAD"
    val isEmployee: Boolean get() = role == "EMPLOYEE"

    /** Matriz de autorización — API_DOCUMENTATION.md §1.3. */
    val canManageCatalogs: Boolean get() = role == "ADMIN"
    val canDeleteCustodyLetters: Boolean get() = role in setOf("ADMIN", "MANAGER", "AREA_HEAD")
    val canRegisterMovement: Boolean get() = role in setOf("ADMIN", "MANAGER", "AREA_HEAD")
    val canGenerateCustodyLetters: Boolean get() = role in setOf("ADMIN", "MANAGER")
    val canDeleteTicket: Boolean get() = role == "ADMIN"
    val canSeeAdminTasks: Boolean get() = role == "ADMIN" || role == "MANAGER"

    /** "Mis tareas" es la vista del empleado (en la web, igual). */
    val canSeeMyTasks: Boolean get() = isEmployee

    /** Crear tareas desde el tablero (en la web, todos menos EMPLOYEE). */
    val canCreateAssignments: Boolean get() = !isEmployee
    val canSeeAudit: Boolean get() = role == "ADMIN"

    /** Expediente completo de personal (médico/oficial/documentos). */
    val canManageHR: Boolean get() = role == "ADMIN" || role == "HUMAN_RESOURCES"

    /** Control de acceso — escanear credenciales y registrar entradas/salidas. */
    val canScanCredential: Boolean get() = role in setOf("GUARD", "ADMIN")

    /** Control de acceso — consultar registros de portería. */
    val canViewAccessLog: Boolean get() = role in setOf("GUARD", "ADMIN")
}

sealed interface AuthState {
    data object Loading : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val user: SessionUser) : AuthState
}
