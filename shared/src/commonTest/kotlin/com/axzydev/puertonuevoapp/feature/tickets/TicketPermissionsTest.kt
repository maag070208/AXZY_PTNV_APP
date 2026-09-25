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
        creadoPorId = "creador",
        asignadoAId = null,
        departmentId = "rec",
        assignmentUserIds = listOf("emp"),
    )

    private fun moves(user: SessionUser, taskOwner: String, status: String) =
        TicketPermissions.allowedAssignmentMoves(user, ticket, taskOwner, status)

    @Test
    fun employeeOnlyAdvancesTheirOwnTaskUntilReview() {
        val emp = user("EMPLEADO", "emp")
        assertEquals(setOf("EN_PROGRESO", "EN_REVISION"), moves(emp, "emp", "PENDIENTE"))
        assertEquals(setOf("EN_REVISION"), moves(emp, "emp", "EN_PROGRESO"))
        assertEquals(emptySet(), moves(emp, "emp", "EN_REVISION"))
        assertEquals(emptySet(), moves(emp, "otro", "PENDIENTE"))
    }

    @Test
    fun onlyAdminAndGerenteCompleteTasks() {
        assertTrue("COMPLETADA" in moves(user("ADMIN", "a"), "emp", "EN_REVISION"))
        assertTrue("COMPLETADA" in moves(user("GERENTE", "g"), "emp", "EN_REVISION"))
        // Quien creó el ticket mueve la tarea a cualquier estado menos completada.
        assertEquals(setOf("PENDIENTE", "EN_PROGRESO"), moves(user("JEFE_DE_AREA", "creador"), "emp", "EN_REVISION"))
    }

    @Test
    fun editingAndClosingFollowTheWeb() {
        val jefeCreadorOtraArea = user("JEFE_DE_AREA", "creador", departmentId = "mant")
        assertTrue(TicketPermissions.canEdit(jefeCreadorOtraArea, ticket))
        assertFalse(TicketPermissions.canClose(jefeCreadorOtraArea, ticket))

        val jefeDeLaArea = user("JEFE_DE_AREA", "jefe")
        assertFalse(TicketPermissions.canEdit(jefeDeLaArea, ticket))
        assertTrue(TicketPermissions.canClose(jefeDeLaArea, ticket))

        for (role in listOf("EMPLEADO", "GUARD")) {
            val creador = user(role, "creador", departmentId = null)
            assertFalse(TicketPermissions.canEdit(creador, ticket), role)
            assertFalse(TicketPermissions.canClose(creador, ticket), role)
        }
    }

    @Test
    fun evidenceGoesOnYourOwnTaskOrAsManager() {
        val emp = user("EMPLEADO", "emp")
        assertTrue(TicketPermissions.canUploadEvidence(emp, ticket, assignmentUserId = "emp"))
        assertFalse(TicketPermissions.canUploadEvidence(emp, ticket, assignmentUserId = "otro"))
        assertTrue(TicketPermissions.canUploadEvidence(user("GERENTE", "g"), ticket, assignmentUserId = "emp"))
    }
}
