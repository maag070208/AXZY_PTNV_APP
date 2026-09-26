package com.axzydev.puertonuevoapp.core.network.permissions

import kotlinx.serialization.Serializable

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
