package com.axzydev.puertonuevoapp.feature.tickets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.users.UserDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.SessionUser
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.initials

/**
 * Alta de ticket. Para ADMIN muestra además **"A nombre de"**: el ticket puede
 * quedar a nombre de otra persona (p. ej. lo pidió por teléfono). Por defecto
 * es "Yo"; al elegir a alguien se avisa "Lo estás capturando a nombre de X" y
 * el API crea el ticket con `onBehalfOfId` (createdBy = esa persona, que recibe
 * la notificación; el historial deja quién lo capturó).
 */
@Composable
fun NewTicketScreen(
    viewModel: TicketFormViewModel = viewModel(key = "ticket-form-new") {
        val user = (AppContainer.authRepository.state.value as? AuthState.LoggedIn)?.user
        TicketFormViewModel(
            ticketId = null,
            ticketsApi = AppContainer.ticketsApi,
            usersApi = AppContainer.usersApi,
            currentUserId = user?.id,
            currentUserName = user?.name.orEmpty(),
            // El API exige el rol ADMIN para onBehalfOfId.
            canCaptureForOthers = user?.isAdmin() == true,
        )
    },
) {
    val navigator = LocalNavigator.current
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.savedTicketId) {
        state.savedTicketId?.let { id ->
            navigator.pop()
            navigator.push(Screen.TicketDetail(id))
        }
    }

    TicketFormContent(state = state, viewModel = viewModel, submitLabel = "Crear ticket")
}

@Composable
fun EditTicketScreen(ticketId: String) {
    val navigator = LocalNavigator.current
    val viewModel: TicketFormViewModel = viewModel(key = "ticket-form-$ticketId") {
        TicketFormViewModel(ticketId, AppContainer.ticketsApi, AppContainer.usersApi)
    }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.savedTicketId) {
        if (state.savedTicketId != null) navigator.pop()
    }

    if (state.loading) {
        com.axzydev.puertonuevoapp.core.ui.LoadingState(modifier = Modifier.fillMaxSize())
    } else {
        TicketFormContent(state = state, viewModel = viewModel, submitLabel = "Guardar cambios")
    }
}

/** ADMIN según los roles efectivos (el API decide con el rol, no con un permiso). */
private fun SessionUser.isAdmin(): Boolean = role == "ADMIN" || "ADMIN" in roles

@Composable
private fun TicketFormContent(
    state: TicketFormUiState,
    viewModel: TicketFormViewModel,
    submitLabel: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(16.dp),
    ) {
        AppTextField(
            value = state.title,
            onValueChange = viewModel::onTitleChange,
            label = { Text("Título") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        AppTextField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text("Descripción") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        SimpleDropdownField(
            label = "Prioridad",
            value = state.priority,
            options = ticketPriorityOptions,
            onSelect = viewModel::onPriorityChange,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        SimpleDropdownField(
            label = "Categoría",
            value = state.categoryId,
            options = state.categoryOptions,
            onSelect = viewModel::onCategoryChange,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.canCaptureForOthers) {
            Spacer(Modifier.height(12.dp))
            RequesterField(state = state, onOpen = viewModel::openRequesterPicker)
        }

        if (state.error != null) {
            Spacer(Modifier.height(12.dp))
            Text(state.error, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = viewModel::submit,
            enabled = state.isValid && !state.saving,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            if (state.saving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AppColors.Surface, strokeWidth = 2.dp)
            } else {
                Text(submitLabel, style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (state.canCaptureForOthers && state.showRequesterPicker) {
        RequesterPickerModal(
            state = state,
            onQueryChange = viewModel::onEmployeeQueryChange,
            onSelect = viewModel::selectRequester,
            onClear = viewModel::clearRequester,
            onRetry = viewModel::reloadEmployees,
            onDismiss = viewModel::dismissRequesterPicker,
        )
    }
}

// ── "A nombre de" (solo ADMIN) ───────────────────────────────────────────────

@Composable
private fun RequesterField(
    state: TicketFormUiState,
    onOpen: () -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(AppColors.EmeraldPrimary.copy(alpha = 0.12f), AppShape.pill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Group,
                    contentDescription = null,
                    tint = AppColors.EmeraldPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "A nombre de",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                )
                Text(
                    state.requesterLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            AppFilterChip(
                label = if (state.isOnBehalf) "Cambiar" else "Elegir",
                selected = false,
                onClick = onOpen,
            )
        }

        if (state.isOnBehalf) {
            val warning = AppBento.tone(BentoTone.Sun)
            Spacer(Modifier.height(10.dp))
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = warning.container,
                borderColor = null,
                contentPadding = PaddingValues(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = warning.content,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Lo estás capturando a nombre de ${state.requesterName.ifBlank { "otra persona" }}. " +
                            "El ticket quedará a su nombre y le llegará la notificación.",
                        style = MaterialTheme.typography.labelSmall,
                        color = warning.content,
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            "El historial del ticket deja constancia de que lo registraste tú.",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextFaint,
        )
    }
}

@Composable
private fun RequesterPickerModal(
    state: TicketFormUiState,
    onQueryChange: (String) -> Unit,
    onSelect: (UserDto) -> Unit,
    onClear: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppModal(
        title = "A nombre de quién",
        icon = Icons.Filled.Group,
        onDismiss = onDismiss,
    ) {
        Text(
            "El ticket quedará creado por esa persona, aparecerá en su \"Mis tickets\" y le llegará la notificación.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        Spacer(Modifier.height(10.dp))
        AppFilterChip(
            label = "A mi nombre",
            selected = !state.isOnBehalf,
            onClick = onClear,
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = state.employeeQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("Buscar por nombre o número") },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))

        when {
            state.loadingEmployees -> BentoState(
                message = "Cargando personal…",
                loading = true,
                modifier = Modifier.fillMaxWidth(),
            )

            state.employeesError != null -> BentoState(
                message = state.employeesError,
                tone = BentoTone.Rose,
                icon = Icons.Filled.ErrorOutline,
                actionLabel = "Reintentar",
                onAction = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )

            state.filteredEmployees.isEmpty() -> Text(
                "Sin coincidencias entre el personal activo.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
            )

            else -> Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                state.filteredEmployees.forEach { user ->
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelect(user) },
                        contentPadding = PaddingValues(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(AppColors.EmeraldPrimary.copy(alpha = 0.12f), AppShape.pill),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    initials(user.name),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColors.EmeraldPrimary,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    user.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    listOfNotNull(
                                        user.employeeNumber?.takeIf { it.isNotBlank() }?.let { "#$it" },
                                        "@${user.username}",
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColors.TextFaint,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (user.id == state.requesterId) {
                                StatusChip(label = "Elegido", tone = BentoTone.Mint)
                            }
                        }
                    }
                }
            }
        }
    }
}
