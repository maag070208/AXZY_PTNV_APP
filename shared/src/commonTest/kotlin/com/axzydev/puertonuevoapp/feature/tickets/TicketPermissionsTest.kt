package com.axzydev.puertonuevoapp.feature.tickets

import com.axzydev.puertonuevoapp.core.session.SessionUser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Las reglas de la API y la web sobre tickets y tareas, por alcance de permiso.
 * Los permisos de cada usuario replican la matriz por defecto del seed del API
 * (`role_permissions.json`).
 */
class TicketPermissionsTest {

    private val admin = mapOf(
        "tickets.view" to "ALL", "tickets.edit" to "ALL", "tickets.close" to "ALL", "tickets.delete" to "ALL",
        "tasks.view" to "ALL", "tasks.assign" to "ALL", "tasks.complete" to "ALL",
    )
    private val manager = mapOf(
        "tickets.view" to "AREA", "tickets.edit" to "AREA", "tickets.close" to "AREA",
        "tasks.view" to "AREA", "tasks.assign" to "OWN", "tasks.complete" to "AREA",
    )
    private val areaHead = mapOf(
        "tickets.view" to "AREA", "tickets.edit" to "OWN", "tickets.close" to "AREA",
        "tasks.view" to "AREA", "tasks.assign" to "OWN",
    )
    private val employee = mapOf("tickets.view" to "OWN", "tasks.view" to "OWN")

    private fun user(permissions: Map<String, String>, id: String, departmentId: String? = "rec") =
        SessionUser(id = id, username = id, name = id, role = "X", departmentId = departmentId, permissions = permissions)

    /** Ticket de Recepción creado por "creador", con una tarea de "emp". */
    private val ticket = TicketAccessInfo(
        createdById = "creador",
        assignedToId = null,
        departmentId = "rec",
        assignmentUserIds = listOf("emp"),
    )

    private fun moves(user: SessionUser, taskOwner: String, status: String) =
        TicketPermissions.allowedAssignmentMoves(user, ticket, taskOwner, status)

    @Test
    fun employeeOnlyAdvancesTheirOwnTaskUntilReview() {
        val emp = user(employee, "emp")
        assertEquals(setOf("IN_PROGRESS", "IN_REVIEW"), moves(emp, "emp", "PENDING"))
        assertEquals(setOf("IN_REVIEW"), moves(emp, "emp", "IN_PROGRESS"))
        assertEquals(emptySet(), moves(emp, "emp", "IN_REVIEW"))
        assertEquals(emptySet(), moves(emp, "otro", "PENDING"))
    }

    @Test
    fun completingNeedsTasksCompleteOnTheTicket() {
        assertTrue("COMPLETED" in moves(user(admin, "a"), "emp", "IN_REVIEW"))
        // MANAGER creador (`tasks.assign` OWN + `tasks.complete` AREA): todos los estados.
        assertTrue("COMPLETED" in moves(user(manager, "creador"), "emp", "IN_REVIEW"))
        // Sin `tasks.assign` sobre el ticket y sin ser dueño de la tarea, la API lo rechaza.
        assertEquals(emptySet(), moves(user(manager, "g"), "emp", "IN_REVIEW"))
        // Quien creó el ticket (`tasks.assign` OWN) la mueve a cualquier estado menos completada.
        assertEquals(setOf("PENDING", "IN_PROGRESS"), moves(user(areaHead, "creador"), "emp", "IN_REVIEW"))
    }

    @Test
    fun editingAndClosingFollowTheScope() {
        // AREA_HEAD: `tickets.edit` OWN, `tickets.close` AREA (incluye lo propio).
        val areaHeadCreatorOtherArea = user(areaHead, "creador", departmentId = "mant")
        assertTrue(TicketPermissions.canEdit(areaHeadCreatorOtherArea, ticket))
        assertTrue(TicketPermissions.canClose(areaHeadCreatorOtherArea, ticket))

        val areaHeadOfArea = user(areaHead, "jefe")
        assertFalse(TicketPermissions.canEdit(areaHeadOfArea, ticket))
        assertTrue(TicketPermissions.canClose(areaHeadOfArea, ticket))

        val areaHeadOtherArea = user(areaHead, "jefe", departmentId = "mant")
        assertFalse(TicketPermissions.canClose(areaHeadOtherArea, ticket))

        val creatorWithoutPermissions = user(employee, "creador", departmentId = null)
        assertFalse(TicketPermissions.canEdit(creatorWithoutPermissions, ticket))
        assertFalse(TicketPermissions.canClose(creatorWithoutPermissions, ticket))
        assertFalse(TicketPermissions.canDelete(creatorWithoutPermissions))
        assertTrue(TicketPermissions.canDelete(user(admin, "a")))
    }

    @Test
    fun evidenceGoesOnYourOwnTaskOrWithEditScope() {
        val emp = user(employee, "emp")
        assertTrue(TicketPermissions.canUploadEvidence(emp, ticket, assignmentUserId = "emp"))
        assertFalse(TicketPermissions.canUploadEvidence(emp, ticket, assignmentUserId = "otro"))
        assertTrue(TicketPermissions.canUploadEvidence(user(manager, "g"), ticket, assignmentUserId = "emp"))
    }

    @Test
    fun withoutPermissionsNothingIsAllowed() {
        val nobody = user(emptyMap(), "creador")
        assertFalse(TicketPermissions.canView(nobody, ticket))
        assertFalse(TicketPermissions.canCreateTasks(nobody, ticket))
        assertEquals(emptySet(), moves(nobody, "otro", "PENDING"))
    }
}
