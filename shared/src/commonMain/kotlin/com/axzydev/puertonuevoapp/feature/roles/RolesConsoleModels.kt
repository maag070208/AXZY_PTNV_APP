package com.axzydev.puertonuevoapp.feature.roles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.ui.graphics.vector.ImageVector
import com.axzydev.puertonuevoapp.core.network.permissions.PermissionCatalogDto
import com.axzydev.puertonuevoapp.core.network.permissions.RoleAdminDto
import com.axzydev.puertonuevoapp.core.session.PermissionScopes
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.util.roleLabel

/** Pestañas de la consola de roles y permisos (mismo orden que la web). */
enum class RolesConsoleTab(val label: String, val icon: ImageVector) {
    Roles("Roles", Icons.Filled.AdminPanelSettings),
    Matrix("Matriz", Icons.Filled.TableChart),
    Access("Acceso", Icons.Filled.Person),
    Policies("Reglas", Icons.Filled.VerifiedUser),
    Catalog("Catálogo", Icons.Filled.Category),
    Activity("Actividad", Icons.Filled.History),
}

/** Nivel de acceso de un rol (o persona) en un módulo, como en `access-levels.ts`. */
enum class ModuleLevel(val label: String, val tone: BentoTone) {
    Full("Completo", BentoTone.Mint),
    Partial("Parcial", BentoTone.Sea),
    Read("Solo lectura", BentoTone.Sun),
    None("Sin acceso", BentoTone.Plain),
}

/** Verbo de un permiso según su clave (`modulo.accion`). */
fun permissionVerb(key: String): String = key.substringAfterLast('.')

/**
 * Nivel de un módulo a partir de lo concedido (solo permisos activos):
 * todo → Completo; nada → Sin acceso; solo `*.view` → Solo lectura; si no, Parcial.
 */
fun moduleLevel(permissions: List<PermissionCatalogDto>, scopeOf: (String) -> String): ModuleLevel {
    val active = permissions.filter { it.active }
    val granted = active.filter { scopeOf(it.key) != PermissionScopes.NONE }
    if (active.isEmpty() || granted.isEmpty()) return ModuleLevel.None
    if (granted.size == active.size) return ModuleLevel.Full
    if (granted.all { permissionVerb(it.key) == "view" }) return ModuleLevel.Read
    return ModuleLevel.Partial
}

/** Concedidos / total de los permisos activos de un módulo. */
fun moduleCoverage(permissions: List<PermissionCatalogDto>, scopeOf: (String) -> String): Pair<Int, Int> {
    val active = permissions.filter { it.active }
    return active.count { scopeOf(it.key) != PermissionScopes.NONE } to active.size
}

/** Nombre visible de un rol (clave dinámica → el que manda el API). */
fun roleDisplayName(key: String, roles: List<RoleAdminDto>): String =
    roles.firstOrNull { it.key == key }?.name ?: roleLabel(key)

/** Etiqueta en español de cada acción de la bitácora del control de acceso. */
fun activityActionLabel(action: String): String = when (action) {
    "ROLE_CREATED" -> "Rol creado"
    "ROLE_UPDATED" -> "Rol actualizado"
    "ROLE_DELETED" -> "Rol eliminado"
    "ROLE_PERMISSIONS_UPDATED" -> "Matriz actualizada"
    "PERMISSION_CREATED" -> "Permiso creado"
    "PERMISSION_UPDATED" -> "Permiso actualizado"
    "POLICY_CREATED" -> "Regla creada"
    "POLICY_UPDATED" -> "Regla actualizada"
    "POLICY_DELETED" -> "Regla eliminada"
    "PERMISSION_EXCEPTION_SET" -> "Excepción otorgada"
    "PERMISSION_EXCEPTION_REMOVED" -> "Excepción retirada"
    "USER_ROLE_ADDED" -> "Rol adicional asignado"
    "USER_ROLE_REMOVED" -> "Rol adicional retirado"
    "ACCESS_DENIED" -> "Acceso denegado"
    else -> action
}

/** Operadores de condición con su etiqueta. */
val policyOperatorLabels: List<Pair<String, String>> = listOf(
    "eq" to "es igual a",
    "neq" to "es distinto de",
    "gt" to "es mayor que",
    "gte" to "es mayor o igual que",
    "lt" to "es menor que",
    "lte" to "es menor o igual que",
    "in" to "está en la lista",
    "not_in" to "no está en la lista",
    "is_empty" to "está vacío",
    "is_not_empty" to "no está vacío",
)

/** Operadores sin valor (comparan contra vacío). */
val operatorsWithoutValue: Set<String> = setOf("is_empty", "is_not_empty")

/** Operadores que tienen sentido para cada tipo de campo. */
fun operatorsForType(type: String): List<Pair<String, String>> {
    val keys = when (type) {
        "number" -> listOf("gt", "gte", "lt", "lte", "eq", "neq")
        "enum" -> listOf("eq", "neq")
        "user" -> listOf("eq", "neq", "is_empty", "is_not_empty")
        "string" -> listOf("eq", "neq", "in", "not_in", "is_empty", "is_not_empty")
        else -> listOf("eq", "neq")
    }
    return policyOperatorLabels.filter { it.first in keys }
}

/** Referencias al usuario que se pueden usar como valor en las condiciones. */
val policyUserReferences: List<Pair<String, String>> = listOf(
    "@user.id" to "El id de la persona",
    "@user.departmentId" to "Su departamento",
    "@user.role" to "Su rol principal",
)
