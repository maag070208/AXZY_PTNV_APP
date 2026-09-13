package com.axzydev.puertonuevoapp.feature.tickets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.TicketDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import com.axzydev.puertonuevoapp.core.util.ticketPriorityLabel
import com.axzydev.puertonuevoapp.core.util.ticketStatusLabel
import kotlinx.coroutines.launch

private val statusFilters = listOf(
    null to "Todos",
    "ABIERTO" to "Abiertos",
    "EN_SEGUIMIENTO" to "Seguimiento",
    "CERRADO" to "Cerrados",
)

@Composable
fun TicketsListScreen() {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val authState by AppContainer.authRepository.state.collectAsState()
    val role = (authState as? AuthState.LoggedIn)?.user?.role
    val canSeeAdminTasks = role == "ADMIN" || role == "GERENTE"

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var tickets by remember { mutableStateOf<List<TicketDto>>(emptyList()) }
    var statusFilter by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }

    suspend fun load() {
        loading = true
        error = null
        try {
            tickets = AppContainer.ticketsApi.list().data
        } catch (e: Exception) {
            error = e.message ?: "No se pudieron cargar los tickets"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    val filtered by remember(tickets, statusFilter, query) {
        derivedStateOf {
            tickets
                .filter { statusFilter == null || it.status == statusFilter }
                .filter {
                    query.isBlank() ||
                        it.titulo.contains(query, ignoreCase = true) ||
                        it.descripcion.contains(query, ignoreCase = true)
                }
                .sortedByDescending { it.creadoEn }
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Seguimiento operativo", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted, modifier = Modifier.padding(top = 3.dp))
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    statusFilters.forEach { (value, label) ->
                        val selected = statusFilter == value
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selected) AppColors.Surface else AppColors.TextMuted,
                            modifier = Modifier
                                .background(
                                    if (selected) AppColors.EmeraldPrimary else AppColors.SurfaceVariant,
                                    RoundedCornerShape(20.dp),
                                )
                                .clickable { statusFilter = value }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tablero",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.EmeraldPrimary,
                        modifier = Modifier
                            .background(AppColors.SurfaceVariant, RoundedCornerShape(20.dp))
                            .clickable { navigator.push(Screen.TicketsKanban()) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                    Text(
                        text = "Mis tareas",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.EmeraldPrimary,
                        modifier = Modifier
                            .background(AppColors.SurfaceVariant, RoundedCornerShape(20.dp))
                            .clickable { navigator.push(Screen.MyTasks) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                    if (canSeeAdminTasks) {
                        Text(
                            text = "Administración",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.EmeraldPrimary,
                            modifier = Modifier
                                .background(AppColors.SurfaceVariant, RoundedCornerShape(20.dp))
                                .clickable { navigator.push(Screen.AdminTasks) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            when {
                loading -> LoadingState(modifier = Modifier.weight(1f))
                error != null -> ErrorState(
                    message = error ?: "Error",
                    modifier = Modifier.weight(1f),
                    onRetry = { scope.launch { load() } },
                )
                filtered.isEmpty() -> EmptyState("No hay tickets con estos filtros", modifier = Modifier.weight(1f))
                else -> LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    items(filtered, key = { it.id }) { ticket ->
                        TicketRow(ticket) { navigator.push(Screen.TicketDetail(ticket.id)) }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
    }
}

@Composable
private fun TicketRow(ticket: TicketDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                ticket.titulo,
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            StatusChip(ticketStatusLabel(ticket.status), AppColors.ticketStatusColor(ticket.status))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            ticket.descripcion,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
            maxLines = 2,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusChip(ticketPriorityLabel(ticket.priority), AppColors.ticketPriorityColor(ticket.priority))
                ticket.department?.let {
                    Text(it.name, style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
                }
            }
            Text(formatShortDate(ticket.creadoEn), style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
        }
    }
}
