package com.axzydev.puertonuevoapp.core.session

import com.axzydev.puertonuevoapp.core.session.PermissionKeys as P

/**
 * Usuario de la sesión. Lo que puede hacer sale de sus **permisos efectivos**
 * (`GET /auth/me` → `permissions`: clave → alcance, solo los distintos de
 * NONE), igual que la web: la app no reimplementa la matriz de roles, así un
 * cambio en "Roles y permisos" aplica sin recompilar.
 */
data class SessionUser(
    val id: String,
    val username: String,
    val name: String,
    val role: String,
    val departmentId: String?,
    val permissions: Map<String, String> = emptyMap(),
) {
    /** Alcance efectivo del permiso (NONE si no lo tiene). */
    fun scopeOf(permission: String): String = permissions[permission] ?: PermissionScopes.NONE

    /** ¿Tiene el permiso con cualquier alcance distinto de NONE? */
    fun can(permission: String): Boolean = scopeOf(permission) != PermissionScopes.NONE

    /**
     * ¿El registro cae dentro del alcance del permiso? Espejo de `withinScope`
     * del API: lo propio es lo que creó, tiene asignado o donde tiene una
     * tarea; AREA incluye lo propio y lo de su departamento.
     */
    fun withinScope(
        permission: String,
        createdById: String?,
        assignedToId: String?,
        departmentId: String?,
        assignmentUserIds: List<String> = emptyList(),
    ): Boolean {
        val own = createdById == id || assignedToId == id || id in assignmentUserIds
        return when (scopeOf(permission)) {
            PermissionScopes.ALL -> true
            PermissionScopes.AREA -> own || (this.departmentId != null && departmentId == this.departmentId)
            PermissionScopes.OWN -> own
            else -> false
        }
    }

    val isEmployee: Boolean get() = role == "EMPLOYEE"
    val isGuard: Boolean get() = role == "GUARD"

    // Panel e informes
    val canViewDashboard: Boolean get() = can(P.DASHBOARD_VIEW)
    val canViewReports: Boolean get() = can(P.REPORTS_VIEW)
    val canSeeAudit: Boolean get() = can(P.AUDIT_VIEW)

    // Inventario
    val canViewDevices: Boolean get() = can(P.DEVICES_VIEW)
    val canCreateDevices: Boolean get() = can(P.DEVICES_CREATE)
    val canEditDevices: Boolean get() = can(P.DEVICES_EDIT)

    /** Los movimientos de inventario los protege `devices.edit` en el API. */
    val canRegisterMovement: Boolean get() = can(P.DEVICES_EDIT)

    // Cartas responsivas (préstamos en el API)
    val canViewCustodyLetters: Boolean get() = can(P.LOANS_VIEW)
    val canCreateCustodyLetters: Boolean get() = can(P.LOANS_CREATE)
    val canEditCustodyLetters: Boolean get() = can(P.LOANS_EDIT)
    val canDeleteCustodyLetters: Boolean get() = can(P.LOANS_DELETE)
    val canGenerateCustodyLetters: Boolean get() = can(P.LOANS_CREATE)

    val canRegisterMaterialOutputs: Boolean get() = can(P.MATERIAL_OUTPUTS_REGISTER)

    val canViewInventory: Boolean
        get() = canViewDevices || canViewCustodyLetters || canRegisterMaterialOutputs

    // Personal, usuarios y catálogos
    val canViewHR: Boolean get() = can(P.HR_RECORDS)
    val canCreateUsers: Boolean get() = can(P.USERS_CREATE)
    val canEditUsers: Boolean get() = can(P.USERS_EDIT)
    val canManageDepartments: Boolean get() = can(P.DEPARTMENTS_MANAGE)
    val canManageCatalogs: Boolean get() = can(P.CATALOGS_MANAGE)
    val canManageRoles: Boolean get() = can(P.ROLES_MANAGE)

    // Tickets y tareas
    val canCreateTickets: Boolean get() = can(P.TICKETS_CREATE)
    val canDeleteTicket: Boolean get() = can(P.TICKETS_DELETE)

    /** "Administrar tareas" (todas las tareas): igual que la web, con `tasks.complete`. */
    val canSeeAdminTasks: Boolean get() = can(P.TASKS_COMPLETE)

    /** "Mis tareas" es la vista del empleado (en la web, igual). */
    val canSeeMyTasks: Boolean get() = isEmployee

    /** Crear tareas desde el tablero. */
    val canCreateAssignments: Boolean get() = can(P.TASKS_ASSIGN)

    // Control de acceso
    /** Escanear credenciales y registrar entradas/salidas. */
    val canScanCredential: Boolean get() = can(P.ACCESS_SCAN)

    /** Bitácora completa de portería (`POST /access/query`). */
    val canQueryAccessLog: Boolean get() = can(P.ACCESS_LOG)

    /** Registros de acceso: la bitácora, o los escaneos propios del día para quien escanea. */
    val canViewAccessLog: Boolean get() = canQueryAccessLog || canScanCredential
}

sealed interface AuthState {
    data object Loading : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val user: SessionUser) : AuthState
}
