package com.axzydev.puertonuevoapp.feature.users

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.roleLabel

@Composable
fun UserHistoryScreen(userId: String) {
    val viewModel: UserHistoryViewModel = viewModel(key = "user-history-$userId") {
        UserHistoryViewModel(userId, AppContainer.usersApi)
    }
    val state by viewModel.uiState.collectAsState()

    when {
        state.loading -> LoadingState(Modifier.fillMaxSize())
        state.error != null || state.user == null -> ErrorState(state.error ?: "Usuario no encontrado", Modifier.fillMaxSize(), onRetry = viewModel::load)
        else -> {
            val current = state.user!!
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp)) {
                AppSurfaceCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp).background(AppColors.EmeraldContainer, MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
                            Text(current.name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = AppColors.EmeraldOnContainer)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(current.name, style = MaterialTheme.typography.titleLarge, color = AppColors.TextPrimary)
                            Text("@${current.username}", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
                        }
                        StatusChip(if (current.active) "Activo" else "Inactivo", if (current.active) AppColors.Success else AppColors.Danger)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("${roleLabel(current.role)} · ${current.numeroEmpleado ?: "Sin número de empleado"}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                    current.department?.let { Text(it.name, style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint, modifier = Modifier.padding(top = 4.dp)) }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(18.dp))
                    Text("  ACTIVIDAD (${state.history.size})", style = MaterialTheme.typography.titleSmall, color = AppColors.TextMuted)
                }
                Spacer(Modifier.height(10.dp))
                if (state.history.isEmpty()) {
                    EmptyState("Sin actividad registrada", Modifier.fillMaxWidth().height(160.dp))
                } else {
                    state.history.forEachIndexed { index, entry ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(18.dp)) {
                                Box(Modifier.size(12.dp).background(AppColors.EmeraldPrimary, MaterialTheme.shapes.extraLarge))
                                if (index < state.history.lastIndex) Box(Modifier.width(1.dp).height(58.dp).background(AppColors.Outline))
                            }
                            AppSurfaceCard(Modifier.padding(start = 10.dp).weight(1f)) {
                                Text(entry.title, style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
                                Text(entry.timestamp, style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
                                Text(entry.detail, style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted, modifier = Modifier.padding(top = 5.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
