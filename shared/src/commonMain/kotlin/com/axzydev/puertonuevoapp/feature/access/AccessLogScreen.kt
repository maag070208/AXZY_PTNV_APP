package com.axzydev.puertonuevoapp.feature.access

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
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
import com.axzydev.puertonuevoapp.core.network.access.AccessEventDto
import com.axzydev.puertonuevoapp.core.network.access.AccessEventType
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.accessEventTypeLabel
import com.axzydev.puertonuevoapp.core.util.accessLocationSourceLabel
import com.axzydev.puertonuevoapp.core.util.formatDateTime

@Composable
fun AccessLogScreen(
    viewModel: AccessLogViewModel = viewModel {
        val role = (AppContainer.authRepository.state.value as? AuthState.LoggedIn)?.user?.role
        AccessLogViewModel(AppContainer.accessApi, isGuard = role == "GUARD")
    },
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "${state.total} registro(s)",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
        }

        when {
            state.loading -> LoadingState(modifier = Modifier.weight(1f))
            state.error != null -> ErrorState(state.error ?: "Error", Modifier.weight(1f), onRetry = viewModel::load)
            state.events.isEmpty() -> EmptyState("Aún no hay registros de acceso", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.events, key = { it.id }) { event -> AccessEventCard(event) }
            }
        }

        if (state.totalPages > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = viewModel::previousPage, enabled = state.page > 1) { Text("Anterior") }
                Text("Página ${state.page} de ${state.totalPages}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                OutlinedButton(onClick = viewModel::nextPage, enabled = state.page < state.totalPages) { Text("Siguiente") }
            }
        }
    }
}

@Composable
private fun AccessEventCard(event: AccessEventDto) {
    val isEntry = event.type == AccessEventType.ENTRY
    val accent = if (isEntry) AppColors.Success else AppColors.Warning

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = null,
        borderColor = AppColors.Outline,
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(38.dp).background(accent.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isEntry) Icons.Filled.CheckCircle else Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                event.employeeNameSnapshot ?: "Empleado",
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextPrimary,
            )
            Text(
                "${accessEventTypeLabel(event.type)} · ${formatDateTime(event.occurredAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
            Text(
                listOfNotNull(
                    event.site?.name,
                    accessLocationSourceLabel(event.locationSource).takeIf { it != "—" },
                ).joinToString(" · ").ifBlank { "—" },
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
                maxLines = 1,
            )
        }
        if (event.voidedAt != null) {
            StatusChip("Anulado", AppColors.Danger)
        }
        }
    }
}
