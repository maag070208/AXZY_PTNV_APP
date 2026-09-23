package com.axzydev.puertonuevoapp.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.PlatformBackHandler
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.nav.chrome
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.SessionUser
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppSnackbarHost
import com.axzydev.puertonuevoapp.core.ui.BrandLogoBadge
import com.axzydev.puertonuevoapp.feature.access.AccessLogScreen
import com.axzydev.puertonuevoapp.feature.access.AccessScanScreen
import com.axzydev.puertonuevoapp.feature.audit.AuditLogsScreen
import com.axzydev.puertonuevoapp.feature.cartas.CartaDetailScreen
import com.axzydev.puertonuevoapp.feature.cartas.CartaFormScreen
import com.axzydev.puertonuevoapp.feature.cartas.CartasListScreen
import com.axzydev.puertonuevoapp.feature.cartas.GenerateCartaScreen
import com.axzydev.puertonuevoapp.feature.departments.DepartmentDetailScreen
import com.axzydev.puertonuevoapp.feature.departments.DepartmentsListScreen
import com.axzydev.puertonuevoapp.feature.devices.DeviceDetailScreen
import com.axzydev.puertonuevoapp.feature.devices.DeviceFormScreen
import com.axzydev.puertonuevoapp.feature.devices.DevicesListScreen
import com.axzydev.puertonuevoapp.feature.devicetypes.DeviceTypeFormScreen
import com.axzydev.puertonuevoapp.feature.devicetypes.DeviceTypesListScreen
import com.axzydev.puertonuevoapp.feature.employees.EmployeesListScreen
import com.axzydev.puertonuevoapp.feature.inventory.InventoryIndexScreen
import com.axzydev.puertonuevoapp.feature.inventory.InventoryMovementsScreen
import com.axzydev.puertonuevoapp.feature.inventory.LocationDetailScreen
import com.axzydev.puertonuevoapp.feature.inventory.LocationFormScreen
import com.axzydev.puertonuevoapp.feature.inventory.LocationsListScreen
import com.axzydev.puertonuevoapp.feature.inventory.NewInventoryMovementScreen
import com.axzydev.puertonuevoapp.feature.notifications.NotificationsScreen
import com.axzydev.puertonuevoapp.feature.reports.ReportsScreen
import com.axzydev.puertonuevoapp.feature.salidas.SalidaFormScreen
import com.axzydev.puertonuevoapp.feature.salidas.SalidasListScreen
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
import kotlinx.coroutines.launch

/**
 * Shell de la app: drawer lateral (sidebar con subitems, como la web), top bar
 * de marca (hamburguesa para abrir el drawer; el logout vive en el drawer) y
 * bottom nav de 2 secciones: Inicio y Portería.
 */
@Composable
fun AppShell() {
    val navigator = LocalNavigator.current
    val authRepository = AppContainer.authRepository
    val authState by authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user

    val current = navigator.current
    val chrome = current.chrome()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var unreadCount by remember { mutableStateOf(0) }
    LaunchedEffect(current) {
        runCatching { AppContainer.notificationsApi.unreadCount() }.onSuccess { unreadCount = it }
    }

    PlatformBackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    PlatformBackHandler(enabled = !drawerState.isOpen && navigator.backStack.size > 1) {
        navigator.pop()
    }

    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    fun go(screen: Screen) {
        navigator.switchTab(screen)
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                user = user,
                current = current,
                unreadCount = unreadCount,
                onNavigate = ::go,
                onLogout = { authRepository.logout() },
            )
        },
    ) {
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
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    Icons.Filled.Menu,
                                    contentDescription = "Menú",
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
                                BrandLogoBadge(size = 34.dp, cornerRadius = 10.dp, innerPadding = 4.dp)
                                Spacer(Modifier.width(10.dp))
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
                            BadgedBox(badge = { if (unreadCount > 0) Badge { Text(unreadCount.toString()) } }) {
                                Icon(
                                    Icons.Filled.Notifications,
                                    contentDescription = "Notificaciones",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
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
                            selected = current is Screen.Home,
                            onClick = { navigator.switchTab(Screen.Home) },
                            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                            label = { Text("Inicio") },
                            colors = navItemColors,
                        )
                        if (user?.canScanCredential == true) {
                            NavigationBarItem(
                                selected = current is Screen.AccessScan || current is Screen.AccessLog,
                                onClick = { navigator.switchTab(Screen.AccessScan) },
                                icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = null) },
                                label = { Text("Portería") },
                                colors = navItemColors,
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
}

private data class DrawerChild(val label: String, val screen: Screen)
private data class DrawerNode(
    val label: String,
    val icon: ImageVector,
    val screen: Screen? = null,
    val children: List<DrawerChild> = emptyList(),
)

private fun drawerNodes(user: SessionUser?): List<DrawerNode> = buildList {
    add(DrawerNode("Inicio", Icons.Filled.Home, Screen.Home))

    add(
        DrawerNode(
            "Tickets", Icons.Filled.ConfirmationNumber, Screen.TicketsList,
            children = buildList {
                add(DrawerChild("Tickets", Screen.TicketsList))
                if (user?.canSeeAdminTasks == true) add(DrawerChild("Administrar tareas", Screen.AdminTasks))
                if (user?.canCreateTicket != true) add(DrawerChild("Mis tareas", Screen.MyTasks))
            },
        )
    )

    if (user?.canScanCredential == true || user?.canViewAccessLog == true) {
        add(
            DrawerNode(
                "Portería", Icons.Filled.QrCodeScanner,
                children = buildList {
                    if (user?.canScanCredential == true) add(DrawerChild("Escanear credencial", Screen.AccessScan))
                    if (user?.canViewAccessLog == true) add(DrawerChild("Registros de acceso", Screen.AccessLog))
                },
            )
        )
    }

    if (user?.canManageCatalogs == true) {
        add(
            DrawerNode(
                "Inventario", Icons.Filled.Inventory,
                children = listOf(
                    DrawerChild("Resumen", Screen.InventoryIndex),
                    DrawerChild("Dispositivos", Screen.DevicesList),
                    DrawerChild("Movimientos", Screen.InventoryMovements),
                    DrawerChild("Cartas responsivas", Screen.CartasList),
                    DrawerChild("Salidas de material", Screen.SalidasList),
                ),
            )
        )
        add(DrawerNode("Reportes", Icons.Filled.QueryStats, Screen.Reports))
    }

    if (user?.canManageHR == true) {
        add(
            DrawerNode(
                "Recursos Humanos", Icons.Filled.Groups,
                children = listOf(
                    DrawerChild("Personal", Screen.EmployeesList),
                    DrawerChild("Departamentos", Screen.DepartmentsList),
                ),
            )
        )
    }

    if (user?.canManageCatalogs == true) {
        add(
            DrawerNode(
                "Configuración", Icons.Filled.Settings,
                children = listOf(
                    DrawerChild("Tipos de dispositivo", Screen.DeviceTypesList),
                    DrawerChild("Usuarios", Screen.UsersList),
                    DrawerChild("Auditoría", Screen.AuditLogs),
                ),
            )
        )
    }

    add(DrawerNode("Notificaciones", Icons.Filled.Notifications, Screen.Notifications))
}

private fun isRoute(current: Screen, target: Screen): Boolean = current::class == target::class

@Composable
private fun AppDrawer(
    user: SessionUser?,
    current: Screen,
    unreadCount: Int,
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit,
) {
    val nodes = drawerNodes(user)
    var expanded by remember { mutableStateOf<Set<String>>(emptySet()) }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandLogoBadge(size = 44.dp, cornerRadius = 12.dp, innerPadding = 5.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Puerto Nuevo", style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
                    Text(
                        user?.name ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                        maxLines = 1,
                    )
                    Text(
                        user?.role ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextFaint,
                    )
                }
            }
            HorizontalDivider(color = AppColors.Outline.copy(alpha = 0.5f))

            nodes.forEach { node ->
                val hasChildren = node.children.isNotEmpty()
                val isOpen = node.label in expanded
                val selected = node.screen?.let { isRoute(current, it) } == true ||
                    node.children.any { isRoute(current, it.screen) }

                NavigationDrawerItem(
                    label = { Text(node.label) },
                    icon = { Icon(node.icon, contentDescription = null) },
                    selected = selected && !hasChildren,
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = AppColors.TextPrimary,
                        unselectedIconColor = AppColors.TextMuted,
                    ),
                    onClick = {
                        when {
                            hasChildren -> expanded = if (isOpen) expanded - node.label else expanded + node.label
                            node.screen != null -> onNavigate(node.screen)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                )

                if (hasChildren && isOpen) {
                    node.children.forEach { child ->
                        val childSelected = isRoute(current, child.screen)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 44.dp, end = 12.dp)
                                .background(
                                    if (childSelected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                                    MaterialTheme.shapes.medium,
                                )
                                .clickable { onNavigate(child.screen) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                child.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (childSelected) MaterialTheme.colorScheme.primary else AppColors.TextMuted,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = AppColors.TextFaint,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = AppColors.Outline.copy(alpha = 0.5f))
            NavigationDrawerItem(
                label = { Text("Cerrar sesión") },
                icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                selected = false,
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedTextColor = AppColors.Danger,
                    unselectedIconColor = AppColors.Danger,
                ),
                onClick = onLogout,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
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
        Screen.InventoryIndex -> InventoryIndexScreen()
        Screen.LocationsList -> LocationsListScreen()
        is Screen.LocationDetail -> LocationDetailScreen(screen.id)
        is Screen.LocationForm -> LocationFormScreen(screen.id)
        Screen.InventoryMovements -> InventoryMovementsScreen()
        is Screen.NewInventoryMovement -> NewInventoryMovementScreen(screen.deviceId)

        // Salidas / Cartas / Reports / Notifications / Audit — módulo 5
        Screen.SalidasList -> SalidasListScreen()
        is Screen.SalidaForm -> SalidaFormScreen(screen.id)
        Screen.CartasList -> CartasListScreen()
        is Screen.CartaDetail -> CartaDetailScreen(screen.id)
        is Screen.CartaForm -> CartaFormScreen(screen.id)
        Screen.GenerateCarta -> GenerateCartaScreen()

        // Control de acceso — módulo 6
        Screen.AccessScan -> AccessScanScreen()
        Screen.AccessLog -> AccessLogScreen()

        Screen.Reports -> ReportsScreen()
        Screen.Notifications -> NotificationsScreen()
        Screen.AuditLogs -> AuditLogsScreen()
    }
}
