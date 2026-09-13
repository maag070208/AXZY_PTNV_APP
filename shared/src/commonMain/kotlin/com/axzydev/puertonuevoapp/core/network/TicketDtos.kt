package com.axzydev.puertonuevoapp.core.network

import kotlinx.serialization.Serializable

@Serializable
data class TicketCommentDto(
    val id: String,
    val ticketId: String,
    val autorId: String,
    val autor: UserRefDto,
    val texto: String,
    val creadoEn: String,
)

@Serializable
data class TicketHistoryEntryDto(
    val id: String,
    val ticketId: String,
    val type: String,
    val detail: String? = null,
    val autor: UserRefDto? = null,
    val createdAt: String,
)

@Serializable
data class TicketDto(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val status: String,
    val priority: String,
    val category: String,
    val creadoPorId: String,
    val creadoPor: UserRefDto,
    val asignadoAId: String? = null,
    val asignadoA: UserRefDto? = null,
    val departmentId: String? = null,
    val department: DepartmentRefDto? = null,
    val closedAt: String? = null,
    val deletedAt: String? = null,
    val assignments: List<TicketAssignmentDto> = emptyList(),
    val comments: List<TicketCommentDto> = emptyList(),
    val history: List<TicketHistoryEntryDto> = emptyList(),
    val creadoEn: String,
    val actualizadoEn: String,
)

@Serializable
data class TicketListResponseDto(
    val data: List<TicketDto>,
    val total: Int,
)

@Serializable
data class TicketInputDto(
    val titulo: String,
    val descripcion: String,
    val priority: String? = null,
    val category: String? = null,
    val departmentId: String? = null,
)

@Serializable
data class TicketUpdateDto(
    val status: String? = null,
    val priority: String? = null,
    val asignadoAId: String? = null,
)

@Serializable
data class AddCommentDto(
    val texto: String,
)


@Serializable
data class TicketAssignmentCommentDto(
    val id: String,
    val assignmentId: String,
    val autorId: String,
    val autor: UserRefDto,
    val texto: String,
    val createdAt: String,
)

@Serializable
data class TicketAssignmentDto(
    val id: String,
    val ticketId: String,
    val userId: String,
    val user: UserRefDto,
    val title: String,
    val description: String,
    val startDate: String? = null,
    val dueDate: String? = null,
    val status: String,
    val comments: List<TicketAssignmentCommentDto> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class TicketRefDto(
    val id: String,
    val titulo: String,
    val status: String,
    val priority: String,
    val deletedAt: String? = null,
    val department: DepartmentRefDto? = null,
)

@Serializable
data class KanbanAssignmentDto(
    val id: String,
    val ticketId: String,
    val userId: String,
    val user: UserRefDto,
    val title: String,
    val description: String,
    val startDate: String? = null,
    val dueDate: String? = null,
    val status: String,
    val createdAt: String,
    val ticket: TicketRefDto,
)

@Serializable
data class KanbanResponseDto(
    val data: List<KanbanAssignmentDto>,
    val total: Int,
)

@Serializable
data class AssignmentCreateDto(
    val userId: String,
    val title: String,
    val description: String? = null,
    val startDate: String? = null,
    val dueDate: String? = null,
)

@Serializable
data class AssignmentUpdateDto(
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    val startDate: String? = null,
    val dueDate: String? = null,
)

@Serializable
data class TicketEditDto(
    val titulo: String? = null,
    val descripcion: String? = null,
    val priority: String? = null,
    val category: String? = null,
    val departmentId: String? = null,
)
