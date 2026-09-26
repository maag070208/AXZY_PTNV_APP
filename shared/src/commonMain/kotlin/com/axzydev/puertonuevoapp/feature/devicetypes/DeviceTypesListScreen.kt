package com.axzydev.puertonuevoapp.feature.devicetypes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypeDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState

@Composable
fun DeviceTypesListScreen(viewModel: DeviceTypesListViewModel = viewModel { DeviceTypesListViewModel(AppContainer.deviceTypesApi) }) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val canManage = (authState as? AuthState.LoggedIn)?.user?.canManageCatalogs == true

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Cada tipo tiene su propio consecutivo (prefijo)", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                if (canManage) {
                    IconButton(onClick = { navigator.push(Screen.DeviceTypeForm()) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Nuevo tipo", tint = AppColors.EmeraldPrimary)
                    }
                }
            }
        }

        when {
            state.loading -> LoadingState(modifier = Modifier.weight(1f))
            state.error != null -> ErrorState(state.error ?: "Error", Modifier.weight(1f), onRetry = viewModel::load)
            state.types.isEmpty() -> EmptyState("Aún no hay tipos de dispositivo", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.types, key = { it.id }) { t ->
                    DeviceTypeCard(
                        type = t,
                        clickable = canManage,
                        onClick = { if (canManage) navigator.push(Screen.DeviceTypeForm(t.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceTypeCard(type: DeviceTypeDto, clickable: Boolean, onClick: () -> Unit) {
    val nextFolio = "${type.prefix}-${(type.counter + 1).toString().padStart(4, '0')}"
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (clickable) onClick else null,
        borderColor = null,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(46.dp).background(
                    if (type.active) AppColors.EmeraldPrimary else AppColors.TextFaint,
                    RoundedCornerShape(14.dp),
                ),
                contentAlignment = Alignment.Center,
            ) { Text(type.prefix, style = MaterialTheme.typography.labelSmall, color = AppColors.Surface) }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(type.name, style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
                    if (!type.active) {
                        Spacer(Modifier.size(6.dp))
                        Text(
                            "INACTIVO",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.Danger,
                            modifier = Modifier
                                .background(AppColors.Danger.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Text("Código ${type.code}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
                Spacer(Modifier.size(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Sell, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Siguiente: $nextFolio", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                    Spacer(Modifier.size(10.dp))
                    Icon(Icons.Filled.Memory, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("${type.count?.devices ?: 0}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                }
            }
            if (clickable) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Editar tipo", tint = AppColors.TextFaint, modifier = Modifier.size(22.dp))
            }
        }
    }
}
