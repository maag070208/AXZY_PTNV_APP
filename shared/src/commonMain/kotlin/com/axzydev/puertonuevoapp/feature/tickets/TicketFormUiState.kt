package com.axzydev.puertonuevoapp.feature.tickets

data class TicketFormUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val title: String = "",
    val description: String = "",
    val priority: String = "MEDIUM",
    /** "" = sin categoría. */
    val categoryId: String = "",
    /** Categoría con la que se cargó el ticket (para mandarla solo si cambió). */
    val initialCategoryId: String = "",
    /** Opciones del catálogo (`id` → nombre), con "Sin categoría" primero. */
    val categoryOptions: List<Pair<String, String>> = listOf(NO_CATEGORY),
    val savedTicketId: String? = null,
) {
    val isValid: Boolean get() = title.isNotBlank() && description.isNotBlank()
}

val NO_CATEGORY: Pair<String, String> = "" to "Sin categoría"

val ticketPriorityOptions: List<Pair<String, String>> = listOf("LOW", "MEDIUM", "HIGH", "URGENT")
    .map { it to com.axzydev.puertonuevoapp.core.util.ticketPriorityLabel(it) }
