package com.axzydev.puertonuevoapp.feature.users

import com.axzydev.puertonuevoapp.core.network.users.UserDto
import com.axzydev.puertonuevoapp.core.network.users.UserHistoryEntryDto

data class UserHistoryUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val user: UserDto? = null,
    val history: List<UserHistoryEntryDto> = emptyList(),
)
