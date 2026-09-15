package com.axzydev.puertonuevoapp.core.network.audit

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import io.ktor.http.encodeURLParameter

class AuditApi(private val client: ApiClient) {
    suspend fun list(
        action: String? = null,
        entityType: String? = null,
        userId: String? = null,
        deviceId: String? = null,
        start: String? = null,
        end: String? = null,
        page: Int = 1,
        limit: Int = 20,
    ): AuditLogListResponseDto {
        val params = buildList {
            action?.takeIf { it.isNotBlank() }?.let { add("action=${it.encodeURLParameter()}") }
            entityType?.takeIf { it.isNotBlank() }?.let { add("entityType=${it.encodeURLParameter()}") }
            userId?.takeIf { it.isNotBlank() }?.let { add("userId=$it") }
            deviceId?.takeIf { it.isNotBlank() }?.let { add("deviceId=$it") }
            start?.takeIf { it.isNotBlank() }?.let { add("start=${it.encodeURLParameter()}") }
            end?.takeIf { it.isNotBlank() }?.let { add("end=${it.encodeURLParameter()}") }
            add("page=$page")
            add("limit=$limit")
        }
        val qs = "?" + params.joinToString("&")
        return client.get("/audit$qs")
    }

    suspend fun get(id: String): AuditLogDto = client.get("/audit/$id")
}
