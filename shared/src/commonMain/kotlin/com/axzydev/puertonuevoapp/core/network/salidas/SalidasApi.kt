package com.axzydev.puertonuevoapp.core.network.salidas

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import io.ktor.http.encodeURLParameter

class SalidasApi(private val client: ApiClient) {
    suspend fun list(
        start: String? = null,
        end: String? = null,
        departamento: String? = null,
        usuario: String? = null,
        q: String? = null,
    ): SalidasListResponseDto {
        val params = buildList {
            start?.takeIf { it.isNotBlank() }?.let { add("start=${it.encodeURLParameter()}") }
            end?.takeIf { it.isNotBlank() }?.let { add("end=${it.encodeURLParameter()}") }
            departamento?.takeIf { it.isNotBlank() }?.let { add("departamento=${it.encodeURLParameter()}") }
            usuario?.takeIf { it.isNotBlank() }?.let { add("usuario=${it.encodeURLParameter()}") }
            q?.takeIf { it.isNotBlank() }?.let { add("q=${it.encodeURLParameter()}") }
        }
        val qs = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
        return client.get("/salidas$qs")
    }

    suspend fun query(request: TableRequest): TableResponse<SalidaDto> = client.post("/salidas/query", request)

    suspend fun suggestions(): SalidaSuggestionsDto = client.get("/salidas/suggestions")

    suspend fun get(id: String): SalidaDto = client.get("/salidas/$id")

    suspend fun create(input: SalidaInput): SalidaDto = client.post("/salidas", input)

    suspend fun update(id: String, input: SalidaInput): SalidaDto = client.put("/salidas/$id", input)

    suspend fun remove(id: String) = client.deleteNoContent("/salidas/$id")
}
