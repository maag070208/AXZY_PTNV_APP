package com.axzydev.puertonuevoapp.core.network.locations

import com.axzydev.puertonuevoapp.core.network.http.ApiClient

class LocationsApi(private val client: ApiClient) {
    suspend fun list(includeInactive: Boolean = false): List<LocationDto> =
        client.get("/locations${if (includeInactive) "?includeInactive=true" else ""}")

    suspend fun get(id: String): LocationDto = client.get("/locations/$id")

    suspend fun create(input: LocationCreateInput): LocationDto = client.post("/locations", input)

    suspend fun update(id: String, input: LocationUpdateInput): LocationDto = client.put("/locations/$id", input)

    suspend fun remove(id: String): LocationDeleteResultDto = client.delete("/locations/$id")

    suspend fun addSublugar(locationId: String, name: String): SublugarDto =
        client.post("/locations/$locationId/sublugares", SublugarCreateInput(name))

    suspend fun removeSublugar(sublugarId: String): SublugarDeleteResultDto =
        client.delete("/locations/sublugares/$sublugarId")
}
