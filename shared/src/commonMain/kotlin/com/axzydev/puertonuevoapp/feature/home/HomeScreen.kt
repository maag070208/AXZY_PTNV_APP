package com.axzydev.puertonuevoapp.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.StatCard

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel { HomeViewModel(AppContainer.authRepository, AppContainer.devicesApi) }) {
    val state by viewModel.uiState.collectAsState()
    HomeContent(
        state = state,
        userName = viewModel.userName,
        onRetry = viewModel::load,
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    userName: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            text = if (userName.isBlank()) "Bienvenido" else "Hola, $userName",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Así va el inventario en Puerto Nuevo hoy",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp, bottom = 20.dp),
        )

        when {
            state.loading -> LoadingState(modifier = Modifier.fillMaxWidth().height(220.dp))
            state.error != null -> ErrorState(
                message = state.error ?: "Error",
                modifier = Modifier.fillMaxWidth().height(220.dp),
                onRetry = onRetry,
            )
            else -> state.summary?.let { summary ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        value = summary.total.toString(),
                        label = "Total de dispositivos",
                        color = AppColors.EmeraldPrimary,
                        icon = { Icon(Icons.Filled.Devices, contentDescription = null, tint = AppColors.EmeraldPrimary, modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        value = summary.disponible.toString(),
                        label = "Disponibles",
                        color = AppColors.Success,
                        icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AppColors.Success, modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        value = summary.asignado.toString(),
                        label = "Asignados",
                        color = AppColors.Warning,
                        icon = { Icon(Icons.Filled.Person, contentDescription = null, tint = AppColors.Warning, modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        value = summary.baja.toString(),
                        label = "Baja",
                        color = AppColors.TextFaint,
                        icon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = AppColors.TextFaint, modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}