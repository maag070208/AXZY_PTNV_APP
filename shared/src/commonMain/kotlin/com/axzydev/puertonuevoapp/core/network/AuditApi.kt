package com.axzydev.puertonuevoapp.core.network

import io.ktor.http.encodeURLParameter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Auditoría del sistema. El backend sí restringe este módulo a ADMIN
 * (audit.routes.ts: `authorize(["ADMIN"])`), a diferencia de la nota que
 * existía antes. Se replica como está: solo administradores consumen la
 * pantalla de Auditoría.
 */
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

@Serializable
data class AuditLogDto(
    val id: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val userId: String? = null,
    val userName: String? = null,
    val deviceId: String? = null,
    val deviceCode: String? = null,
    val previousState: JsonObject? = null,
    val newState: JsonObject? = null,
    val metadata: JsonObject? = null,
    val createdAt: String,
)

@Serializable
data class AuditLogListResponseDto(
    val data: List<AuditLogDto>,
    val total: Int,
    val page: Int = 1,
    val limit: Int = 20,
)
