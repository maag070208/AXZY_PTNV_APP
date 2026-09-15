package com.axzydev.puertonuevoapp.feature.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
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
import com.axzydev.puertonuevoapp.core.network.locations.LocationDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState

@Composable
fun LocationsListScreen(viewModel: LocationsListViewModel = viewModel { LocationsListViewModel(AppContainer.locationsApi) }) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val isAdmin = (authState as? AuthState.LoggedIn)?.user?.canManageCatalogs == true

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${state.filtered.size} ubicación(es)", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                if (isAdmin) {
                    IconButton(onClick = { navigator.push(Screen.LocationForm()) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Nueva ubicación", tint = AppColors.EmeraldPrimary)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            AppSearchField(state.query, viewModel::onQueryChange, "Buscar ubicación")
        }

        when {
            state.loading -> LoadingState(modifier = Modifier.weight(1f))
            state.error != null -> ErrorState(state.error ?: "Error", Modifier.weight(1f), onRetry = viewModel::load)
            state.filtered.isEmpty() -> EmptyState("Aún no hay ubicaciones registradas", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.filtered, key = { it.id }) { loc ->
                    LocationCard(
                        location = loc,
                        isAdmin = isAdmin,
                        onClick = { navigator.push(Screen.LocationDetail(loc.id)) },
                        onEdit = { navigator.push(Screen.LocationForm(loc.id)) },
                        onDelete = { viewModel.requestDelete(loc) },
                    )
                }
            }
        }
    }

    state.deleteTarget?.let { loc ->
        AppModal(
            title = "Eliminar ubicación",
            icon = Icons.Filled.DeleteOutline,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissDelete,
            confirmLabel = "Eliminar",
            saving = state.actionSaving,
            onConfirm = viewModel::confirmDelete,
        ) {
            Text("¿Eliminar ${loc.lugar}? Esta acción no se puede deshacer.", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
        }
    }

    state.actionError?.let { message ->
        AppModal(
            title = "No se pudo completar la acción",
            icon = Icons.Filled.LocationOn,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissActionError,
        ) { Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted) }
    }
}

@Composable
private fun LocationCard(
    location: LocationDto,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(8.dp))
            .border(1.dp, AppColors.Outline, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(AppColors.Info.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = AppColors.Info, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f).clickable(onClick = onClick)) {
            Text(location.lugar, style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
            Text(
                location.descripcion?.ifBlank { null } ?: "${location.count?.devices ?: 0} dispositivo(s)",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
                maxLines = 1,
            )
        }
        if (isAdmin) {
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = AppColors.TextFaint, modifier = Modifier.size(18.dp)) }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar", tint = AppColors.Danger, modifier = Modifier.size(18.dp)) }
        }
    }
}
