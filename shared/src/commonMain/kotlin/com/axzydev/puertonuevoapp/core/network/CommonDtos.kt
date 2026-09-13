package com.axzydev.puertonuevoapp.core.network

import kotlinx.serialization.Serializable

@Serializable
data class DepartmentRefDto(
    val id: String? = null,
    val name: String,
)

@Serializable
data class UserRefDto(
    val id: String,
    val name: String,
    val username: String,
    val puesto: String? = null,
    val numeroEmpleado: String? = null,
)

@Serializable
data class ApiErrorBody(
    val error: String? = null,
    val message: String? = null,
)

// DELETE /locations/:id (y otros borrados sin payload real): el backend responde
// `{ success: true }` (location.dto.ts: DeleteSuccessSchema). Parsear LocationDto
// aquí crasheaba con MissingFieldException al no existir `id` en la respuesta.
@Serializable
data class DeleteSuccessResponse(
    val success: Boolean,
)

// ---- Tablas server-side (ITDataTable) ----
// Espejo de `web/src/shared/api/table.ts`. El frontend POSTea
// { page, limit, filters, sort } y el backend responde { data, total }.
// API: `parseTableParams`/`ci`/`orderByOf` en `api/src/core/utils/table.ts`.
// Usa este contrato para los listados paginados; NO hand-rollees otro.

@Serializable
data class TableRequest(
    val page: Int = 1,
    val limit: Int = 20,
    val filters: Map<String, String> = emptyMap(),
    val sort: TableSort? = null,
)

@Serializable
data class TableSort(
    val key: String = "",
    val direction: String = "asc",
)

@Serializable
data class TableResponse<T>(
    val data: List<T>,
    val total: Int,
)

@Serializable
data class TableFilters(
    val search: String = "",
    val status: String? = null,
    val departmentId: String? = null,
    val deviceTypeId: String? = null,
)
