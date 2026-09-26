package com.axzydev.puertonuevoapp.core.network.permissions

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
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
}
