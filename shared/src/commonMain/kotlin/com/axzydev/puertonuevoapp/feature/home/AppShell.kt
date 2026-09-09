package com.axzydev.puertonuevoapp.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.feature.devices.DeviceDetailScreen
import com.axzydev.puertonuevoapp.feature.devices.DevicesListScreen
import com.axzydev.puertonuevoapp.feature.tickets.NewTicketScreen
import com.axzydev.puertonuevoapp.feature.tickets.TicketDetailScreen
import com.axzydev.puertonuevoapp.feature.tickets.TicketsListScreen

private enum class BottomTab(val label: String) {
    HOME("Inicio"),
    TICKETS("Tickets"),
    DEVICES("Dispositivos"),
}

/** Host principal una vez logueado: bottom nav + el screen activo del navegador. */
@Composable
fun AppShell() {
    val navigator = LocalNavigator.current
    val current = navigator.current

    val selectedTab = when (current) {
        Screen.Home -> BottomTab.HOME
        Screen.TicketsList, Screen.NewTicket -> BottomTab.TICKETS
        is Screen.TicketDetail -> BottomTab.TICKETS
        Screen.DevicesList -> BottomTab.DEVICES
        is Screen.DeviceDetail -> BottomTab.DEVICES
    }

    Scaffold(
        containerColor = AppColors.Background,
        bottomBar = {
            NavigationBar(containerColor = AppColors.Surface, tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = selectedTab == BottomTab.HOME,
                    onClick = { navigator.switchTab(Screen.Home) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Inicio", style = MaterialTheme.typography.labelSmall) },
                    colors = tabColors(),
                )
                NavigationBarItem(
                    selected = selectedTab == BottomTab.TICKETS,
                    onClick = { navigator.switchTab(Screen.TicketsList) },
                    icon = { Icon(Icons.Filled.ConfirmationNumber, contentDescription = null) },
                    label = { Text("Tickets", style = MaterialTheme.typography.labelSmall) },
                    colors = tabColors(),
                )
                NavigationBarItem(
                    selected = selectedTab == BottomTab.DEVICES,
                    onClick = { navigator.switchTab(Screen.DevicesList) },
                    icon = { Icon(Icons.Filled.Devices, contentDescription = null) },
                    label = { Text("Dispositivos", style = MaterialTheme.typography.labelSmall) },
                    colors = tabColors(),
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val screen = current) {
                Screen.Home -> HomeScreen()
                Screen.TicketsList -> TicketsListScreen()
                Screen.NewTicket -> NewTicketScreen()
                is Screen.TicketDetail -> TicketDetailScreen(ticketId = screen.id)
                Screen.DevicesList -> DevicesListScreen()
                is Screen.DeviceDetail -> DeviceDetailScreen(deviceId = screen.id)
            }
        }
    }
}

@Composable
private fun tabColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = AppColors.EmeraldPrimary,
    selectedTextColor = AppColors.EmeraldPrimary,
    unselectedIconColor = AppColors.TextFaint,
    unselectedTextColor = AppColors.TextFaint,
    indicatorColor = AppColors.EmeraldContainer,
)
