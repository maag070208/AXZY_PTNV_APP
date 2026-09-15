package com.axzydev.puertonuevoapp.core.nav

data class ScreenChrome(
    val title: String,
    val showBack: Boolean = false,
    val showBottomNavigation: Boolean = true,
    val section: MainSection = MainSection.HOME,
)

enum class MainSection {
    HOME,
    TICKETS,
    DEVICES,
    USERS,
}

fun Screen.chrome(): ScreenChrome = when (this) {
    Screen.Home -> ScreenChrome("Inicio", section = MainSection.HOME)

    // Tickets
    Screen.TicketsList -> ScreenChrome("Tickets", section = MainSection.TICKETS)
    Screen.NewTicket -> ScreenChrome("Nuevo ticket", showBack = true, showBottomNavigation = false, section = MainSection.TICKETS)
    is Screen.TicketDetail -> ScreenChrome("Ticket", showBack = true, showBottomNavigation = false, section = MainSection.TICKETS)
    is Screen.EditTicket -> ScreenChrome("Editar ticket", showBack = true, showBottomNavigation = false, section = MainSection.TICKETS)
    is Screen.TicketsKanban -> ScreenChrome(if (ticketId != null) "Tareas del ticket" else "Tablero de tareas", showBack = true, showBottomNavigation = false, section = MainSection.TICKETS)
    Screen.MyTasks -> ScreenChrome("Mis tareas", showBack = true, showBottomNavigation = false, section = MainSection.TICKETS)
    Screen.AdminTasks -> ScreenChrome("Administración de tareas", showBack = true, showBottomNavigation = false, section = MainSection.TICKETS)

    // Devices
    Screen.DevicesList -> ScreenChrome("Dispositivos", section = MainSection.DEVICES)
    is Screen.DeviceDetail -> ScreenChrome("Dispositivo", showBack = true, showBottomNavigation = false, section = MainSection.DEVICES)
    is Screen.DeviceForm -> ScreenChrome(if (id == null) "Nuevo dispositivo" else "Editar dispositivo", showBack = true, showBottomNavigation = false, section = MainSection.DEVICES)
    Screen.DeviceTypesList -> ScreenChrome("Tipos de dispositivo", showBack = true, showBottomNavigation = false, section = MainSection.DEVICES)
    is Screen.DeviceTypeForm -> ScreenChrome(if (id == null) "Nuevo tipo de dispositivo" else "Editar tipo de dispositivo", showBack = true, showBottomNavigation = false, section = MainSection.DEVICES)

    // Users / employees
    Screen.UsersList -> ScreenChrome("Usuarios", section = MainSection.USERS)
    is Screen.UserForm -> ScreenChrome(if (id == null) "Nuevo usuario" else "Editar usuario", showBack = true, showBottomNavigation = false, section = MainSection.USERS)
    is Screen.UserHistory -> ScreenChrome("Historial del usuario", showBack = true, showBottomNavigation = false, section = MainSection.USERS)
    Screen.EmployeesList -> ScreenChrome("Empleados", showBack = true, showBottomNavigation = false, section = MainSection.HOME)

    // Departments
    Screen.DepartmentsList -> ScreenChrome("Departamentos", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    is Screen.DepartmentDetail -> ScreenChrome("Detalle del departamento", showBack = true, showBottomNavigation = false, section = MainSection.HOME)

    // Inventory
    Screen.InventoryIndex -> ScreenChrome("Inventario", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    Screen.LocationsList -> ScreenChrome("Ubicaciones", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    is Screen.LocationDetail -> ScreenChrome("Ubicación", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    is Screen.LocationForm -> ScreenChrome(if (id == null) "Nueva ubicación" else "Editar ubicación", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    Screen.InventoryMovements -> ScreenChrome("Kardex", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    is Screen.NewInventoryMovement -> ScreenChrome("Registrar movimiento", showBack = true, showBottomNavigation = false, section = MainSection.HOME)

    // Salidas
    Screen.SalidasList -> ScreenChrome("Salidas de material", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    is Screen.SalidaForm -> ScreenChrome(if (id == null) "Nueva salida" else "Editar salida", showBack = true, showBottomNavigation = false, section = MainSection.HOME)

    // Cartas
    Screen.CartasList -> ScreenChrome("Cartas responsivas", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    is Screen.CartaDetail -> ScreenChrome("Carta responsiva", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    is Screen.CartaForm -> ScreenChrome(if (id == null) "Nueva carta" else "Editar carta", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    Screen.GenerateCarta -> ScreenChrome("Generar cartas", showBack = true, showBottomNavigation = false, section = MainSection.HOME)

    Screen.Reports -> ScreenChrome("Reportes", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    Screen.Notifications -> ScreenChrome("Notificaciones", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    Screen.AuditLogs -> ScreenChrome("Auditoría", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
}
