package com.axzydev.puertonuevoapp.core.session

import com.axzydev.puertonuevoapp.core.network.auth.AuthUserDto
import com.axzydev.puertonuevoapp.core.network.http.apiJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Permisos efectivos de la sesión (`GET /auth/me`) y alcance por registro. */
class SessionUserTest {

    private fun user(permissions: Map<String, String>, departmentId: String? = "rec") =
        SessionUser(id = "u1", username = "u1", name = "U", role = "X", departmentId = departmentId, permissions = permissions)

    @Test
    fun missingPermissionIsNone() {
        val u = user(mapOf("tickets.view" to "OWN"))
        assertEquals("OWN", u.scopeOf("tickets.view"))
        assertEquals("NONE", u.scopeOf("roles.manage"))
        assertTrue(u.can("tickets.view"))
        assertFalse(u.can("roles.manage"))
        assertFalse(u.canManageRoles)
    }

    @Test
    fun withinScopeFollowsTheApi() {
        val own = user(mapOf("p" to "OWN"))
        assertTrue(own.withinScope("p", createdById = "u1", assignedToId = null, departmentId = "x"))
        assertTrue(own.withinScope("p", createdById = "o", assignedToId = null, departmentId = "x", assignmentUserIds = listOf("u1")))
        assertFalse(own.withinScope("p", createdById = "o", assignedToId = null, departmentId = "rec"))

        val area = user(mapOf("p" to "AREA"))
        assertTrue(area.withinScope("p", createdById = "o", assignedToId = null, departmentId = "rec"))
        assertFalse(area.withinScope("p", createdById = "o", assignedToId = null, departmentId = "mant"))
        // Sin departamento, AREA se comporta como OWN.
        assertFalse(user(mapOf("p" to "AREA"), departmentId = null).withinScope("p", "o", null, null))

        assertTrue(user(mapOf("p" to "ALL")).withinScope("p", "o", null, "mant"))
        assertFalse(user(emptyMap()).withinScope("p", "u1", null, "rec"))
    }

    @Test
    fun accessLogFallsBackToOwnScans() {
        val guard = user(mapOf("access.scan" to "ALL"))
        assertTrue(guard.canViewAccessLog)
        assertFalse(guard.canQueryAccessLog)
        assertFalse(user(emptyMap()).canViewAccessLog)
    }

    @Test
    fun authMeDecodesPermissions() {
        val me = apiJson.decodeFromString(
            AuthUserDto.serializer(),
            """{"id":"u1","username":"a","name":"A","role":"MANAGER","permissions":{"tickets.view":"AREA","roles.manage":"ALL"},"language":"es"}""",
        )
        assertEquals(mapOf("tickets.view" to "AREA", "roles.manage" to "ALL"), me.permissions)
        assertEquals("es", me.language)
        // El login no trae permisos.
        val login = apiJson.decodeFromString(AuthUserDto.serializer(), """{"id":"u1","username":"a","name":"A","role":"MANAGER"}""")
        assertEquals(null, login.permissions)
    }
}
