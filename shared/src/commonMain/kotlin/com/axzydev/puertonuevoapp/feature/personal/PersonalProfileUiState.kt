package com.axzydev.puertonuevoapp.feature.personal

import com.axzydev.puertonuevoapp.core.network.personal.DocumentTypeDto
import com.axzydev.puertonuevoapp.core.network.personal.EmployeeDocumentDto
import com.axzydev.puertonuevoapp.core.network.personal.PersonalProfileDto

data class PersonalProfileUiState(
    val loading: Boolean = true,
    val profile: PersonalProfileDto? = null,
    val documents: List<EmployeeDocumentDto> = emptyList(),
    val documentTypes: List<DocumentTypeDto> = emptyList(),
    val error: String? = null,
)