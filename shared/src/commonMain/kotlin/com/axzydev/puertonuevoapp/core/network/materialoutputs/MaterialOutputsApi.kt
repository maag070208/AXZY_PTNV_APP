package com.axzydev.puertonuevoapp.core.network.materialoutputs

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import io.ktor.http.encodeURLParameter

class MaterialOutputsApi(private val client: ApiClient) {
    suspend fun list(
        start: String? = null,
        end: String? = null,
        departmentName: String? = null,
        userName: String? = null,
        q: String? = null,
    ): MaterialOutputsListResponseDto {
        val params = buildList {
            start?.takeIf { it.isNotBlank() }?.let { add("start=${it.encodeURLParameter()}") }
            end?.takeIf { it.isNotBlank() }?.let { add("end=${it.encodeURLParameter()}") }
            departmentName?.takeIf { it.isNotBlank() }?.let { add("departmentName=${it.encodeURLParameter()}") }
            userName?.takeIf { it.isNotBlank() }?.let { add("userName=${it.encodeURLParameter()}") }
            q?.takeIf { it.isNotBlank() }?.let { add("q=${it.encodeURLParameter()}") }
        }
        val qs = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
        return client.get("/material-outputs$qs")
    }

    suspend fun query(request: TableRequest): TableResponse<MaterialOutputDto> = client.post("/material-outputs/query", request)

    suspend fun suggestions(): MaterialOutputSuggestionsDto = client.get("/material-outputs/suggestions")

    suspend fun get(id: String): MaterialOutputDto = client.get("/material-outputs/$id")

    suspend fun create(input: MaterialOutputInput): MaterialOutputDto = client.post("/material-outputs", input)

    suspend fun update(id: String, input: MaterialOutputInput): MaterialOutputDto = client.put("/material-outputs/$id", input)

    suspend fun remove(id: String) = client.deleteNoContent("/material-outputs/$id")
}
