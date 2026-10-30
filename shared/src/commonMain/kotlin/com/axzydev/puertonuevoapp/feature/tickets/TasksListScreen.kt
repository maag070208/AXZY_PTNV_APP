package com.axzydev.puertonuevoapp.feature.tickets

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoIconButton
import com.axzydev.puertonuevoapp.core.ui.BentoMiniStat
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.RegisterTopBarAction
import com.axzydev.puertonuevoapp.core.util.AppEvents

/**
 * Listado de tareas ("Mis tareas" y "Administrar tareas"): franja de KPIs
 * compacta, tarjetas de tarea con su responsable y acceso al tablero kanban
 * desde la barra superior.
 */
@Composable
fun MyTasksScreen(viewModel: TasksListViewModel = viewModel(key = "my-tasks") { TasksListViewModel(AppContainer.ticketsApi) }) {
    TasksListContent(
        viewModel = viewModel,
        description = "Tareas que te asignaron otros equipos.",
        emptyLabel = "No tienes tareas asignadas",
    )
}

@Composable
fun AdminTasksScreen(viewModel: TasksListViewModel = viewModel(key = "admin-tasks") { TasksListViewModel(AppContainer.ticketsApi) }) {
    val authState by AppContainer.authRepository.state.collectAsState()
    val allowed = (authState as? AuthState.LoggedIn)?.user?.canSeeAdminTasks == true

    if (!allowed) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            BentoErrorState(
                message = "No autorizado — requiere el permiso Completar tareas.",
                onRetry = null,
            )
        }
        return
    }

    TasksListContent(
        viewModel = viewModel,
        description = "Tareas de todos los tickets.",
        emptyLabel = "No hay tareas registradas",
    )
}

@Composable
private fun TasksListContent(
    viewModel: TasksListViewModel,
    description: String,
    emptyLabel: String,
) {
    val navigator = LocalNavigator.current
    val state by viewModel.uiState.collectAsState()

    // Acción primaria en la barra superior: abrir el tablero kanban.
    RegisterTopBarAction(Icons.Filled.Dashboard, "Ver tablero kanban") {
        navigator.push(Screen.TicketsKanban())
    }

    // Tarea nueva (desde Langostín u otra pantalla) → el listado se recarga.
    val ticketsVersion by AppEvents.version("tickets:changed").collectAsState()
    LaunchedEffect(ticketsVersion) {
        if (ticketsVersion != null) viewModel.load()
    }

    val completed = state.rows.count { it.status == "COMPLETED" }
    val inProgress = state.rows.count { it.status == "IN_PROGRESS" || it.status == "IN_REVIEW" }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                    modifier = Modifier.weight(1f),
                )
                BentoIconButton(
                    icon = Icons.Filled.Sync,
                    contentDescription = "Actualizar tareas",
                    onClick = viewModel::load,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BentoMiniStat(
                    label = "Tareas",
                    value = state.rows.size.toString(),
                    tone = BentoTone.Sea,
                    labelMaxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                BentoMiniStat(
                    label = "Vencidas",
                    value = state.overdueCount.toString(),
                    tone = if (state.overdueCount > 0) BentoTone.Rose else BentoTone.Plain,
                    labelMaxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                BentoMiniStat(
                    label = "En curso",
                    value = inProgress.toString(),
                    tone = BentoTone.Lavender,
                    labelMaxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                BentoMiniStat(
                    label = "Completas",
                    value = completed.toString(),
                    tone = BentoTone.Mint,
                    labelMaxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        when {
            state.loading -> LoadingState(modifier = Modifier.weight(1f))

            state.error != null -> ErrorState(
                message = state.error ?: "No se pudieron cargar las tareas",
                modifier = Modifier.weight(1f),
                onRetry = viewModel::load,
            )

            state.rows.isEmpty() -> Box(
                modifier = Modifier.weight(1f).padding(16.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                BentoState(
                    message = emptyLabel,
                    actionLabel = "Ver tablero",
                    onAction = { navigator.push(Screen.TicketsKanban()) },
                )
            }

            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.rows, key = { it.id }) { row ->
                    AssignmentRow(
                        assignment = row,
                        onClick = { navigator.push(Screen.TicketDetail(row.ticketId)) },
                    )
                }
            }
        }
    }
}
