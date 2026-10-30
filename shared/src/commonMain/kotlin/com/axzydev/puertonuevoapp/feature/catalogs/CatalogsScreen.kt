package com.axzydev.puertonuevoapp.feature.catalogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.BentoTabItem
import com.axzydev.puertonuevoapp.core.ui.BentoTabs
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.SectionLabel

/**
 * Catálogos del sistema (`/catalogs` de la web) con siete pestañas:
 * Departamentos/Subáreas, Tipos de documento, Géneros, Tipos de sangre,
 * Categorías de ticket, Notificaciones (sys-config) y Tipos de dispositivo
 * (acceso a su pantalla propia).
 *
 * La web exige `catalogs.manage` para toda la página; la app también admite a
 * quien solo tiene `system.configure`, porque esa persona necesita la pestaña
 * de Notificaciones. Cada pestaña vuelve a comprobar su permiso de escritura.
 */
@Composable
fun CatalogsScreen() {
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val navigator = LocalNavigator.current

    if (user == null ||
        !(user.can(PermissionKeys.CATALOGS_MANAGE) || user.can(PermissionKeys.SYSTEM_CONFIGURE))
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            BentoState(
                message = "No autorizado — se requiere el permiso de Catálogos o de configuración del sistema",
                tone = BentoTone.Rose,
                icon = Icons.Filled.Lock,
            )
        }
        return
    }

    val tabs = CatalogsTab.entries
    var selected by rememberSaveable { mutableIntStateOf(0) }
    // Las fuentes de los catálogos simples se arman una vez (envuelven las APIs).
    val sources = remember { simpleCatalogSources(AppContainer.hrApi, AppContainer.ticketsApi) }
    val currentTab = tabs[selected.coerceIn(0, tabs.lastIndex)]

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 10.dp)) {
            BentoTabs(
                items = tabs.map { BentoTabItem(it.label, it.icon) },
                selectedIndex = selected.coerceIn(0, tabs.lastIndex),
                onSelect = { selected = it },
            )
            Spacer(Modifier.height(2.dp))
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (currentTab) {
                CatalogsTab.Departments -> DepartmentsCatalogTab(user = user)
                CatalogsTab.Notifications -> NotificationsCatalogTab(user = user)
                CatalogsTab.DeviceTypes -> DeviceTypesCatalogTab(
                    canOpen = user.can(PermissionKeys.CATALOGS_MANAGE),
                    onOpen = { navigator.push(Screen.DeviceTypesList) },
                )
                else -> sources[currentTab]?.let { source ->
                    SimpleCatalogTab(tab = currentTab, source = source, user = user)
                }
            }
        }
    }
}

/**
 * Pestaña informativa de Tipos de dispositivo: la administración (prefijo,
 * campos y estatus) vive en su propia pantalla, que ya existía; aquí solo se
 * enlaza para que Catálogos siga siendo el índice de catálogos de la web.
 */
@Composable
private fun DeviceTypesCatalogTab(
    canOpen: Boolean,
    onOpen: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Tipos de dispositivo")
            Spacer(Modifier.height(6.dp))
            Text(
                "Cada tipo define el prefijo del activo fijo y qué datos lleva cada unidad (serie, MAC, IP, nombre de equipo, sistema, RAM o almacenamiento). Se administra en su pantalla, con el mismo permiso de Catálogos.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
            Spacer(Modifier.height(14.dp))
            PillButton(
                text = "Abrir tipos de dispositivo",
                onClick = onOpen,
                enabled = canOpen,
                leadingIcon = Icons.Filled.Devices,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (!canOpen) {
            BentoState(
                message = "Solo lectura: se requiere el permiso de Catálogos para administrar los tipos de dispositivo.",
                tone = BentoTone.Sun,
                icon = Icons.Filled.Lock,
            )
        }
    }
}
