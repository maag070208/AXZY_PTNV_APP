package com.axzydev.puertonuevoapp.core.network.cartas

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import io.ktor.http.encodeURLParameter

class CartasApi(private val client: ApiClient) {
    suspend fun list(q: String? = null): CartasListResponseDto {
        val qs = q?.takeIf { it.isNotBlank() }?.let { "?q=${it.encodeURLParameter()}" } ?: ""
        return client.get("/cartas$qs")
    }

    suspend fun query(request: TableRequest): TableResponse<CartaDto> = client.post("/cartas/query", request)

    suspend fun get(id: String): CartaDto = client.get("/cartas/$id")

    suspend fun create(input: CartaCreateInput): CartaDto = client.post("/cartas", input)

    suspend fun update(id: String, input: CartaUpdateInput): CartaDto = client.put("/cartas/$id", input)

    suspend fun remove(id: String) = client.deleteNoContent("/cartas/$id")

    suspend fun consecutivo(): ConsecutivoStateDto = client.get("/cartas/consecutivo")

    suspend fun peekConsecutivo(): String = client.get<ConsecutivoPeekDto>("/cartas/consecutivo/peek").siguiente

    suspend fun resetConsecutivo() = client.postNoBody<ConsecutivoStateDto>("/cartas/consecutivo/reset")

    suspend fun generate(typeId: String): CartaGeneratedDto = client.post("/cartas/generate", CartaGenerateInput(typeId))

    suspend fun returnCarta(id: String, returnedBy: String, returnCondition: String): CartaDto =
        client.post("/cartas/$id/return", CartaReturnInput(returnedBy, returnCondition))

    suspend fun undoReturn(id: String): CartaDto = client.delete("/cartas/$id/return")
}
