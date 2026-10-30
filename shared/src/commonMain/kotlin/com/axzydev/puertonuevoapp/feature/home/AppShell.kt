package com.axzydev.puertonuevoapp.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.MainSection
import com.axzydev.puertonuevoapp.core.nav.PlatformBackHandler
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.nav.chrome
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.SessionUser
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.ui.AppSnackbarHost
import com.axzydev.puertonuevoapp.core.ui.BentoBottomBar
import com.axzydev.puertonuevoapp.core.ui.BentoDrawer
import com.axzydev.puertonuevoapp.core.ui.BentoNavItem
import com.axzydev.puertonuevoapp.core.ui.BentoTopBar
import com.axzydev.puertonuevoapp.core.ui.LocalTopBarActions
import com.axzydev.puertonuevoapp.core.ui.TopBarActionsState
import com.axzydev.puertonuevoapp.core.ui.langostin.LangostinMascot
import com.axzydev.puertonuevoapp.feature.access.AccessLogScreen
import com.axzydev.puertonuevoapp.feature.access.AccessScanScreen
import com.axzydev.puertonuevoapp.feature.audit.AuditLogsScreen
import com.axzydev.puertonuevoapp.feature.credential.MyCredentialScreen
import com.axzydev.puertonuevoapp.feature.custodyletters.CustodyLetterDetailScreen
import com.axzydev.puertonuevoapp.feature.custodyletters.CustodyLetterFormScreen
import com.axzydev.puertonuevoapp.feature.custodyletters.CustodyLettersListScreen
import com.axzydev.puertonuevoapp.feature.custodyletters.GenerateCustodyLetterScreen
import com.axzydev.puertonuevoapp.feature.departments.DepartmentDetailScreen
import com.axzydev.puertonuevoapp.feature.departments.DepartmentsListScreen
import com.axzydev.puertonuevoapp.feature.devices.DeviceDetailScreen
import com.axzydev.puertonuevoapp.feature.devices.DeviceFormScreen
import com.axzydev.puertonuevoapp.feature.devices.DevicesListScreen
import com.axzydev.puertonuevoapp.feature.devicetypes.DeviceTypeFormScreen
import com.axzydev.puertonuevoapp.feature.devicetypes.DeviceTypesListScreen
import com.axzydev.puertonuevoapp.feature.hr.EmployeeProfileScreen
import com.axzydev.puertonuevoapp.feature.hr.EmployeesListScreen
import com.axzydev.puertonuevoapp.feature.inventory.DeviceStockInScreen
import com.axzydev.puertonuevoapp.feature.inventory.InventoryAuditScreen
import com.axzydev.puertonuevoapp.feature.inventory.InventoryImportScreen
import com.axzydev.puertonuevoapp.feature.inventory.InventoryIndexScreen
import com.axzydev.puertonuevoapp.feature.inventory.InventoryReturnsScreen
import com.axzydev.puertonuevoapp.feature.inventory.InventoryMovementsScreen
import com.axzydev.puertonuevoapp.feature.inventory.LocationDetailScreen
import com.axzydev.puertonuevoapp.feature.inventory.LocationFormScreen
import com.axzydev.puertonuevoapp.feature.inventory.LocationsListScreen
import com.axzydev.puertonuevoapp.feature.inventory.NewInventoryMovementScreen
import com.axzydev.puertonuevoapp.feature.materialoutputs.MaterialOutputFormScreen
import com.axzydev.puertonuevoapp.feature.materialoutputs.MaterialOutputsListScreen
import com.axzydev.puertonuevoapp.feature.langostin.LangostinOverlay
import com.axzydev.puertonuevoapp.feature.notifications.NotificationsScreen
import com.axzydev.puertonuevoapp.feature.reports.ReportsScreen
import com.axzydev.puertonuevoapp.feature.roles.RolesPermissionsScreen
import com.axzydev.puertonuevoapp.feature.tickets.AdminTasksScreen
import com.axzydev.puertonuevoapp.feature.tickets.EditTicketScreen
import com.axzydev.puertonuevoapp.feature.tickets.MyTasksScreen
import com.axzydev.puertonuevoapp.feature.tickets.NewTicketScreen
import com.axzydev.puertonuevoapp.feature.tickets.TicketDetailScreen
import com.axzydev.puertonuevoapp.feature.tickets.TicketsKanbanScreen
import com.axzydev.puertonuevoapp.feature.tickets.TicketsListScreen
import com.axzydev.puertonuevoapp.feature.users.UserFormScreen
import com.axzydev.puertonuevoapp.feature.users.UserHistoryScreen
import com.axzydev.puertonuevoapp.feature.users.UserImportScreen
import com.axzydev.puertonuevoapp.feature.users.UsersListScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Cada cuánto se consulta el contador de notificaciones sin leer mientras el shell está montado. */
private const val UNREAD_POLL_MS = 45_000L

/**
 * Shell de la app: header bento ([BentoTopBar]: hamburguesa/atrás, marca o título de la
 * pantalla, campana y avatar), drawer lateral por permisos ([BentoDrawer]) y barra inferior
 * blanca ([BentoBottomBar]) con destinos por permisos y botón central "Nuevo".
 */
@Composable
fun AppShell() {
    val navigator = LocalNavigator.current
    val authRepository = AppContainer.authRepository
    val authState by authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user

    val current = navigator.current
    val chrome = current.chrome()
    val isHome = current is Screen.Home
    // Desde el menú la pila queda en una sola pantalla: sin a dónde regresar, va la hamburguesa.
    val showBack = chrome.showBack && navigator.backStack.size > 1

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val topBarActions = remember { TopBarActionsState() }
    // El Inicio usa el lienzo bento; el resto de pantallas aún conserva el fondo anterior.
    val canvas = if (isHome) AppBento.canvas else MaterialTheme.colorScheme.background

    var unreadCount by remember { mutableStateOf(0) }
    // Panel de Langostín (botón central de la barra): se cierra al cambiar de pantalla.
    var langostinOpen by remember { mutableStateOf(false) }
    // Sondeo de no leídas (la web usa Ably; la app no tiene SDK todavía):
    // se refresca al navegar y cada 45 s mientras el shell está montado.
    LaunchedEffect(current) {
        langostinOpen = false
        while (true) {
            runCatching { AppContainer.notificationsApi.unreadCount() }
                .onSuccess { unreadCount = it }
            delay(UNREAD_POLL_MS)
        }
    }

    // Al abrir el menú no debe saltar el teclado (el buscador del drawer quedaba enfocado
    // en algunos teléfonos y tapaba el menú).
    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.isOpen }.collect { open ->
            if (open) keyboardController?.hide()
        }
    }
    PlatformBackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    PlatformBackHandler(enabled = langostinOpen) {
        langostinOpen = false
    }
    PlatformBackHandler(enabled = !drawerState.isOpen && navigator.backStack.size > 1) {
        navigator.pop()
    }

    fun go(screen: Screen) {
        navigator.switchTab(screen)
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            BentoDrawer(
                user = user,
                current = current,
                unreadCount = unreadCount,
                onNavigate = ::go,
                onLogout = { authRepository.logout() },
                onClose = { scope.launch { drawerState.close() } },
            )
        },
    ) {
        CompositionLocalProvider(LocalTopBarActions provides topBarActions) {
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    containerColor = canvas,
                    topBar = {
                        BentoTopBar(
                            title = chrome.title,
                            subtitle = if (showBack) sectionLabel(chrome.section) else null,
                            showBack = showBack,
                            onBack = { navigator.pop() },
                            onMenu = { scope.launch { drawerState.open() } },
                            unreadCount = unreadCount,
                            onNotifications = { navigator.push(Screen.Notifications) },
                            user = user,
                            onProfile = { navigator.push(Screen.MyCredential) },
                            action = topBarActions.action,
                            containerColor = canvas,
                        )
                    },
                    bottomBar = {
                        if (chrome.showBottomNavigation && user != null) {
                            val items = bottomBarItems(user, current) { navigator.switchTab(it) }
                            // Acción central: Langostín (asistente). El alta de ticket vive dentro
                            // del panel con `tickets.create`; abrir la ayuda no pide permiso.
                            val action = BentoNavItem(
                                icon = null,
                                label = "Langostín",
                                selected = false,
                                onClick = { langostinOpen = true },
                                contentDescription = "Abrir Langostín",
                            )
                            if (items.size > 1) {
                                BentoBottomBar(
                                    items = items,
                                    action = action,
                                    actionContent = {
                                        LangostinMascot(size = 58.dp, animated = true, showBadge = true)
                                    },
                                )
                            }
                        }
                    },
                    snackbarHost = { AppSnackbarHost() },
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .background(canvas),
                    ) {
                        AppNavHost(current)
                    }
                }
                // Panel de Langostín por encima del scaffold (y de la barra inferior).
                if (langostinOpen) {
                    LangostinOverlay(
                        current = current,
                        user = user,
                        onNavigate = { navigator.push(it) },
                        onClose = { langostinOpen = false },
                    )
                }
            }
        }
    }
}

/**
 * Destinos de la barra inferior (máx. 4), todos existentes en el drawer y con sus mismos permisos:
 * Inicio, Tickets, uno de Portería / Inventario / Personal (el primero permitido) y Perfil
 * (avatar del usuario → credencial). Con dos opciones permitidas entran las dos.
 */
private fun bottomBarItems(user: SessionUser, current: Screen, go: (Screen) -> Unit): List<BentoNavItem> {
    val section = current.chrome().section
    val middle = buildList {
        if (user.canViewTickets) {
            add(BentoNavItem(Icons.Filled.ConfirmationNumber, "Tickets", section == MainSection.TICKETS, { go(Screen.TicketsList) }))
        }
        if (user.canScanCredential) {
            add(BentoNavItem(Icons.Filled.QrCodeScanner, "Portería", section == MainSection.ACCESS, { go(Screen.AccessScan) }))
        }
        if (user.canViewDevices) {
            add(BentoNavItem(Icons.Filled.Inventory, "Inventario", section == MainSection.DEVICES, { go(Screen.DevicesList) }))
        }
        if (user.canViewHR) {
            add(BentoNavItem(Icons.Filled.Groups, "Personal", current is Screen.EmployeesList, { go(Screen.EmployeesList) }))
        }
    }.take(2)
    return buildList {
        add(BentoNavItem(Icons.Filled.Home, "Inicio", current is Screen.Home, { go(Screen.Home) }))
        addAll(middle)
        add(BentoNavItem(null, "Perfil", current is Screen.MyCredential, { go(Screen.MyCredential) }, avatarName = user.name))
    }
}

/** Contexto del header en las pantallas que tienen atrás: a qué módulo pertenece la pantalla. */
private fun sectionLabel(section: MainSection): String? = when (section) {
    MainSection.HOME -> null
    MainSection.TICKETS -> "Tickets"
    MainSection.DEVICES -> "Inventario"
    MainSection.USERS -> "Personal"
    MainSection.ACCESS -> "Control de acceso"
}

@Composable
private fun AppNavHost(screen: Screen) {
    when (screen) {
        Screen.Home -> HomeScreen()
        Screen.MyCredential -> MyCredentialScreen()

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
        is Screen.UserForm -> UserFormScreen(screen.id)
        Screen.UsersList -> UsersListScreen()
        is Screen.UserHistory -> UserHistoryScreen(screen.userId)
        Screen.UserImport -> UserImportScreen()
        Screen.EmployeesList -> EmployeesListScreen()
        is Screen.EmployeeProfile -> EmployeeProfileScreen(screen.id)

        // Departments — módulo 3
        Screen.DepartmentsList -> DepartmentsListScreen()
        is Screen.DepartmentDetail -> DepartmentDetailScreen(screen.id)

        // Inventory — módulo 4
        Screen.InventoryIndex -> InventoryIndexScreen()
        Screen.LocationsList -> LocationsListScreen()
        is Screen.LocationDetail -> LocationDetailScreen(screen.id)
        is Screen.LocationForm -> LocationFormScreen(screen.id)
        Screen.InventoryMovements -> InventoryMovementsScreen()
        is Screen.NewInventoryMovement -> NewInventoryMovementScreen(screen.deviceId)
        is Screen.DeviceStockIn -> DeviceStockInScreen(screen.deviceId)
        Screen.InventoryReturns -> InventoryReturnsScreen()
        Screen.InventoryAudit -> InventoryAuditScreen()
        Screen.DeviceImport -> InventoryImportScreen()

        // Salidas / Cartas / Reports / Notifications / Audit — módulo 5
        Screen.MaterialOutputsList -> MaterialOutputsListScreen()
        is Screen.MaterialOutputForm -> MaterialOutputFormScreen(screen.id)
        Screen.CustodyLettersList -> CustodyLettersListScreen()
        is Screen.CustodyLetterDetail -> CustodyLetterDetailScreen(screen.id)
        is Screen.CustodyLetterForm -> CustodyLetterFormScreen(screen.id)
        Screen.GenerateCustodyLetter -> GenerateCustodyLetterScreen()

        // Control de acceso — módulo 6
        Screen.AccessScan -> AccessScanScreen()
        Screen.AccessLog -> AccessLogScreen()

        Screen.Reports -> ReportsScreen()
        Screen.Notifications -> NotificationsScreen()
        Screen.AuditLogs -> AuditLogsScreen()
        Screen.RolesPermissions -> RolesPermissionsScreen()
    }
}
