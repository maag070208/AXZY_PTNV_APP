package com.axzydev.puertonuevoapp.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.dashboard.AccessTodayWidgetDto
import com.axzydev.puertonuevoapp.core.network.dashboard.AttendanceCountsDto
import com.axzydev.puertonuevoapp.core.network.dashboard.AttendanceTodayWidgetDto
import com.axzydev.puertonuevoapp.core.network.dashboard.DashboardActivityDto
import com.axzydev.puertonuevoapp.core.network.dashboard.DashboardSummaryDto
import com.axzydev.puertonuevoapp.core.network.dashboard.MyEquipmentWidgetDto
import com.axzydev.puertonuevoapp.core.network.dashboard.OvertimeWeekWidgetDto
import com.axzydev.puertonuevoapp.core.network.dashboard.SetupGapsWidgetDto
import com.axzydev.puertonuevoapp.core.network.dashboard.SystemHealthWidgetDto
import com.axzydev.puertonuevoapp.core.network.dashboard.TasksWidgetDto
import com.axzydev.puertonuevoapp.core.network.dashboard.TicketsWidgetDto
import com.axzydev.puertonuevoapp.core.network.inventory.DeviceCategoryDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.SessionUser
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.BentoAvatar
import com.axzydev.puertonuevoapp.core.ui.BentoBarRow
import com.axzydev.puertonuevoapp.core.ui.BentoDonut
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoFeed
import com.axzydev.puertonuevoapp.core.ui.BentoFeedGroup
import com.axzydev.puertonuevoapp.core.ui.BentoFeedItem
import com.axzydev.puertonuevoapp.core.ui.BentoGauge
import com.axzydev.puertonuevoapp.core.ui.BentoInsight
import com.axzydev.puertonuevoapp.core.ui.BentoLivePill
import com.axzydev.puertonuevoapp.core.ui.BentoMiniList
import com.axzydev.puertonuevoapp.core.ui.BentoMiniListItem
import com.axzydev.puertonuevoapp.core.ui.BentoMiniStat
import com.axzydev.puertonuevoapp.core.ui.BentoSegment
import com.axzydev.puertonuevoapp.core.ui.BentoSegments
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.BentoTabItem
import com.axzydev.puertonuevoapp.core.ui.BentoTabs
import com.axzydev.puertonuevoapp.core.ui.BentoTile
import com.axzydev.puertonuevoapp.core.ui.BentoWave
import com.axzydev.puertonuevoapp.core.ui.BentoWeekBars
import com.axzydev.puertonuevoapp.core.ui.BentoWeekDay
import com.axzydev.puertonuevoapp.core.ui.OnBrandChip
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.ui.rememberCountUp
import com.axzydev.puertonuevoapp.core.util.currentTimeMillis
import com.axzydev.puertonuevoapp.core.util.daysAgoLocal
import com.axzydev.puertonuevoapp.core.util.formatDateTime
import com.axzydev.puertonuevoapp.core.util.formatIsoUtc
import com.axzydev.puertonuevoapp.core.util.formatLocalTime
import com.axzydev.puertonuevoapp.core.util.formatMinutesAsHours
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import com.axzydev.puertonuevoapp.core.util.greetingForNow
import com.axzydev.puertonuevoapp.core.util.localDayKey
import com.axzydev.puertonuevoapp.core.util.localWeekdayIndex
import com.axzydev.puertonuevoapp.core.util.parseIsoToEpochMillis
import com.axzydev.puertonuevoapp.core.util.relativeDayLabel
import com.axzydev.puertonuevoapp.core.util.shortDateEs
import com.axzydev.puertonuevoapp.core.util.ticketPriorityLabel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/*
 * Inicio = clon del tablero de la web (`HomePage` / `RoleDashboard`): mismas pestañas, mismos
 * mosaicos, mismos textos y mismos endpoints, con la misma compuerta de permisos. Cocina queda
 * fuera de alcance. En el teléfono los mosaicos se acomodan en una columna (2 columnas para pares).
 */

/** Pestañas del Inicio (orden fijo de la web); solo aparecen las que tienen algún bloque permitido. */
private enum class HomeTab(val label: String, val icon: ImageVector) {
    Operations("Operación", Icons.Filled.Dashboard),
    People("Personal", Icons.Filled.Groups),
    Clock("Reloj y accesos", Icons.Filled.AccessTime),
    System("Sistema", Icons.Filled.Speed),
    Mine("Mi espacio", Icons.Filled.Laptop),
}

private val WEEK_LABELS = listOf("L", "M", "X", "J", "V", "S", "D")
private val SECTION_GAP = 12.dp

/** Mismo criterio que `STALE_SYNC_MS` de la web: un reloj sin sincronizar en 2 h está atrasado. */
private const val STALE_SYNC_MS = 2 * 60 * 60 * 1000L

/** Refresco periódico del Inicio (la web se actualiza en vivo con Ably; la app consulta cada minuto). */
private const val REFRESH_MS = 60_000L

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel {
        HomeViewModel(
            AppContainer.authRepository,
            AppContainer.inventoryApi,
            AppContainer.dashboardApi,
            AppContainer.notificationsApi,
        )
    },
) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user

    LaunchedEffect(Unit) {
        while (true) {
            delay(REFRESH_MS)
            viewModel.load()
        }
    }

    HomeContent(
        state = state,
        user = user,
        onRetry = viewModel::load,
        onNavigate = { navigator.push(it) },
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    user: SessionUser?,
    onRetry: () -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    if (user == null) return
    val tabs = buildList {
        if (user.canViewDashboard || user.canViewTickets || user.canViewTasks) add(HomeTab.Operations)
        if (user.canViewAttendance || user.canViewOvertime || user.canViewHR) add(HomeTab.People)
        if (user.canLinkTimeClock || user.canScanCredential) add(HomeTab.Clock)
        if (user.canConfigureSystem) add(HomeTab.System)
        add(HomeTab.Mine)
    }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val tab = tabs[selected.coerceIn(0, tabs.lastIndex)]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBento.canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
    ) {
        GreetingHeader(user = user)
        if (tabs.size > 1) {
            BentoTabs(
                items = tabs.map { BentoTabItem(it.label, it.icon) },
                selectedIndex = tabs.indexOf(tab),
                onSelect = { selected = it },
            )
        }

        val errors = state.errors.filterKeys { it in sectionsOf(tab) }
        val nothingYet = state.loading && !state.hasAnyData()
        if (nothingYet) {
            BentoState(message = "Cargando…", loading = true)
        } else {
            errors.values.distinct().forEach { BentoErrorState(it, onRetry) }
            when (tab) {
                HomeTab.Operations -> OperationsTab(state, user, onNavigate)
                HomeTab.People -> PeopleTab(state, user, onNavigate)
                HomeTab.Clock -> ClockTab(state, user, onNavigate)
                HomeTab.System -> SystemTab(state, onNavigate)
                HomeTab.Mine -> MineTab(state)
            }
        }
        // Aire al final: la barra inferior sobresale y el último mosaico no debe quedar pegado a ella.
        Spacer(Modifier.height(24.dp))
    }
}

private fun sectionsOf(tab: HomeTab): Set<HomeSection> = when (tab) {
    HomeTab.Operations -> setOf(HomeSection.Summary, HomeSection.Tickets, HomeSection.Tasks)
    HomeTab.People -> setOf(HomeSection.Attendance, HomeSection.Overtime, HomeSection.HrRecords)
    HomeTab.Clock -> setOf(HomeSection.SetupGaps, HomeSection.AccessToday)
    HomeTab.System -> setOf(HomeSection.SystemHealth)
    HomeTab.Mine -> setOf(HomeSection.Equipment)
}

private fun HomeUiState.hasAnyData(): Boolean =
    summary != null || tickets != null || tasks != null || equipment != null || attendance != null ||
        overtime != null || hrRecords != null || setupGaps != null || accessToday != null ||
        systemHealth != null || errors.isNotEmpty()

// ── Encabezado ──────────────────────────────────────────────────────────────

/**
 * Saludo en dos líneas; debajo, fecha corta (una línea con elipsis) y, a la derecha de esa
 * misma línea, "En vivo" (solo con `dashboard.view`, como en la web). La campana y el avatar
 * viven en el header del shell (`BentoTopBar`), así están en todas las pantallas.
 */
@Composable
private fun GreetingHeader(user: SessionUser) {
    val firstName = user.name.trim().substringBefore(' ')
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "${greetingForNow()},",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                firstName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${shortDateEs()} · Puerto Nuevo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (user.canViewDashboard) {
                Spacer(Modifier.width(8.dp))
                BentoLivePill()
            }
        }
    }
}

// ── Pestaña Operación ───────────────────────────────────────────────────────

/**
 * Con `dashboard.view` es el panel de operación (`AdminDashboard` de la web); sin él la web
 * muestra los widgets de tickets y tareas con el alcance del usuario.
 */
@Composable
private fun OperationsTab(state: HomeUiState, user: SessionUser, onNavigate: (Screen) -> Unit) {
    val summary = state.summary
    if (summary != null) {
        SummaryTiles(summary, state.categories, user, onNavigate)
    } else {
        state.tickets?.let { TicketsWidgetCard(it, user, onNavigate) }
        state.tasks?.let { TasksWidgetCard(it, user, onNavigate) }
    }
}

/** Mosaico del panel de operación, en el mismo orden que la web. */
@Composable
private fun SummaryTiles(
    summary: DashboardSummaryDto,
    categories: List<DeviceCategoryDto>,
    user: SessionUser,
    onNavigate: (Screen) -> Unit,
) {
    val devices = summary.devices
    val custody = summary.custodyLetters
    val metrics = summary.ticketMetrics
    val brand = AppBento.tone(BentoTone.Brand)
    val plain = AppBento.tone(BentoTone.Plain)
    val hasDevices = devices.total > 0
    val availablePct = if (hasDevices) (devices.available * 100f / devices.total).roundToInt() else 0
    // Igual criterio que la web: verde si casi todo está disponible, ámbar si baja y rojo si es crítico.
    val gaugeColor = when {
        availablePct >= 80 -> AppColors.Success
        availablePct >= 50 -> AppColors.Warning
        else -> AppColors.Danger
    }

    // Dispositivos: tarjeta principal con medidor de disponibilidad.
    BentoTile(
        label = "Dispositivos",
        tone = if (hasDevices) BentoTone.Brand else BentoTone.Plain,
        icon = Icons.Filled.Devices,
        bigValue = hasDevices,
        value = if (hasDevices) rememberCountUp(devices.total).toString() else "0",
        aside = if (hasDevices) "${devices.available} disponibles" else "Sin datos",
        onClick = if (user.canViewDevices) ({ onNavigate(Screen.DevicesList) }) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (hasDevices) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BentoGauge(
                    fraction = devices.available.toFloat() / devices.total,
                    color = gaugeColor,
                    trackColor = brand.content.copy(alpha = 0.18f),
                    height = 84.dp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // El disponible ya va en el aside; aquí solo el préstamo activo.
                    OnBrandChip("En préstamo ${devices.assigned}")
                }
            }
        } else {
            Text(
                "Sin dispositivos registrados",
                style = MaterialTheme.typography.bodySmall,
                color = plain.content.copy(alpha = 0.65f),
            )
        }
    }

    // Cartas activas: olas (en cero, tono neutro y sin la ola vacía).
    val hasLetters = custody.total > 0
    BentoTile(
        label = "Cartas activas",
        tone = if (hasLetters) BentoTone.Lavender else BentoTone.Plain,
        icon = Icons.Filled.Description,
        bigValue = hasLetters,
        value = if (hasLetters) rememberCountUp(custody.active).toString() else "0",
        aside = if (hasLetters) "de ${custody.total} cartas" else "Sin datos",
        onClick = if (user.canViewCustodyLetters) ({ onNavigate(Screen.CustodyLettersList) }) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (hasLetters) {
            BentoWave(
                level = custody.active.toFloat() / custody.total,
                color = AppBento.tone(BentoTone.Lavender).content,
                height = 40.dp,
            )
        } else {
            Text(
                "Sin cartas registradas",
                style = MaterialTheme.typography.bodySmall,
                color = plain.content.copy(alpha = 0.65f),
            )
        }
    }

    // Salidas dañadas + resolución promedio, en pareja.
    val damaged = summary.materialOutputs.damaged
    val avg = metrics.avgResolutionDays
    TileRow {
        BentoTile(
            label = "Salidas dañadas",
            tone = if (damaged > 0) BentoTone.Rose else BentoTone.Plain,
            icon = Icons.Filled.Delete,
            bigValue = damaged > 0,
            value = damaged.toString(),
            valueColor = if (damaged > 0) AppColors.Danger else plain.content,
            onClick = if (user.canViewDevices) ({ onNavigate(Screen.InventoryMovements) }) else null,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
            Text(
                if (damaged == 0) "Todo en orden" else "de ${summary.materialOutputs.total} salidas",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        BentoTile(
            label = "Resolución promedio",
            tone = if (avg != null) BentoTone.Sea else BentoTone.Plain,
            icon = Icons.Filled.Schedule,
            bigValue = avg != null,
            value = if (avg != null) "${oneDecimal(rememberCountUp(avg.toFloat()))} d" else "—",
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
            Text(
                if (avg != null) "por tarea" else "Sin datos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }

    // Tareas resueltas: segmentos (sin datos = nada que dibujar).
    val resolved = metrics.resolvedTasks
    val totalTasks = resolved + metrics.pendingTasks
    val hasTasks = totalTasks > 0
    BentoTile(
        label = "Tareas resueltas",
        tone = if (hasTasks) BentoTone.Mint else BentoTone.Plain,
        icon = Icons.Filled.TaskAlt,
        bigValue = hasTasks,
        value = if (hasTasks) rememberCountUp(resolved).toString() else "0",
        aside = if (hasTasks) "/ $totalTasks · ${metrics.pendingTasks} pendientes" else "Sin datos",
        onClick = if (user.canSeeAdminTasks) ({ onNavigate(Screen.AdminTasks) }) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (hasTasks) {
            BentoSegments(
                total = totalTasks,
                done = resolved,
                color = AppColors.Success,
                trackColor = AppBento.tone(BentoTone.Mint).content.copy(alpha = 0.15f),
            )
        } else {
            Text(
                "Sin tareas registradas",
                style = MaterialTheme.typography.bodySmall,
                color = plain.content.copy(alpha = 0.65f),
            )
        }
    }

    WeekActivityCard(summary.recentActivity)
    TeamEfficiencyCard(summary, user, onNavigate)
    CategoriesCard(categories, user, onNavigate)
    RecentActivityCard(summary.recentActivity, onNavigate)
}

/** Actividad de la semana: barras de los últimos 7 días (mismos eventos del hilo), hoy resaltado. */
@Composable
private fun WeekActivityCard(activity: List<DashboardActivityDto>) {
    val counts = IntArray(7)
    activity.forEach { a ->
        val ago = daysAgoLocal(a.at)
        if (ago != null && ago in 0..6) counts[6 - ago]++
    }
    val todayIdx = localWeekdayIndex(formatIsoUtc(currentTimeMillis())) ?: 0
    val plain = AppBento.tone(BentoTone.Plain)
    val total = counts.sum()
    BentoTile(
        label = "Actividad de la semana",
        icon = Icons.Filled.Timeline,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (total > 0) {
            BentoWeekBars(
                days = counts.mapIndexed { i, n ->
                    BentoWeekDay(WEEK_LABELS[((todayIdx - (6 - i)) % 7 + 7) % 7], n, today = i == 6)
                },
                color = MaterialTheme.colorScheme.primary,
                todayColor = AppColors.Warning,
                contentColor = plain.content,
            )
        } else {
            Text(
                "Sin actividad esta semana",
                style = MaterialTheme.typography.bodySmall,
                color = plain.content.copy(alpha = 0.65f),
            )
        }
    }
}

/** Eficiencia del equipo: dona de tareas resueltas por persona, barras (4) e insight. */
@Composable
private fun TeamEfficiencyCard(summary: DashboardSummaryDto, user: SessionUser, onNavigate: (Screen) -> Unit) {
    val rows = summary.ticketEfficiency
    val total = rows.sumOf { it.resolved }
    val plain = AppBento.tone(BentoTone.Plain)
    BentoTile(
        label = "Eficiencia del equipo",
        icon = Icons.Filled.PieChart,
        onClick = if (user.canViewTickets) ({ onNavigate(Screen.TicketsList) }) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (rows.isEmpty() || total == 0) {
            Text(
                "Sin tareas resueltas todavía",
                style = MaterialTheme.typography.bodySmall,
                color = plain.content.copy(alpha = 0.65f),
            )
        } else {
            val series = AppBento.series
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    BentoDonut(
                        segments = rows.mapIndexed { i, e -> BentoSegment(e.user.name, e.resolved, series[i % series.size]) },
                        trackColor = plain.content.copy(alpha = 0.08f),
                        centerColor = plain.content,
                        center = rememberCountUp(total).toString(),
                        caption = "resueltos",
                        size = 132.dp,
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val max = (rows.take(4).maxOfOrNull { it.resolved } ?: 1).coerceAtLeast(1)
                        rows.take(4).forEachIndexed { i, e ->
                            val pct = if (total > 0) (e.resolved * 100f / total).roundToInt() else 0
                            BentoBarRow(
                                label = e.user.name,
                                valueText = e.resolved.toString(),
                                trailingText = "$pct%",
                                fraction = e.resolved.toFloat() / max,
                                color = series[i % series.size],
                                trackColor = plain.content.copy(alpha = 0.08f),
                                contentColor = plain.content,
                            )
                        }
                    }
                }
                val top = rows.maxByOrNull { it.resolved }
                val idle = rows.filter { it.resolved == 0 }.map { it.user.name }
                BentoInsight(
                    text = when {
                        top == null -> "Sin datos de resolución"
                        idle.isNotEmpty() -> "${top.user.name} resolvió más; ${idle.first()} aún no tiene tareas resueltas."
                        else -> "Todo el equipo con tareas resueltas."
                    },
                    contentColor = plain.content,
                )
            }
        }
    }
}

/** Dispositivos por categoría: top 5 de mayor a menor, con su porción y barra relativa al máximo. */
@Composable
private fun CategoriesCard(categories: List<DeviceCategoryDto>, user: SessionUser, onNavigate: (Screen) -> Unit) {
    val plain = AppBento.tone(BentoTone.Plain)
    val rows = categories.filter { it.total > 0 }.sortedByDescending { it.total }.take(5)
    val sum = rows.sumOf { it.total }.coerceAtLeast(1)
    val max = (rows.maxOfOrNull { it.total } ?: 1).coerceAtLeast(1)
    BentoTile(
        label = "Dispositivos por categoría",
        icon = Icons.Filled.Category,
        onClick = if (user.canManageCatalogs) ({ onNavigate(Screen.DeviceTypesList) }) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (rows.isEmpty()) {
            Text("Sin dispositivos registrados.", style = MaterialTheme.typography.bodySmall, color = plain.content.copy(alpha = 0.6f))
        } else {
            val series = AppBento.series
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                rows.forEachIndexed { i, row ->
                    BentoBarRow(
                        label = row.name.lowercase().replaceFirstChar { it.uppercase() },
                        valueText = row.total.toString(),
                        trailingText = "${(row.total * 100f / sum).roundToInt()}%",
                        fraction = row.total.toFloat() / max,
                        color = series[i % series.size],
                        trackColor = plain.content.copy(alpha = 0.08f),
                        contentColor = plain.content,
                        leading = { Box(Modifier.size(9.dp).background(series[i % series.size], AppShape.row)) },
                    )
                }
            }
        }
    }
}

/** Actividad reciente agrupada por día (Hoy / Ayer / fecha); lista con scroll propio como en la web. */
@Composable
private fun RecentActivityCard(activity: List<DashboardActivityDto>, onNavigate: (Screen) -> Unit) {
    val plain = AppBento.tone(BentoTone.Plain)
    BentoTile(label = "Actividad reciente", icon = Icons.Filled.Timer, modifier = Modifier.fillMaxWidth()) {
        if (activity.isEmpty()) {
            Text("Aún no hay actividad registrada.", style = MaterialTheme.typography.bodySmall, color = plain.content.copy(alpha = 0.6f))
        } else {
            val groups = buildList<Pair<String, MutableList<BentoFeedItem>>> {
                activity.forEach { a ->
                    val label = relativeDayLabel(a.at)
                    val group = lastOrNull()?.takeIf { it.first == label }
                        ?: (label to mutableListOf<BentoFeedItem>()).also { add(it) }
                    val screen = activityScreen(a)
                    val (icon, tone) = activityLook(a.scope)
                    group.second += BentoFeedItem(
                        id = a.id,
                        title = a.message,
                        meta = formatLocalTime(a.at),
                        icon = icon,
                        tone = tone,
                        onClick = screen?.let { s -> { onNavigate(s) } },
                    )
                }
            }
            Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                BentoFeed(groups.map { BentoFeedGroup(it.first, it.second) }, contentColor = plain.content)
            }
        }
    }
}

/** Mismo mapeo de la web: cartas en violeta, el resto en verde. */
private fun activityLook(scope: String): Pair<ImageVector, BentoTone> = when (scope) {
    "tickets" -> Icons.Filled.ConfirmationNumber to BentoTone.Mint
    "custodyLetters" -> Icons.Filled.Description to BentoTone.Lavender
    "materialOutputs" -> Icons.Filled.Delete to BentoTone.Mint
    "devices" -> Icons.Filled.Inventory to BentoTone.Mint
    else -> Icons.Filled.Description to BentoTone.Mint
}

private fun activityScreen(a: DashboardActivityDto): Screen? = when (a.scope) {
    "tickets" -> a.targetId?.let { Screen.TicketDetail(it) }
    "custodyLetters" -> a.targetId?.let { Screen.CustodyLetterDetail(it) }
    "inventory" -> a.deviceId?.let { Screen.DeviceDetail(it) }
    else -> null
}

// ── Widgets de tickets y tareas (Operación sin `dashboard.view`) ────────────

/** Tickets con el alcance de `tickets.view`: por estado, antiguos y abiertos por responsable. */
@Composable
private fun TicketsWidgetCard(data: TicketsWidgetDto, user: SessionUser, onNavigate: (Screen) -> Unit) {
    val plain = AppBento.tone(BentoTone.Plain)
    val title = when (data.scope) {
        "ALL" -> "Tickets"
        "AREA" -> "Tickets de mi departamento"
        else -> "Mis tickets"
    }
    BentoTile(
        label = title,
        icon = Icons.Filled.ConfirmationNumber,
        onClick = if (user.canViewTickets) ({ onNavigate(Screen.TicketsList) }) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniGrid(
                listOf(
                    MiniSpec("Abiertos", data.open, alarm(data.open, BentoTone.Sun)),
                    MiniSpec("En seguimiento", data.inProgress, alarm(data.inProgress, BentoTone.Sea)),
                    MiniSpec("Sin asignar", data.unassigned, alarm(data.unassigned, BentoTone.Rose)),
                    MiniSpec("Cerrados", data.closed, alarm(data.closed, BentoTone.Mint)),
                ),
                columns = 2,
            )
            SubLabel("Más de ${data.staleDays} días abiertos")
            if (data.stale.isEmpty()) {
                Text("Todo en orden", style = MaterialTheme.typography.bodySmall, color = plain.content.copy(alpha = 0.6f))
            } else {
                data.stale.forEach { t ->
                    PersonRow(
                        name = t.assignedTo,
                        title = t.title,
                        subtitle = t.assignedTo?.let { "Asignado a $it" } ?: "Sin asignar",
                        right = "${ticketPriorityLabel(t.priority)} · ${agoLabel(daysAgoLocal(t.createdAt))}",
                        onClick = { onNavigate(Screen.TicketDetail(t.id)) },
                    )
                }
            }
            if (data.scope != "OWN") {
                SubLabel("Abiertos por responsable")
                if (data.openByAssignee.isEmpty()) {
                    Text("Todo en orden", style = MaterialTheme.typography.bodySmall, color = plain.content.copy(alpha = 0.6f))
                } else {
                    data.openByAssignee.forEach { p ->
                        PersonRow(name = p.name, title = p.name, subtitle = null, right = "${p.open} abiertos")
                    }
                }
            }
        }
    }
}

/** Tareas con el alcance de `tasks.view`: por estado, atrasadas y pendientes por persona. */
@Composable
private fun TasksWidgetCard(data: TasksWidgetDto, user: SessionUser, onNavigate: (Screen) -> Unit) {
    val plain = AppBento.tone(BentoTone.Plain)
    val title = when (data.scope) {
        "ALL" -> "Tareas"
        "AREA" -> "Tareas de mi equipo"
        else -> "Mis tareas"
    }
    val target = when {
        data.scope == "OWN" && user.canSeeMyTasks -> Screen.MyTasks
        user.canSeeAdminTasks -> Screen.AdminTasks
        else -> Screen.TicketsKanban()
    }
    BentoTile(
        label = title,
        icon = Icons.Filled.TaskAlt,
        onClick = { onNavigate(target) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniGrid(
                listOf(
                    MiniSpec("Pendientes", data.pending, alarm(data.pending, BentoTone.Sun)),
                    MiniSpec("En progreso", data.inProgress, alarm(data.inProgress, BentoTone.Sea)),
                    MiniSpec("En revisión", data.inReview, alarm(data.inReview, BentoTone.Lavender)),
                    MiniSpec("Completadas", data.completed, alarm(data.completed, BentoTone.Mint)),
                ),
                columns = 2,
            )
            SubLabel(if (data.overdue.isEmpty()) "Atrasadas" else "Atrasadas (${data.overdue.size})")
            if (data.overdue.isEmpty()) {
                Text("Todo en orden", style = MaterialTheme.typography.bodySmall, color = plain.content.copy(alpha = 0.6f))
            } else {
                data.overdue.forEach { t ->
                    PersonRow(
                        name = t.name,
                        title = t.title,
                        subtitle = t.name,
                        right = t.dueDate?.let { "Vencía el ${formatShortDate(it)}" },
                        onClick = { onNavigate(Screen.TicketsKanban(t.ticketId)) },
                    )
                }
            }
            if (data.scope != "OWN") {
                SubLabel("Pendientes por persona")
                if (data.pendingByUser.isEmpty()) {
                    Text("Todo en orden", style = MaterialTheme.typography.bodySmall, color = plain.content.copy(alpha = 0.6f))
                } else {
                    data.pendingByUser.forEach { p ->
                        PersonRow(name = p.name, title = p.name, subtitle = null, right = "${p.pending} pendientes")
                    }
                }
            }
        }
    }
}

// ── Pestaña Personal ────────────────────────────────────────────────────────

/**
 * Con `hr.records` es el mosaico de la web (expedientes, asistencia, horas extra y seguimiento);
 * sin él, los widgets de asistencia y horas extra según su permiso.
 * El tablero de expedientes (tabla con filtros y subida de documentos) no se replica en el móvil.
 */
@Composable
private fun PeopleTab(state: HomeUiState, user: SessionUser, onNavigate: (Screen) -> Unit) {
    if (user.canViewHR) {
        PersonalPanel(state, user, onNavigate)
    } else {
        state.attendance?.let { AttendanceWidgetCard(it, user, onNavigate) }
        state.overtime?.let { OvertimeWidgetCard(it) }
    }
}

@Composable
private fun PersonalPanel(state: HomeUiState, user: SessionUser, onNavigate: (Screen) -> Unit) {
    val rows = state.hrRecords?.rows.orEmpty()
    val total = rows.size
    val required = rows.count { it.missingRequired.isNotEmpty() }
    val incomplete = rows.count { it.missingRequired.isEmpty() && (it.missingDocuments.isNotEmpty() || it.missingFields.isNotEmpty()) }
    val complete = total - required - incomplete
    val brand = AppBento.tone(BentoTone.Brand)
    val plain = AppBento.tone(BentoTone.Plain)

    // Expedientes: tarjeta principal con medidor y chips (en cero, tono neutro).
    val hasPeople = total > 0
    BentoTile(
        label = "Plantilla activa",
        tone = if (hasPeople) BentoTone.Brand else BentoTone.Plain,
        icon = Icons.Filled.Groups,
        bigValue = hasPeople,
        value = if (hasPeople) rememberCountUp(total).toString() else "0",
        aside = if (hasPeople) "${total - complete} sin expediente completo" else "Sin datos",
        onClick = { onNavigate(Screen.EmployeesList) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (hasPeople) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BentoGauge(
                    fraction = complete.toFloat() / total,
                    color = if (complete == total) AppColors.Success else AppColors.Warning,
                    trackColor = brand.content.copy(alpha = 0.18f),
                    height = 84.dp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OnBrandChip(
                        if (required > 0) "Faltan obligatorios $required" else "Sin obligatorios pendientes",
                    )
                    OnBrandChip("Completos $complete")
                }
                OnBrandChip("Incompletos $incomplete")
            }
        } else {
            Text(
                "Sin expedientes registrados",
                style = MaterialTheme.typography.bodySmall,
                color = plain.content.copy(alpha = 0.65f),
            )
        }
    }

    // Asistencia de hoy.
    val c = state.attendance?.counts ?: AttendanceCountsDto()
    BentoTile(label = "Asistencia de hoy", icon = Icons.Filled.HowToReg, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val arrived = c.present + c.late
            StatusChip(
                if (arrived > 0) "$arrived han llegado" else "Nadie ha llegado aún",
                if (arrived > 0) BentoTone.Mint else BentoTone.Plain,
            )
            MiniGrid(attendanceSpecs(c), columns = 2)
        }
    }

    // Horas extra de la semana.
    val o = state.overtime ?: OvertimeWeekWidgetDto()
    val hasExtra = o.extraMin > 0
    BentoTile(
        label = "Horas extra de la semana",
        tone = if (hasExtra) BentoTone.Sun else BentoTone.Plain,
        icon = Icons.Filled.Timer,
        bigValue = hasExtra,
        value = formatMinutesAsHours(o.extraMin),
        aside = if (hasExtra) "Calculadas" else "Sin datos",
        modifier = Modifier.fillMaxWidth(),
    ) {
        MiniGrid(
            listOf(
                MiniSpec("Aprobadas", formatMinutesAsHours(o.approvedExtraMin), alarm(o.approvedExtraMin, BentoTone.Mint)),
                MiniSpec("No aprobadas", formatMinutesAsHours(o.pendingExtraMin), alarm(o.pendingExtraMin, BentoTone.Sun)),
                MiniSpec("Rechazadas", formatMinutesAsHours(o.rejectedExtraMin), alarm(o.rejectedExtraMin, BentoTone.Rose)),
            ),
            columns = 2,
            labelLines = 2,
        )
    }

    // Seguimiento del día: tres listas cortas.
    val att = state.attendance
    val notArrived = att?.rows.orEmpty().filter { it.status == "NOT_ARRIVED" }
    val late = att?.rows.orEmpty().filter { it.status == "LATE" }.sortedByDescending { it.lateMin }
    BentoTile(label = "Seguimiento del día", icon = Icons.Filled.TaskAlt, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BentoMiniList(
                icon = Icons.Filled.PersonOff,
                title = "No han llegado",
                emptyLabel = "Sin pendientes",
                contentColor = plain.content,
                items = notArrived.map { BentoMiniListItem(it.userId, it.name, it.shift ?: "—") },
            )
            BentoMiniList(
                icon = Icons.Filled.EventBusy,
                title = "Llegaron tarde",
                emptyLabel = "Sin pendientes",
                contentColor = plain.content,
                items = late.map { BentoMiniListItem(it.userId, it.name, "${it.lateMin} min") },
            )
            BentoMiniList(
                icon = Icons.Filled.Timer,
                title = "Más horas sin aprobar",
                emptyLabel = "Sin pendientes",
                contentColor = plain.content,
                items = o.topPending.take(4).map { BentoMiniListItem(it.userId, it.name, formatMinutesAsHours(it.pendingExtraMin)) },
            )
        }
    }
}

private fun attendanceSpecs(c: AttendanceCountsDto): List<MiniSpec> = listOf(
    MiniSpec("A tiempo", c.present, alarm(c.present, BentoTone.Mint)),
    MiniSpec("Retardo", c.late, alarm(c.late, BentoTone.Sun)),
    MiniSpec("No ha llegado", c.notArrived, alarm(c.notArrived, BentoTone.Rose)),
    MiniSpec("Aún no es su hora", c.notYet, alarm(c.notYet, BentoTone.Sea)),
    MiniSpec("En sitio", c.onSite, alarm(c.onSite, BentoTone.Lavender)),
    MiniSpec("Sin vínculo al reloj", c.unlinked, alarm(c.unlinked, BentoTone.Sun)),
)

/** Asistencia de hoy con el alcance de `attendance.view` (RH/gerencia: todos; jefe de área: su departamento; empleado: su día). */
@Composable
private fun AttendanceWidgetCard(data: AttendanceTodayWidgetDto, user: SessionUser, onNavigate: (Screen) -> Unit) {
    val plain = AppBento.tone(BentoTone.Plain)
    val title = when (data.scope) {
        "ALL" -> "Asistencia de hoy"
        "AREA" -> "Asistencia de mi departamento"
        else -> "Mi asistencia de hoy"
    }
    val own = if (data.scope == "OWN") data.rows.firstOrNull() else null
    BentoTile(label = title, icon = Icons.Filled.HowToReg, modifier = Modifier.fillMaxWidth()) {
        if (own != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(
                    statusLabel(own.status),
                    if (own.status == "LATE" || own.status == "NOT_ARRIVED") BentoTone.Sun else BentoTone.Mint,
                )
                Text(
                    ownAttendanceMessage(own.status, own.entryAt, own.scheduledStartAt, own.lateMin),
                    style = MaterialTheme.typography.bodyMedium,
                    color = plain.content,
                )
                own.shift?.let { Text("Horario $it", style = MaterialTheme.typography.labelSmall, color = plain.content.copy(alpha = 0.6f)) }
            }
        } else {
            val c = data.counts
            val dayTotal = c.present + c.late + c.notArrived + c.notYet
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniGrid(attendanceSpecs(c).filterNot { it.label == "Sin vínculo al reloj" && c.unlinked == 0 }, columns = 2)
                SubLabel("Cómo va el día")
                val segments = listOf(
                    BentoSegment("A tiempo", c.present, AppColors.Success),
                    BentoSegment("Retardo", c.late, AppColors.Warning),
                    BentoSegment("No ha llegado", c.notArrived, AppColors.Danger),
                    BentoSegment("Aún no es su hora", c.notYet, AppColors.Info),
                )
                if (dayTotal == 0) {
                    Text(
                        "Sin datos de asistencia hoy",
                        style = MaterialTheme.typography.bodySmall,
                        color = plain.content.copy(alpha = 0.65f),
                    )
                }
                if (dayTotal > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    BentoDonut(
                        segments = segments,
                        trackColor = plain.content.copy(alpha = 0.08f),
                        centerColor = plain.content,
                        center = segments.sumOf { it.value }.toString(),
                        size = 126.dp,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        segments.forEach { seg ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(9.dp).background(seg.color, AppShape.row))
                                Spacer(Modifier.width(8.dp))
                                Text("${seg.label} ${seg.value}", style = MaterialTheme.typography.bodySmall, color = plain.content)
                            }
                        }
                    }
                }
                SubLabel("No han llegado")
                val notArrived = data.rows.filter { it.status == "NOT_ARRIVED" }
                if (notArrived.isEmpty()) {
                    Text("Todo en orden", style = MaterialTheme.typography.bodySmall, color = plain.content.copy(alpha = 0.6f))
                } else {
                    notArrived.forEach {
                        PersonRow(
                            name = it.name,
                            title = it.name,
                            subtitle = it.departmentName ?: "Sin departamento",
                            right = "Turno ${it.shift ?: "—"}",
                        )
                    }
                }
                SubLabel("Llegaron tarde")
                val late = data.rows.filter { it.status == "LATE" }.sortedByDescending { it.lateMin }
                if (late.isEmpty()) {
                    Text("Todo en orden", style = MaterialTheme.typography.bodySmall, color = plain.content.copy(alpha = 0.6f))
                } else {
                    late.forEach {
                        PersonRow(
                            name = it.name,
                            title = it.name,
                            subtitle = listOfNotNull(
                                it.departmentName,
                                it.entryAt?.let { e -> "Entrada ${formatLocalTime(e)}" },
                            ).joinToString(" · ").ifBlank { "Sin departamento" },
                            right = "${it.lateMin} min",
                        )
                    }
                }
            }
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    "PRESENT" -> "A tiempo"
    "LATE" -> "Retardo"
    "NOT_ARRIVED" -> "No ha llegado"
    "NOT_YET" -> "Aún no es su hora"
    "REST" -> "Descanso"
    "NO_SCHEDULE" -> "Sin horario"
    "UNLINKED" -> "Sin vínculo al reloj"
    else -> status
}

private fun ownAttendanceMessage(status: String, entryAt: String?, scheduledStartAt: String?, lateMin: Int): String = when (status) {
    "PRESENT", "LATE" ->
        "Entraste a las ${formatLocalTime(entryAt)}" + if (lateMin > 0) " · $lateMin min tarde" else ""
    "NOT_ARRIVED" -> "Aún no registras tu entrada (tu horario empieza a las ${formatLocalTime(scheduledStartAt)})"
    "NOT_YET" -> "Tu entrada es a las ${formatLocalTime(scheduledStartAt)}"
    "REST" -> "Hoy es tu día de descanso"
    "UNLINKED" -> "Tu número del reloj no está vinculado; pide a RH que lo vincule"
    else -> "No tienes horario asignado hoy"
}

/** Horas extra de la semana (`overtime.view`): calculadas, aprobadas, no aprobadas y rechazadas. */
@Composable
private fun OvertimeWidgetCard(data: OvertimeWeekWidgetDto) {
    val plain = AppBento.tone(BentoTone.Plain)
    BentoTile(label = "Horas extra de la semana", icon = Icons.Filled.Timer, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniGrid(
                listOf(
                    MiniSpec("Calculadas", formatMinutesAsHours(data.extraMin), BentoTone.Sea),
                    MiniSpec("Aprobadas", formatMinutesAsHours(data.approvedExtraMin), BentoTone.Mint),
                    MiniSpec("No aprobadas", formatMinutesAsHours(data.pendingExtraMin), BentoTone.Sun),
                    MiniSpec("Rechazadas", formatMinutesAsHours(data.rejectedExtraMin), BentoTone.Rose),
                ),
                columns = 2,
            )
            SubLabel("Decisión sobre las horas extra")
            val parts = listOf(
                Triple("Aprobadas", data.approvedExtraMin, AppColors.Success),
                Triple("No aprobadas", data.pendingExtraMin, AppColors.Warning),
                Triple("Rechazadas", data.rejectedExtraMin, AppColors.Danger),
            )
            val sum = parts.sumOf { it.second }
            Row(
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(AppShape.pill).background(plain.content.copy(alpha = 0.08f)),
            ) {
                if (sum > 0) parts.filter { it.second > 0 }.forEach { (_, v, color) ->
                    Box(Modifier.weight(v.toFloat()).fillMaxHeight().background(color))
                }
            }
            SubLabel("Más horas sin aprobar")
            if (data.topPending.isEmpty()) {
                Text("Sin pendientes", style = MaterialTheme.typography.bodySmall, color = plain.content.copy(alpha = 0.6f))
            } else {
                data.topPending.forEach {
                    PersonRow(it.name, it.name, it.departmentName ?: "—", formatMinutesAsHours(it.pendingExtraMin))
                }
            }
        }
    }
}

// ── Pestaña Reloj y accesos ─────────────────────────────────────────────────

@Composable
private fun ClockTab(state: HomeUiState, user: SessionUser, onNavigate: (Screen) -> Unit) {
    val setup = state.setupGaps ?: SetupGapsWidgetDto()
    val access = state.accessToday ?: AccessTodayWidgetDto()
    val brand = AppBento.tone(BentoTone.Brand)
    val plain = AppBento.tone(BentoTone.Plain)

    // Pendientes de configuración: lo que impide calcular bien la asistencia.
    BentoTile(label = "Pendientes de configuración", icon = Icons.Filled.Build, modifier = Modifier.fillMaxWidth()) {
        MiniGrid(
            listOf(
                MiniSpec("Números del reloj sin vincular", setup.unlinkedClockNumbers, alarm(setup.unlinkedClockNumbers, BentoTone.Sun)),
                MiniSpec("Personal sin vínculo al reloj", setup.personalWithoutClock, alarm(setup.personalWithoutClock, BentoTone.Sun)),
                MiniSpec("Personal sin horario", setup.personalWithoutSchedule, alarm(setup.personalWithoutSchedule, BentoTone.Sun)),
            ),
            columns = 3,
            labelLines = 4,
        )
    }

    // Accesos de hoy: tarjeta principal con medidor de quién sigue dentro.
    val inside = access.onSite.size
    val hasAccess = access.entries > 0
    BentoTile(
        label = "Accesos de hoy",
        tone = if (hasAccess) BentoTone.Brand else BentoTone.Plain,
        icon = Icons.Filled.MeetingRoom,
        bigValue = hasAccess,
        value = rememberCountUp(inside).toString(),
        aside = if (hasAccess) "de ${access.entries} entradas" else "Sin datos",
        onClick = if (user.canQueryAccessLog) ({ onNavigate(Screen.AccessLog) }) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hasAccess) {
                BentoGauge(
                    fraction = inside.toFloat() / access.entries,
                    color = AppColors.Info,
                    trackColor = brand.content.copy(alpha = 0.18f),
                    height = 84.dp,
                )
            } else {
                Text(
                    "Sin accesos registrados hoy",
                    style = MaterialTheme.typography.bodySmall,
                    color = brand.content.copy(alpha = 0.7f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OnBrandChip("Entradas ${access.entries}")
                OnBrandChip("Salidas ${access.exits}")
            }
            OnBrandChip("Registrados por mí ${access.registeredByMe}")
        }
    }

    // Quién sigue en sitio.
    BentoTile(label = "En sitio", icon = Icons.Filled.AccessTime, modifier = Modifier.fillMaxWidth()) {
        if (access.onSite.isEmpty()) {
            Text("Nadie dentro del hotel ahora", style = MaterialTheme.typography.bodySmall, color = plain.content.copy(alpha = 0.6f))
        } else {
            Column(
                modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                access.onSite.forEach { p ->
                    PersonRow(
                        name = p.name,
                        title = p.name ?: "—",
                        subtitle = p.site ?: "Sin sitio",
                        right = formatLocalTime(p.since),
                    )
                }
            }
        }
    }

    // Últimos registros del día.
    BentoTile(label = "Últimos registros", icon = Icons.Filled.MeetingRoom, modifier = Modifier.fillMaxWidth()) {
        if (access.latest.isEmpty()) {
            Text("Todavía no hay registros hoy", style = MaterialTheme.typography.bodySmall, color = plain.content.copy(alpha = 0.6f))
        } else {
            Column(modifier = Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
                BentoFeed(
                    groups = listOf(
                        BentoFeedGroup(
                            "Hoy",
                            access.latest.map { e ->
                                BentoFeedItem(
                                    id = e.id,
                                    title = e.name ?: "—",
                                    meta = listOfNotNull(formatLocalTime(e.at), e.site).joinToString(" · "),
                                    icon = Icons.Filled.MeetingRoom,
                                    tone = if (e.type == "ENTRY") BentoTone.Mint else BentoTone.Lavender,
                                )
                            },
                        ),
                    ),
                    contentColor = plain.content,
                )
            }
        }
    }
}

// ── Pestaña Sistema ─────────────────────────────────────────────────────────

@Composable
private fun SystemTab(state: HomeUiState, onNavigate: (Screen) -> Unit) {
    val health = state.systemHealth ?: SystemHealthWidgetDto()
    val brand = AppBento.tone(BentoTone.Brand)
    val plain = AppBento.tone(BentoTone.Plain)
    val now = currentTimeMillis()
    fun isStale(syncedAt: String?): Boolean {
        val at = syncedAt?.let(::parseIsoToEpochMillis) ?: return true
        return now - at > STALE_SYNC_MS
    }
    val clocks = health.clocks
    val total = clocks.size
    val fresh = clocks.count { !isStale(it.syncedAt) }
    val stale = total - fresh

    // Salud general: relojes sincronizados en las últimas 2 h.
    BentoTile(
        label = "Salud del sistema",
        tone = if (total > 0) BentoTone.Brand else BentoTone.Plain,
        icon = Icons.Filled.Speed,
        bigValue = total > 0,
        value = rememberCountUp(fresh).toString(),
        aside = if (total > 0) "de $total relojes" else "Sin datos",
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (total > 0) {
                BentoGauge(
                    fraction = fresh.toFloat() / total,
                    color = if (stale > 0) AppColors.Warning else AppColors.Success,
                    trackColor = brand.content.copy(alpha = 0.18f),
                    height = 84.dp,
                )
            } else {
                Text("Sin relojes registrados", style = MaterialTheme.typography.bodySmall, color = brand.content.copy(alpha = 0.7f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OnBrandChip("$fresh sincronizados")
                OnBrandChip("$stale atrasados")
            }
        }
    }

    // Correos.
    val failed = health.failedEmailsLast7Days
    val pending = health.pendingEmails
    BentoTile(label = "Correos", icon = Icons.Filled.Email, modifier = Modifier.fillMaxWidth()) {
        MiniGrid(
            listOf(
                MiniSpec("Correos fallidos (7 días)", failed, if (failed > 0) BentoTone.Rose else BentoTone.Mint),
                MiniSpec("Correos en cola", pending, if (pending > 0) BentoTone.Sun else BentoTone.Mint),
            ),
            columns = 2,
            labelLines = 2,
        )
    }

    // Auditor de inventario.
    val audit = health.inventoryAudit
    BentoTile(
        label = "Auditor de inventario",
        icon = Icons.Filled.VerifiedUser,
        onClick = { onNavigate(Screen.InventoryIndex) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (audit != null) {
            Column {
                Text(audit.title, style = MaterialTheme.typography.titleSmall, color = plain.content)
                Text(
                    listOfNotNull(audit.detail, formatDateTime(audit.at)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = plain.content.copy(alpha = 0.6f),
                )
            }
        } else {
            Text("Sin revisión registrada", style = MaterialTheme.typography.bodySmall, color = plain.content.copy(alpha = 0.6f))
        }
    }

    // Relojes con su última sincronización.
    BentoTile(label = "Relojes", icon = Icons.Filled.AccessTime, modifier = Modifier.fillMaxWidth()) {
        if (total == 0) {
            Text("Sin relojes registrados", style = MaterialTheme.typography.bodySmall, color = plain.content.copy(alpha = 0.6f))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                clocks.forEach { c ->
                    val late = isStale(c.syncedAt)
                    val color = if (late) AppColors.Warning else AppColors.Success
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppShape.row)
                            .background(plain.content.copy(alpha = 0.04f), AppShape.row)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(8.dp).background(color, CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(c.name ?: c.serialNumber, style = MaterialTheme.typography.bodyMedium, color = plain.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                if (c.syncedAt != null) formatDateTime(c.syncedAt) else "Nunca sincronizado",
                                style = MaterialTheme.typography.labelSmall,
                                color = plain.content.copy(alpha = 0.6f),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        StatusChip(
                            if (late) "Atrasado" else "Al día",
                            if (late) BentoTone.Sun else BentoTone.Mint,
                        )
                    }
                }
            }
        }
    }

    // Atajos de configuración (solo los destinos que existen en la app).
    BentoTile(label = "Atajos de configuración", icon = Icons.Filled.Tune, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShortcutRow("Roles y permisos") { onNavigate(Screen.RolesPermissions) }
            ShortcutRow("Catálogos") { onNavigate(Screen.DeviceTypesList) }
            ShortcutRow("Auditoría de inventario") { onNavigate(Screen.InventoryIndex) }
        }
    }
}

@Composable
private fun ShortcutRow(label: String, onClick: () -> Unit) {
    val content = AppBento.tone(BentoTone.Plain).content
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShape.row)
            .background(content.copy(alpha = 0.04f), AppShape.row)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = content, modifier = Modifier.weight(1f))
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = content.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp),
        )
    }
}

// ── Pestaña Mi espacio ──────────────────────────────────────────────────────

@Composable
private fun MineTab(state: HomeUiState) {
    state.equipment?.let { MyEquipmentCard(it) }
}

/** Equipo en mis cartas responsivas vigentes. */
@Composable
private fun MyEquipmentCard(equipment: MyEquipmentWidgetDto) {
    val plain = AppBento.tone(BentoTone.Plain)
    BentoTile(label = "Mi equipo asignado", icon = Icons.Filled.Laptop, modifier = Modifier.fillMaxWidth()) {
        if (equipment.loans.isEmpty()) {
            Text("No tienes equipo asignado", style = MaterialTheme.typography.bodySmall, color = plain.content.copy(alpha = 0.6f))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                equipment.loans.forEach { loan ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppShape.row)
                            .background(plain.content.copy(alpha = 0.05f), AppShape.row)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Carta ${loan.number}",
                                style = MaterialTheme.typography.titleSmall,
                                color = plain.content,
                                modifier = Modifier.weight(1f),
                            )
                            Text(formatShortDate(loan.date), style = MaterialTheme.typography.labelSmall, color = plain.content.copy(alpha = 0.6f))
                        }
                        loan.items.forEach { item ->
                            Column {
                                Text(
                                    "${item.name} · ${item.brand} ${item.model} ×${item.pending}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = plain.content,
                                )
                                item.units.forEach { u ->
                                    Text(
                                        buildString {
                                            append(u.assetTag)
                                            if (!u.serialNumber.isNullOrBlank()) append(" · Serie ${u.serialNumber}")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = plain.content.copy(alpha = 0.6f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Piezas locales ──────────────────────────────────────────────────────────

private data class MiniSpec(val label: String, val value: String, val tone: BentoTone) {
    constructor(label: String, value: Int, tone: BentoTone) : this(label, value.toString(), tone)
}

/** Color solo si hay algo que atender; en cero, neutral (igual que `alarmTone` de la web). */
private fun alarm(count: Int, tone: BentoTone): BentoTone = if (count > 0) tone else BentoTone.Plain

/** Fila de dos mosaicos de la misma altura (pares del mosaico en el teléfono). */
@Composable
private fun TileRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(SECTION_GAP),
        content = content,
    )
}

/** Cuadrícula de mini mosaicos de [columns] columnas (rellena la última fila). */
@Composable
private fun MiniGrid(items: List<MiniSpec>, columns: Int, labelLines: Int = 2) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                row.forEach {
                    BentoMiniStat(
                        it.label, it.value, tone = it.tone, labelMaxLines = labelLines,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Subtítulo de una lista dentro de un mosaico. */
@Composable
private fun SubLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = AppBento.tone(BentoTone.Plain).content,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/** Fila de lista (avatar con iniciales si hay [name], título, subtítulo tenue y valor a la derecha). */
@Composable
private fun PersonRow(name: String?, title: String, subtitle: String?, right: String?, onClick: (() -> Unit)? = null) {
    val content = AppBento.tone(BentoTone.Plain).content
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShape.row)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (name != null) {
            BentoAvatar(name, MaterialTheme.colorScheme.primary, size = 32.dp)
            Spacer(Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (right != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                right,
                style = MaterialTheme.typography.labelSmall,
                color = content.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun oneDecimal(x: Float): String = ((x * 10f).roundToInt() / 10f).toString()

/** "hoy" / "ayer" / "hace 3 d": fechas humanas, nunca un ISO ni un número suelto. */
private fun agoLabel(days: Int?): String = when {
    days == null -> "—"
    days <= 0 -> "hoy"
    days == 1 -> "ayer"
    else -> "hace $days d"
}
