package com.axzydev.puertonuevoapp.feature.tickets

import com.axzydev.puertonuevoapp.core.session.SessionUser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Las reglas de la API y la web sobre tickets y tareas, por rol. */
class TicketPermissionsTest {

    private fun user(role: String, id: String, departmentId: String? = "rec") =
        SessionUser(id = id, username = id, name = id, role = role, departmentId = departmentId)

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
        val emp = user("EMPLOYEE", "emp")
        assertEquals(setOf("IN_PROGRESS", "IN_REVIEW"), moves(emp, "emp", "PENDING"))
        assertEquals(setOf("IN_REVIEW"), moves(emp, "emp", "IN_PROGRESS"))
        assertEquals(emptySet(), moves(emp, "emp", "IN_REVIEW"))
        assertEquals(emptySet(), moves(emp, "otro", "PENDING"))
    }

    @Test
    fun onlyAdminAndManagerCompleteTasks() {
        assertTrue("COMPLETED" in moves(user("ADMIN", "a"), "emp", "IN_REVIEW"))
        assertTrue("COMPLETED" in moves(user("MANAGER", "g"), "emp", "IN_REVIEW"))
        // Quien creó el ticket mueve la tarea a cualquier estado menos completada.
        assertEquals(setOf("PENDING", "IN_PROGRESS"), moves(user("AREA_HEAD", "creador"), "emp", "IN_REVIEW"))
    }

    @Test
    fun editingAndClosingFollowTheWeb() {
        val areaHeadCreatorOtherArea = user("AREA_HEAD", "creador", departmentId = "mant")
        assertTrue(TicketPermissions.canEdit(areaHeadCreatorOtherArea, ticket))
        assertFalse(TicketPermissions.canClose(areaHeadCreatorOtherArea, ticket))

        val areaHeadOfArea = user("AREA_HEAD", "jefe")
        assertFalse(TicketPermissions.canEdit(areaHeadOfArea, ticket))
        assertTrue(TicketPermissions.canClose(areaHeadOfArea, ticket))

        for (role in listOf("EMPLOYEE", "GUARD")) {
            val creator = user(role, "creador", departmentId = null)
            assertFalse(TicketPermissions.canEdit(creator, ticket), role)
            assertFalse(TicketPermissions.canClose(creator, ticket), role)
        }
    }

    @Test
    fun evidenceGoesOnYourOwnTaskOrAsManager() {
        val emp = user("EMPLOYEE", "emp")
        assertTrue(TicketPermissions.canUploadEvidence(emp, ticket, assignmentUserId = "emp"))
        assertFalse(TicketPermissions.canUploadEvidence(emp, ticket, assignmentUserId = "otro"))
        assertTrue(TicketPermissions.canUploadEvidence(user("MANAGER", "g"), ticket, assignmentUserId = "emp"))
    }
}
