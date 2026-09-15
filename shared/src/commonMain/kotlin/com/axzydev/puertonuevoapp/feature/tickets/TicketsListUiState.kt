package com.axzydev.puertonuevoapp.feature.tickets

import com.axzydev.puertonuevoapp.core.network.tickets.TicketDto

data class TicketsListUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val tickets: List<TicketDto> = emptyList(),
    val query: String = "",
    val statusFilter: String? = null,
) {
    val filtered: List<TicketDto>
        get() = tickets.filter { t ->
            (statusFilter == null || t.status == statusFilter) &&
                (query.isBlank() || t.titulo.contains(query, ignoreCase = true) || t.descripcion.contains(query, ignoreCase = true))
        }
}

val ticketStatusFilters: List<Pair<String?, String>> = listOf(
    null to "Todos",
    "ABIERTO" to "Abiertos",
    "EN_SEGUIMIENTO" to "Seguimiento",
    "CERRADO" to "Cerrados",
)
