package com.axzydev.puertonuevoapp.feature.tickets

data class TicketFormUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val titulo: String = "",
    val descripcion: String = "",
    val priority: String = "MEDIA",
    val category: String = "OTRO",
    val savedTicketId: String? = null,
) {
    val isValid: Boolean get() = titulo.isNotBlank() && descripcion.isNotBlank()
}

val ticketPriorityOptions: List<Pair<String, String>> = listOf("BAJA", "MEDIA", "ALTA", "URGENTE")
    .map { it to com.axzydev.puertonuevoapp.core.util.ticketPriorityLabel(it) }

val ticketCategoryOptions: List<Pair<String, String>> = listOf("MANTENIMIENTO", "EQUIPO", "SISTEMA", "OTRO")
    .map { it to com.axzydev.puertonuevoapp.core.util.ticketCategoryLabel(it) }
