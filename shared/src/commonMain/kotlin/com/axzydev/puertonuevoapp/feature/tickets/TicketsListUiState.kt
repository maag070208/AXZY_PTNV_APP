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
                (query.isBlank() || t.title.contains(query, ignoreCase = true) || t.description.contains(query, ignoreCase = true))
        }
}

val ticketStatusFilters: List<Pair<String?, String>> = listOf(
    null to "Todos",
    "OPEN" to "Abiertos",
    "IN_PROGRESS" to "Seguimiento",
    "CLOSED" to "Cerrados",
)
