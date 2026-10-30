package com.axzydev.puertonuevoapp.core.network.catalogs

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import io.ktor.http.encodeURLPathPart

/**
 * Configuración del sistema (`sys-config`) para la pantalla de Catálogos.
 *
 * Espejo de `web/src/entities/sys-config/api/sysConfigApi.ts`. El listado
 * completo es de ADMIN (`system.configure`); leer una clave suelta está
 * abierto a cualquier sesión, y las claves que el producto edita hoy son las
 * de [SysConfigKeys].
 *
 * Los demás catálogos **no** viven aquí para no duplicar endpoints:
 * departamentos/subáreas están en `core/network/departments/DepartmentsApi`,
 * tipos de dispositivo en `core/network/devicetypes/DeviceTypesApi`, los
 * catálogos de RH en `core/network/hr/HrApi` y las categorías de ticket en
 * `core/network/tickets/TicketsApi`.
 */
class CatalogsApi(private val client: ApiClient) {

    /** Todos los ajustes (`system.configure`). */
    suspend fun list(): List<SysConfigDto> =
        client.get("/sys-config")

    /** Un ajuste por clave (cualquier sesión). 404 si no existe. */
    suspend fun get(key: String): SysConfigDto =
        client.get("/sys-config/${key.encodeURLPathPart()}")

    /** Crea o actualiza una clave (`system.configure`). */
    suspend fun update(key: String, value: String, description: String? = null): SysConfigDto =
        client.put(
            "/sys-config/${key.encodeURLPathPart()}",
            SysConfigUpdateInput(value = value, description = description),
        )

    /** Borra una clave (`system.configure`); responde 204. */
    suspend fun remove(key: String) =
        client.deleteNoContent("/sys-config/${key.encodeURLPathPart()}")

    // ── Atajos de las claves que ya usa la web ─────────────────────────────

    /** Valor de los destinatarios CC de notificaciones. */
    suspend fun emailRecipients(): String =
        get(SysConfigKeys.EMAIL_NOTIFICATION_RECIPIENTS).value

    suspend fun setEmailRecipients(value: String): SysConfigDto =
        update(
            SysConfigKeys.EMAIL_NOTIFICATION_RECIPIENTS,
            value,
            "Notification CC recipients (comma-separated)",
        )

    /** Interruptor global del correo transaccional. */
    suspend fun sendEmailEnabled(): Boolean =
        get(SysConfigKeys.ENABLE_SEND_EMAIL).value.trim().equals("true", ignoreCase = true)

    suspend fun setSendEmail(enabled: Boolean): SysConfigDto =
        update(
            SysConfigKeys.ENABLE_SEND_EMAIL,
            if (enabled) "true" else "false",
            "Global switch for transactional email",
        )

    /** Primer día de la semana laboral (`SUNDAY`…`SATURDAY`); `null` si no está configurado. */
    suspend fun weekStartDay(): String? =
        runCatching { get(SysConfigKeys.WEEK_START_DAY).value }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }

    suspend fun setWeekStartDay(value: String): SysConfigDto =
        update(
            SysConfigKeys.WEEK_START_DAY,
            value,
            "First day of the work week for report ranges (SUNDAY…SATURDAY)",
        )
}
