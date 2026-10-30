package com.axzydev.puertonuevoapp.feature.users

import com.axzydev.puertonuevoapp.core.network.departments.DepartmentDto
import com.axzydev.puertonuevoapp.core.network.users.UserDto
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.util.roleLabel

/** Totales reales de cuentas para la tira del mosaico (tres consultas de una fila). */
data class UsersCounts(
    val total: Int = 0,
    val active: Int = 0,
    val inactive: Int = 0,
)

/**
 * Estado propio de la lista de cuentas, separado de la tabla server-side
 * ([com.axzydev.puertonuevoapp.core.ui.table.TableUiState]): catálogo de
 * departamentos para el filtro, totales del mosaico y los diálogos de baja,
 * reactivación y contraseña.
 */
data class UsersListUiState(
    val counts: UsersCounts? = null,
    val departments: List<DepartmentDto> = emptyList(),
    /** Cuenta a la que se le pedirá el motivo de la baja. */
    val deactivateTarget: UserDto? = null,
    val deactivateReason: String = "",
    /** Cuenta inactiva que se va a reactivar (pide confirmación). */
    val reactivateTarget: UserDto? = null,
    /** Cuenta a la que se le cambiará la contraseña. */
    val passwordTarget: UserDto? = null,
    val newPassword: String = "",
    val saving: Boolean = false,
    val error: String? = null,
) {
    /** El API exige un motivo de al menos 3 caracteres. */
    val canConfirmDeactivate: Boolean get() = deactivateReason.trim().length >= 3 && !saving

    /** El API exige una contraseña de al menos 6 caracteres. */
    val canConfirmPassword: Boolean get() = newPassword.trim().length >= 6 && !saving
}

/**
 * Roles base del catálogo para el filtro de la tabla. Un rol nuevo (creado en
 * Roles y permisos) no aparece aquí pero sí se ve en su fila con su clave: la
 * app no consulta el catálogo dinámico para no bloquear la lista con
 * `roles.manage` (`GET /permissions/roles` requiere ese permiso).
 */
val userRoleFilterOptions: List<Pair<String, String>> = listOf(
    "ADMIN",
    "MANAGER",
    "AREA_HEAD",
    "HUMAN_RESOURCES",
    "CHEF",
    "GUARD",
    "EMPLOYEE",
).map { it to roleLabel(it) }

/** Tono bento del rol: mismo lenguaje que la consola de roles y el historial. */
internal fun roleTone(role: String): BentoTone = when (role) {
    "ADMIN" -> BentoTone.Rose
    "MANAGER" -> BentoTone.Sea
    "AREA_HEAD" -> BentoTone.Sun
    "HUMAN_RESOURCES" -> BentoTone.Lavender
    "GUARD" -> BentoTone.Plain
    "EMPLOYEE" -> BentoTone.Mint
    else -> BentoTone.Plain
}
