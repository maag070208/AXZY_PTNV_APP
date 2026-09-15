package com.axzydev.puertonuevoapp.feature.cartas

import com.axzydev.puertonuevoapp.core.network.cartas.CartaDto

data class CartasListUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val cartas: List<CartaDto> = emptyList(),
    val query: String = "",
    val deleteTarget: CartaDto? = null,
    val actionSaving: Boolean = false,
    val actionError: String? = null,
) {
    val filtered: List<CartaDto>
        get() = cartas.filter { c ->
            query.isBlank() ||
                c.consecutivo.contains(query, ignoreCase = true) ||
                c.numeroEmpleado.contains(query, ignoreCase = true) ||
                c.departamento.contains(query, ignoreCase = true) ||
                (c.items.firstOrNull()?.descripcion ?: "").contains(query, ignoreCase = true)
        }
}
