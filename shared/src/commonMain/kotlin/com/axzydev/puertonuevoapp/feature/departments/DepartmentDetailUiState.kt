package com.axzydev.puertonuevoapp.feature.departments

import com.axzydev.puertonuevoapp.core.network.departments.DepartmentDto
import com.axzydev.puertonuevoapp.core.network.departments.SubareaDto

data class DepartmentDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val department: DepartmentDto? = null,
    val newSubarea: String = "",
    val subareaToDelete: SubareaDto? = null,
    val showDeleteDept: Boolean = false,
    val saving: Boolean = false,
    val deleted: Boolean = false,
)
