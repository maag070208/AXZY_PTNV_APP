package com.axzydev.puertonuevoapp.feature.tickets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatDateTime
import com.axzydev.puertonuevoapp.core.util.ticketPriorityLabel
import com.axzydev.puertonuevoapp.core.util.ticketStatusLabel
import com.axzydev.puertonuevoapp.core.ui.AppTextField

@Composable
fun TicketDetailScreen(ticketId: String) {
    val viewModel: TicketDetailViewModel = viewModel(key = "ticket-$ticketId") {
        TicketDetailViewModel(ticketId, AppContainer.ticketsApi)
    }
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user

    when {
        state.loading -> LoadingState(modifier = Modifier.fillMaxSize())
        state.error != null || state.ticket == null -> ErrorState(
            message = state.error ?: "Ticket no encontrado",
            modifier = Modifier.fillMaxSize(),
            onRetry = viewModel::load,
        )
        else -> {
            val t = state.ticket!!
            val access = t.accessInfo()
            val canEdit = user != null && TicketPermissions.canEdit(user, access)
            val canClose = user != null && TicketPermissions.canClose(user, access)
            // Editar mueve entre abierto y en seguimiento; cerrar tiene su propio permiso.
            val statusOptions = ticketStatusOptions.filter { (value, _) ->
                value == t.status || if (value == "CERRADO") canClose else canEdit
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        t.titulo,
                        style = MaterialTheme.typography.headlineSmall,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    StatusChip(ticketPriorityLabel(t.priority), AppColors.ticketPriorityColor(t.priority))
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "${t.category?.nombre ?: "Sin categoría"} · ${t.department?.name ?: "Sin depto."} · #${t.id.take(8).uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextFaint,
                )

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canEdit) {
                        Text(
                            "Editar",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.EmeraldPrimary,
                            modifier = Modifier
                                .clip(AppShape.pill)
                                .background(AppColors.SurfaceVariant, AppShape.pill)
                                .clickable { navigator.push(Screen.EditTicket(t.id)) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    // El tablero solo trae las tareas que el rol puede ver (el empleado, las suyas).
                    Text(
                        "Tareas del ticket",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.EmeraldPrimary,
                        modifier = Modifier
                            .clip(AppShape.pill)
                            .background(AppColors.SurfaceVariant, AppShape.pill)
                            .clickable { navigator.push(Screen.TicketsKanban(t.id)) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }

                Spacer(Modifier.height(16.dp))
                InfoCard {
                    SectionLabel("Descripción")
                    Text(t.descripcion, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                }

                Spacer(Modifier.height(14.dp))
                InfoCard {
                    SectionLabel("Estado")
                    when {
                        state.updatingStatus ->
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AppColors.EmeraldPrimary)
                        statusOptions.size > 1 -> SimpleDropdownField(
                            label = "Cambiar estado",
                            value = t.status,
                            options = statusOptions,
                            onSelect = viewModel::onStatusChange,
                        )
                        else -> StatusChip(ticketStatusLabel(t.status), AppColors.ticketStatusColor(t.status))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Creado por ${t.creadoPor.name} · ${formatDateTime(t.creadoEn)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextFaint,
                    )
                    Text(
                        "Asignado a: ${t.asignadoA?.name ?: "Sin asignar"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextFaint,
                    )
                }

                Spacer(Modifier.height(14.dp))
                InfoCard {
                    SectionLabel("Comentarios (${t.comments.size})")
                    t.comments.forEach { c ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(
                                "${c.autor.name} · ${formatDateTime(c.creadoEn)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextFaint,
                            )
                            Text(c.texto, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppTextField(
                            value = state.newComment,
                            onValueChange = viewModel::onCommentChange,
                            placeholder = { Text("Escribe un comentario…") },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            enabled = state.newComment.isNotBlank() && !state.sendingComment,
                            onClick = viewModel::sendComment,
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = "Enviar", tint = AppColors.EmeraldPrimary)
                        }
                    }
                }

                if (t.history.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    InfoCard {
                        SectionLabel("Historial (${t.history.size})")
                        t.history.forEach { h ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(h.detail ?: h.type, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                                Text(
                                    "${h.autor?.name ?: "Sistema"} · ${formatDateTime(h.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextFaint,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun InfoCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = null,
        contentPadding = PaddingValues(14.dp),
        content = content,
    )
}
