package com.axzydev.puertonuevoapp.core.network.notifications

import kotlinx.serialization.Serializable

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
    val total: Int = 0,
)

@Serializable
data class UnreadCountDto(val count: Int)
