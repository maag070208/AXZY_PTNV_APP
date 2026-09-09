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
