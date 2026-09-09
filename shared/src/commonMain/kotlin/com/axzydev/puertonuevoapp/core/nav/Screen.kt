package com.axzydev.puertonuevoapp.core.nav

sealed interface Screen {
    data object Home : Screen
    data object TicketsList : Screen
    data class TicketDetail(val id: String) : Screen
    data object NewTicket : Screen
    data object DevicesList : Screen
    data class DeviceDetail(val id: String) : Screen
}
