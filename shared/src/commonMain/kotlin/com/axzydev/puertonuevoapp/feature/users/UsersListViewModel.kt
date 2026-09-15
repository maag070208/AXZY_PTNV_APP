package com.axzydev.puertonuevoapp.feature.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.users.UserDto
import com.axzydev.puertonuevoapp.core.network.users.UserUpdateInput
import com.axzydev.puertonuevoapp.core.network.users.UsersApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UsersListViewModel(
    private val usersApi: UsersApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsersListUiState())
    val uiState: StateFlow<UsersListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { usersApi.list() }
            _uiState.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let(::networkMessage),
                    users = result.getOrDefault(it.users),
                )
            }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }
    fun onRoleFilterChange(value: String?) = _uiState.update { it.copy(roleFilter = value) }

    fun requestToggleActive(user: UserDto) = _uiState.update { it.copy(selectedForDelete = user) }
    fun dismissToggleActive() = _uiState.update { it.copy(selectedForDelete = null) }

    fun confirmToggleActive() {
        val user = _uiState.value.selectedForDelete ?: return
        _uiState.update { it.copy(actionSaving = true) }
        viewModelScope.launch {
            val result = runCatching {
                if (user.active) usersApi.delete(user.id) else usersApi.update(user.id, UserUpdateInput(active = true))
            }
            _uiState.update {
                it.copy(
                    actionSaving = false,
                    selectedForDelete = null,
                    actionError = result.exceptionOrNull()?.let(::networkMessage),
                )
            }
            if (result.isSuccess) load()
        }
    }

    fun requestChangePassword(user: UserDto) = _uiState.update { it.copy(selectedForPassword = user, newPassword = "") }
    fun dismissChangePassword() = _uiState.update { it.copy(selectedForPassword = null) }
    fun onNewPasswordChange(value: String) = _uiState.update { it.copy(newPassword = value) }

    fun confirmChangePassword() {
        val user = _uiState.value.selectedForPassword ?: return
        val password = _uiState.value.newPassword
        if (password.isBlank()) return
        _uiState.update { it.copy(actionSaving = true) }
        viewModelScope.launch {
            val result = runCatching { usersApi.changePassword(user.id, password) }
            _uiState.update {
                it.copy(
                    actionSaving = false,
                    selectedForPassword = null,
                    actionError = result.exceptionOrNull()?.let(::networkMessage),
                )
            }
        }
    }

    fun dismissActionError() = _uiState.update { it.copy(actionError = null) }
}
