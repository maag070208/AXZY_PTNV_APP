package com.axzydev.puertonuevoapp.feature.salidas

import com.axzydev.puertonuevoapp.core.network.salidas.SalidaDto

data class SalidasListUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val salidas: List<SalidaDto> = emptyList(),
    val query: String = "",
    val deleteTarget: SalidaDto? = null,
    val actionSaving: Boolean = false,
    val actionError: String? = null,
) {
    val filtered: List<SalidaDto>
        get() = salidas.filter { s ->
            query.isBlank() ||
                s.descripcion.contains(query, ignoreCase = true) ||
                s.departamento.contains(query, ignoreCase = true) ||
                s.usuario.contains(query, ignoreCase = true) ||
                s.proyecto.orEmpty().contains(query, ignoreCase = true) ||
                s.marca.orEmpty().contains(query, ignoreCase = true) ||
                s.modelo.orEmpty().contains(query, ignoreCase = true)
        }
}
