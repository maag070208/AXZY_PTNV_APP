package com.axzydev.puertonuevoapp.core.network

import kotlinx.serialization.Serializable

class NotificationsApi(private val client: ApiClient) {
    suspend fun list(unreadOnly: Boolean = false): List<NotificationDto> =
        client.get<NotificationsListResponseDto>("/notifications${if (unreadOnly) "?unread=true" else ""}").data

    suspend fun unreadCount(): Int = client.get<UnreadCountDto>("/notifications/unread-count").count

    suspend fun markRead(id: String) {
        client.postNoContent("/notifications/$id/read")
    }

    suspend fun markAllRead() {
        client.postNoContent("/notifications/read-all")
    }

    suspend fun remove(id: String) {
        client.deleteNoContent("/notifications/$id")
    }
}

@Serializable
data class NotificationDto(
    val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val detail: String? = null,
    val ticketId: String? = null,
    val read: Boolean = false,
    val createdAt: String,
)

@Serializable
data class NotificationsListResponseDto(
    val data: List<NotificationDto>,
    val total: Int,
)

@Serializable
data class UnreadCountDto(
    val count: Int,
)
