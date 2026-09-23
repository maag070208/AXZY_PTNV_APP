package com.axzydev.puertonuevoapp.core.network.dashboard

import com.axzydev.puertonuevoapp.core.network.http.ApiClient

class DashboardApi(private val client: ApiClient) {
    suspend fun summary(): DashboardSummaryDto = client.get("/dashboard/summary")
}
