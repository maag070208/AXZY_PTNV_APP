package com.axzydev.puertonuevoapp.core.network.http

import kotlinx.serialization.Serializable

/**
 * Contrato de tablas server-side (paginación/filtro/orden en el backend),
 * espejo de `web/src/shared/api/table.ts`. Todos los listados paginados del
 * API (`POST /<recurso>/query`) usan esta forma — no hand-rollees otra.
 */
@Serializable
data class TableRequest(
    val page: Int = 1,
    val limit: Int = 20,
    val filters: Map<String, String> = emptyMap(),
    val sort: TableSort? = null,
)

@Serializable
data class TableSort(
    val key: String,
    val direction: String = "asc",
)

@Serializable
data class TableResponse<T>(
    val data: List<T>,
    val total: Int,
)
