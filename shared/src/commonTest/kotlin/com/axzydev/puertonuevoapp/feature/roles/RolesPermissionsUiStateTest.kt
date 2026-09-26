package com.axzydev.puertonuevoapp.feature.roles

import com.axzydev.puertonuevoapp.core.network.permissions.MatrixCellDto
import com.axzydev.puertonuevoapp.core.network.permissions.MatrixChangeDto
import com.axzydev.puertonuevoapp.core.network.permissions.PermissionCatalogDto
import com.axzydev.puertonuevoapp.core.network.permissions.RolesAdminDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Matriz de roles (borrador y diff) y validación del formulario del catálogo. */
class RolesPermissionsUiStateTest {

    private val data = RolesAdminDto(
        roles = listOf("ADMIN", "EMPLOYEE"),
        catalog = listOf(
            PermissionCatalogDto(key = "tickets.view", module = "Tickets", name = "Ver tickets", scopes = listOf("OWN", "AREA", "ALL")),
            PermissionCatalogDto(key = "roles.manage", module = "Sistema", name = "Roles", scopes = listOf("NONE", "ALL"), sensitive = true),
        ),
        matrix = listOf(
            MatrixCellDto("ADMIN", "tickets.view", "ALL"),
            MatrixCellDto("ADMIN", "roles.manage", "ALL"),
            MatrixCellDto("EMPLOYEE", "tickets.view", "OWN"),
        ),
    )

    private fun state(): RolesPermissionsUiState {
        val baseline = data.baseline()
        return RolesPermissionsUiState(loading = false, data = data, baseline = baseline, draft = baseline, selectedRole = "ADMIN")
    }

    @Test
    fun noChangesUntilACellMoves() {
        val s = state()
        assertTrue(s.changes.isEmpty())
        assertEquals("NONE", s.scopeFor("EMPLOYEE", "roles.manage"))

        val edited = s.copy(draft = s.draft + (cellKey("EMPLOYEE", "tickets.view") to "AREA"))
        assertEquals(listOf(MatrixChangeDto("EMPLOYEE", "tickets.view", "AREA")), edited.changes)
        assertEquals(mapOf("EMPLOYEE" to 1), edited.changesByRole)

        // Volver al valor original no deja cambio pendiente; NONE explícito = ausente.
        val back = edited.copy(draft = edited.draft + (cellKey("EMPLOYEE", "tickets.view") to "OWN") + (cellKey("EMPLOYEE", "roles.manage") to "NONE"))
        assertTrue(back.changes.isEmpty())
    }

    @Test
    fun assignableScopesAlwaysIncludeNoneInOrder() {
        assertEquals(listOf("NONE", "OWN", "AREA", "ALL"), data.catalog[0].assignableScopes())
        assertEquals(listOf("NONE", "ALL"), data.catalog[1].assignableScopes())
    }

    @Test
    fun formValidationMatchesTheApi() {
        assertNotNull(PermissionForm(key = "Tickets.Ver", module = "M", name = "N").error)
        assertNotNull(PermissionForm(key = "tickets.view", module = "", name = "N").error)
        assertNotNull(PermissionForm(key = "tickets.view", module = "M", name = "N", scopes = emptySet()).error)
        assertNotNull(PermissionForm(key = "tickets.view", module = "M", name = "N", sortOrder = "").error)
        assertNull(PermissionForm(key = "tickets.view_all", module = "M", name = "N").error)
        // En edición la clave no se valida (no se puede cambiar).
        assertNull(PermissionForm.from(data.catalog[1]).copy(key = "X").error)
        assertEquals(listOf("OWN", "ALL"), PermissionForm(scopes = setOf("ALL", "OWN")).orderedScopes)
    }
}
