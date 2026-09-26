package com.axzydev.puertonuevoapp.feature.tickets

import com.axzydev.puertonuevoapp.core.network.tickets.KanbanAssignmentDto
import com.axzydev.puertonuevoapp.core.network.tickets.TicketDto
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.session.SessionUser

/**
 * Qué puede hacer el usuario con un ticket y sus tareas, según el **alcance**
 * de sus permisos (`tickets.*`, `tasks.*`). Espejo de las reglas de la API
 * (`ticket.service.ts` y `ticket-attachment.service.ts`), que es la que
 * decide: la app solo esconde lo que la API rechazaría.
 *
 * Trabaja con los campos mínimos del ticket para servir tanto al detalle
 * (`TicketDto`) como al tablero (`TicketRefDto` del kanban).
 */
data class TicketAccessInfo(
    val createdById: String?,
    val assignedToId: String?,
    val departmentId: String?,
    /** Usuarios con alguna tarea en el ticket (en el tablero solo se conoce la propia). */
    val assignmentUserIds: List<String> = emptyList(),
)

fun TicketDto.accessInfo(): TicketAccessInfo =
    TicketAccessInfo(createdById, assignedToId, departmentId, assignments.map { it.userId })

/** En el tablero solo se conoce la tarea propia; basta para las reglas de esa tarea. */
fun KanbanAssignmentDto.accessInfo(): TicketAccessInfo =
    TicketAccessInfo(ticket.createdById, ticket.assignedToId, ticket.departmentId, listOf(userId))

object TicketPermissions {
    private const val ASSIGNMENT_COMPLETED = "COMPLETED"

    private fun SessionUser.within(permission: String, t: TicketAccessInfo): Boolean =
        withinScope(permission, t.createdById, t.assignedToId, t.departmentId, t.assignmentUserIds)

    /** Creador, asignado o con alguna tarea en el ticket. */
    fun isInvolved(user: SessionUser, t: TicketAccessInfo): Boolean =
        t.createdById == user.id || t.assignedToId == user.id || user.id in t.assignmentUserIds

    /** `canViewTicket` de la API (`tickets.view`). */
    fun canView(user: SessionUser, t: TicketAccessInfo): Boolean =
        user.within(PermissionKeys.TICKETS_VIEW, t)

    /** Editar el ticket (título, descripción, prioridad, categoría, estado): `tickets.edit`. */
    fun canEdit(user: SessionUser, t: TicketAccessInfo): Boolean =
        user.within(PermissionKeys.TICKETS_EDIT, t)

    /** Cerrar el ticket: `tickets.close`. */
    fun canClose(user: SessionUser, t: TicketAccessInfo): Boolean =
        user.within(PermissionKeys.TICKETS_CLOSE, t)

    /** Mandar el ticket a la papelera: `tickets.delete` (sin alcance por registro). */
    fun canDelete(user: SessionUser): Boolean = user.canDeleteTicket

    /** Crear tareas en el ticket: `tasks.assign`. */
    fun canCreateTasks(user: SessionUser, t: TicketAccessInfo): Boolean =
        user.within(PermissionKeys.TASKS_ASSIGN, t)

    /** Adjuntar al ticket: basta con poder verlo. */
    fun canUploadToTicket(user: SessionUser, t: TicketAccessInfo): Boolean = canView(user, t)

    /** Subir evidencia a una tarea: quien la tiene asignada o quien puede editar el ticket. */
    fun canUploadEvidence(user: SessionUser, t: TicketAccessInfo, assignmentUserId: String): Boolean =
        canView(user, t) && (canEdit(user, t) || assignmentUserId == user.id)

    /**
     * Estados a los que el usuario puede mover una tarea (sin contar el actual).
     * Con `tasks.complete` sobre el ticket: todos. Quien la tiene asignada:
     * solo avanzar hasta revisión. Con `tasks.assign`: todos menos completarla.
     */
    fun allowedAssignmentMoves(
        user: SessionUser,
        t: TicketAccessInfo,
        assignmentUserId: String,
        currentStatus: String,
    ): Set<String> {
        val all = assignmentStatusOrder.toSet() - currentStatus
        val canManage = user.within(PermissionKeys.TASKS_ASSIGN, t)
        val canComplete = user.within(PermissionKeys.TASKS_COMPLETE, t)
        val isOwner = assignmentUserId == user.id
        return when {
            !canManage && !isOwner -> emptySet()
            canComplete -> all
            isOwner -> ownerForwardMoves[currentStatus].orEmpty()
            else -> all - ASSIGNMENT_COMPLETED
        }
    }

    /** Avance permitido a quien tiene la tarea (`allowedTransitions` de la API). */
    private val ownerForwardMoves = mapOf(
        "PENDING" to setOf("IN_PROGRESS", "IN_REVIEW"),
        "IN_PROGRESS" to setOf("IN_REVIEW"),
    )
}
