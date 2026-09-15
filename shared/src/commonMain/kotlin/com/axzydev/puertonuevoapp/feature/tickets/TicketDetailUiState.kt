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
    "ABIERTO" to "Abierto",
    "EN_SEGUIMIENTO" to "En seguimiento",
    "CERRADO" to "Cerrado",
)
