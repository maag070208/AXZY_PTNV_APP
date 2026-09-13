package com.axzydev.puertonuevoapp.core.network.devices

import com.axzydev.puertonuevoapp.core.network.http.ApiClient

class DevicesApi(private val client: ApiClient) {
    suspend fun summary(): DeviceSummaryDto = client.get("/devices/summary")
}