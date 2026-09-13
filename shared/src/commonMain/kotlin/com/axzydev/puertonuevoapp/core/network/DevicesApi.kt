package com.axzydev.puertonuevoapp.core.network

import io.ktor.http.encodeURLParameter

class DevicesApi(private val client: ApiClient) {
    suspend fun list(estado: String? = null, q: String? = null, typeId: String? = null): DeviceListResponseDto {
        val params = buildList {
            estado?.takeIf { it.isNotBlank() }?.let { add("estado=$it") }
            q?.takeIf { it.isNotBlank() }?.let { add("q=${it.encodeURLParameter()}") }
            typeId?.takeIf { it.isNotBlank() }?.let { add("typeId=$it") }
        }
        val qs = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
        return client.get("/devices$qs")
    }

    suspend fun summary(): DeviceSummaryDto = client.get("/devices/summary")

    suspend fun get(id: String): DeviceDto = client.get("/devices/$id")

    suspend fun create(input: DeviceCreateDto): DeviceDto = client.post("/devices", input)

    suspend fun update(id: String, input: DeviceUpdateDto): DeviceDto = client.put("/devices/$id", input)
}
