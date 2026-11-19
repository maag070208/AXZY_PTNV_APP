package com.axzydev.puertonuevoapp.feature.housekeeping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.network.housekeeping.HousekeepingTaskDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoActionButton
import com.axzydev.puertonuevoapp.core.ui.BentoStatusRow
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import kotlinx.coroutines.delay

private const val POLL_MILLIS = 20_000L

/** "Limpieza": mis habitaciones (camarista) y por inspeccionar (supervisora). */
@Composable
fun HousekeepingScreen() {
    val user = (AppContainer.authRepository.state.collectAsState().value as? AuthState.LoggedIn)?.user
    if (user == null || !user.canUseHousekeeping) {
        ErrorState("Tu puesto no tiene pantalla de limpieza.")
        return
    }
    val viewModel: HousekeepingViewModel = viewModel {
        HousekeepingViewModel(
            AppContainer.housekeepingApi,
            userId = user.id,
            canOperate = user.canOperateHousekeeping,
            canInspect = user.canInspectHousekeeping,
        )
    }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            delay(POLL_MILLIS)
            viewModel.refresh(silent = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBento.canvas).padding(horizontal = 20.dp)) {
        when {
            state.loading && !state.loaded -> LoadingState()
            state.error != null && !state.loaded -> ErrorState(state.error ?: "Error", onRetry = viewModel::load)
            else -> {
                PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val empty = state.mine.isEmpty() && state.toInspect.isEmpty()
                    if (empty) {
                        EmptyState(if (user.canInspectHousekeeping && !user.canOperateHousekeeping) "Nada por inspeccionar." else "No tienes habitaciones asignadas.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (state.mine.isNotEmpty()) {
                                item { SectionTitle("Mis habitaciones · ${state.mine.size}") }
                                items(state.mine, key = { "m-${it.id}" }) { task -> MineRow(task, state.busyId != null, viewModel) }
                            }
                            if (state.toInspect.isNotEmpty()) {
                                item { SectionTitle("Por inspeccionar · ${state.toInspect.size}") }
                                items(state.toInspect, key = { "i-${it.id}" }) { task -> InspectRow(task, state.busyId != null, viewModel) }
                            }
                        }
                    }
                }
            }
        }
    }

    state.reject?.let { RejectDialog(it, state.reasons.map { r -> r.id to r.name }, viewModel) }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = AppColors.TextPrimary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

private fun cleaningTypeLabel(type: String): String = when (type) {
    "CHECKOUT" -> "Salida"
    "STAY" -> "Estancia"
    "TURNDOWN" -> "Cobertura"
    else -> type
}

@Composable
private fun MineRow(task: HousekeepingTaskDto, busy: Boolean, vm: HousekeepingViewModel) {
    val started = task.status == "IN_PROGRESS"
    val suffix = if (task.isRework) " · Retrabajo" else ""
    BentoStatusRow(
        title = task.room?.number ?: "—",
        status = "${if (started) "En limpieza" else "Por limpiar"} · ${cleaningTypeLabel(task.cleaningType)}$suffix",
        icon = Icons.Filled.CleaningServices,
        tone = if (started) BentoTone.Sun else BentoTone.Rose,
        trailing = {
            BentoActionButton(
                label = if (started) "Terminé" else "Empezar",
                enabled = !busy,
                onClick = { if (started) vm.finish(task) else vm.start(task) },
            )
        },
    )
}

@Composable
private fun InspectRow(task: HousekeepingTaskDto, busy: Boolean, vm: HousekeepingViewModel) {
    BentoStatusRow(
        title = task.room?.number ?: "—",
        status = "Lista para inspección · ${cleaningTypeLabel(task.cleaningType)}",
        icon = Icons.Filled.CleaningServices,
        tone = BentoTone.Sea,
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppFilterChip(label = "Rechazar", selected = false, enabled = !busy, onClick = { vm.openReject(task) })
                BentoActionButton(label = "Aprobar", enabled = !busy, onClick = { vm.approve(task) })
            }
        },
    )
}

@Composable
private fun RejectDialog(dialog: RejectDialogState, reasons: List<Pair<String, String>>, vm: HousekeepingViewModel) {
    AppModal(
        title = "Rechazar · Hab. ${dialog.task.room?.number.orEmpty()}",
        onDismiss = vm::closeReject,
        icon = Icons.Filled.CleaningServices,
        confirmLabel = "Rechazar",
        onConfirm = vm::submitReject,
        confirmEnabled = dialog.canSubmit,
        saving = dialog.submitting,
    ) {
        Column(
            modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Motivo", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
            if (reasons.isEmpty()) Text("Cargando…", color = AppColors.TextMuted)
            reasons.forEach { (id, name) ->
                AppFilterChip(
                    label = name,
                    selected = dialog.reasonId == id,
                    onClick = { vm.setRejectReason(id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AppTextField(
                value = dialog.notes,
                onValueChange = vm::setRejectNotes,
                label = { Text("Notas (opcional)") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
