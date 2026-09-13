package com.axzydev.puertonuevoapp.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.feature.audit.AuditLogsScreen
import com.axzydev.puertonuevoapp.feature.cartas.CartaDetailScreen
import com.axzydev.puertonuevoapp.feature.cartas.CartaFormScreen
import com.axzydev.puertonuevoapp.feature.cartas.CartasListScreen
import com.axzydev.puertonuevoapp.feature.notifications.NotificationsScreen
import com.axzydev.puertonuevoapp.feature.reports.ReportsScreen
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.MainSection
import com.axzydev.puertonuevoapp.core.nav.PlatformBackHandler
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.nav.chrome
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppSnackbarHost
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
import com.axzydev.puertonuevoapp.feature.inventory.LocationsScreen
import com.axzydev.puertonuevoapp.feature.inventory.NewInventoryMovementScreen
import com.axzydev.puertonuevoapp.feature.salidas.SalidaFormScreen
import com.axzydev.puertonuevoapp.feature.salidas.SalidasListScreen
import com.axzydev.puertonuevoapp.feature.tickets.AdminTasksScreen
import com.axzydev.puertonuevoapp.feature.tickets.EditTicketScreen
import com.axzydev.puertonuevoapp.feature.tickets.MyTasksScreen
import com.axzydev.puertonuevoapp.feature.tickets.NewTicketScreen
import com.axzydev.puertonuevoapp.feature.tickets.TicketDetailScreen
import com.axzydev.puertonuevoapp.feature.tickets.TicketKanbanScreen
import com.axzydev.puertonuevoapp.feature.tickets.TicketsListScreen
import com.axzydev.puertonuevoapp.feature.users.UserFormScreen
import com.axzydev.puertonuevoapp.feature.users.UserHistoryScreen
import com.axzydev.puertonuevoapp.feature.users.UsersListScreen
import kotlinx.coroutines.launch

private enum class AppTab(val label: String, val section: MainSection, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Inicio", MainSection.HOME, Icons.Filled.Home),
    TICKETS("Tickets", MainSection.TICKETS, Icons.Filled.ConfirmationNumber),
    DEVICES("Dispositivos", MainSection.DEVICES, Icons.Filled.Devices),
    USERS("Usuarios", MainSection.USERS, Icons.Filled.Person),
}

private const val WideLayoutBreakpoint = 700

@Composable
fun AppShell() {
    val navigator = LocalNavigator.current
    val current = navigator.current
    val chrome = current.chrome()
    val authState by AppContainer.authRepository.state.collectAsState()
    val canManageUsers = (authState as? AuthState.LoggedIn)?.user?.role == "ADMIN"
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    PlatformBackHandler(enabled = navigator.backStack.size > 1) {
        navigator.pop()
    }

    fun selectTab(tab: AppTab) {
        navigator.switchTab(tab.screen())
        scope.launch { drawerState.close() }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        val isWide = maxWidth >= WideLayoutBreakpoint.dp
        val drawer: @Composable () -> Unit = {
            AppDrawer(
                selectedSection = chrome.section,
                showUsers = canManageUsers,
                onSelect = ::selectTab,
                modifier = Modifier.fillMaxHeight(),
            )
        }

        if (isWide) {
            Row(modifier = Modifier.fillMaxSize()) {
                drawer()
                Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = AppColors.Outline)
                ShellScaffold(
                    current = current,
                    chrome = chrome,
                    navigator = navigator,
                    showDrawerButton = false,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = AppColors.Surface,
                        modifier = Modifier.width(300.dp),
                    ) { drawer() }
                },
            ) {
                ShellScaffold(
                    current = current,
                    chrome = chrome,
                    navigator = navigator,
                    showDrawerButton = true,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                )
            }
        }
    }
}

@Composable
private fun ShellScaffold(
    current: Screen,
    chrome: com.axzydev.puertonuevoapp.core.nav.ScreenChrome,
    navigator: com.axzydev.puertonuevoapp.core.nav.Navigator,
    showDrawerButton: Boolean,
    modifier: Modifier = Modifier,
    onOpenDrawer: (() -> Unit)? = null,
) {
    val shellAuthState by AppContainer.authRepository.state.collectAsState()
    val isAdmin = (shellAuthState as? AuthState.LoggedIn)?.user?.role == "ADMIN"

    Scaffold(
        modifier = modifier,
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text(chrome.title, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    if (chrome.showBack) {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    } else if (showDrawerButton && onOpenDrawer != null) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Filled.Menu, contentDescription = "Abrir menú")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Surface),
            )
        },
        bottomBar = {
            if (chrome.showBottomNavigation) {
                CompactBottomBar(selected = chrome.section, onSelect = { navigator.switchTab(it.screen()) })
            }
        },
        floatingActionButton = {
            when (current) {
                Screen.NewTicket, is Screen.UserForm -> Unit
                Screen.TicketsList -> FloatingActionButton(
                    onClick = { navigator.push(Screen.NewTicket) },
                    containerColor = AppColors.EmeraldPrimary,
                    contentColor = AppColors.Surface,
                ) { Icon(Icons.Filled.Add, contentDescription = "Nuevo ticket") }
                Screen.UsersList -> FloatingActionButton(
                    onClick = { navigator.push(Screen.UserForm()) },
                    containerColor = AppColors.EmeraldPrimary,
                    contentColor = AppColors.Surface,
                ) { Icon(Icons.Filled.Add, contentDescription = "Nuevo usuario") }
                Screen.DevicesList -> if (isAdmin) {
                    FloatingActionButton(
                        onClick = { navigator.push(Screen.DeviceForm()) },
                        containerColor = AppColors.EmeraldPrimary,
                        contentColor = AppColors.Surface,
                    ) { Icon(Icons.Filled.Add, contentDescription = "Nuevo dispositivo") }
                }
                else -> Unit
            }
        },
        snackbarHost = { AppSnackbarHost() },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AppScreen(current)
        }
    }
}

@Composable
private fun CompactBottomBar(selected: MainSection, onSelect: (AppTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(88.dp).navigationBarsPadding().background(AppColors.Surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppTab.values().filter { it != AppTab.USERS }.forEach { tab ->
            CompactTabButton(tab, selected == tab.section, { onSelect(tab) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CompactTabButton(
    tab: AppTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(52.dp).background(
                if (selected) AppColors.EmeraldContainer else Color.Transparent,
                MaterialTheme.shapes.small,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tab.icon, contentDescription = tab.label, tint = if (selected) AppColors.EmeraldPrimary else AppColors.TextFaint, modifier = Modifier.size(28.dp))
        }
        Text(tab.label, style = MaterialTheme.typography.bodyMedium, color = if (selected) AppColors.EmeraldPrimary else AppColors.TextFaint)
    }
}

@Composable
private fun AppDrawer(
    selectedSection: MainSection,
    showUsers: Boolean,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(AppColors.Surface)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            Box(Modifier.size(42.dp).background(AppColors.EmeraldContainer, MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
                Text("PN", style = MaterialTheme.typography.titleMedium, color = AppColors.EmeraldOnContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Puerto Nuevo", style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
                Text("Operaciones", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
            }
        }
        Spacer(Modifier.size(32.dp))
        Text("NAVEGACIÓN", style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint, modifier = Modifier.padding(horizontal = 12.dp))
        Spacer(Modifier.size(8.dp))
        AppTab.values().filter { it != AppTab.USERS || showUsers }.forEach { tab ->
            NavigationDrawerItem(
                label = { Text(tab.label, style = MaterialTheme.typography.titleSmall) },
                selected = selectedSection == tab.section,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = AppColors.EmeraldContainer,
                    selectedIconColor = AppColors.EmeraldOnContainer,
                    selectedTextColor = AppColors.EmeraldOnContainer,
                    unselectedContainerColor = Color.Transparent,
                    unselectedIconColor = AppColors.TextMuted,
                    unselectedTextColor = AppColors.TextMuted,
                ),
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Divider(color = AppColors.Outline)
        NavigationDrawerItem(
            label = { Text("Cerrar sesión", style = MaterialTheme.typography.titleSmall) },
            selected = false,
            onClick = { AppContainer.authRepository.logout() },
            icon = { Icon(Icons.Filled.Logout, contentDescription = null) },
            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedIconColor = AppColors.TextMuted, unselectedTextColor = AppColors.TextMuted),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun AppScreen(current: Screen) {
    when (current) {
        Screen.Home -> HomeScreen()
        Screen.TicketsList -> TicketsListScreen()
        Screen.NewTicket -> NewTicketScreen()
        is Screen.TicketDetail -> TicketDetailScreen(ticketId = current.id)
        Screen.DevicesList -> DevicesListScreen()
        is Screen.DeviceDetail -> DeviceDetailScreen(deviceId = current.id)
        Screen.UsersList -> UsersListScreen()
        is Screen.UserForm -> UserFormScreen(userId = current.id)
        is Screen.UserHistory -> UserHistoryScreen(userId = current.id)
        is Screen.EditTicket -> EditTicketScreen(ticketId = current.id)
        is Screen.TicketsKanban -> TicketKanbanScreen(ticketId = current.ticketId)
        Screen.MyTasks -> MyTasksScreen()
        Screen.AdminTasks -> AdminTasksScreen()
        is Screen.DeviceForm -> DeviceFormScreen(deviceId = current.id)
        Screen.DepartmentsList -> DepartmentsListScreen()
        is Screen.DepartmentDetail -> DepartmentDetailScreen(departmentId = current.id)
        Screen.DeviceTypesList -> DeviceTypesListScreen()
        is Screen.DeviceTypeForm -> DeviceTypeFormScreen(typeId = current.id)
        Screen.EmployeesList -> EmployeesListScreen()
        Screen.InventoryIndex -> InventoryIndexScreen()
        Screen.LocationsList -> LocationsScreen()
        Screen.InventoryMovements -> InventoryMovementsScreen()
        is Screen.NewInventoryMovement -> NewInventoryMovementScreen(deviceId = current.deviceId)
        Screen.SalidasList -> SalidasListScreen()
        is Screen.SalidaForm -> SalidaFormScreen(salidaId = current.id)
        Screen.CartasList -> CartasListScreen()
        is Screen.CartaDetail -> CartaDetailScreen(cartaId = current.id)
        is Screen.CartaForm -> CartaFormScreen(cartaId = current.id)
        Screen.Reports -> ReportsScreen()
        Screen.Notifications -> NotificationsScreen()
        Screen.AuditLogs -> AuditLogsScreen()
    }
}

private fun AppTab.screen(): Screen = when (this) {
    AppTab.HOME -> Screen.Home
    AppTab.TICKETS -> Screen.TicketsList
    AppTab.DEVICES -> Screen.DevicesList
    AppTab.USERS -> Screen.UsersList
}
