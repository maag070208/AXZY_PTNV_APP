package com.axzydev.puertonuevoapp.core.network.notifications

import com.axzydev.puertonuevoapp.core.network.http.ApiClient

class NotificationsApi(private val client: ApiClient) {
    suspend fun list(unreadOnly: Boolean = false): List<NotificationDto> =
        client.get<NotificationsListResponseDto>("/notifications${if (unreadOnly) "?unread=true" else ""}").data

    suspend fun unreadCount(): Int = client.get<UnreadCountDto>("/notifications/unread-count").count

    suspend fun markRead(id: String) = client.postNoContent("/notifications/$id/read")

    suspend fun markAllRead() = client.postNoContent("/notifications/read-all")

    suspend fun remove(id: String) = client.deleteNoContent("/notifications/$id")
}
