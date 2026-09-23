package com.axzydev.puertonuevoapp.feature.access

import com.axzydev.puertonuevoapp.core.network.access.AccessEventDto

data class AccessLogUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val events: List<AccessEventDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
) {
    val limit: Int get() = 20
    val totalPages: Int get() = if (total == 0) 1 else ((total - 1) / limit) + 1
}
