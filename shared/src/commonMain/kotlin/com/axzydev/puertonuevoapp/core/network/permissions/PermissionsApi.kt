package com.axzydev.puertonuevoapp.core.network.permissions

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import io.ktor.http.encodeURLPathPart

/**
 * Administración de roles y permisos (`/permissions`, espejo de
 * `web/src/entities/permission`). Todo salvo el catálogo activo requiere
 * `roles.manage`.
 */
class PermissionsApi(private val client: ApiClient) {
    /** Roles, catálogo completo (incluye inactivos) y matriz. */
    suspend fun admin(): RolesAdminDto = client.get("/permissions/admin")

    /** Catálogo activo, para resolver nombres de permiso (cualquier sesión). */
    suspend fun catalog(): List<PermissionCatalogDto> = client.get("/permissions/catalog")

    /** Aplica un lote de celdas de la matriz (máx. 500 por llamada). */
    suspend fun saveMatrix(changes: List<MatrixChangeDto>): MatrixSaveResultDto =
        client.put("/permissions/matrix", MatrixSaveInput(changes))

    /** Alta de un permiso del catálogo. */
    suspend fun createCatalog(input: PermissionCatalogCreateInput): PermissionCatalogDto =
        client.post("/permissions/catalog", input)

    /** Edita metadatos, alcances, sensibilidad, orden o activo de un permiso. */
    suspend fun updateCatalog(key: String, input: PermissionCatalogUpdateInput): PermissionCatalogDto =
        client.patch("/permissions/catalog/${key.encodeURLPathPart()}", input)

    // ── Consola de roles (Fase 3C) ──────────────────────────────────────────

    /** Roles con su conteo de personas (`GET /permissions/roles`, solo sesión). */
    suspend fun roles(): List<RoleAdminDto> = client.get("/permissions/roles")

    /** Alta de rol; con `copyFrom` duplica la matriz del rol origen en la misma transacción. */
    suspend fun createRole(input: RoleCreateInput): RoleAdminDto =
        client.post("/permissions/roles", input)

    suspend fun updateRole(key: String, input: RoleUpdateInput): RoleAdminDto =
        client.patch("/permissions/roles/${key.encodeURLPathPart()}", input)

    /** Elimina un rol (los `system` y los que tienen personas responden 409). */
    suspend fun deleteRole(key: String) =
        client.deleteNoContent("/permissions/roles/${key.encodeURLPathPart()}")

    /** Personas con su rol principal, roles adicionales y excepciones vigentes. */
    suspend fun members(): List<AccessMemberDto> = client.get("/permissions/members")

    /** Acceso efectivo de una persona, permiso por permiso, con su origen. */
    suspend fun memberAccess(userId: String): UserAccessDto =
        client.get("/permissions/members/${userId.encodeURLPathPart()}/access")

    /** Agrega el rol como **adicional** a la persona; el principal se cambia en su cuenta. */
    suspend fun addRoleMember(roleKey: String, userId: String): AccessMemberDto =
        client.put(
            "/permissions/roles/${roleKey.encodeURLPathPart()}/members/${userId.encodeURLPathPart()}",
            EmptyInput(),
        )

    /** Quita un rol adicional (el principal responde 409 `ROLE_IS_PRIMARY`). */
    suspend fun removeRoleMember(roleKey: String, userId: String): AccessMemberDto =
        client.delete(
            "/permissions/roles/${roleKey.encodeURLPathPart()}/members/${userId.encodeURLPathPart()}",
        )

    /** Políticas ABAC y el catálogo de acciones/campos que admiten condiciones. */
    suspend fun policies(): PoliciesResponseDto = client.get("/permissions/policies")

    suspend fun createPolicy(input: PolicyCreateInput): PolicyDto =
        client.post("/permissions/policies", input)

    suspend fun updatePolicy(id: String, input: PolicyUpdateInput): PolicyDto =
        client.patch("/permissions/policies/${id.encodeURLPathPart()}", input)

    /** Borra una política (las base con `key` responden 409 y no se borran). */
    suspend fun deletePolicy(id: String) =
        client.deleteNoContent("/permissions/policies/${id.encodeURLPathPart()}")

    /** Probador del control de acceso: identidad → RBAC → ABAC, con traza. */
    suspend fun simulate(input: SimulationInput): SimulationDto =
        client.post("/permissions/simulate", input)

    /** Bitácora del control de acceso (tabla server-side de `audit_logs`). */
    suspend fun activity(request: TableRequest): TableResponse<ActivityRowDto> =
        client.post("/permissions/activity/query", request)
}
