package com.axzydev.puertonuevoapp.core.network.devices

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
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

    suspend fun query(request: TableRequest): TableResponse<DeviceDto> = client.post("/devices/query", request)

    suspend fun summary(): DeviceSummaryDto = client.get("/devices/summary")

    suspend fun availability(): List<DeviceAvailabilityGroupDto> = client.get("/devices/availability")

    suspend fun get(id: String): DeviceDto = client.get("/devices/$id")

    suspend fun create(input: DeviceCreateInput): DeviceDto = client.post("/devices", input)

    suspend fun update(id: String, input: DeviceUpdateInput): DeviceDto = client.put("/devices/$id", input)

    suspend fun remove(id: String, force: Boolean = false): DeviceRemoveResponseDto =
        client.delete("/devices/$id${if (force) "?force=true" else ""}")

    suspend fun getLote(loteId: String): DeviceListResponseDto = client.get("/devices/lotes/$loteId")

    suspend fun updateLote(loteId: String, input: DeviceLoteUpdateInput): DeviceListResponseDto =
        client.put("/devices/lotes/$loteId", input)

    suspend fun history(id: String): List<DeviceHistoryEntryDto> = client.get("/devices/$id/history")

    suspend fun addHistory(id: String, type: String, detail: String? = null) =
        client.postNoContent("/devices/$id/history", DeviceHistoryInput(type, detail))

    suspend fun batch(input: DeviceBatchInput): DeviceListResponseDto = client.post("/devices/batch", input)

    suspend fun addUnits(id: String, cantidad: Int): AddUnitsResponseDto =
        client.post("/devices/$id/add-units", AddUnitsInput(cantidad))
}

@kotlinx.serialization.Serializable
data class DeviceHistoryInput(val type: String, val detail: String? = null)
