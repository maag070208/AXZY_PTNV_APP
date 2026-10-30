package com.axzydev.puertonuevoapp.feature.tickets

import com.axzydev.puertonuevoapp.core.network.tickets.TicketAttachmentDto
import com.axzydev.puertonuevoapp.core.network.tickets.TicketDto
import com.axzydev.puertonuevoapp.core.platform.PickedFile

data class TicketDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val ticket: TicketDto? = null,
    val updatingStatus: Boolean = false,
    val newComment: String = "",
    val sendingComment: Boolean = false,

    // ── Adjuntos ─────────────────────────────────────────────────────────────
    // Se gestionan aparte del resto del detalle: un fallo al subir o bajar un
    // archivo no debe tumbar la pantalla (por eso no usan `error`).
    /** Lista vigente de adjuntos (se refresca tras subir). */
    val attachments: List<TicketAttachmentDto> = emptyList(),
    val loadingAttachments: Boolean = false,
    val attachmentsError: String? = null,
    /** Archivo elegido en el selector, pendiente de subir. */
    val attachmentFile: PickedFile? = null,
    val uploadingAttachment: Boolean = false,
    /** Id del adjunto que se está descargando para compartir. */
    val downloadingAttachmentId: String? = null,
) {
    /** Se puede pulsar "Subir": hay archivo y no hay otra subida en vuelo. */
    val canUploadAttachment: Boolean get() = attachmentFile != null && !uploadingAttachment
}

val ticketStatusOptions: List<Pair<String, String>> = listOf(
    "OPEN" to "Abierto",
    "IN_PROGRESS" to "En seguimiento",
    "CLOSED" to "Cerrado",
)
