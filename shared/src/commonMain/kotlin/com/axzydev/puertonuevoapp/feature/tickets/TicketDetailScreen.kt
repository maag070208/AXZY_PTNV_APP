package com.axzydev.puertonuevoapp.feature.tickets

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.tickets.TicketAttachmentDto
import com.axzydev.puertonuevoapp.core.network.tickets.TicketDto
import com.axzydev.puertonuevoapp.core.platform.FileMimeTypes
import com.axzydev.puertonuevoapp.core.platform.PickedFile
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.AttachmentFileRow
import com.axzydev.puertonuevoapp.core.ui.AttachmentPickerField
import com.axzydev.puertonuevoapp.core.ui.BentoAvatar
import com.axzydev.puertonuevoapp.core.ui.BentoMiniStat
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.ui.formatFileSize
import com.axzydev.puertonuevoapp.core.util.formatDateTime
import com.axzydev.puertonuevoapp.core.util.ticketPriorityLabel
import com.axzydev.puertonuevoapp.core.util.ticketStatusLabel

/**
 * Detalle de un ticket: hero de marca con folio, estado y prioridad; resumen
 * compacto; acciones en píldora; y secciones de descripción, estado,
 * adjuntos, comentarios e historial.
 */
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
            val ticket = state.ticket ?: return
            val access = ticket.accessInfo()
            val canEdit = user != null && TicketPermissions.canEdit(user, access)
            val canClose = user != null && TicketPermissions.canClose(user, access)
            // Misma compuerta que la web para subir adjuntos: quien puede editar
            // el ticket o está involucrado (creador, asignado o con una tarea).
            // El API vuelve a validar el acceso al ticket.
            val canUpload = user != null && (canEdit || TicketPermissions.isInvolved(user, access))
            // Editar mueve entre abierto y en seguimiento; cerrar tiene su propio permiso.
            val statusOptions = ticketStatusOptions.filter { (value, _) ->
                value == ticket.status || if (value == "CLOSED") canClose else canEdit
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(16.dp),
            ) {
                TicketHero(ticket)

                Spacer(Modifier.height(12.dp))
                TicketSummary(state = state, ticket = ticket)

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canEdit) {
                        PillButton(
                            text = "Editar",
                            onClick = { navigator.push(Screen.EditTicket(ticket.id)) },
                            variant = PillButtonVariant.Secondary,
                            leadingIcon = Icons.Filled.Edit,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    PillButton(
                        text = "Tareas del ticket",
                        onClick = { navigator.push(Screen.TicketsKanban(ticket.id)) },
                        leadingIcon = Icons.Filled.Dashboard,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(12.dp))
                TicketSection("Descripción") {
                    Text(
                        ticket.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextPrimary,
                    )
                }

                Spacer(Modifier.height(12.dp))
                TicketSection("Estado y responsables") {
                    when {
                        state.updatingStatus -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = AppColors.EmeraldPrimary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Actualizando…",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextMuted,
                            )
                        }
                        statusOptions.size > 1 -> SimpleDropdownField(
                            label = "Cambiar estado",
                            value = ticket.status,
                            options = statusOptions,
                            onSelect = viewModel::onStatusChange,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        else -> StatusChip(ticketStatusLabel(ticket.status), ticketStatusTone(ticket.status))
                    }

                    Spacer(Modifier.height(10.dp))
                    PersonRow(
                        name = ticket.createdBy.name,
                        caption = "Creado el ${formatDateTime(ticket.createdAt)}",
                    )
                    Spacer(Modifier.height(8.dp))
                    PersonRow(
                        name = ticket.assignedTo?.name ?: "Sin asignar",
                        caption = if (ticket.assignedTo != null) "Responsable del ticket" else "Aún sin responsable",
                        highlighted = ticket.assignedTo != null,
                    )
                }

                Spacer(Modifier.height(12.dp))
                AttachmentsSection(
                    ticket = ticket,
                    state = state,
                    canUpload = canUpload,
                    currentUserId = user?.id,
                    onPick = viewModel::onAttachmentPicked,
                    onClearFile = viewModel::clearAttachmentFile,
                    onUpload = viewModel::uploadAttachment,
                    onShare = viewModel::downloadAndShare,
                )

                Spacer(Modifier.height(12.dp))
                TicketSection("Comentarios (${ticket.comments.size})") {
                    if (ticket.comments.isEmpty()) {
                        Text(
                            "Todavía no hay comentarios.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextFaint,
                        )
                    } else {
                        ticket.comments.forEachIndexed { index, comment ->
                            if (index > 0) Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                BentoAvatar(
                                    name = comment.author.name,
                                    color = AppColors.EmeraldPrimary,
                                    size = 28.dp,
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${comment.author.name} · ${formatDateTime(comment.createdAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppColors.TextFaint,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        comment.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AppColors.TextPrimary,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    AppTextField(
                        value = state.newComment,
                        onValueChange = viewModel::onCommentChange,
                        placeholder = { Text("Escribe un comentario…") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    PillButton(
                        text = "Enviar comentario",
                        onClick = viewModel::sendComment,
                        enabled = state.newComment.isNotBlank(),
                        loading = state.sendingComment,
                        leadingIcon = Icons.AutoMirrored.Filled.Send,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (ticket.history.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    TicketSection("Historial (${ticket.history.size})") {
                        ticket.history.forEachIndexed { index, entry ->
                            if (index > 0) {
                                Spacer(Modifier.height(8.dp))
                                SectionDivider()
                                Spacer(Modifier.height(8.dp))
                            }
                            Text(
                                entry.detail ?: entry.type,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${entry.author?.name ?: "Sistema"} · ${formatDateTime(entry.createdAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextFaint,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Hero del ticket: folio, título, estado/prioridad y contexto. */
@Composable
private fun TicketHero(ticket: TicketDto) {
    val hero = AppBento.tone(BentoTone.Sea)
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = hero.container,
        borderColor = null,
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                ticketFolio(ticket.id),
                style = MaterialTheme.typography.labelSmall,
                color = hero.content.copy(alpha = 0.75f),
                modifier = Modifier.weight(1f),
            )
            StatusChip(ticketStatusLabel(ticket.status), ticketStatusTone(ticket.status))
            Spacer(Modifier.width(6.dp))
            StatusChip(ticketPriorityLabel(ticket.priority), ticketPriorityTone(ticket.priority))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            ticket.title,
            style = MaterialTheme.typography.titleLarge,
            color = hero.content,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            listOfNotNull(
                ticket.category?.name?.takeIf { it.isNotBlank() },
                ticket.department?.name?.takeIf { it.isNotBlank() },
                "Creado por ${ticket.createdBy.name}",
                formatDateTime(ticket.createdAt),
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = hero.content.copy(alpha = 0.8f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Resumen compacto: comentarios, adjuntos, tareas e historial. */
@Composable
private fun TicketSummary(state: TicketDetailUiState, ticket: TicketDto) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BentoMiniStat(
            label = "Comentarios",
            value = ticket.comments.size.toString(),
            tone = BentoTone.Sea,
            labelMaxLines = 1,
            modifier = Modifier.weight(1f),
        )
        BentoMiniStat(
            label = "Adjuntos",
            value = state.attachments.size.toString(),
            tone = BentoTone.Lavender,
            labelMaxLines = 1,
            modifier = Modifier.weight(1f),
        )
        BentoMiniStat(
            label = "Tareas",
            value = ticket.assignments.size.toString(),
            tone = BentoTone.Sun,
            labelMaxLines = 1,
            modifier = Modifier.weight(1f),
        )
        BentoMiniStat(
            label = "Historial",
            value = ticket.history.size.toString(),
            tone = BentoTone.Plain,
            labelMaxLines = 1,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Persona con avatar y una línea de contexto (creador/responsable). */
@Composable
private fun PersonRow(name: String, caption: String, highlighted: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BentoAvatar(
            name = name,
            color = if (highlighted) AppColors.EmeraldPrimary else AppColors.TextFaint,
            size = 28.dp,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SectionDivider() {
    androidx.compose.material3.HorizontalDivider(color = AppColors.Outline.copy(alpha = 0.35f))
}

/** MIME que acepta el API para adjuntos de ticket (imágenes, PDF y video). */
private val attachmentMimeTypes = listOf(FileMimeTypes.IMAGE, FileMimeTypes.PDF, "video/*")

/**
 * Sección **Adjuntos** del detalle: lista con nombre, tamaño, fecha y autor;
 * descarga con la hoja de compartir del sistema y, para quien puede (editar o
 * estar involucrado, como la web), el selector + "Subir adjunto".
 *
 * El API no expone borrado de adjuntos, así que no hay acción de eliminar.
 */
@Composable
private fun AttachmentsSection(
    ticket: TicketDto,
    state: TicketDetailUiState,
    canUpload: Boolean,
    currentUserId: String?,
    onPick: (PickedFile) -> Unit,
    onClearFile: () -> Unit,
    onUpload: () -> Unit,
    onShare: (TicketAttachmentDto) -> Unit,
) {
    // El listado del API solo trae `uploadedById`; los nombres de quienes están
    // en el ticket (creador, asignado, comentaristas y responsables de tareas)
    // alcanzan para mostrar el autor sin otra petición.
    val uploaderNames = remember(ticket.id, ticket.comments.size, currentUserId) {
        buildMap {
            put(ticket.createdBy.id, ticket.createdBy.name)
            ticket.assignedTo?.let { put(it.id, it.name) }
            ticket.comments.forEach { put(it.author.id, it.author.name) }
            ticket.assignments.forEach { put(it.user.id, it.user.name) }
        }
    }

    TicketSection("Adjuntos (${state.attachments.size})") {
        if (state.loadingAttachments) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = AppColors.EmeraldPrimary,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Cargando adjuntos…",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextFaint,
                )
            }
        } else if (state.attachments.isEmpty()) {
            Text(
                "Sin adjuntos todavía.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
            )
        } else {
            state.attachments.forEachIndexed { index, attachment ->
                if (index > 0) Spacer(Modifier.height(8.dp))
                AttachmentFileRow(
                    name = attachment.originalName,
                    mime = attachment.mimeType,
                    subtitle = listOf(
                        formatFileSize(attachment.sizeBytes),
                        formatDateTime(attachment.createdAt),
                        attachmentAuthor(attachment, uploaderNames, currentUserId),
                    ).joinToString(" · "),
                    onShare = { onShare(attachment) },
                )
            }
        }

        state.attachmentsError?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
        }

        if (state.downloadingAttachmentId != null) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = AppColors.EmeraldPrimary,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Preparando el archivo para compartir…",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                )
            }
        }

        if (canUpload) {
            Spacer(Modifier.height(14.dp))
            AttachmentPickerField(
                label = "Adjuntar archivo",
                file = state.attachmentFile,
                onFilePicked = onPick,
                onClear = onClearFile,
                hint = "Imagen, PDF o video · máx. 50 MB",
                mimeTypes = attachmentMimeTypes,
                enabled = !state.uploadingAttachment,
            )
            Spacer(Modifier.height(10.dp))
            PillButton(
                text = "Subir adjunto",
                onClick = onUpload,
                enabled = state.canUploadAttachment,
                loading = state.uploadingAttachment,
                leadingIcon = Icons.Filled.UploadFile,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Autor visible del adjunto: "Tú", un nombre conocido del ticket o "Usuario". */
private fun attachmentAuthor(
    attachment: TicketAttachmentDto,
    knownNames: Map<String, String>,
    currentUserId: String?,
): String = when {
    attachment.uploadedById == currentUserId -> "Tú"
    else -> knownNames[attachment.uploadedById] ?: "Usuario"
}
