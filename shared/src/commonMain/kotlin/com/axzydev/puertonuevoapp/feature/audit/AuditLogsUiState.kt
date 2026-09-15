package com.axzydev.puertonuevoapp.feature.audit

import com.axzydev.puertonuevoapp.core.network.audit.AuditLogDto

data class AuditLogsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val logs: List<AuditLogDto> = emptyList(),
    val filterAction: String = "",
    val filterStart: String = "",
    val filterEnd: String = "",
    val page: Int = 1,
    val total: Int = 0,
) {
    val limit: Int get() = 20
    val totalPages: Int get() = if (total == 0) 1 else ((total - 1) / limit) + 1
}
