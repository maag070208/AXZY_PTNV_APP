package com.axzydev.puertonuevoapp.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ConfirmationNumber
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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.ui.AppSnackbarHost
import com.axzydev.puertonuevoapp.core.ui.BrandLogoBadge
import com.axzydev.puertonuevoapp.core.ui.LocalTopBarActions
import com.axzydev.puertonuevoapp.core.ui.TopBarActionsState
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
import com.axzydev.puertonuevoapp.feature.personal.PersonalListScreen
import com.axzydev.puertonuevoapp.feature.personal.PersonalProfileScreen
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
import kotlinx.coroutines.launch

/**
 * Shell de la app: drawer lateral estilo iOS (filas compactas, logout al
 * fondo), top bar de marca (hamburguesa para abrir el drawer) y bottom nav
 * compacto de 2 secciones: Inicio y Portería.
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
    val topBarActions = remember { TopBarActionsState() }

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
        CompositionLocalProvider(LocalTopBarActions provides topBarActions) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopAppBar(
                        modifier = Modifier.padding(top = 20.dp),
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
                                    BrandLogoBadge(size = 32.dp, cornerRadius = 10.dp, innerPadding = 4.dp)
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
                            topBarActions.action?.let { action ->
                                IconButton(onClick = action.onClick) {
                                    Icon(
                                        action.icon,
                                        contentDescription = action.contentDescription,
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
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ) {
                            NavigationBarItem(
                                selected = current is Screen.Home,
                                onClick = { navigator.switchTab(Screen.Home) },
                                icon = { Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                label = { Text("Inicio", style = MaterialTheme.typography.labelSmall) },
                                colors = navItemColors,
                            )
                            if (user?.canScanCredential == true) {
                                NavigationBarItem(
                                    selected = current is Screen.AccessScan || current is Screen.AccessLog,
                                    onClick = { navigator.switchTab(Screen.AccessScan) },
                                    icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    label = { Text("Portería", style = MaterialTheme.typography.labelSmall) },
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
                    DrawerChild("Personal", Screen.PersonalList),
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
    val divider = AppColors.Outline.copy(alpha = 0.4f)

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandLogoBadge(size = 40.dp, cornerRadius = 12.dp, innerPadding = 5.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(user?.name ?: "Puerto Nuevo", style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary, maxLines = 1)
                    Text(
                        user?.role ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextFaint,
                    )
                }
            }
            HorizontalDivider(color = divider)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                nodes.forEach { node ->
                    val hasChildren = node.children.isNotEmpty()
                    val isOpen = node.label in expanded
                    val selected = node.screen?.let { isRoute(current, it) } == true ||
                        node.children.any { isRoute(current, it.screen) }

                    DrawerRow(
                        label = node.label,
                        icon = node.icon,
                        selected = selected && !hasChildren,
                        badge = if (node.screen == Screen.Notifications) unreadCount else 0,
                        trailing = if (hasChildren) {
                            { Icon(if (isOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = AppColors.TextFaint, modifier = Modifier.size(18.dp)) }
                        } else {
                            null
                        },
                        onClick = {
                            when {
                                hasChildren -> expanded = if (isOpen) expanded - node.label else expanded + node.label
                                node.screen != null -> onNavigate(node.screen)
                            }
                        },
                    )

                    if (hasChildren && isOpen) {
                        node.children.forEach { child ->
                            DrawerSubRow(
                                label = child.label,
                                selected = isRoute(current, child.screen),
                                onClick = { onNavigate(child.screen) },
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = divider)
            DrawerRow(
                label = "Cerrar sesión",
                icon = Icons.AutoMirrored.Filled.Logout,
                selected = false,
                tint = AppColors.Danger,
                onClick = onLogout,
            )
        }
    }
}

@Composable
private fun DrawerRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    badge: Int = 0,
    trailing: (@Composable () -> Unit)? = null,
    tint: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit,
) {
    val contentColor = tint ?: if (selected) MaterialTheme.colorScheme.primary else AppColors.TextPrimary
    val iconColor = tint ?: if (selected) MaterialTheme.colorScheme.primary else AppColors.TextMuted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(AppShape.row)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                AppShape.row,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = contentColor, modifier = Modifier.weight(1f))
        if (badge > 0) {
            Text(badge.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
        }
        trailing?.invoke()
    }
}

@Composable
private fun DrawerSubRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 44.dp, end = 8.dp, top = 1.dp, bottom = 1.dp)
            .clip(AppShape.row)
            .background(androidx.compose.ui.graphics.Color.Transparent, AppShape.row)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) MaterialTheme.colorScheme.primary else AppColors.TextMuted,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
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
        is Screen.UserForm -> UserFormScreen(screen.id)
        Screen.PersonalList -> PersonalListScreen()
        is Screen.PersonalProfile -> PersonalProfileScreen(screen.id)

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
