package com.axzydev.puertonuevoapp.feature.notifications

import com.axzydev.puertonuevoapp.core.network.notifications.NotificationDto

data class NotificationsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val notifications: List<NotificationDto> = emptyList(),
) {
    val unreadCount: Int get() = notifications.count { !it.read }
}
