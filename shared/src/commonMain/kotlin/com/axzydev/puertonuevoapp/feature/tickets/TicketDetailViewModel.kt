package com.axzydev.puertonuevoapp.feature.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.tickets.TicketAttachmentDto
import com.axzydev.puertonuevoapp.core.network.tickets.TicketUpdateInput
import com.axzydev.puertonuevoapp.core.network.tickets.TicketsApi
import com.axzydev.puertonuevoapp.core.platform.PickedFile
import com.axzydev.puertonuevoapp.core.platform.shareFile
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TicketDetailViewModel(
    private val ticketId: String,
    private val ticketsApi: TicketsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketDetailUiState())
    val uiState: StateFlow<TicketDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { ticketsApi.get(ticketId) }
            val ticket = result.getOrNull()
            _uiState.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let(::networkMessage),
                    ticket = ticket,
                    // El detalle ya trae sus adjuntos: se pintan mientras llega la lista fresca.
                    attachments = ticket?.attachments ?: it.attachments,
                )
            }
            if (ticket != null) loadAttachments()
        }
    }

    /**
     * Relee solo la lista de adjuntos (`GET /tickets/:id/attachments`). Va
     * aparte del resto del detalle para que un fallo de red en los archivos no
     * tumbe la pantalla.
     */
    fun loadAttachments() {
        if (_uiState.value.loadingAttachments) return
        _uiState.update { it.copy(loadingAttachments = true, attachmentsError = null) }
        viewModelScope.launch {
            val result = runCatching { ticketsApi.attachments(ticketId) }
            _uiState.update { state ->
                val fresh = result.getOrNull()
                state.copy(
                    loadingAttachments = false,
                    attachments = fresh ?: state.attachments,
                    attachmentsError = result.exceptionOrNull()?.let(::networkMessage),
                    ticket = fresh?.let { list -> state.ticket?.copy(attachments = list) } ?: state.ticket,
                )
            }
        }
    }

    // ── Adjuntos: elegir, subir y descargar/compartir ────────────────────────

    fun onAttachmentPicked(file: PickedFile) =
        _uiState.update { it.copy(attachmentFile = file, attachmentsError = null) }

    fun clearAttachmentFile() = _uiState.update { it.copy(attachmentFile = null) }

    /** Sube el archivo elegido (`POST /tickets/:id/attachments`) y refresca la lista. */
    fun uploadAttachment() {
        val state = _uiState.value
        val file = state.attachmentFile ?: return
        if (state.uploadingAttachment) return
        _uiState.update { it.copy(uploadingAttachment = true, attachmentsError = null) }
        viewModelScope.launch {
            val result = runCatching { ticketsApi.uploadAttachment(ticketId, file) }
            result.fold(
                onSuccess = { uploaded ->
                    _uiState.update { current ->
                        val list = listOf(uploaded) + current.attachments.filterNot { it.id == uploaded.id }
                        current.copy(
                            uploadingAttachment = false,
                            attachmentFile = null,
                            attachments = list,
                            ticket = current.ticket?.copy(attachments = list),
                        )
                    }
                    AppSnackbar.show("Adjunto subido")
                    loadAttachments()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(uploadingAttachment = false, attachmentsError = networkMessage(error)) }
                },
            )
        }
    }

    /** Descarga los bytes del adjunto y abre la hoja de compartir del sistema. */
    fun downloadAndShare(attachment: TicketAttachmentDto) {
        if (_uiState.value.downloadingAttachmentId != null) return
        _uiState.update { it.copy(downloadingAttachmentId = attachment.id, attachmentsError = null) }
        viewModelScope.launch {
            val result = runCatching { ticketsApi.downloadAttachment(ticketId, attachment.id) }
            result.fold(
                onSuccess = { bytes ->
                    runCatching { shareFile(attachment.originalName, attachment.mimeType, bytes) }
                    _uiState.update { it.copy(downloadingAttachmentId = null) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(downloadingAttachmentId = null, attachmentsError = networkMessage(error))
                    }
                },
            )
        }
    }

    fun onStatusChange(newStatus: String) {
        val current = _uiState.value.ticket ?: return
        if (newStatus == current.status) return
        _uiState.update { it.copy(updatingStatus = true) }
        viewModelScope.launch {
            val result = runCatching { ticketsApi.update(ticketId, TicketUpdateInput(status = newStatus)) }
            _uiState.update {
                it.copy(
                    updatingStatus = false,
                    error = result.exceptionOrNull()?.let(::networkMessage) ?: it.error,
                    ticket = result.getOrNull() ?: it.ticket,
                )
            }
        }
    }

    fun onCommentChange(value: String) = _uiState.update { it.copy(newComment = value) }

    fun sendComment() {
        val text = _uiState.value.newComment.trim()
        if (text.isBlank() || _uiState.value.sendingComment) return
        _uiState.update { it.copy(sendingComment = true) }
        viewModelScope.launch {
            val result = runCatching {
                ticketsApi.addComment(ticketId, text)
                ticketsApi.get(ticketId)
            }
            _uiState.update {
                it.copy(
                    sendingComment = false,
                    newComment = if (result.isSuccess) "" else it.newComment,
                    error = result.exceptionOrNull()?.let(::networkMessage) ?: it.error,
                    ticket = result.getOrNull() ?: it.ticket,
                )
            }
        }
    }
}
