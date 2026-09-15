package com.axzydev.puertonuevoapp.core.network.inventory

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import io.ktor.http.encodeURLParameter

class InventoryApi(private val client: ApiClient) {
    suspend fun movements(
        deviceId: String? = null,
        locationId: String? = null,
        start: String? = null,
        end: String? = null,
    ): List<InventoryMovementDto> {
        val params = buildList {
            deviceId?.takeIf { it.isNotBlank() }?.let { add("deviceId=$it") }
            locationId?.takeIf { it.isNotBlank() }?.let { add("locationId=$it") }
            start?.takeIf { it.isNotBlank() }?.let { add("start=${it.encodeURLParameter()}") }
            end?.takeIf { it.isNotBlank() }?.let { add("end=${it.encodeURLParameter()}") }
        }
        val qs = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
        return client.get("/inventory/movements$qs")
    }

    suspend fun kardex(deviceId: String): KardexDto = client.get("/inventory/kardex/$deviceId")

    suspend fun registerMovement(input: MovementInput): InventoryMovementDto =
        client.post("/inventory/movements", input)

    suspend fun summary(): InventorySummaryDto = client.get("/inventory/summary")
}
