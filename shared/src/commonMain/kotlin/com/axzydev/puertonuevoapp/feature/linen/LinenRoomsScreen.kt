package com.axzydev.puertonuevoapp.feature.linen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.BentoActionButton
import com.axzydev.puertonuevoapp.core.ui.BentoStatusRow
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.util.formatDateTime

/**
 * Habitaciones para el check-out (recepción, permiso `hotel_rooms.status`): una ocupada muestra
 * "Check-out" y, al confirmar, pasa a limpieza; el servidor crea el pedido de blancos.
 */
@Composable
fun LinenRoomsScreen(viewModel: LinenViewModel = linenViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val user = currentLinenUser()
    LinenPolling(viewModel)

    Column(modifier = Modifier.fillMaxSize().background(AppBento.canvas).padding(horizontal = 20.dp)) {
        when {
            user?.canChangeRoomStatus != true -> ErrorState("Tu puesto no puede hacer check-out.")
            state.loading && !state.loaded -> LoadingState()
            else -> {
                Row(modifier = Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (state.showAllRooms) "Todas (${state.visibleRooms.size})" else "Ocupadas (${state.visibleRooms.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    AppFilterChip(
                        label = if (state.showAllRooms) "Ver solo ocupadas" else "Ver todas",
                        selected = state.showAllRooms,
                        onClick = viewModel::toggleShowAllRooms,
                    )
                }
                PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val rooms = state.visibleRooms
                    if (rooms.isEmpty()) {
                        EmptyState(if (state.showAllRooms) "No hay habitaciones en tu zona." else "No hay habitaciones ocupadas.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(rooms, key = { it.id }) { room ->
                                val open = state.openByRoom[room.id]
                                BentoStatusRow(
                                    title = room.number,
                                    status = if (open != null) "Pedido en curso · ${linenStatusLabel(open)}" else roomStatusLabel(room.status),
                                    icon = Icons.Filled.Bed,
                                    tone = if (open != null) open.bucket.tone() else BentoTone.Sea,
                                    trailing = if (room.status == "OCCUPIED") ({
                                        BentoActionButton(
                                            label = "Check-out",
                                            enabled = !state.checkingOut,
                                            onClick = { viewModel.askCheckOut(room) },
                                        )
                                    }) else null,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    state.checkOutRoom?.let { room ->
        AppModal(
            title = "Check-out · Hab. ${room.number}",
            onDismiss = viewModel::cancelCheckOut,
            icon = Icons.Filled.Bed,
            confirmLabel = "Confirmar check-out",
            onConfirm = viewModel::confirmCheckOut,
            saving = state.checkingOut,
        ) {
            Text(
                "La habitación pasa a limpieza y se pide el cambio de blancos a lavandería.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
        }
    }
}

private fun roomStatusLabel(status: String): String = when (status) {
    "OCCUPIED" -> "Ocupada"
    "CLEANING" -> "En limpieza"
    "AVAILABLE" -> "Disponible"
    else -> status
}

/** Historial de pedidos por habitación (últimos 60 días, paginado). Solo lectura. */
@Composable
fun LinenHistoryScreen(viewModel: LinenViewModel = linenViewModel()) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadHistory() }

    Column(modifier = Modifier.fillMaxSize().background(AppBento.canvas).padding(horizontal = 20.dp)) {
        when {
            state.historyError != null && state.history.isEmpty() ->
                ErrorState(state.historyError ?: "Error", onRetry = { viewModel.loadHistory() })
            state.historyLoading && state.history.isEmpty() -> LoadingState()
            state.history.isEmpty() -> EmptyState("Sin pedidos en los últimos 60 días.")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.history, key = { it.id }) { item ->
                    BentoStatusRow(
                        title = item.room?.number ?: item.folio,
                        status = "${linenStatusLabel(item)} · ${formatDateTime(item.createdAt)}",
                        icon = Icons.Filled.Bed,
                        tone = item.bucket.tone(),
                        muted = item.bucket == LinenBucket.Done,
                    )
                }
                if (state.historyHasMore) {
                    item {
                        AppFilterChip(
                            label = if (state.historyLoading) "Cargando…" else "Ver más",
                            selected = false,
                            enabled = !state.historyLoading,
                            onClick = { viewModel.loadHistory(more = true) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
