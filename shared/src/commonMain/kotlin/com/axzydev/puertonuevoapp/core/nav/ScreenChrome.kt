package com.axzydev.puertonuevoapp.core.nav

import androidx.compose.runtime.staticCompositionLocalOf

/** Abre el menú lateral del shell (para pantallas con encabezado propio, p. ej. el Inicio de blancos). */
val LocalOpenMenu = staticCompositionLocalOf<() -> Unit> { {} }

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
    ACCESS,
}

fun Screen.chrome(): ScreenChrome = when (this) {
    Screen.Home -> ScreenChrome("Inicio", section = MainSection.HOME)
    Screen.MyCredential -> ScreenChrome("Mi credencial", section = MainSection.HOME)

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
    is Screen.UserForm -> ScreenChrome(if (id == null) "Nuevo personal" else "Editar personal", showBack = true, showBottomNavigation = false, section = MainSection.USERS)
    Screen.UsersList -> ScreenChrome("Usuarios", showBack = true, showBottomNavigation = false, section = MainSection.USERS)
    is Screen.UserHistory -> ScreenChrome("Historial de la cuenta", showBack = true, showBottomNavigation = false, section = MainSection.USERS)
    Screen.UserImport -> ScreenChrome("Importar usuarios", showBack = true, showBottomNavigation = false, section = MainSection.USERS)
    Screen.EmployeesList -> ScreenChrome("Personal", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    is Screen.EmployeeProfile -> ScreenChrome("Perfil", showBack = true, showBottomNavigation = false, section = MainSection.HOME)

    // Departments
    Screen.EmployeeRecords -> ScreenChrome("Expedientes", showBack = true, showBottomNavigation = false, section = MainSection.USERS)
    Screen.DisciplinaryReports -> ScreenChrome("Actas administrativas", showBack = true, showBottomNavigation = false, section = MainSection.USERS)
    is Screen.DisciplinaryReportDetail -> ScreenChrome("Acta administrativa", showBack = true, showBottomNavigation = false, section = MainSection.USERS)
    Screen.TimeClock -> ScreenChrome("Asistencia", showBack = true, showBottomNavigation = false, section = MainSection.USERS)
    Screen.TimeClockEntriesExits -> ScreenChrome("Entradas y salidas", showBack = true, showBottomNavigation = false, section = MainSection.USERS)
    Screen.TimeClockRoster -> ScreenChrome("Rol de turnos", showBack = true, showBottomNavigation = false, section = MainSection.USERS)
    Screen.TimeClockEmployees -> ScreenChrome("Padrón del reloj", showBack = true, showBottomNavigation = false, section = MainSection.USERS)
    Screen.TimeClocks -> ScreenChrome("Relojes", showBack = true, showBottomNavigation = false, section = MainSection.USERS)
    Screen.Schedules -> ScreenChrome("Horarios", showBack = true, showBottomNavigation = false, section = MainSection.USERS)
    is Screen.ScheduleForm -> ScreenChrome(if (scheduleId == null) "Nuevo horario" else "Editar horario", showBack = true, showBottomNavigation = false, section = MainSection.USERS)
    Screen.ScheduleAssign -> ScreenChrome("Asignar horarios", showBack = true, showBottomNavigation = false, section = MainSection.USERS)
    Screen.OvertimeApproval -> ScreenChrome("Tiempo extra", showBack = true, showBottomNavigation = false, section = MainSection.USERS)
    Screen.Payroll -> ScreenChrome("Pre nómina", showBack = true, showBottomNavigation = false, section = MainSection.USERS)
    Screen.Catalogs -> ScreenChrome("Catálogos", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    Screen.DepartmentsList -> ScreenChrome("Departamentos", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    is Screen.DepartmentDetail -> ScreenChrome("Detalle del departamento", showBack = true, showBottomNavigation = false, section = MainSection.HOME)

    // Inventory
    Screen.InventoryIndex -> ScreenChrome("Inventario", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    Screen.LocationsList -> ScreenChrome("Ubicaciones", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    is Screen.LocationDetail -> ScreenChrome("Ubicación", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    is Screen.LocationForm -> ScreenChrome(if (id == null) "Nueva ubicación" else "Editar ubicación", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    Screen.InventoryMovements -> ScreenChrome("Kardex", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    is Screen.NewInventoryMovement -> ScreenChrome("Registrar movimiento", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    is Screen.DeviceStockIn -> ScreenChrome("Agregar unidades", showBack = true, showBottomNavigation = false, section = MainSection.DEVICES)
    Screen.InventoryReturns -> ScreenChrome("Devoluciones", showBack = true, showBottomNavigation = false, section = MainSection.DEVICES)
    Screen.InventoryAudit -> ScreenChrome("Auditoría de inventario", showBack = true, showBottomNavigation = false, section = MainSection.DEVICES)
    Screen.DeviceImport -> ScreenChrome("Cargar dispositivos", showBack = true, showBottomNavigation = false, section = MainSection.DEVICES)

    // Salidas
    Screen.MaterialOutputsList -> ScreenChrome("Salidas de material", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    is Screen.MaterialOutputForm -> ScreenChrome(if (id == null) "Nueva salida" else "Editar salida", showBack = true, showBottomNavigation = false, section = MainSection.HOME)

    // Cartas
    Screen.CustodyLettersList -> ScreenChrome("Cartas responsivas", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    is Screen.CustodyLetterDetail -> ScreenChrome("Carta responsiva", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    is Screen.CustodyLetterForm -> ScreenChrome(if (id == null) "Nueva carta" else "Editar carta", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    Screen.GenerateCustodyLetter -> ScreenChrome("Generar cartas", showBack = true, showBottomNavigation = false, section = MainSection.HOME)

    // Control de acceso (portería)
    Screen.AccessScan -> ScreenChrome("Portería", section = MainSection.ACCESS)
    Screen.AccessLog -> ScreenChrome("Registros de acceso", section = MainSection.ACCESS)

    // Blancos
    // "Blancos" y "Mi equipo" son destinos de la barra inferior: pantalla raíz, sin flecha de
    // regreso (la flecha sobraba y confundía con un tab; el header queda con el menú).
    Screen.LinenOrders -> ScreenChrome("Blancos", showBack = false, showBottomNavigation = true, section = MainSection.HOME)
    Screen.LinenEquipment -> ScreenChrome("Mi equipo", showBack = false, showBottomNavigation = true, section = MainSection.HOME)
    Screen.LinenRooms -> ScreenChrome("Habitaciones", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    Screen.Housekeeping -> ScreenChrome("Limpieza", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    Screen.LinenHistory -> ScreenChrome("Historial", showBack = true, showBottomNavigation = false, section = MainSection.HOME)

    Screen.Reports -> ScreenChrome("Reportes", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    Screen.Notifications -> ScreenChrome("Notificaciones", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    Screen.AuditLogs -> ScreenChrome("Auditoría", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
    Screen.RolesPermissions -> ScreenChrome("Roles y permisos", showBack = true, showBottomNavigation = false, section = MainSection.HOME)
}
