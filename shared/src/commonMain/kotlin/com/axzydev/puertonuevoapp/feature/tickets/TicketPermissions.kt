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
    val creadoPorId: String?,
    val asignadoAId: String?,
    val departmentId: String?,
    /** Usuarios con alguna tarea en el ticket (en el tablero solo se conoce la propia). */
    val assignmentUserIds: List<String> = emptyList(),
)

fun TicketDto.accessInfo(): TicketAccessInfo =
    TicketAccessInfo(creadoPorId, asignadoAId, departmentId, assignments.map { it.userId })

/** En el tablero solo se conoce la tarea propia; basta para las reglas de esa tarea. */
fun KanbanAssignmentDto.accessInfo(): TicketAccessInfo =
    TicketAccessInfo(ticket.creadoPorId, ticket.asignadoAId, ticket.departmentId, listOf(userId))

object TicketPermissions {
    private const val ASSIGNMENT_COMPLETED = "COMPLETADA"

    /** Creador, asignado o con alguna tarea en el ticket. */
    fun isInvolved(user: SessionUser, t: TicketAccessInfo): Boolean =
        t.creadoPorId == user.id || t.asignadoAId == user.id || user.id in t.assignmentUserIds

    private fun sameDepartment(user: SessionUser, t: TicketAccessInfo): Boolean =
        user.departmentId != null && t.departmentId == user.departmentId

    /** `canManageTicket` de la API: ADMIN, GERENTE de su departamento o JEFE que lo creó. */
    fun canManage(user: SessionUser, t: TicketAccessInfo): Boolean =
        user.isAdmin ||
            (user.isGerente && sameDepartment(user, t)) ||
            (user.isJefeArea && t.creadoPorId == user.id)

    /** `assertTicketAccess` / `canAccessTicket` de la API. */
    fun canAccess(user: SessionUser, t: TicketAccessInfo): Boolean =
        user.isAdmin ||
            isInvolved(user, t) ||
            ((user.isGerente || user.isJefeArea) && sameDepartment(user, t)) ||
            canManage(user, t)

    /** Editar el ticket (título, descripción, prioridad, categoría, estado): igual que la web. */
    fun canEdit(user: SessionUser, t: TicketAccessInfo): Boolean =
        user.isAdmin || user.isGerente || (user.isJefeArea && t.creadoPorId == user.id)

    /** Cerrar el ticket: ADMIN, GERENTE o JEFE de su departamento (web y API). */
    fun canClose(user: SessionUser, t: TicketAccessInfo): Boolean =
        user.isAdmin || user.isGerente || (user.isJefeArea && sameDepartment(user, t))

    /** Crear tareas en el ticket: ADMIN, quien lo creó o a quien está asignado (web). */
    fun canCreateTasks(user: SessionUser, t: TicketAccessInfo): Boolean =
        user.isAdmin || t.creadoPorId == user.id || t.asignadoAId == user.id

    /** Adjuntar al ticket: con acceso y, si es EMPLEADO, solo si lo creó o tiene una tarea. */
    fun canUploadToTicket(user: SessionUser, t: TicketAccessInfo): Boolean =
        canAccess(user, t) && (!user.isEmpleado || t.creadoPorId == user.id || user.id in t.assignmentUserIds)

    /** Subir evidencia a una tarea: quien la tiene asignada o quien administra el ticket. */
    fun canUploadEvidence(user: SessionUser, t: TicketAccessInfo, assignmentUserId: String): Boolean =
        canAccess(user, t) && (canManage(user, t) || assignmentUserId == user.id)

    /**
     * Estados a los que el usuario puede mover una tarea (sin contar el actual).
     * ADMIN y GERENTE: todos. Quien la tiene asignada: solo avanzar hasta
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
            user.isAdmin || user.isGerente -> all
            assignmentUserId == user.id -> ownerForwardMoves[currentStatus].orEmpty()
            t.creadoPorId == user.id || t.asignadoAId == user.id -> all - ASSIGNMENT_COMPLETED
            else -> emptySet()
        }
    }

    /** Avance permitido a quien tiene la tarea (`allowedTransitions` de la API). */
    private val ownerForwardMoves = mapOf(
        "PENDIENTE" to setOf("EN_PROGRESO", "EN_REVISION"),
        "EN_PROGRESO" to setOf("EN_REVISION"),
    )
}
