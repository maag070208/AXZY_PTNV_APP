package com.axzydev.puertonuevoapp.core.network.audit

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

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
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20,
)

val auditActionOptions: List<Pair<String, String>> = listOf(
    "" to "Todas",
    "MOVEMENT_ENTRADA" to "Entrada",
    "MOVEMENT_SALIDA" to "Salida",
    "MOVEMENT_TRASLADO" to "Traslado",
    "MOVEMENT_BAJA" to "Baja",
    "MOVEMENT_PRESTAMO" to "Asignado",
    "MOVEMENT_DEVOLUCION" to "Devolución",
    "CARTA_CREATED" to "Carta creada",
    "DEVICE_UPDATED" to "Dispositivo actualizado",
)
