package com.axzydev.puertonuevoapp.feature.custodyletters

import com.axzydev.puertonuevoapp.core.network.custodyletters.CustodyLetterDto

data class CustodyLettersListUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val custodyLetters: List<CustodyLetterDto> = emptyList(),
    val query: String = "",
    val deleteTarget: CustodyLetterDto? = null,
    val actionSaving: Boolean = false,
    val actionError: String? = null,
) {
    val filtered: List<CustodyLetterDto>
        get() = custodyLetters.filter { c ->
            query.isBlank() ||
                c.consecutive.contains(query, ignoreCase = true) ||
                c.employeeNumber.contains(query, ignoreCase = true) ||
                c.department.contains(query, ignoreCase = true) ||
                (c.items.firstOrNull()?.description ?: "").contains(query, ignoreCase = true)
        }
}
