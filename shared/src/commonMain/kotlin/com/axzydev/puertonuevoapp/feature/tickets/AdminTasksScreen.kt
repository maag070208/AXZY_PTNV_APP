package com.axzydev.puertonuevoapp.feature.tickets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.KanbanAssignmentDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import kotlinx.coroutines.launch

@Composable
fun AdminTasksScreen() {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val authState by AppContainer.authRepository.state.collectAsState()
    val role = (authState as? AuthState.LoggedIn)?.user?.role
    val allowed = role == "ADMIN" || role == "GERENTE"

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var rows by remember { mutableStateOf<List<KanbanAssignmentDto>>(emptyList()) }

    suspend fun load() {
        loading = true
        error = null
        try {
            rows = AppContainer.ticketsApi.kanban().data.filter { it.ticket.deletedAt == null }
        } catch (e: Exception) {
            error = e.message ?: "No se pudieron cargar las tareas"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(allowed) { if (allowed) load() else loading = false }

    if (!allowed) {
        ErrorState(message = "No autorizado — sólo ADMIN o GERENTE", modifier = Modifier.fillMaxSize())
        return
    }

    val overdueCount = rows.count { it.overdue() }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${rows.size} tarea(s) de todos los tickets", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (overdueCount > 0) {
                        StatusChip("$overdueCount vencida(s)", AppColors.Danger)
                        Spacer(Modifier.width(8.dp))
                    }
                    OutlinedButton(onClick = { scope.launch { load() } }) {
                        Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { navigator.push(Screen.TicketsKanban()) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Dashboard, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Ver kanban completo", style = MaterialTheme.typography.labelLarge)
            }
        }

        when {
            loading -> LoadingState(modifier = Modifier.weight(1f))
            error != null -> ErrorState(message = error ?: "Error", modifier = Modifier.weight(1f), onRetry = { scope.launch { load() } })
            rows.isEmpty() -> EmptyState("No hay tareas registradas", modifier = Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(rows, key = { it.id }) { row ->
                    AssignmentRow(row, onClick = { navigator.push(Screen.TicketDetail(row.ticketId)) })
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}
