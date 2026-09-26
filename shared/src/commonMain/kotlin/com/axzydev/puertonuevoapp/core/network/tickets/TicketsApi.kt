package com.axzydev.puertonuevoapp.core.network.tickets

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse

class TicketsApi(private val client: ApiClient) {
    suspend fun list(): TicketListResponseDto = client.get("/tickets")

    suspend fun query(request: TableRequest): TableResponse<TicketDto> =
        client.post("/tickets/query", request)

    suspend fun kanban(): KanbanResponseDto = client.get("/tickets/kanban")

    /** Categorías activas del catálogo (para el formulario de ticket). */
    suspend fun categories(): List<TicketCategoryRefDto> = client.get("/tickets/categories")

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
}
