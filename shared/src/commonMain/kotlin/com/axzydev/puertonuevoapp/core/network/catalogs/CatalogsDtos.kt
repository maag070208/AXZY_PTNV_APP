package com.axzydev.puertonuevoapp.core.network.catalogs

import kotlinx.serialization.Serializable

/**
 * DTOs de la configuración del sistema (`sys_config`).
 *
 * Contrato autoritativo: `api/src/modules/config/routes/config.routes.ts` y
 * `api/src/modules/config/services/sys-config.service.ts`. Espejo de
 * `web/src/entities/sys-config/api/sysConfigApi.ts`.
 *
 * `GET /sys-config` (el listado completo) exige `system.configure`; en cambio
 * `GET /sys-config/:key` está abierto a cualquier sesión, que es como la web
 * lee una sola clave desde pantallas de gerente o jefe de área.
 */

/** Usuario que tocó la configuración por última vez. */
@Serializable
data class SysConfigUserRefDto(
    val id: String,
    val name: String,
)

/** Un ajuste de `sys_config` (`key` en MAYÚSCULAS con guiones bajos). */
@Serializable
data class SysConfigDto(
    val id: String,
    val key: String = "",
    val value: String = "",
    val description: String? = null,
    val updatedAt: String = "",
    val updatedById: String? = null,
    val updatedBy: SysConfigUserRefDto? = null,
)

/** Body de `PUT /sys-config/:key`: valor obligatorio y descripción opcional. */
@Serializable
data class SysConfigUpdateInput(
    val value: String,
    val description: String? = null,
)

/**
 * Claves de `sys_config` que la app consulta (las demás son del servidor).
 * Los valores son texto: `ENABLE_SEND_EMAIL` es "true"/"false" y
 * `WEEK_START_DAY` es SUNDAY…SATURDAY.
 */
object SysConfigKeys {
    /** Destinatarios CC de las notificaciones (lista separada por comas). */
    const val EMAIL_NOTIFICATION_RECIPIENTS = "EMAIL_NOTIFICATION_RECIPIENTS"

    /** Interruptor global del correo transaccional. */
    const val ENABLE_SEND_EMAIL = "ENABLE_SEND_EMAIL"

    /** Primer día de la semana laboral para los rangos de los reportes. */
    const val WEEK_START_DAY = "WEEK_START_DAY"
}
