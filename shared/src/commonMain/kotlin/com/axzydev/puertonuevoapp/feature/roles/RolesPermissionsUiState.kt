package com.axzydev.puertonuevoapp.feature.roles

import com.axzydev.puertonuevoapp.core.network.permissions.MatrixChangeDto
import com.axzydev.puertonuevoapp.core.network.permissions.PermissionCatalogDto
import com.axzydev.puertonuevoapp.core.network.permissions.RolesAdminDto
import com.axzydev.puertonuevoapp.core.session.PermissionScopes

/** Clave de una celda de la matriz. */
internal fun cellKey(role: String, permission: String): String = "$permission|$role"

/** Celdas persistidas (lo que no viene es NONE). */
internal fun RolesAdminDto.baseline(): Map<String, String> =
    matrix.associate { cellKey(it.role, it.permission) to it.scope }

/** Alcances que se pueden asignar a un permiso: siempre NONE + los que admite, en orden canónico. */
fun PermissionCatalogDto.assignableScopes(): List<String> {
    val allowed = scopes.toSet() + PermissionScopes.NONE
    return PermissionScopes.ORDER.filter { it in allowed }
}

/** Formulario de alta/edición de un permiso del catálogo. */
data class PermissionForm(
    /** Clave del permiso en edición; `null` = alta. */
    val editingKey: String? = null,
    val key: String = "",
    val module: String = "",
    val name: String = "",
    val description: String = "",
    val scopes: Set<String> = setOf(PermissionScopes.ALL),
    val sensitive: Boolean = false,
    val sortOrder: String = "0",
) {
    val isEdit: Boolean get() = editingKey != null

    /** Primer error de validación (mismas reglas que la web y el API), o `null`. */
    val error: String?
        get() = when {
            !isEdit && !PERMISSION_KEY_REGEX.matches(key.trim()) ->
                "La clave debe tener el formato modulo.accion (minúsculas, dígitos y guion bajo)"
            module.isBlank() -> "El módulo es obligatorio"
            name.isBlank() -> "El nombre es obligatorio"
            scopes.isEmpty() -> "Selecciona al menos un alcance"
            sortOrder.trim().toIntOrNull()?.takeIf { it >= 0 } == null ->
                "El orden debe ser un entero mayor o igual a 0"
            else -> null
        }

    /** Alcances seleccionados en orden canónico (el API los exige así). */
    val orderedScopes: List<String> get() = PermissionScopes.ORDER.filter { it in scopes }

    companion object {
        val PERMISSION_KEY_REGEX = Regex("^[a-z][a-z0-9_]*\\.[a-z][a-z0-9_]*$")

        fun from(permission: PermissionCatalogDto) = PermissionForm(
            editingKey = permission.key,
            key = permission.key,
            module = permission.module,
            name = permission.name,
            description = permission.description.orEmpty(),
            scopes = permission.scopes.toSet(),
            sensitive = permission.sensitive,
            sortOrder = permission.sortOrder.toString(),
        )
    }
}

data class RolesPermissionsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val tab: Int = 0,
    val data: RolesAdminDto? = null,
    /** Alcances persistidos por celda (`permiso|rol`). */
    val baseline: Map<String, String> = emptyMap(),
    /** Alcances en edición por celda (`permiso|rol`). */
    val draft: Map<String, String> = emptyMap(),
    val selectedRole: String? = null,
    val savingMatrix: Boolean = false,
    val catalogQuery: String = "",
    val form: PermissionForm? = null,
    val savingCatalog: Boolean = false,
    /** Permiso con activación/desactivación en curso. */
    val togglingKey: String? = null,
) {
    val roles: List<String> get() = data?.roles.orEmpty()
    val catalog: List<PermissionCatalogDto> get() = data?.catalog.orEmpty()

    fun scopeFor(role: String, permission: String): String =
        draft[cellKey(role, permission)] ?: PermissionScopes.NONE

    /** Cambios pendientes respecto a lo persistido (todas las celdas, de todos los roles). */
    val changes: List<MatrixChangeDto>
        get() = buildList {
            for (permission in catalog) {
                for (role in roles) {
                    val key = cellKey(role, permission.key)
                    val current = draft[key] ?: PermissionScopes.NONE
                    val original = baseline[key] ?: PermissionScopes.NONE
                    if (current != original) add(MatrixChangeDto(role, permission.key, current))
                }
            }
        }

    val dirty: Boolean get() = changes.isNotEmpty()

    /** Cambios pendientes por rol, para marcar los chips de rol. */
    val changesByRole: Map<String, Int> get() = changes.groupingBy { it.role }.eachCount()

    /** Catálogo agrupado por módulo, en el orden del API. */
    val modules: List<Pair<String, List<PermissionCatalogDto>>>
        get() = catalog.groupBy { it.module }.toList()

    val hasInactive: Boolean get() = catalog.any { !it.active }

    val filteredCatalog: List<PermissionCatalogDto>
        get() = catalogQuery.trim().takeIf { it.isNotEmpty() }?.let { q ->
            catalog.filter {
                it.key.contains(q, ignoreCase = true) ||
                    it.name.contains(q, ignoreCase = true) ||
                    it.module.contains(q, ignoreCase = true)
            }
        } ?: catalog
}
