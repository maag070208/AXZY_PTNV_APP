package com.axzydev.puertonuevoapp.feature.tickets

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.StatusChip

@Composable
fun MyTasksScreen(viewModel: TasksListViewModel = viewModel(key = "my-tasks") { TasksListViewModel(AppContainer.ticketsApi) }) {
    TasksListContent(
        viewModel = viewModel,
        emptyLabel = "No tienes tareas asignadas",
        countLabel = { "$it tarea(s) asignada(s) a ti" },
    )
}

@Composable
fun AdminTasksScreen(viewModel: TasksListViewModel = viewModel(key = "admin-tasks") { TasksListViewModel(AppContainer.ticketsApi) }) {
    val authState by AppContainer.authRepository.state.collectAsState()
    val allowed = (authState as? AuthState.LoggedIn)?.user?.canSeeAdminTasks == true

    if (!allowed) {
        ErrorState(message = "No autorizado — requiere el permiso Completar tareas", modifier = Modifier.fillMaxSize())
        return
    }

    TasksListContent(
        viewModel = viewModel,
        emptyLabel = "No hay tareas registradas",
        countLabel = { "$it tarea(s) de todos los tickets" },
    )
}

@Composable
private fun TasksListContent(
    viewModel: TasksListViewModel,
    emptyLabel: String,
    countLabel: (Int) -> String,
) {
    val navigator = LocalNavigator.current
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(countLabel(state.rows.size), style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.overdueCount > 0) {
                        StatusChip("${state.overdueCount} vencida(s)", AppColors.Danger)
                        Spacer(Modifier.width(8.dp))
                    }
                    OutlinedButton(onClick = viewModel::load) {
                        Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { navigator.push(Screen.TicketsKanban()) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Dashboard, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Ver tablero kanban", style = MaterialTheme.typography.labelLarge)
            }
        }

        when {
            state.loading -> LoadingState(modifier = Modifier.weight(1f))
            state.error != null -> ErrorState(message = state.error ?: "Error", modifier = Modifier.weight(1f), onRetry = viewModel::load)
            state.rows.isEmpty() -> EmptyState(emptyLabel, modifier = Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(state.rows, key = { it.id }) { row ->
                    AssignmentRow(row, onClick = { navigator.push(Screen.TicketDetail(row.ticketId)) })
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}
