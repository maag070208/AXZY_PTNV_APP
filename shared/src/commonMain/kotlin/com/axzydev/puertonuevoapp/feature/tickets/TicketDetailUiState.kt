package com.axzydev.puertonuevoapp.feature.tickets

import com.axzydev.puertonuevoapp.core.network.tickets.TicketDto

data class TicketDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val ticket: TicketDto? = null,
    val updatingStatus: Boolean = false,
    val newComment: String = "",
    val sendingComment: Boolean = false,
)

val ticketStatusOptions: List<Pair<String, String>> = listOf(
    "OPEN" to "Abierto",
    "IN_PROGRESS" to "En seguimiento",
    "CLOSED" to "Cerrado",
)
