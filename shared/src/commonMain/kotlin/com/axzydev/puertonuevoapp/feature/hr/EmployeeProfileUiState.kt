package com.axzydev.puertonuevoapp.feature.hr

import com.axzydev.puertonuevoapp.core.network.hr.DocumentTypeDto
import com.axzydev.puertonuevoapp.core.network.hr.EmployeeDocumentDto
import com.axzydev.puertonuevoapp.core.network.hr.EmployeeProfileDto

data class EmployeeProfileUiState(
    val loading: Boolean = true,
    val profile: EmployeeProfileDto? = null,
    val documents: List<EmployeeDocumentDto> = emptyList(),
    val documentTypes: List<DocumentTypeDto> = emptyList(),
    val error: String? = null,
)