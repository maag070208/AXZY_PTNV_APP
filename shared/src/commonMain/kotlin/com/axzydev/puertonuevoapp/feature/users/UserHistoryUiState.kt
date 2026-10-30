package com.axzydev.puertonuevoapp.feature.users

import com.axzydev.puertonuevoapp.core.network.users.UserDto
import com.axzydev.puertonuevoapp.core.network.users.UserHistoryEntryDto
import com.axzydev.puertonuevoapp.core.util.localDayKey
import com.axzydev.puertonuevoapp.core.util.parseIsoToEpochMillis

/**
 * Familia de un evento del historial: es lo que filtran las píldoras del feed
 * (como en la web). El API manda el `type` exacto; un tipo nuevo cae en [Other]
 * en vez de perderse.
 */
enum class HistoryFamily(val label: String) {
    All("Todo"),
    Equipment("Equipos"),
    Tickets("Tickets"),
    Account("Cuenta"),
    Other("Otros"),
}

/** Familia de un `type` de `GET /users/:id/history`. */
fun familyOf(type: String): HistoryFamily = when (type) {
    "LOAN_CUSTODIAN", "MOVEMENT" -> HistoryFamily.Equipment
    "TICKET_CREATED", "TICKET_ASSIGNED", "TICKET_COMMENT" -> HistoryFamily.Tickets
    "USER_DEACTIVATED", "USER_REACTIVATED" -> HistoryFamily.Account
    else -> HistoryFamily.Other
}

/**
 * Estado del historial de una cuenta: la persona, sus eventos y la familia
 * seleccionada en las píldoras.
 */
data class UserHistoryUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val user: UserDto? = null,
    val entries: List<UserHistoryEntryDto> = emptyList(),
    val family: HistoryFamily = HistoryFamily.All,
) {
    /** Eventos del más nuevo al más viejo (el API ya los manda así; se ordena por las dudas). */
    val sorted: List<UserHistoryEntryDto>
        get() = entries.sortedByDescending { parseIsoToEpochMillis(it.timestamp) ?: 0L }

    /** Eventos visibles con la familia elegida (la píldora "Todo" no filtra). */
    val visible: List<UserHistoryEntryDto>
        get() = if (family == HistoryFamily.All) sorted else sorted.filter { familyOf(it.type) == family }

    /**
     * Cuántos eventos hay de cada familia. `groupingBy` evita el choque de
     * nombres con `entries` del builder de un mapa.
     */
    val countsByFamily: Map<HistoryFamily, Int>
        get() = entries.groupingBy { familyOf(it.type) }.eachCount()

    val total: Int get() = entries.size

    /** Días locales distintos con actividad (mismo cálculo que la web). */
    val daysWithActivity: Int
        get() = entries.mapNotNull { localDayKey(it.timestamp) }.distinct().size
}
