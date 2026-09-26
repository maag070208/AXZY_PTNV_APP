package com.axzydev.puertonuevoapp.core.session

/**
 * Claves del catálogo de permisos del API (`permissions.key`, formato
 * `modulo.accion`). El catálogo vive en la BD y puede crecer sin recompilar;
 * aquí solo están las claves que la app consulta para mostrar/ocultar UI.
 * La API es la que decide: la app solo esconde lo que la API rechazaría.
 */
object PermissionKeys {
    const val TICKETS_VIEW = "tickets.view"
    const val TICKETS_CREATE = "tickets.create"
    const val TICKETS_EDIT = "tickets.edit"
    const val TICKETS_CLOSE = "tickets.close"
    const val TICKETS_DELETE = "tickets.delete"

    const val TASKS_VIEW = "tasks.view"
    const val TASKS_ASSIGN = "tasks.assign"
    const val TASKS_COMPLETE = "tasks.complete"

    const val DEVICES_VIEW = "devices.view"
    const val DEVICES_CREATE = "devices.create"
    const val DEVICES_EDIT = "devices.edit"
    const val DEVICES_DELETE = "devices.delete"

    const val LOANS_VIEW = "loans.view"
    const val LOANS_CREATE = "loans.create"
    const val LOANS_EDIT = "loans.edit"
    const val LOANS_DELETE = "loans.delete"

    const val MATERIAL_OUTPUTS_REGISTER = "material_outputs.register"

    const val REPORTS_VIEW = "reports.view"

    const val HR_RECORDS = "hr.records"

    const val USERS_VIEW = "users.view"
    const val USERS_CREATE = "users.create"
    const val USERS_EDIT = "users.edit"

    const val DEPARTMENTS_MANAGE = "departments.manage"
    const val CATALOGS_MANAGE = "catalogs.manage"

    const val ACCESS_SCAN = "access.scan"
    const val ACCESS_LOG = "access.log"

    const val DASHBOARD_VIEW = "dashboard.view"
    const val AUDIT_VIEW = "audit.view"
    const val ROLES_MANAGE = "roles.manage"
}

/** Alcance efectivo de un permiso (enum `PermissionScope` del API). */
object PermissionScopes {
    const val NONE = "NONE"
    const val OWN = "OWN"
    const val AREA = "AREA"
    const val ALL = "ALL"

    /** Orden canónico, de menor a mayor alcance. */
    val ORDER: List<String> = listOf(NONE, OWN, AREA, ALL)
}

/** Etiqueta visible de un alcance. */
fun permissionScopeLabel(scope: String): String = when (scope) {
    PermissionScopes.NONE -> "Ninguno"
    PermissionScopes.OWN -> "Propio"
    PermissionScopes.AREA -> "Área"
    PermissionScopes.ALL -> "Todo"
    else -> scope
}
