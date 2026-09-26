package com.axzydev.puertonuevoapp.core.network.access

import kotlinx.serialization.Serializable

/**
 * DTOs del módulo de control de acceso (`/access`). Contrato autoritativo:
 * `api/src/modules/access/models/dto/access.dto.ts`. Todos los textos de
 * campos llegan ya resueltos del backend; aquí no se traduce nada.
 */

/** Referencia liviana a un sitio/portería embebida en un evento. */
@Serializable
data class AccessSiteRefDto(
    val id: String,
    val name: String,
)

/** Resumen de un evento devuelto por `lastEvent` (lookup/status). */
@Serializable
data class AccessLastEventDto(
    val id: String,
    val type: String,
    val occurredAt: String,
    val voidedAt: String? = null,
    val siteId: String? = null,
)

/** Evento de acceso completo (`POST /access/events`, `POST /access/query`). */
@Serializable
data class AccessEventDto(
    val id: String,
    val type: String,
    val occurredAt: String,
    val deviceTimestamp: String? = null,
    val employeeId: String,
    val employeeNameSnapshot: String? = null,
    val employeeNumberSnapshot: String? = null,
    val guardId: String? = null,
    val siteId: String? = null,
    val site: AccessSiteRefDto? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val gpsAccuracyMeters: Double? = null,
    val locationSource: String = "SITE_ONLY",
    val method: String = "QR_SCAN",
    val credentialVersion: Int? = null,
    val scannedPayloadHash: String? = null,
    val clientEventId: String? = null,
    val deviceId: String? = null,
    val deviceCode: String? = null,
    val notes: String? = null,
    val voidedAt: String? = null,
    val voidedById: String? = null,
    val voidReason: String? = null,
    val createdAt: String = "",
)

/** Resultado de `POST /access/lookup` — resumen del empleado, sin registrar. */
@Serializable
data class AccessLookupResultDto(
    val id: String,
    val name: String,
    val employeeNumber: String? = null,
    val jobTitle: String? = null,
    val department: String? = null,
    val active: Boolean = true,
    /** Ruta relativa a la base del API (`.../api/v1`); `null` si no hay foto. */
    val photoUrl: String? = null,
    val credentialVersion: Int = 2,
    val lastEvent: AccessLastEventDto? = null,
    val suggestedType: String = "ENTRY",
)

/** Empleado embebido en `GET /access/status/:employeeId`. */
@Serializable
data class AccessStatusEmployeeDto(
    val id: String,
    val name: String,
    val employeeNumber: String? = null,
    val active: Boolean = true,
)

/** Resultado de `GET /access/status/:employeeId`. */
@Serializable
data class AccessStatusDto(
    val employee: AccessStatusEmployeeDto,
    val lastEvent: AccessLastEventDto? = null,
    val suggestedType: String = "ENTRY",
    val hasOpenEntry: Boolean = false,
)

/** Respuesta de `GET /access/me/today`. */
@Serializable
data class AccessTodayResponseDto(
    val data: List<AccessEventDto> = emptyList(),
    val total: Int = 0,
)

/** Sitio/portería del catálogo (`GET /access/sites`). */
@Serializable
data class SiteDto(
    val id: String,
    val name: String,
    val code: String? = null,
    val active: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Int? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
)

/**
 * Body de `POST /access/events`. `occurredAt` NO viaja: lo fija el servidor.
 * `deviceTimestamp` es solo auditoría del reloj del dispositivo.
 */
@Serializable
data class AccessEventInput(
    val qr: String? = null,
    val employeeId: String? = null,
    val type: String,
    val siteId: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Double? = null,
    val deviceTimestamp: String? = null,
    val clientEventId: String,
    val deviceId: String? = null,
    val deviceCode: String? = null,
    val notes: String? = null,
)

/** Body de `POST /access/lookup`. */
@Serializable
data class AccessLookupInput(
    val qr: String,
)

/** Tipos de evento de acceso (espejo del enum del backend). */
object AccessEventType {
    const val ENTRY = "ENTRY"
    const val EXIT = "EXIT"
}
