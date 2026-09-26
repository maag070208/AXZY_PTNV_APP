package com.axzydev.puertonuevoapp.feature.tickets

import com.axzydev.puertonuevoapp.core.network.tickets.KanbanAssignmentDto
import com.axzydev.puertonuevoapp.core.network.tickets.TicketDto
import com.axzydev.puertonuevoapp.core.session.SessionUser

/**
 * Qué puede hacer el usuario con un ticket y sus tareas. Espejo de las reglas de
 * la API (`ticket.service.ts` y `ticket-attachment.service.ts`), que es la que
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

    /** Creador, asignado o con alguna tarea en el ticket. */
    fun isInvolved(user: SessionUser, t: TicketAccessInfo): Boolean =
        t.createdById == user.id || t.assignedToId == user.id || user.id in t.assignmentUserIds

    private fun sameDepartment(user: SessionUser, t: TicketAccessInfo): Boolean =
        user.departmentId != null && t.departmentId == user.departmentId

    /** `canManageTicket` de la API: ADMIN, MANAGER de su departamento o AREA_HEAD que lo creó. */
    fun canManage(user: SessionUser, t: TicketAccessInfo): Boolean =
        user.isAdmin ||
            (user.isManager && sameDepartment(user, t)) ||
            (user.isAreaHead && t.createdById == user.id)

    /** `assertTicketAccess` / `canAccessTicket` de la API. */
    fun canAccess(user: SessionUser, t: TicketAccessInfo): Boolean =
        user.isAdmin ||
            isInvolved(user, t) ||
            ((user.isManager || user.isAreaHead) && sameDepartment(user, t)) ||
            canManage(user, t)

    /** Editar el ticket (título, descripción, prioridad, categoría, estado): igual que la web. */
    fun canEdit(user: SessionUser, t: TicketAccessInfo): Boolean =
        user.isAdmin || user.isManager || (user.isAreaHead && t.createdById == user.id)

    /** Cerrar el ticket: ADMIN, MANAGER o AREA_HEAD de su departamento (web y API). */
    fun canClose(user: SessionUser, t: TicketAccessInfo): Boolean =
        user.isAdmin || user.isManager || (user.isAreaHead && sameDepartment(user, t))

    /** Crear tareas en el ticket: ADMIN, quien lo creó o a quien está asignado (web). */
    fun canCreateTasks(user: SessionUser, t: TicketAccessInfo): Boolean =
        user.isAdmin || t.createdById == user.id || t.assignedToId == user.id

    /** Adjuntar al ticket: con acceso y, si es EMPLOYEE, solo si lo creó o tiene una tarea. */
    fun canUploadToTicket(user: SessionUser, t: TicketAccessInfo): Boolean =
        canAccess(user, t) && (!user.isEmployee || t.createdById == user.id || user.id in t.assignmentUserIds)

    /** Subir evidencia a una tarea: quien la tiene asignada o quien administra el ticket. */
    fun canUploadEvidence(user: SessionUser, t: TicketAccessInfo, assignmentUserId: String): Boolean =
        canAccess(user, t) && (canManage(user, t) || assignmentUserId == user.id)

    /**
     * Estados a los que el usuario puede mover una tarea (sin contar el actual).
     * ADMIN y MANAGER: todos. Quien la tiene asignada: solo avanzar hasta
     * revisión. Quien creó el ticket o lo tiene asignado: todos menos completarla.
     */
    fun allowedAssignmentMoves(
        user: SessionUser,
        t: TicketAccessInfo,
        assignmentUserId: String,
        currentStatus: String,
    ): Set<String> {
        val all = assignmentStatusOrder.toSet() - currentStatus
        return when {
            user.isAdmin || user.isManager -> all
            assignmentUserId == user.id -> ownerForwardMoves[currentStatus].orEmpty()
            t.createdById == user.id || t.assignedToId == user.id -> all - ASSIGNMENT_COMPLETED
            else -> emptySet()
        }
    }

    /** Avance permitido a quien tiene la tarea (`allowedTransitions` de la API). */
    private val ownerForwardMoves = mapOf(
        "PENDING" to setOf("IN_PROGRESS", "IN_REVIEW"),
        "IN_PROGRESS" to setOf("IN_REVIEW"),
    )
}
