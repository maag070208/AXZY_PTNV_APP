package com.axzydev.puertonuevoapp.core.session

data class SessionUser(
    val id: String,
    val username: String,
    val name: String,
    val role: String,
    val departmentId: String?,
) {
    val isAdmin: Boolean get() = role == "ADMIN"
    val isGerente: Boolean get() = role == "GERENTE"
    val isJefeArea: Boolean get() = role == "JEFE_DE_AREA"
    val isEmpleado: Boolean get() = role == "EMPLEADO"

    /** Matriz de autorización — API_DOCUMENTATION.md §1.3. */
    val canManageCatalogs: Boolean get() = role == "ADMIN"
    val canDeleteCartas: Boolean get() = role in setOf("ADMIN", "GERENTE", "JEFE_DE_AREA")
    val canRegisterMovement: Boolean get() = role in setOf("ADMIN", "GERENTE", "JEFE_DE_AREA")
    val canGenerateCartas: Boolean get() = role in setOf("ADMIN", "GERENTE")
    val canDeleteTicket: Boolean get() = role == "ADMIN"
    val canSeeAdminTasks: Boolean get() = role == "ADMIN" || role == "GERENTE"

    /** "Mis tareas" es la vista del empleado (en la web, igual). */
    val canSeeMyTasks: Boolean get() = isEmpleado

    /** Crear tareas desde el tablero (en la web, todos menos EMPLEADO). */
    val canCreateAssignments: Boolean get() = !isEmpleado
    val canSeeAudit: Boolean get() = role == "ADMIN"

    /** Expediente completo de personal (médico/oficial/documentos). */
    val canManageHR: Boolean get() = role == "ADMIN" || role == "RECURSOS_HUMANOS"

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
