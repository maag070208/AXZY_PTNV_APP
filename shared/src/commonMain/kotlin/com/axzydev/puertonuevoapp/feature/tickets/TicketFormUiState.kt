package com.axzydev.puertonuevoapp.feature.tickets

data class TicketFormUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val titulo: String = "",
    val descripcion: String = "",
    val priority: String = "MEDIA",
    /** "" = sin categoría. */
    val categoryId: String = "",
    /** Categoría con la que se cargó el ticket (para mandarla solo si cambió). */
    val initialCategoryId: String = "",
    /** Opciones del catálogo (`id` → nombre), con "Sin categoría" primero. */
    val categoryOptions: List<Pair<String, String>> = listOf(NO_CATEGORY),
    val savedTicketId: String? = null,
) {
    val isValid: Boolean get() = titulo.isNotBlank() && descripcion.isNotBlank()
}

val NO_CATEGORY: Pair<String, String> = "" to "Sin categoría"

val ticketPriorityOptions: List<Pair<String, String>> = listOf("BAJA", "MEDIA", "ALTA", "URGENTE")
    .map { it to com.axzydev.puertonuevoapp.core.util.ticketPriorityLabel(it) }
