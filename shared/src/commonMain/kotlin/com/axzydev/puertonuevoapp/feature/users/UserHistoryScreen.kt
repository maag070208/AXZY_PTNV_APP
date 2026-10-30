package com.axzydev.puertonuevoapp.feature.users

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.users.UserDto
import com.axzydev.puertonuevoapp.core.network.users.UserHistoryEntryDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoFeed
import com.axzydev.puertonuevoapp.core.ui.BentoFeedGroup
import com.axzydev.puertonuevoapp.core.ui.BentoFeedItem
import com.axzydev.puertonuevoapp.core.ui.BentoMiniStat
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatLocalTime
import com.axzydev.puertonuevoapp.core.util.initials
import com.axzydev.puertonuevoapp.core.util.relativeDayLabel
import com.axzydev.puertonuevoapp.core.util.roleLabel

/**
 * Historial de una cuenta (`/users/:id/history` de la web): ficha de la
 * persona, totales reales de su actividad, píldoras por familia de evento y
 * feed agrupado por día (Hoy / Ayer / fecha).
 *
 * Los tickets y las cartas del feed abren su pantalla solo si la sesión tiene
 * el permiso de esa pantalla; el resto de eventos son informativos.
 */
@Composable
fun UserHistoryScreen(
    userId: String,
    viewModel: UserHistoryViewModel = viewModel(key = "user-history-$userId") {
        UserHistoryViewModel(userId, AppContainer.usersApi)
    },
) {
    val state by viewModel.uiState.collectAsState()
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val navigator = LocalNavigator.current

    if (user == null || !user.can(PermissionKeys.USERS_VIEW)) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            BentoState(
                message = "No autorizado — se requiere el permiso Usuarios",
                tone = BentoTone.Rose,
                icon = Icons.Filled.Lock,
            )
        }
        return
    }

    when {
        state.loading -> Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            BentoState(message = "Cargando el historial de la cuenta…", loading = true)
        }

        state.error != null -> Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            BentoErrorState(message = state.error ?: "No se pudo cargar el historial", onRetry = viewModel::load)
        }

        else -> {
            val canTickets = user.can(PermissionKeys.TICKETS_VIEW)
            val canLoans = user.can(PermissionKeys.LOANS_VIEW)
            val grouped = linkedMapOf<String, MutableList<BentoFeedItem>>()
            state.visible.forEach { entry ->
                val label = relativeDayLabel(entry.timestamp)
                grouped.getOrPut(label) { mutableListOf() } += historyFeedItem(entry, canTickets, canLoans) {
                    navigator.push(it)
                }
            }
            val groups = grouped.map { (label, items) -> BentoFeedGroup(label, items) }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.user?.let { account ->
                    item(key = "header") { UserHeaderCard(account) }
                }
                item(key = "stats") { HistoryStatsRow(state) }
                if (state.countsByFamily.any { (family, count) -> family != HistoryFamily.All && count > 0 }) {
                    item(key = "filters") { HistoryFamilyFilters(state, viewModel::onFamilyChange) }
                }
                if (groups.isEmpty()) {
                    item(key = "empty") {
                        BentoState(
                            message = if (state.total == 0) {
                                "Esta cuenta todavía no tiene actividad registrada."
                            } else {
                                "No hay eventos de esta familia en el historial."
                            },
                            icon = Icons.Filled.History,
                        )
                    }
                } else {
                    groups.forEach { group ->
                        item(key = "day-${group.label}") {
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                BentoFeed(
                                    groups = listOf(group),
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Ficha de la cuenta: identidad, rol, estado y adscripción. */
@Composable
private fun UserHeaderCard(account: UserDto) {
    val accent = AppColors.roleAccent(account.role)
    val gradient = Brush.verticalGradient(listOf(accent, lerp(accent, Color.Black, 0.30f)))

    AppCard(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(gradient, AppShape.pill),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initials(account.name),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "@${account.username}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        account.department?.name,
                        account.subarea?.name?.takeIf { it.isNotBlank() },
                        account.jobTitle?.takeIf { it.isNotBlank() },
                    ).joinToString(" · ").ifBlank { "Sin adscripción registrada" },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(label = roleLabel(account.role), tone = roleTone(account.role))
                Spacer(Modifier.height(6.dp))
                StatusChip(
                    label = if (account.active) "Activa" else "Inactiva",
                    tone = if (account.active) BentoTone.Mint else BentoTone.Rose,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = AppColors.Outline.copy(alpha = 0.35f))
        Spacer(Modifier.height(10.dp))
        Text(
            listOfNotNull(
                account.employeeNumber?.takeIf { it.isNotBlank() }?.let { "No. empleado $it" },
                account.email?.takeIf { it.isNotBlank() },
                account.company?.takeIf { it.isNotBlank() },
            ).joinToString(" · ").ifBlank { "Sin datos de contacto" },
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextFaint,
        )
    }
}

/** Totales del historial: eventos, días con actividad y cambios de la cuenta. */
@Composable
private fun HistoryStatsRow(state: UserHistoryUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BentoMiniStat(
            label = "Eventos",
            value = state.total.toString(),
            tone = BentoTone.Sea,
            modifier = Modifier.weight(1f),
        )
        BentoMiniStat(
            label = "Días",
            value = state.daysWithActivity.toString(),
            tone = BentoTone.Lavender,
            modifier = Modifier.weight(1f),
        )
        BentoMiniStat(
            label = "Cuenta",
            value = (state.countsByFamily[HistoryFamily.Account] ?: 0).toString(),
            tone = if ((state.countsByFamily[HistoryFamily.Account] ?: 0) > 0) BentoTone.Sun else BentoTone.Plain,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Píldoras por familia de evento, solo con las familias que tienen eventos. */
@Composable
private fun HistoryFamilyFilters(
    state: UserHistoryUiState,
    onFamilyChange: (HistoryFamily) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("Filtrar por tipo de evento")
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HistoryFamily.entries
                .filter { it == HistoryFamily.All || (state.countsByFamily[it] ?: 0) > 0 }
                .forEach { family ->
                    val count = if (family == HistoryFamily.All) state.total else state.countsByFamily[family] ?: 0
                    AppFilterChip(
                        label = "${family.label} ($count)",
                        selected = state.family == family,
                        onClick = { onFamilyChange(family) },
                    )
                }
        }
    }
}

/** Evento del feed: título ya traducido por el API, detalle y hora local. */
private fun historyFeedItem(
    entry: UserHistoryEntryDto,
    canTickets: Boolean,
    canLoans: Boolean,
    onOpen: (Screen) -> Unit,
): BentoFeedItem {
    val (icon, tone) = historyLook(entry.type)
    val target = historyTarget(entry, canTickets, canLoans)
    return BentoFeedItem(
        id = entry.id,
        title = if (entry.detail.isBlank()) entry.title else "${entry.title}: ${entry.detail}",
        meta = formatLocalTime(entry.timestamp),
        icon = icon,
        tone = tone,
        onClick = target?.let { destination -> { onOpen(destination) } },
    )
}

/** Ícono y tono por tipo de evento (el API manda el `type`, no el ícono). */
private fun historyLook(type: String): Pair<ImageVector, BentoTone> = when (type) {
    "LOAN_CUSTODIAN" -> Icons.Filled.Description to BentoTone.Lavender
    "MOVEMENT" -> Icons.Filled.SwapHoriz to BentoTone.Mint
    "TICKET_CREATED" -> Icons.Filled.ConfirmationNumber to BentoTone.Mint
    "TICKET_ASSIGNED" -> Icons.AutoMirrored.Filled.Assignment to BentoTone.Sea
    "TICKET_COMMENT" -> Icons.AutoMirrored.Filled.Comment to BentoTone.Sea
    "USER_DEACTIVATED" -> Icons.Filled.PersonOff to BentoTone.Rose
    "USER_REACTIVATED" -> Icons.Filled.RestartAlt to BentoTone.Mint
    else -> Icons.Filled.Timer to BentoTone.Plain
}

/** Pantalla a la que lleva un evento cuando tiene `refId` y la sesión puede verla. */
private fun historyTarget(
    entry: UserHistoryEntryDto,
    canTickets: Boolean,
    canLoans: Boolean,
): Screen? {
    val refId = entry.refId?.takeIf { it.isNotBlank() } ?: return null
    return when (familyOf(entry.type)) {
        HistoryFamily.Tickets -> if (canTickets) Screen.TicketDetail(refId) else null
        HistoryFamily.Equipment -> if (entry.type == "LOAN_CUSTODIAN" && canLoans) {
            Screen.CustodyLetterDetail(refId)
        } else {
            null
        }
        else -> null
    }
}
