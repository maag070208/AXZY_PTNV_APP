package com.axzydev.puertonuevoapp.core.nav

sealed interface Screen {
    data object Home : Screen

    /** Credencial digital del usuario (QR para el guardia). */
    data object MyCredential : Screen

    // Tickets
    data object TicketsList : Screen
    data class TicketDetail(val id: String) : Screen
    data object NewTicket : Screen
    data class EditTicket(val id: String) : Screen
    data class TicketsKanban(val ticketId: String? = null) : Screen
    data object MyTasks : Screen
    data object AdminTasks : Screen

    // Devices
    data object DevicesList : Screen
    data class DeviceDetail(val id: String) : Screen
    data class DeviceForm(val id: String? = null) : Screen
    data object DeviceTypesList : Screen
    data class DeviceTypeForm(val id: String? = null) : Screen

    // Users / employees
    data class UserForm(val id: String? = null) : Screen
    /** Lista de cuentas de usuario (`/users` en la web). */
    data object UsersList : Screen
    /** Historial de una cuenta (`/users/:id/history`). */
    data class UserHistory(val userId: String) : Screen
    /** Importación de cuentas por Excel (`/users/import`). */
    data object UserImport : Screen
    data object EmployeesList : Screen
    data class EmployeeProfile(val id: String) : Screen

    // Departments
    data object DepartmentsList : Screen
    data class DepartmentDetail(val id: String) : Screen

    // Inventory
    data object InventoryIndex : Screen
    data object LocationsList : Screen
    data class LocationDetail(val id: String) : Screen
    data class LocationForm(val id: String? = null) : Screen
    data object InventoryMovements : Screen
    data class NewInventoryMovement(val deviceId: String? = null) : Screen
    /** Entrada de unidades nuevas a un dispositivo existente (`STOCK_IN`). */
    data class DeviceStockIn(val deviceId: String) : Screen
    /** Bandeja de devoluciones del inventario (bitácora de `/inventory/returns`). */
    data object InventoryReturns : Screen
    /** Auditor de inventario: reglas y conciliación de unidades. */
    data object InventoryAudit : Screen
    /** Carga masiva de dispositivos por Excel. */
    data object DeviceImport : Screen

    // Salidas
    data object MaterialOutputsList : Screen
    data class MaterialOutputForm(val id: String? = null) : Screen

    // Cartas
    data object CustodyLettersList : Screen
    data class CustodyLetterDetail(val id: String) : Screen
    data class CustodyLetterForm(val id: String? = null) : Screen
    data object GenerateCustodyLetter : Screen

    // Control de acceso (portería)
    data object AccessScan : Screen
    data object AccessLog : Screen

    // RH: expedientes y actas administrativas
    data object EmployeeRecords : Screen
    data object DisciplinaryReports : Screen
    data class DisciplinaryReportDetail(val reportId: String) : Screen

    // Reloj checador (asistencia, reportes, padrón y relojes)
    data object TimeClock : Screen
    data object TimeClockEntriesExits : Screen
    data object TimeClockRoster : Screen
    data object TimeClockEmployees : Screen
    data object TimeClocks : Screen

    // Horarios, asignación, tiempo extra y pre nómina
    data object Schedules : Screen
    data class ScheduleForm(val scheduleId: String? = null) : Screen
    data object ScheduleAssign : Screen
    data object OvertimeApproval : Screen
    data object Payroll : Screen

    // Administración de catálogos
    data object Catalogs : Screen

    data object Reports : Screen
    data object Notifications : Screen
    data object AuditLogs : Screen
    data object RolesPermissions : Screen
}
