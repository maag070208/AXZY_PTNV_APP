package com.axzydev.puertonuevoapp.feature.materialoutputs

import com.axzydev.puertonuevoapp.core.network.materialoutputs.MaterialOutputDto

data class MaterialOutputsListUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val materialOutputs: List<MaterialOutputDto> = emptyList(),
    val query: String = "",
    val deleteTarget: MaterialOutputDto? = null,
    val actionSaving: Boolean = false,
    val actionError: String? = null,
) {
    val filtered: List<MaterialOutputDto>
        get() = materialOutputs.filter { s ->
            query.isBlank() ||
                s.description.contains(query, ignoreCase = true) ||
                s.departmentName.contains(query, ignoreCase = true) ||
                s.userName.contains(query, ignoreCase = true) ||
                s.project.orEmpty().contains(query, ignoreCase = true) ||
                s.brand.orEmpty().contains(query, ignoreCase = true) ||
                s.model.orEmpty().contains(query, ignoreCase = true)
        }
}
