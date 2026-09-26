package com.axzydev.puertonuevoapp.core.network.tickets

import com.axzydev.puertonuevoapp.core.network.common.DepartmentRefDto
import com.axzydev.puertonuevoapp.core.network.common.UserRefDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class TicketCommentDto(
    val id: String,
    val ticketId: String,
    val authorId: String,
    val author: UserRefDto,
    val text: String,
    val createdAt: String,
)

@Serializable
data class TicketHistoryEntryDto(
    val id: String,
    val ticketId: String,
    val type: String,
    val detail: String? = null,
    val author: UserRefDto? = null,
    val createdAt: String,
)

@Serializable
data class TicketAssignmentCommentDto(
    val id: String,
    val assignmentId: String,
    val authorId: String,
    val author: UserRefDto,
    val text: String,
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
data class TicketAttachmentDto(
    val id: String,
    val originalName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val kind: String,
    val createdAt: String,
    val uploadedById: String,
    val assignmentId: String? = null,
    val url: String,
)

/** Categoría del ticket: catálogo de la API (`/tickets/categories`). */
@Serializable
data class TicketCategoryRefDto(
    val id: String,
    val name: String,
    val active: Boolean = true,
)

@Serializable
data class TicketDto(
    val id: String,
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val categoryId: String? = null,
    val category: TicketCategoryRefDto? = null,
    val createdById: String,
    val createdBy: UserRefDto,
    val assignedToId: String? = null,
    val assignedTo: UserRefDto? = null,
    val departmentId: String? = null,
    val department: DepartmentRefDto? = null,
    val closedAt: String? = null,
    val closedBy: String? = null,
    val deletedAt: String? = null,
    val assignments: List<TicketAssignmentDto> = emptyList(),
    val attachments: List<TicketAttachmentDto> = emptyList(),
    val comments: List<TicketCommentDto> = emptyList(),
    val history: List<TicketHistoryEntryDto> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class TicketListResponseDto(
    val data: List<TicketDto>,
    val total: Int = 0,
)

@Serializable
data class TicketCreateInput(
    val title: String,
    val description: String,
    val priority: String? = null,
    val categoryId: String? = null,
    val departmentId: String? = null,
    val assignedToId: String? = null,
)

/**
 * PUT /tickets/{id} — parcial: solo se envían los campos que cambiaron.
 * [categoryId] es un `JsonElement` para poder mandar `null` explícito (quitar la
 * categoría): el cliente omite las propiedades en `null` (`explicitNulls = false`).
 */
@Serializable
data class TicketUpdateInput(
    val title: String? = null,
    val description: String? = null,
    val priority: String? = null,
    val categoryId: JsonElement? = null,
    val departmentId: String? = null,
    val assignedToId: String? = null,
    val status: String? = null,
)

@Serializable
data class TicketCommentInput(val text: String)

@Serializable
data class TicketRefDto(
    val id: String,
    val title: String,
    val status: String,
    val priority: String,
    val deletedAt: String? = null,
    /** Para decidir qué puede hacer el usuario con la tarea (ver `TicketPermissions`). */
    val createdById: String? = null,
    val assignedToId: String? = null,
    val departmentId: String? = null,
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
    val comments: List<TicketAssignmentCommentDto> = emptyList(),
    val ticket: TicketRefDto,
)

@Serializable
data class KanbanResponseDto(
    val data: List<KanbanAssignmentDto>,
    val total: Int = 0,
)

@Serializable
data class TicketAssignmentCreateInput(
    val userId: String,
    val title: String,
    val description: String? = null,
    val startDate: String? = null,
    val dueDate: String? = null,
)

@Serializable
data class TicketAssignmentUpdateInput(
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    val startDate: String? = null,
    val dueDate: String? = null,
)

@Serializable
data class TicketAssignmentCommentInput(val text: String)
