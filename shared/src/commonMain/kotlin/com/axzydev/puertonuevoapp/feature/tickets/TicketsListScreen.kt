package com.axzydev.puertonuevoapp.feature.tickets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.tickets.TicketDto
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import com.axzydev.puertonuevoapp.core.util.ticketPriorityLabel
import com.axzydev.puertonuevoapp.core.util.ticketStatusLabel

@Composable
fun TicketsListScreen(viewModel: TicketsListViewModel = viewModel { TicketsListViewModel(AppContainer.ticketsApi) }) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            // Cualquier rol levanta tickets (igual que la web y la API).
            FloatingActionButton(
                onClick = { navigator.push(Screen.NewTicket) },
                containerColor = AppColors.EmeraldPrimary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo ticket")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp)) {
                AppSearchField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = "Buscar ticket…",
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ticketStatusFilters.forEach { (value, label) ->
                        val selected = state.statusFilter == value
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selected) AppColors.Surface else AppColors.TextMuted,
                            modifier = Modifier
                                .clip(AppShape.pill)
                                .background(if (selected) AppColors.EmeraldPrimary else AppColors.SurfaceVariant, AppShape.pill)
                                .clickable { viewModel.onStatusFilterChange(value) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            when {
                state.loading -> LoadingState(modifier = Modifier.weight(1f))
                state.error != null -> ErrorState(message = state.error ?: "Error", modifier = Modifier.weight(1f), onRetry = viewModel::load)
                state.filtered.isEmpty() -> EmptyState("No hay tickets", modifier = Modifier.weight(1f))
                else -> LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    items(state.filtered, key = { it.id }) { ticket ->
                        TicketRow(ticket, onClick = { navigator.push(Screen.TicketDetail(ticket.id)) })
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketRow(ticket: TicketDto, onClick: () -> Unit) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        borderColor = null,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Text(
                ticket.titulo,
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            StatusChip(ticketPriorityLabel(ticket.priority), AppColors.ticketPriorityColor(ticket.priority))
        }
        Spacer(Modifier.height(6.dp))
        Text(ticket.descripcion, style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted, maxLines = 2)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            StatusChip(ticketStatusLabel(ticket.status), AppColors.ticketStatusColor(ticket.status))
            Text(formatShortDate(ticket.creadoEn), style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
        }
    }
}
