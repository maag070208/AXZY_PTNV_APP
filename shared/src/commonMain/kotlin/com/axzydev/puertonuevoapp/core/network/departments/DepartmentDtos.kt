package com.axzydev.puertonuevoapp.core.network.departments

import com.axzydev.puertonuevoapp.core.network.common.UserRefDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubareaDto(
    val id: String,
    val departmentId: String,
    val name: String,
    val active: Boolean = true,
)

@Serializable
data class DepartmentLocationDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val active: Boolean = true,
)

@Serializable
data class DepartmentTicketDto(
    val id: String,
    val title: String,
    val status: String,
    val priority: String,
    /** Nombre de la categoría (el API la manda aplanada como texto). */
    val category: String? = null,
    val createdAt: String,
    val assignedTo: UserRefDto? = null,
)

@Serializable
data class DepartmentCustodyLetterDto(
    val id: String,
    val consecutive: String,
    val date: String,
    val returnDate: String? = null,
    val custodian: UserRefDto? = null,
    val supervisor: UserRefDto? = null,
    val itemsCount: Int = 0,
)

@Serializable
data class DeptCountDto(val users: Int = 0)

@Serializable
data class DepartmentDto(
    val id: String,
    val name: String,
    val active: Boolean = true,
    val subareas: List<SubareaDto> = emptyList(),
    val locations: List<DepartmentLocationDto> = emptyList(),
    val tickets: List<DepartmentTicketDto> = emptyList(),
    val ticketsTotal: Int = 0,
    val custodyLetters: List<DepartmentCustodyLetterDto> = emptyList(),
    val custodyLettersTotal: Int = 0,
    @SerialName("_count") val count: DeptCountDto? = null,
)

@Serializable
data class DepartmentCreateInput(val name: String)

@Serializable
data class DepartmentUpdateInput(
    val name: String? = null,
    val active: Boolean? = null,
)

@Serializable
data class SubareaCreateInput(val name: String)

@Serializable
data class DepartmentDeleteResultDto(
    val soft: Boolean = false,
    val data: DepartmentDto? = null,
)

@Serializable
data class SubareaDeleteResultDto(
    val soft: Boolean = false,
    val data: SubareaDto? = null,
)

@Serializable
data class LocationRefIdInput(val locationId: String)
