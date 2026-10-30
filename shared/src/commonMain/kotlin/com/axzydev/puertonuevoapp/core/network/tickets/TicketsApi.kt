package com.axzydev.puertonuevoapp.core.network.tickets

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import com.axzydev.puertonuevoapp.core.platform.PickedFile
import com.axzydev.puertonuevoapp.core.platform.toMultipartPart
import io.ktor.http.encodeURLPathPart

class TicketsApi(private val client: ApiClient) {
    suspend fun list(): TicketListResponseDto = client.get("/tickets")

    suspend fun query(request: TableRequest): TableResponse<TicketDto> =
        client.post("/tickets/query", request)

    suspend fun kanban(): KanbanResponseDto = client.get("/tickets/kanban")

    /**
     * Catálogo de categorías. Sin [includeInactive] solo devuelve las activas
     * (formulario de ticket); Catálogos pide todas para poder reactivarlas.
     */
    suspend fun categories(includeInactive: Boolean = false): List<TicketCategoryRefDto> =
        client.get("/tickets/categories${if (includeInactive) "?includeInactive=true" else ""}")

    /** Alta de categoría (`POST /tickets/categories`, permiso `catalogs.manage`). */
    suspend fun createCategory(name: String): TicketCategoryRefDto =
        client.post("/tickets/categories", TicketCategoryCreateInput(name))

    /** Renombrar y/o activar/desactivar una categoría (`PATCH`). */
    suspend fun updateCategory(id: String, input: TicketCategoryUpdateInput): TicketCategoryRefDto =
        client.patch("/tickets/categories/${id.encodeURLPathPart()}", input)

    /** Baja de categoría: desactiva la primera vez y borra si ya estaba inactiva. */
    suspend fun removeCategory(id: String): TicketCategoryDeleteResultDto =
        client.delete("/tickets/categories/${id.encodeURLPathPart()}")

    suspend fun get(id: String): TicketDto = client.get("/tickets/$id")

    suspend fun create(input: TicketCreateInput): TicketDto = client.post("/tickets", input)

    suspend fun update(id: String, input: TicketUpdateInput): TicketDto = client.put("/tickets/$id", input)

    suspend fun delete(id: String) = client.deleteNoContent("/tickets/$id")

    suspend fun addComment(id: String, text: String) =
        client.postNoContent("/tickets/$id/comments", TicketCommentInput(text))

    suspend fun createAssignment(id: String, input: TicketAssignmentCreateInput) =
        client.postNoContent("/tickets/$id/assignments", input)

    suspend fun updateAssignment(id: String, assignmentId: String, input: TicketAssignmentUpdateInput) =
        client.putNoContent("/tickets/$id/assignments/$assignmentId", input)

    suspend fun removeAssignment(id: String, assignmentId: String) =
        client.deleteNoContent("/tickets/$id/assignments/$assignmentId")

    suspend fun addAssignmentComment(id: String, assignmentId: String, text: String) =
        client.postNoContent(
            "/tickets/$id/assignments/$assignmentId/comments",
            TicketAssignmentCommentInput(text),
        )

    suspend fun attachments(id: String): List<TicketAttachmentDto> = client.get("/tickets/$id/attachments")

    /**
     * Sube un adjunto del ticket (`POST /tickets/:id/attachments`, multipart
     * campo `file`). El API solo acepta imágenes, PDF y video, y lo guarda con
     * `kind = PHOTO` por defecto (lo mismo que manda la web). Devuelve el
     * adjunto creado con su `url` pública y su tamaño real.
     */
    suspend fun uploadAttachment(id: String, file: PickedFile): TicketAttachmentDto =
        client.postMultipart(
            path = "/tickets/$id/attachments",
            parts = listOf(file.toMultipartPart()),
            fields = mapOf("kind" to "PHOTO"),
        )

    /** Bytes del adjunto para compartirlo con `shareFile` (la web lo descarga igual). */
    suspend fun downloadAttachment(id: String, attachmentId: String): ByteArray =
        client.getBytes("/tickets/$id/attachments/$attachmentId/download")
}
