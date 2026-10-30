package com.axzydev.puertonuevoapp.feature.tickets

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.tickets.TicketDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.RegisterTopBarAction
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import com.axzydev.puertonuevoapp.core.util.ticketPriorityLabel
import com.axzydev.puertonuevoapp.core.util.ticketStatusLabel
import com.axzydev.puertonuevoapp.feature.langostin.LangostinHints
import com.axzydev.puertonuevoapp.feature.langostin.LangostinLauncher
import com.axzydev.puertonuevoapp.feature.langostin.LangostinTicketsHint

/** Cuántos tickets se muestran antes de pedir "Cargar más". */
private const val TICKETS_PAGE = 20

/**
 * Lista de tickets: encabezado con descripción y acción primaria "Nuevo" en la
 * barra superior, búsqueda, filtros por estado con contador y filas con título,
 * meta y chips de estado/prioridad. El texto no se repite: la descripción solo
 * se pinta cuando aporta algo distinto al título.
 */
@Composable
fun TicketsListScreen(viewModel: TicketsListViewModel = viewModel { TicketsListViewModel(AppContainer.ticketsApi) }) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canCreate = user?.canCreateTickets == true
    // Globo proactivo: solo con `tickets.create` y si no se descartó en esta sesión.
    val showLangostinHint = canCreate && LangostinHints.showsTicketHint(user)

    // Acción primaria en la barra superior (a la derecha), nunca flotando sobre la lista.
    if (canCreate) {
        RegisterTopBarAction(Icons.Filled.Add, "Nuevo ticket") {
            navigator.push(Screen.NewTicket)
        }
    }

    var visibleCount by remember { mutableStateOf(TICKETS_PAGE) }
    LaunchedEffect(state.query, state.statusFilter) { visibleCount = TICKETS_PAGE }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp)) {
                Text(
                    "Da seguimiento a los tickets del hotel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                )
                Spacer(Modifier.height(10.dp))
                AppSearchField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = "Buscar por título o descripción…",
                )
                Spacer(Modifier.height(10.dp))
                StatusFilterRow(state = state, onSelect = viewModel::onStatusFilterChange)
            }

            when {
                state.loading -> LoadingState(modifier = Modifier.weight(1f))

                state.error != null -> ErrorState(
                    message = state.error ?: "No se pudieron cargar los tickets",
                    modifier = Modifier.weight(1f),
                    onRetry = viewModel::load,
                )

                state.filtered.isEmpty() -> Box(
                    modifier = Modifier.weight(1f).padding(16.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    val filtering = state.query.isNotBlank() || state.statusFilter != null
                    BentoState(
                        message = if (filtering) {
                            "No hay tickets con estos filtros."
                        } else {
                            "Todavía no hay tickets registrados."
                        },
                        actionLabel = when {
                            filtering -> "Limpiar filtros"
                            canCreate -> "Nuevo ticket"
                            else -> "Reintentar"
                        },
                        onAction = when {
                            filtering -> {
                                {
                                    viewModel.onQueryChange("")
                                    viewModel.onStatusFilterChange(null)
                                }
                            }
                            canCreate -> ({ navigator.push(Screen.NewTicket) })
                            else -> viewModel::load
                        },
                    )
                }

                else -> {
                    val shown = state.filtered.take(visibleCount)
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        // Holgura abajo: la barra inferior sobresale y el último ticket no debe quedar pegado.
                        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(shown, key = { it.id }) { ticket ->
                            TicketRow(ticket, onClick = { navigator.push(Screen.TicketDetail(ticket.id)) })
                        }
                        if (shown.size < state.filtered.size) {
                            item {
                                PillButton(
                                    text = "Cargar más (${state.filtered.size - shown.size})",
                                    onClick = { visibleCount += TICKETS_PAGE },
                                    variant = PillButtonVariant.Ghost,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Globo proactivo sobre la barra: "Nuevo ticket" abre el panel en el agente.
        if (showLangostinHint) {
            LangostinTicketsHint(
                user = user,
                onOpenAgent = { LangostinLauncher.openTicketAgent() },
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 8.dp, vertical = 80.dp),
            )
        }
    }
}

/** Filtros de estado en una fila desplazable, con el contador de cada estado. */
@Composable
private fun StatusFilterRow(state: TicketsListUiState, onSelect: (String?) -> Unit) {
    val counts = remember(state.tickets) { state.tickets.groupingBy { it.status }.eachCount() }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ticketStatusFilters.forEach { (value, label) ->
            val count = if (value == null) state.tickets.size else counts[value] ?: 0
            AppFilterChip(
                label = "$label ($count)",
                selected = state.statusFilter == value,
                onClick = { onSelect(value) },
            )
        }
    }
}

/**
 * Fila de ticket: título (2 líneas), meta con folio/categoría/departamento y
 * fecha, descripción solo si no repite el título, y chips de estado/prioridad
 * al pie con `BentoTone` (sin colores sueltos).
 */
@Composable
private fun TicketRow(ticket: TicketDto, onClick: () -> Unit) {
    val meta = listOfNotNull(
        ticketFolio(ticket.id),
        ticket.category?.name?.takeIf { it.isNotBlank() },
        ticket.department?.name?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")
    // La web y la app a veces repiten la categoría en la descripción: no se muestra.
    val description = ticket.description.trim()
    val showDescription = description.isNotBlank() &&
        !ticket.title.trim().contains(description, ignoreCase = true) &&
        !description.contains(ticket.title.trim(), ignoreCase = true)

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        borderColor = null,
        contentPadding = PaddingValues(14.dp),
    ) {
        Text(
            ticket.title,
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            meta,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (showDescription) {
            Spacer(Modifier.height(6.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(ticketStatusLabel(ticket.status), ticketStatusTone(ticket.status))
            Spacer(Modifier.width(6.dp))
            StatusChip(ticketPriorityLabel(ticket.priority), ticketPriorityTone(ticket.priority))
            Spacer(Modifier.weight(1f))
            Text(
                formatShortDate(ticket.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
            )
        }
    }
}
