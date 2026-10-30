package com.axzydev.puertonuevoapp.feature.tickets

import com.axzydev.puertonuevoapp.core.network.users.UserDto

/**
 * Estado del formulario de ticket ([ticketId] null en el ViewModel = alta).
 *
 * El bloque "A nombre de" solo se activa en el alta y para ADMIN
 * ([canCaptureForOthers]): el API (`POST /tickets` con `onBehalfOfId`) deja el
 * ticket creado por esa persona, le manda la notificación y guarda la traza de
 * quién lo capturó.
 */
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

    // ── "A nombre de" (alta de ADMIN) ─────────────────────────────────────

    /** Puede levantar el ticket a nombre de otra persona (ADMIN, y solo en el alta). */
    val canCaptureForOthers: Boolean = false,
    /** Cuenta en sesión y su nombre, para pintar "Yo (nombre)". */
    val currentUserId: String = "",
    val currentUserName: String = "",
    /** "" = a mi nombre; si no, la cuenta a nombre de quien se captura. */
    val requesterId: String = "",
    val requesterName: String = "",
    /** Empleados activos para el selector (`GET /users/employees`). */
    val employees: List<UserDto> = emptyList(),
    val loadingEmployees: Boolean = false,
    val employeesError: String? = null,
    val showRequesterPicker: Boolean = false,
    /** Búsqueda dentro del selector. */
    val employeeQuery: String = "",
) {
    val isValid: Boolean get() = title.isNotBlank() && description.isNotBlank()

    /** Se está capturando a nombre de otra persona (no la cuenta en sesión). */
    val isOnBehalf: Boolean get() = requesterId.isNotBlank() && requesterId != currentUserId

    /** Texto del campo: "Yo (nombre)" o la persona elegida. */
    val requesterLabel: String
        get() = when {
            isOnBehalf -> requesterName.ifBlank { "Otra persona" }
            currentUserName.isNotBlank() -> "Yo ($currentUserName)"
            else -> "Yo"
        }

    /**
     * Empleados activos (sin la cuenta en sesión: para eso está "A mi nombre")
     * que coinciden con la búsqueda por nombre, usuario o número.
     */
    val filteredEmployees: List<UserDto>
        get() {
            val query = employeeQuery.trim().lowercase()
            val candidates = employees.filter { it.active && it.id != currentUserId }
            val matched = if (query.isEmpty()) {
                candidates
            } else {
                candidates.filter { user ->
                    user.name.lowercase().contains(query) ||
                        user.username.lowercase().contains(query) ||
                        user.employeeNumber?.lowercase()?.contains(query) == true
                }
            }
            return matched.sortedBy { it.name.lowercase() }
        }
}

val NO_CATEGORY: Pair<String, String> = "" to "Sin categoría"

val ticketPriorityOptions: List<Pair<String, String>> = listOf("LOW", "MEDIUM", "HIGH", "URGENT")
    .map { it to com.axzydev.puertonuevoapp.core.util.ticketPriorityLabel(it) }
