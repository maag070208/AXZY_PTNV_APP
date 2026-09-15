package com.axzydev.puertonuevoapp.core.network.devicetypes

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse

class DeviceTypesApi(private val client: ApiClient) {
    suspend fun list(includeInactive: Boolean = false): List<DeviceTypeDto> =
        client.get("/device-types${if (includeInactive) "?includeInactive=true" else ""}")

    suspend fun query(request: TableRequest): TableResponse<DeviceTypeDto> =
        client.post("/device-types/query", request)

    suspend fun get(id: String): DeviceTypeDto = client.get("/device-types/$id")

    suspend fun peek(id: String): String = client.get<NextFolioDto>("/device-types/$id/peek").siguiente

    suspend fun peekCarta(id: String): String = client.get<NextFolioDto>("/device-types/$id/peek-carta").siguiente

    suspend fun create(input: DeviceTypeCreateInput): DeviceTypeDto = client.post("/device-types", input)

    suspend fun update(id: String, input: DeviceTypeUpdateInput): DeviceTypeDto = client.put("/device-types/$id", input)

    suspend fun delete(id: String) = client.deleteNoContent("/device-types/$id")
}
