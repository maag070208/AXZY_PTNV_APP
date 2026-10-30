package com.axzydev.puertonuevoapp.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.session.SessionUser
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.util.roleLabel

/**
 * Drawer lateral del shell bento.
 *
 * Panel blanco de 312dp con las esquinas derechas de 28dp ([AppShape.sheetEnd])
 * pensado para ir como `drawerContent` de un `ModalNavigationDrawer`. Arriba,
 * hero de marca sobre [OceanBackdrop] (logo, usuario, rol y botón de cierre);
 * debajo, buscador y menú agrupado en cinco secciones con filas de 52dp,
 * ícono en contenedor de 36dp, chevron animado y subnodos con riel.
 *
 * Los ítems, subítems y compuertas por permiso son EXACTAMENTE los del bloque
 * `drawerNodes` de `feature/home/AppShell.kt` (solo cambia la presentación).
 * El grupo que contiene [current] arranca abierto.
 *
 * @param user usuario de la sesión (define nombre, rol y qué ítems se ven).
 * @param current pantalla activa, para resaltar y auto-expandir su grupo.
 * @param unreadCount contador de notificaciones sin leer (insignia rose).
 * @param onNavigate navega a una pantalla del menú (el shell cierra el drawer).
 * @param onLogout cierra la sesión.
 * @param onClose cierra el drawer (botón X del hero).
 */
@Composable
fun BentoDrawer(
    user: SessionUser?,
    current: Screen,
    unreadCount: Int,
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit,
    onClose: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(emptySet<String>()) }

    // El grupo de la pantalla actual arranca abierto (y se abre al navegar).
    LaunchedEffect(current, user) {
        val activeGroups = drawerSections(user)
            .flatMap { it.nodes }
            .filter { node -> node.children.any { isRoute(current, it.screen) } }
            .map { it.label }
            .toSet()
        if (activeGroups.isNotEmpty()) expanded = expanded + activeGroups
    }

    val visibleSections = filterSections(drawerSections(user), query)
    val searching = query.isNotBlank()

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(312.dp)
            .shadow(
                elevation = 20.dp,
                shape = AppShape.sheetEnd,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.16f),
                spotColor = Color.Black.copy(alpha = 0.24f),
            )
            .clip(AppShape.sheetEnd),
        shape = AppShape.sheetEnd,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            DrawerHero(user = user, onClose = onClose)

            AppSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Buscar en el menú",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                if (visibleSections.isEmpty() && searching) {
                    Text(
                        text = "Sin resultados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                    )
                } else {
                    visibleSections.forEach { section ->
                        DrawerSectionHeader(section.label)
                        section.nodes.forEach { node ->
                            val isExpanded = node.label in expanded
                            DrawerNodeRow(
                                node = node,
                                current = current,
                                unreadCount = unreadCount,
                                isExpanded = isExpanded,
                                forceOpen = searching,
                                onToggle = {
                                    expanded = if (isExpanded) expanded - node.label else expanded + node.label
                                },
                                onNavigate = onNavigate,
                            )
                            if (node.children.isNotEmpty() && (isExpanded || searching)) {
                                node.children.forEach { child ->
                                    DrawerChildRow(
                                        label = child.label,
                                        selected = isRoute(current, child.screen),
                                        onClick = { onNavigate(child.screen) },
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            DrawerFooter(onLogout = onLogout)
        }
    }
}

/** Hero de marca del drawer: logo, usuario, chip de rol, @usuario y botón de cierre. */
@Composable
private fun DrawerHero(user: SessionUser?, onClose: () -> Unit) {
    val onBrand = MaterialTheme.colorScheme.onPrimary
    val displayName = user?.name?.takeIf { it.isNotBlank() } ?: "Puerto Nuevo"
    val displayRole = user?.let { roleLabel(it.role) } ?: "Invitado"
    val handle = user?.username?.takeIf { it.isNotBlank() }

    OceanBackdrop(
        modifier = Modifier
            .padding(start = 12.dp, end = 12.dp, top = 6.dp)
            .fillMaxWidth()
            .clip(AppShape.bento),
        wrapContent = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 12.dp, top = 14.dp, bottom = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandLogoBadge(size = 44.dp, cornerRadius = 14.dp, innerPadding = 6.dp)
                Spacer(Modifier.weight(1f))
                BentoIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = "Cerrar menú",
                    onClick = onClose,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium,
                color = onBrand,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OnBrandChip(label = displayRole)
                if (handle != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "@$handle",
                        style = MaterialTheme.typography.labelSmall,
                        color = onBrand.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }
    }
}

/** Encabezado de sección del menú (11sp, versalitas suaves, tenue). */
@Composable
private fun DrawerSectionHeader(label: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

/** Fila de primer nivel del menú: ícono en contenedor de 36dp, chevron e insignia. */
@Composable
private fun DrawerNodeRow(
    node: DrawerNode,
    current: Screen,
    unreadCount: Int,
    isExpanded: Boolean,
    forceOpen: Boolean,
    onToggle: () -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val hasChildren = node.children.isNotEmpty()
    val active = node.screen?.let { isRoute(current, it) } == true ||
        node.children.any { isRoute(current, it.screen) }
    val open = hasChildren && (isExpanded || forceOpen)
    val highlighted = active || open

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .height(52.dp)
            .clip(AppShape.row)
            .background(
                when {
                    hasChildren && open -> scheme.primaryContainer.copy(alpha = 0.4f)
                    active -> scheme.primaryContainer.copy(alpha = 0.55f)
                    else -> Color.Transparent
                },
            )
            .clickable {
                if (hasChildren) {
                    onToggle()
                } else {
                    node.screen?.let { onNavigate(it) }
                }
            }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(AppShape.icon)
                .background(if (highlighted) scheme.primaryContainer else scheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = node.icon,
                contentDescription = null,
                tint = when {
                    active -> scheme.primary
                    open -> scheme.onPrimaryContainer
                    else -> scheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = node.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = if (active) scheme.primary else scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (node.screen == Screen.Notifications && unreadCount > 0) {
            Spacer(Modifier.width(8.dp))
            NotificationBadge(unreadCount)
        }
        if (hasChildren) {
            Spacer(Modifier.width(6.dp))
            val rotation by animateFloatAsState(
                targetValue = if (open) 180f else 0f,
                animationSpec = tween(durationMillis = 220),
                label = "drawerChevron",
            )
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = if (open) scheme.primary else scheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotation),
            )
        }
    }
}

/** Subnodo: riel vertical fino, punto y texto; el activo va en primario y negrita. */
@Composable
private fun DrawerChildRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val railColor = scheme.outline.copy(alpha = 0.7f)
    val dotColor = if (selected) scheme.primary else scheme.outline

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 26.dp, end = 10.dp)
            .height(44.dp)
            .clip(AppShape.row)
            .background(if (selected) scheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.width(24.dp).fillMaxHeight()) {
            val centerX = size.width / 2f
            drawLine(
                color = railColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, size.height),
                strokeWidth = 1.dp.toPx(),
            )
            drawCircle(
                color = dotColor,
                radius = if (selected) 4.dp.toPx() else 2.5.dp.toPx(),
                center = Offset(centerX, size.height / 2f),
            )
        }
        Spacer(Modifier.width(18.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) scheme.primary else scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Insignia rose del contador de notificaciones. */
@Composable
private fun NotificationBadge(count: Int) {
    val rose = AppBento.tone(BentoTone.Rose)
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .clip(AppShape.pill)
            .background(rose.container)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = rose.content,
        )
    }
}

/** Pie del drawer: separador, cierre de sesión en tono rose y nota de marca. */
@Composable
private fun DrawerFooter(onLogout: () -> Unit) {
    val rose = AppBento.tone(BentoTone.Rose)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = AppColors.Outline.copy(alpha = 0.4f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .height(52.dp)
                .clip(AppShape.row)
                .background(rose.container.copy(alpha = 0.65f))
                .clickable(onClick = onLogout)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(AppShape.icon)
                    .background(rose.container),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = AppColors.Danger,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Cerrar sesión",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Danger,
            )
        }
        Text(
            text = "Puerto Nuevo · Hotel y Villas",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 12.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Modelo del menú — copia exacta de `drawerNodes`/`DrawerNode`/`DrawerChild`
// de `feature/home/AppShell.kt` (mismos ítems, hijos y compuertas por permiso).
// ---------------------------------------------------------------------------

private data class DrawerChild(val label: String, val screen: Screen)

private data class DrawerNode(
    val label: String,
    val icon: ImageVector,
    val screen: Screen? = null,
    val children: List<DrawerChild> = emptyList(),
)

private data class DrawerSection(val label: String, val nodes: List<DrawerNode>)

private fun drawerSections(user: SessionUser?): List<DrawerSection> = buildList {
    add(
        DrawerSection(
            label = "Principal",
            nodes = listOf(
                DrawerNode("Inicio", Icons.Filled.Home, Screen.Home),
                DrawerNode("Mi credencial", Icons.Filled.Badge, Screen.MyCredential),
                DrawerNode("Notificaciones", Icons.Filled.Notifications, Screen.Notifications),
            ),
        ),
    )

    add(
        DrawerSection(
            label = "Operación",
            nodes = buildList {
                add(
                    DrawerNode(
                        label = "Tickets",
                        icon = Icons.Filled.ConfirmationNumber,
                        screen = Screen.TicketsList,
                        children = buildList {
                            add(DrawerChild("Tickets", Screen.TicketsList))
                            add(DrawerChild("Tablero", Screen.TicketsKanban()))
                            if (user?.canSeeMyTasks == true) add(DrawerChild("Mis tareas", Screen.MyTasks))
                            if (user?.canSeeAdminTasks == true) add(DrawerChild("Administrar tareas", Screen.AdminTasks))
                        },
                    ),
                )
                if (user?.canScanCredential == true || user?.canViewAccessLog == true) {
                    add(
                        DrawerNode(
                            label = "Portería",
                            icon = Icons.Filled.QrCodeScanner,
                            children = buildList {
                                if (user.canScanCredential) add(DrawerChild("Escanear credencial", Screen.AccessScan))
                                if (user.canViewAccessLog) add(DrawerChild("Registros de acceso", Screen.AccessLog))
                            },
                        ),
                    )
                }
            },
        ),
    )

    val inventory = buildList {
        if (user?.canViewInventory == true) {
            add(
                DrawerNode(
                    label = "Inventario",
                    icon = Icons.Filled.Inventory,
                    children = buildList {
                        if (user.canViewDevices) {
                            add(DrawerChild("Resumen", Screen.InventoryIndex))
                            add(DrawerChild("Dispositivos", Screen.DevicesList))
                            add(DrawerChild("Movimientos", Screen.InventoryMovements))
                        }
                        if (user.canViewCustodyLetters) add(DrawerChild("Cartas responsivas", Screen.CustodyLettersList))
                        if (user.canRegisterMaterialOutputs) add(DrawerChild("Salidas de material", Screen.MaterialOutputsList))
                    },
                ),
            )
        }
        if (user?.canViewReports == true) {
            add(DrawerNode("Reportes", Icons.Filled.QueryStats, Screen.Reports))
        }
    }
    if (inventory.isNotEmpty()) add(DrawerSection("Inventario", inventory))

    val organization = buildList {
        if (user?.canViewHR == true || user?.canManageDepartments == true) {
            add(
                DrawerNode(
                    label = "Recursos Humanos",
                    icon = Icons.Filled.Groups,
                    children = buildList {
                        if (user.canViewHR) add(DrawerChild("Personal", Screen.EmployeesList))
                        add(DrawerChild("Departamentos", Screen.DepartmentsList))
                    },
                ),
            )
        }
    }
    if (organization.isNotEmpty()) add(DrawerSection("Organización", organization))

    val configuration = buildList {
        if (user?.canManageCatalogs == true || user?.canSeeAudit == true || user?.canManageRoles == true) {
            add(
                DrawerNode(
                    label = "Configuración",
                    icon = Icons.Filled.Settings,
                    children = buildList {
                        if (user.canManageCatalogs) add(DrawerChild("Tipos de dispositivo", Screen.DeviceTypesList))
                        if (user.canSeeAudit) add(DrawerChild("Auditoría", Screen.AuditLogs))
                        if (user.canManageRoles) add(DrawerChild("Roles y permisos", Screen.RolesPermissions))
                    },
                ),
            )
        }
    }
    if (configuration.isNotEmpty()) add(DrawerSection("Configuración", configuration))
}

/**
 * Filtro del buscador: conserva los nodos cuyo título coincide (con todos sus
 * hijos) y los que tienen al menos un hijo que coincide (solo con esos hijos).
 * Si la consulta está vacía devuelve el menú completo.
 */
private fun filterSections(sections: List<DrawerSection>, query: String): List<DrawerSection> {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return sections
    return sections.mapNotNull { section ->
        val nodes = section.nodes.mapNotNull { node ->
            when {
                node.label.lowercase().contains(needle) -> node
                node.children.isEmpty() -> null
                else -> {
                    val matchingChildren = node.children.filter { it.label.lowercase().contains(needle) }
                    if (matchingChildren.isEmpty()) null else node.copy(children = matchingChildren)
                }
            }
        }
        if (nodes.isEmpty()) null else section.copy(nodes = nodes)
    }
}

private fun isRoute(current: Screen, target: Screen): Boolean = current::class == target::class
