package com.axzydev.puertonuevoapp.core.network

class DeviceTypesApi(private val client: ApiClient) {
    suspend fun list(includeInactive: Boolean = false): List<DeviceTypeDto> =
        client.get("/device-types${if (includeInactive) "?includeInactive=true" else ""}")

    suspend fun get(id: String): DeviceTypeDto = client.get("/device-types/$id")

    suspend fun create(input: DeviceTypeCreateDto): DeviceTypeDto = client.post("/device-types", input)

    suspend fun update(id: String, input: DeviceTypeUpdateDto): DeviceTypeDto = client.put("/device-types/$id", input)

    // Folio de siguiente carta responsiva para este tipo (basado en su propio
    // contador `cartaContador`, distinto del consecutivo global de /cartas).
    suspend fun peekCarta(id: String): String = client.get<ConsecutivoPeekDto>("/device-types/$id/peek-carta").siguiente
}
