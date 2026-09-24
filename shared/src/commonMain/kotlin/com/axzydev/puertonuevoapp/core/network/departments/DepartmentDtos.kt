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
    val lugar: String,
    val descripcion: String? = null,
    val active: Boolean = true,
)

@Serializable
data class DepartmentTicketDto(
    val id: String,
    val titulo: String,
    val status: String,
    val priority: String,
    val category: com.axzydev.puertonuevoapp.core.network.tickets.TicketCategoryRefDto? = null,
    val creadoEn: String,
    val asignadoA: UserRefDto? = null,
)

@Serializable
data class DepartmentCartaDto(
    val id: String,
    val consecutive: String,
    val fecha: String,
    val returnDate: String? = null,
    val responsable: UserRefDto? = null,
    val encargado: UserRefDto? = null,
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
    val cartas: List<DepartmentCartaDto> = emptyList(),
    val cartasTotal: Int = 0,
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
