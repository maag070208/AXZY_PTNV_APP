package com.axzydev.puertonuevoapp.core.network.permissions

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Permiso del catálogo tal como lo expone la administración (incluye inactivos). */
@Serializable
data class PermissionCatalogDto(
    val key: String,
    val module: String,
    val name: String,
    val description: String? = null,
    /** Alcances que admite el permiso (subconjunto ordenado de NONE/OWN/AREA/ALL). */
    val scopes: List<String> = emptyList(),
    val sensitive: Boolean = false,
    val active: Boolean = true,
    val sortOrder: Int = 0,
)

/** Celda vigente de la matriz rol → permiso → alcance (solo las distintas de NONE). */
@Serializable
data class MatrixCellDto(
    val role: String,
    val permission: String,
    val scope: String,
)

/** `GET /permissions/admin`: roles, catálogo completo y matriz. */
@Serializable
data class RolesAdminDto(
    val roles: List<String> = emptyList(),
    val catalog: List<PermissionCatalogDto> = emptyList(),
    val matrix: List<MatrixCellDto> = emptyList(),
)

/** Cambio de una celda de la matriz. */
@Serializable
data class MatrixChangeDto(
    val role: String,
    val permission: String,
    val scope: String,
)

@Serializable
data class MatrixSaveInput(val changes: List<MatrixChangeDto>)

@Serializable
data class MatrixSaveResultDto(val updated: Int = 0)

/** Body de `POST /permissions/catalog`. */
@Serializable
data class PermissionCatalogCreateInput(
    val key: String,
    val module: String,
    val name: String,
    val description: String? = null,
    val scopes: List<String>,
    val sensitive: Boolean? = null,
    val sortOrder: Int? = null,
)

/** Body de `PATCH /permissions/catalog/:key` (parcial: lo nulo no viaja). */
@Serializable
data class PermissionCatalogUpdateInput(
    val module: String? = null,
    val name: String? = null,
    val description: String? = null,
    val scopes: List<String>? = null,
    val sensitive: Boolean? = null,
    val active: Boolean? = null,
    val sortOrder: Int? = null,
)

// ── Consola de roles (Fase 3C) ──────────────────────────────────────────────
// Contratos de `api/src/modules/permissions` (entities access/policy y
// `policy.service`): roles con conteo de personas, consola de acceso
// (personas, acceso efectivo, probador y actividad) y políticas ABAC.

/** Cuerpo vacío para los PUT que no llevan datos (p. ej. agregar una persona a un rol). */
@Serializable
class EmptyInput

/** Rol del sistema tal como lo expone `GET /permissions/roles`. */
@Serializable
data class RoleAdminDto(
    val key: String,
    val name: String,
    val description: String? = null,
    val module: String? = null,
    val staff: Boolean = false,
    val system: Boolean = false,
    val active: Boolean = true,
    val sortOrder: Int = 0,
    /** Cuántas cuentas tienen este rol (principal o adicional). */
    val userCount: Int = 0,
)

/** Alta de rol (`POST /permissions/roles`). Con [copyFrom] duplica la matriz del rol origen. */
@Serializable
data class RoleCreateInput(
    val key: String,
    val name: String,
    val description: String? = null,
    val module: String? = null,
    val staff: Boolean? = null,
    val sortOrder: Int? = null,
    val copyFrom: String? = null,
)

/** Edición parcial de rol (`PATCH /permissions/roles/:key`). */
@Serializable
data class RoleUpdateInput(
    val name: String? = null,
    val description: String? = null,
    val module: String? = null,
    val staff: Boolean? = null,
    val active: Boolean? = null,
    val sortOrder: Int? = null,
)

/** Persona tal como la ve la consola de roles (`GET /permissions/members`). */
@Serializable
data class AccessMemberDto(
    val id: String,
    val name: String,
    val username: String,
    val employeeNumber: String? = null,
    val jobTitle: String? = null,
    val department: String? = null,
    val active: Boolean = true,
    /** Rol principal (`users.role`). */
    val role: String,
    /** Roles adicionales (`user_roles`), sin el principal. */
    val extraRoles: List<String> = emptyList(),
    /** Excepciones de permiso vigentes. */
    val exceptions: Int = 0,
)

/** Excepción vigente de un permiso (reemplaza lo que dan los roles). */
@Serializable
data class AccessExceptionDto(
    val scope: String,
    val reason: String? = null,
    val expiresAt: String? = null,
)

/** Rol dentro del acceso de una persona. */
@Serializable
data class UserRoleRefDto(
    val key: String,
    val primary: Boolean = false,
    val active: Boolean = true,
)

/** Un permiso del catálogo activo con su alcance efectivo y su origen. */
@Serializable
data class AccessPermissionDto(
    val key: String,
    val effective: String,
    /** Alcance que aporta cada rol de la persona (solo ≠ NONE). */
    val byRole: Map<String, String> = emptyMap(),
    val exception: AccessExceptionDto? = null,
)

/** `GET /permissions/members/:userId/access`. */
@Serializable
data class UserAccessDto(
    val user: AccessMemberDto,
    val roles: List<UserRoleRefDto> = emptyList(),
    val permissions: List<AccessPermissionDto> = emptyList(),
)

/** Body de `POST /permissions/simulate`. */
@Serializable
data class SimulationInput(
    val userId: String,
    val permission: String,
    /** Campos del registro para las políticas ABAC (solo los que declara la acción). */
    val resource: Map<String, String>? = null,
)

/** Decisión del motor de políticas. */
@Serializable
data class SimulationDecisionDto(
    val allowed: Boolean = true,
    val code: String? = null,
    val reason: String? = null,
)

@Serializable
data class SimulationConditionTraceDto(
    val field: String = "",
    val operator: String = "",
    val value: String? = null,
    val actual: JsonElement? = null,
    val expected: JsonElement? = null,
    val matches: Boolean = false,
)

@Serializable
data class SimulationRuleTraceDto(
    val id: String = "",
    val key: String? = null,
    val name: String = "",
    val effect: String = "ALLOW",
    val priority: Int = 0,
    val active: Boolean = true,
    val roles: List<String> = emptyList(),
    val appliesToUser: Boolean = false,
    val conditions: List<SimulationConditionTraceDto> = emptyList(),
    val matches: Boolean = false,
)

@Serializable
data class SimulationExplanationDto(
    val decision: SimulationDecisionDto = SimulationDecisionDto(),
    val decidedBy: SimulationRuleTraceDto? = null,
    val rules: List<SimulationRuleTraceDto> = emptyList(),
)

@Serializable
data class SimulationIdentityDto(val ok: Boolean = false, val active: Boolean = false)

@Serializable
data class SimulationRbacDto(
    val ok: Boolean = false,
    val permissionActive: Boolean = false,
    val scope: String = "NONE",
    val byRole: Map<String, String> = emptyMap(),
    val exception: AccessExceptionDto? = null,
)

@Serializable
data class SimulationAbacDto(
    val evaluated: Boolean = false,
    val ok: Boolean = true,
    val hasRules: Boolean = false,
    val supportsContext: Boolean = false,
    val explanation: SimulationExplanationDto? = null,
)

/** Resultado del probador: las tres capas del control de acceso. */
@Serializable
data class SimulationDto(
    val allowed: Boolean = false,
    val identity: SimulationIdentityDto = SimulationIdentityDto(),
    val rbac: SimulationRbacDto = SimulationRbacDto(),
    val abac: SimulationAbacDto = SimulationAbacDto(),
)

/** Una condición de una política ABAC (`campo operador valor`). */
@Serializable
data class PolicyConditionDto(
    val field: String,
    val operator: String,
    val value: String? = null,
)

/** Política ABAC tal como la expone `GET /permissions/policies`. */
@Serializable
data class PolicyDto(
    val id: String,
    /** Clave de las políticas base (null en las creadas a mano; no se pueden borrar). */
    val key: String? = null,
    val name: String,
    val description: String? = null,
    val action: String,
    val effect: String = "DENY",
    val priority: Int = 100,
    val active: Boolean = true,
    val roles: List<String> = emptyList(),
    val conditions: List<PolicyConditionDto> = emptyList(),
    val createdAt: String = "",
)

/** Campo del registro que una acción declara para las condiciones. */
@Serializable
data class PolicyFieldDefDto(
    val key: String,
    val label: String,
    /** `string` | `number` | `boolean` | `enum` | `user`. */
    val type: String = "string",
    val options: List<String> = emptyList(),
)

/** Acción con políticas ABAC (clave de permiso). */
@Serializable
data class PolicyActionDefDto(
    val key: String,
    val label: String,
    val module: String = "",
    val fields: List<PolicyFieldDefDto> = emptyList(),
)

/** `GET /permissions/policies`: reglas + catálogo de acciones/condiciones. */
@Serializable
data class PoliciesResponseDto(
    val policies: List<PolicyDto> = emptyList(),
    val actions: List<PolicyActionDefDto> = emptyList(),
)

/** Alta de política (`POST /permissions/policies`). */
@Serializable
data class PolicyCreateInput(
    val name: String,
    val description: String? = null,
    val action: String,
    val effect: String,
    val priority: Int? = null,
    val active: Boolean? = null,
    val roles: List<String> = emptyList(),
    val conditions: List<PolicyConditionDto> = emptyList(),
)

/** Edición parcial de política (`PATCH /permissions/policies/:id`). */
@Serializable
data class PolicyUpdateInput(
    val name: String? = null,
    val description: String? = null,
    val action: String? = null,
    val effect: String? = null,
    val priority: Int? = null,
    val active: Boolean? = null,
    val roles: List<String>? = null,
    val conditions: List<PolicyConditionDto>? = null,
)

/** Persona que hizo un cambio en la bitácora del control de acceso. */
@Serializable
data class ActivityActorDto(
    val id: String? = null,
    val name: String? = null,
    val username: String? = null,
)

/** Renglón de `POST /permissions/activity/query` (tabla server-side). */
@Serializable
data class ActivityRowDto(
    val id: String,
    val createdAt: String = "",
    val action: String = "",
    val entityType: String = "",
    val entityId: String = "",
    val actor: ActivityActorDto = ActivityActorDto(),
    val previousState: JsonElement? = null,
    val newState: JsonElement? = null,
    val metadata: JsonElement? = null,
)
