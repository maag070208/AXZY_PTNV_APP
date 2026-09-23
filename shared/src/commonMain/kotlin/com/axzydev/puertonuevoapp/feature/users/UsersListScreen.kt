package com.axzydev.puertonuevoapp.feature.users

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.users.UserDto
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.util.roleLabel
import com.axzydev.puertonuevoapp.core.ui.AppTextField

@Composable
fun UsersListScreen(viewModel: UsersListViewModel = viewModel { UsersListViewModel(AppContainer.usersApi) }) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text("Directorio del equipo · ${state.filtered.size} resultados", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
            Spacer(Modifier.height(12.dp))
            AppSearchField(state.query, viewModel::onQueryChange, "Buscar personas")
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                userRoleFilters.forEach { (role, label) ->
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.roleFilter == role) AppColors.Surface else AppColors.TextMuted,
                        modifier = Modifier
                            .background(if (state.roleFilter == role) AppColors.EmeraldPrimary else AppColors.SurfaceVariant, RoundedCornerShape(20.dp))
                            .clickable { viewModel.onRoleFilterChange(role) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }

        when {
            state.loading -> LoadingState(modifier = Modifier.weight(1f))
            state.error != null -> ErrorState(state.error ?: "Error", Modifier.weight(1f), onRetry = viewModel::load)
            state.filtered.isEmpty() -> EmptyState("No hay usuarios con estos filtros", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.filtered, key = { it.id }) { user ->
                    UserCard(
                        user = user,
                        onClick = { navigator.push(Screen.UserHistory(user.id)) },
                    )
                }
            }
        }
    }

    state.actionError?.let { message ->
        AppModal(
            title = "No se pudo completar la acción",
            icon = Icons.Filled.Person,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissActionError,
        ) { Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted) }
    }

    state.selectedForDelete?.let { user ->
        AppModal(
            title = if (user.active) "Desactivar usuario" else "Reactivar usuario",
            icon = Icons.Filled.Person,
            tone = if (user.active) AppModalTone.Danger else AppModalTone.Success,
            onDismiss = viewModel::dismissToggleActive,
            confirmLabel = if (user.active) "Desactivar" else "Reactivar",
            saving = state.actionSaving,
            onConfirm = viewModel::confirmToggleActive,
        ) {
            Text(
                if (user.active) "¿Desactivar a ${user.username}? No podrá iniciar sesión." else "¿Reactivar a ${user.username}? Volverá a poder iniciar sesión.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
        }
    }

    state.selectedForPassword?.let {
        AppModal(
            title = "Cambiar contraseña",
            icon = Icons.Filled.Person,
            onDismiss = viewModel::dismissChangePassword,
            confirmLabel = "Actualizar",
            confirmEnabled = state.newPassword.isNotBlank(),
            saving = state.actionSaving,
            onConfirm = viewModel::confirmChangePassword,
        ) {
            AppTextField(
                value = state.newPassword,
                onValueChange = viewModel::onNewPasswordChange,
                label = { Text("Nueva contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun UserCard(user: UserDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(8.dp))
            .border(1.dp, AppColors.Outline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(AppColors.EmeraldPrimary, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    user.name.trim().split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
                Text("@${user.username}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted, maxLines = 1)
            }
            Box(Modifier.size(10.dp).background(if (user.active) AppColors.Success else AppColors.Danger, CircleShape))
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(roleLabel(user.role).uppercase(), style = MaterialTheme.typography.labelSmall, color = AppColors.TextMuted)
                Text(
                    listOfNotNull(user.puesto, user.department?.name, user.numeroEmpleado).joinToString(" · ").ifBlank { "Sin información adicional" },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                )
                Text(user.email ?: "Sin correo registrado", style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint, maxLines = 1, modifier = Modifier.padding(top = 3.dp))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = "Abrir usuario", tint = AppColors.TextFaint, modifier = Modifier.size(24.dp))
        }
    }
}
