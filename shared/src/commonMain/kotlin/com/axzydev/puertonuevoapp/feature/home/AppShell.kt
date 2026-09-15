package com.axzydev.puertonuevoapp.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.MainSection
import com.axzydev.puertonuevoapp.core.nav.PlatformBackHandler
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.nav.chrome
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.ui.AppSnackbarHost
import com.axzydev.puertonuevoapp.core.ui.BrandLogoBadge
import com.axzydev.puertonuevoapp.core.ui.PlaceholderScreen
import com.axzydev.puertonuevoapp.feature.departments.DepartmentDetailScreen
import com.axzydev.puertonuevoapp.feature.departments.DepartmentsListScreen
import com.axzydev.puertonuevoapp.feature.devices.DeviceDetailScreen
import com.axzydev.puertonuevoapp.feature.devices.DeviceFormScreen
import com.axzydev.puertonuevoapp.feature.devices.DevicesListScreen
import com.axzydev.puertonuevoapp.feature.devicetypes.DeviceTypeFormScreen
import com.axzydev.puertonuevoapp.feature.devicetypes.DeviceTypesListScreen
import com.axzydev.puertonuevoapp.feature.employees.EmployeesListScreen
import com.axzydev.puertonuevoapp.feature.tickets.AdminTasksScreen
import com.axzydev.puertonuevoapp.feature.tickets.EditTicketScreen
import com.axzydev.puertonuevoapp.feature.tickets.MyTasksScreen
import com.axzydev.puertonuevoapp.feature.tickets.NewTicketScreen
import com.axzydev.puertonuevoapp.feature.tickets.TicketDetailScreen
import com.axzydev.puertonuevoapp.feature.tickets.TicketsKanbanScreen
import com.axzydev.puertonuevoapp.feature.tickets.TicketsListScreen
import com.axzydev.puertonuevoapp.feature.users.UserFormScreen
import com.axzydev.puertonuevoapp.feature.users.UserHistoryScreen
import com.axzydev.puertonuevoapp.feature.users.UsersListScreen

/**
 * Shell de la app: top bar de marca (o back+título en pantallas hijas),
 * bottom nav de 4 secciones (Home/Tickets/Devices/Users — Users solo si el
 * rol puede administrar catálogos) y el `AppNavHost` que despacha la
 * pantalla actual del [com.axzydev.puertonuevoapp.core.nav.Navigator].
 */
@Composable
fun AppShell() {
    val navigator = LocalNavigator.current
    val authRepository = AppContainer.authRepository
    val authState by authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user

    val current = navigator.current
    val chrome = current.chrome()

    PlatformBackHandler(enabled = navigator.backStack.size > 1) {
        navigator.pop()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (chrome.showBack) {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atrás",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
                title = {
                    if (chrome.showBack) {
                        Text(
                            chrome.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BrandLogoBadge(size = 40.dp, cornerRadius = 12.dp, innerPadding = 5.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                chrome.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navigator.push(Screen.Notifications) }) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "Notificaciones",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { authRepository.logout() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Cerrar sesión",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            if (chrome.showBottomNavigation) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        selected = chrome.section == MainSection.HOME,
                        onClick = { navigator.switchTab(Screen.Home) },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text("Inicio") },
                    )
                    NavigationBarItem(
                        selected = chrome.section == MainSection.TICKETS,
                        onClick = { navigator.switchTab(Screen.TicketsList) },
                        icon = { Icon(Icons.Filled.ConfirmationNumber, contentDescription = null) },
                        label = { Text("Tickets") },
                    )
                    NavigationBarItem(
                        selected = chrome.section == MainSection.DEVICES,
                        onClick = { navigator.switchTab(Screen.DevicesList) },
                        icon = { Icon(Icons.Filled.Devices, contentDescription = null) },
                        label = { Text("Equipos") },
                    )
                    if (user?.canManageCatalogs == true) {
                        NavigationBarItem(
                            selected = chrome.section == MainSection.USERS,
                            onClick = { navigator.switchTab(Screen.UsersList) },
                            icon = { Icon(Icons.Filled.Group, contentDescription = null) },
                            label = { Text("Usuarios") },
                        )
                    }
                }
            }
        },
        snackbarHost = { AppSnackbarHost() },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            AppNavHost(current)
        }
    }
}

@Composable
private fun AppNavHost(screen: Screen) {
    when (screen) {
        Screen.Home -> HomeScreen()

        // Tickets — módulo 1
        Screen.TicketsList -> TicketsListScreen()
        is Screen.TicketDetail -> TicketDetailScreen(screen.id)
        Screen.NewTicket -> NewTicketScreen()
        is Screen.EditTicket -> EditTicketScreen(screen.id)
        is Screen.TicketsKanban -> TicketsKanbanScreen(screen.ticketId)
        Screen.MyTasks -> MyTasksScreen()
        Screen.AdminTasks -> AdminTasksScreen()

        // Devices — módulo 2
        Screen.DevicesList -> DevicesListScreen()
        is Screen.DeviceDetail -> DeviceDetailScreen(screen.id)
        is Screen.DeviceForm -> DeviceFormScreen(screen.id)
        Screen.DeviceTypesList -> DeviceTypesListScreen()
        is Screen.DeviceTypeForm -> DeviceTypeFormScreen(screen.id)

        // Users / employees — módulo 3
        Screen.UsersList -> UsersListScreen()
        is Screen.UserForm -> UserFormScreen(screen.id)
        is Screen.UserHistory -> UserHistoryScreen(screen.id)
        Screen.EmployeesList -> EmployeesListScreen()

        // Departments — módulo 3
        Screen.DepartmentsList -> DepartmentsListScreen()
        is Screen.DepartmentDetail -> DepartmentDetailScreen(screen.id)

        // Inventory — módulo 4
        Screen.InventoryIndex -> PlaceholderScreen("Inventario")
        Screen.LocationsList -> PlaceholderScreen("Ubicaciones")
        is Screen.LocationDetail -> PlaceholderScreen("Ubicación")
        is Screen.LocationForm -> PlaceholderScreen("Formulario de ubicación")
        Screen.InventoryMovements -> PlaceholderScreen("Kardex")
        is Screen.NewInventoryMovement -> PlaceholderScreen("Registrar movimiento")

        // Salidas / Cartas / Reports / Notifications / Audit — módulo 5
        Screen.SalidasList -> PlaceholderScreen("Salidas de material")
        is Screen.SalidaForm -> PlaceholderScreen("Formulario de salida")
        Screen.CartasList -> PlaceholderScreen("Cartas responsivas")
        is Screen.CartaDetail -> PlaceholderScreen("Carta responsiva")
        is Screen.CartaForm -> PlaceholderScreen("Formulario de carta")
        Screen.Reports -> PlaceholderScreen("Reportes")
        Screen.Notifications -> PlaceholderScreen("Notificaciones")
        Screen.AuditLogs -> PlaceholderScreen("Auditoría")
    }
}
