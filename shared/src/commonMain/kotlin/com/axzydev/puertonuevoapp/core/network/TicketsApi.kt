package com.axzydev.puertonuevoapp.core.network

import io.ktor.http.encodeURLParameter

class TicketsApi(private val client: ApiClient) {
    suspend fun list(q: String? = null): TicketListResponseDto {
        val qs = q?.takeIf { it.isNotBlank() }?.let { "?q=${it.encodeURLParameter()}" } ?: ""
        return client.get("/tickets$qs")
    }

    suspend fun get(id: String): TicketDto = client.get("/tickets/$id")

    suspend fun create(input: TicketInputDto): TicketDto = client.post("/tickets", input)

    suspend fun updateStatus(id: String, status: String): TicketDto =
        client.put("/tickets/$id", TicketUpdateDto(status = status))

    suspend fun updatePriority(id: String, priority: String): TicketDto =
        client.put("/tickets/$id", TicketUpdateDto(priority = priority))

    suspend fun addComment(id: String, texto: String): TicketCommentDto =
        client.post("/tickets/$id/comments", AddCommentDto(texto))
}
