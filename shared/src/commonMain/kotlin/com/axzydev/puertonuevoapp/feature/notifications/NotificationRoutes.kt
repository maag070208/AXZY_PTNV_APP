package com.axzydev.puertonuevoapp.feature.notifications

import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.notifications.NotificationDto
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.session.SessionUser

/**
 * Deep links de las notificaciones: qué pantalla abre cada tipo.
 *
 * Es el ÚNICO lugar donde vive el mapeo tipo → pantalla (espejo de
 * `notificationRoute` de la web, pero cubriendo las pantallas y permisos de la
 * app). Reglas:
 *
 * - Si la notificación trae `ticketId` (tickets, comentarios, asignaciones…) se
 *   abre el detalle del ticket, con `tickets.view`.
 * - `ACCESS*` / `VISITOR*` → bitácora de acceso (`access.log`).
 * - `INVENTORY_AUDIT` → auditor de inventario (`inventory.audit`).
 * - `TIME_CLOCK*` → asistencia/reloj (`time_clock.view`).
 * - `OVERTIME*` → aprobación de tiempo extra (`overtime.view`).
 * - `SCHEDULE*` → horarios (`schedules.view`).
 * - `KITCHEN*` → **no navega**: el módulo de cocina está oculto en la app
 *   (`FEATURES.kitchen = false` en la web); la notificación solo se marca leída.
 * - Cualquier otro tipo (usuarios, documentos, avisos sueltos, tipos nuevos del
 *   API) devuelve `null`: se marca leída y no se navega. Un tipo desconocido
 *   nunca rompe la pantalla de notificaciones.
 *
 * `null` = sin destino; el llamador solo marca leída. El permiso se evalúa con
 * los permisos efectivos de la sesión; si no lo tiene, tampoco navega (la API
 * lo rechazaría) y la notificación queda solo como leída.
 */
object NotificationRoutes {

    fun screenFor(notification: NotificationDto, user: SessionUser?): Screen? {
        // Tickets/comentarios/asignaciones: el detalle manda sobre el tipo.
        notification.ticketId?.takeIf { it.isNotBlank() }?.let { ticketId ->
            return if (user?.can(PermissionKeys.TICKETS_VIEW) == true) {
                Screen.TicketDetail(ticketId)
            } else {
                null
            }
        }

        val type = notification.type.uppercase()
        return when {
            type.startsWith("ACCESS") || type.startsWith("VISITOR") ->
                if (user?.can(PermissionKeys.ACCESS_LOG) == true) Screen.AccessLog else null

            type == "INVENTORY_AUDIT" ->
                if (user?.can(PermissionKeys.INVENTORY_AUDIT) == true) Screen.InventoryAudit else null

            type.startsWith("TIME_CLOCK") ->
                if (user?.can(PermissionKeys.TIME_CLOCK_VIEW) == true) Screen.TimeClock else null

            type.startsWith("OVERTIME") ->
                if (user?.can(PermissionKeys.OVERTIME_VIEW) == true) Screen.OvertimeApproval else null

            type.startsWith("SCHEDULE") ->
                if (user?.can(PermissionKeys.SCHEDULES_VIEW) == true) Screen.Schedules else null

            // Módulo oculto en la app: no hay pantalla de cocina a la que ir.
            type.startsWith("KITCHEN") -> null

            // Tipo nuevo/desconocido: solo se marca leída.
            else -> null
        }
    }
}
