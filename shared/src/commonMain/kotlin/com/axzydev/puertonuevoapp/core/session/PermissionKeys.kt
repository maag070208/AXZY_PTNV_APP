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
    const val ATTENDANCE_VIEW = "attendance.view"
    const val OVERTIME_VIEW = "overtime.view"

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

    const val TIME_CLOCK_LINK = "time_clock.link"
    const val SYSTEM_CONFIGURE = "system.configure"

    // Claves del catálogo del API que usan las pantallas nuevas (el catálogo lo manda la BD).
    const val INVENTORY_AUDIT = "inventory.audit"
    const val REPORTS_EXPORT = "reports.export"
    const val HR_DISCIPLINARY_REPORTS = "hr.disciplinary_reports"
    const val USERS_DELETE = "users.delete"
    const val USERS_PERMISSIONS = "users.permissions"
    const val ACCESS_VOID = "access.void"
    const val ACCESS_SITES = "access.sites"
    const val TIME_CLOCK_VIEW = "time_clock.view"
    const val TIME_CLOCK_SYNC = "time_clock.sync"
    const val TIME_CLOCKS_MANAGE = "time_clocks.manage"
    const val SCHEDULES_VIEW = "schedules.view"
    const val SCHEDULES_MANAGE = "schedules.manage"
    const val OVERTIME_APPROVE = "overtime.approve"
    const val PAYROLL_VIEW = "payroll.view"
    const val KITCHEN_VIEW = "kitchen.view"
    const val KITCHEN_MANAGE = "kitchen.manage"
    const val KITCHEN_STOCK_IN = "kitchen.stock_in"
    const val KITCHEN_STOCK_OUT = "kitchen.stock_out"
    const val KITCHEN_ADJUST = "kitchen.adjust"
    const val PURCHASE_ORDERS_VIEW = "purchase_orders.view"
    const val PURCHASE_ORDERS_CREATE = "purchase_orders.create"
    const val PURCHASE_ORDERS_APPROVE = "purchase_orders.approve"
    const val PURCHASE_ORDERS_CANCEL = "purchase_orders.cancel"
    const val INVOICES_VIEW = "invoices.view"
    const val INVOICES_REGISTER = "invoices.register"
    const val TIME_CLOCK_WRITE = "time_clock.write"
    const val HOTEL_ROOMS_VIEW = "hotel_rooms.view"
    const val HOTEL_ROOMS_MANAGE = "hotel_rooms.manage"
    const val HOTEL_ROOMS_STATUS = "hotel_rooms.status"
    const val HOUSEKEEPING_ASSIGN = "housekeeping.assign"
    const val HOUSEKEEPING_INSPECT = "housekeeping.inspect"
    const val HOTEL_STORE_MANAGE = "hotel_store.manage"
    const val SUPPLIERS_VIEW = "suppliers.view"
    const val SUPPLIERS_MANAGE = "suppliers.manage"
    const val HOTEL_STORE_VIEW = "hotel_store.view"
    const val HOTEL_STORE_STOCK_IN = "hotel_store.stock_in"
    const val HOTEL_STORE_STOCK_OUT = "hotel_store.stock_out"
    const val HOTEL_STORE_TRANSFER = "hotel_store.transfer"
    const val HOTEL_STORE_ADJUST = "hotel_store.adjust"
    const val HOTEL_STORE_ADJUST_APPROVE = "hotel_store.adjust_approve"
    const val HOTEL_STORE_STOCKTAKE = "hotel_store.stocktake"
    const val HOUSEKEEPING_OPERATE = "housekeeping.operate"
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
