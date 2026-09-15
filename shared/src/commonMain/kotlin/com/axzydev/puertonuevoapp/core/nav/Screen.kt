package com.axzydev.puertonuevoapp.core.nav

sealed interface Screen {
    data object Home : Screen

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
    data object UsersList : Screen
    data class UserForm(val id: String? = null) : Screen
    data class UserHistory(val id: String) : Screen
    data object EmployeesList : Screen

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

    // Salidas
    data object SalidasList : Screen
    data class SalidaForm(val id: String? = null) : Screen

    // Cartas
    data object CartasList : Screen
    data class CartaDetail(val id: String) : Screen
    data class CartaForm(val id: String? = null) : Screen

    data object Reports : Screen
    data object Notifications : Screen
    data object AuditLogs : Screen
}
