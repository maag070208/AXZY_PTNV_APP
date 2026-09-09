package com.axzydev.puertonuevoapp.core.session

import com.axzydev.puertonuevoapp.core.network.ApiClient
import com.axzydev.puertonuevoapp.core.network.ApiConfig
import com.axzydev.puertonuevoapp.core.network.AuthApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Dueño de la sesión del usuario: restaura el token guardado al abrir la
 * app, hace login/logout contra el API y expone el estado actual para que
 * la UI decida qué mostrar (login vs. shell de la app).
 */
class AuthRepository(
    private val tokenStore: TokenStore,
    apiClient: ApiClient,
) {
    private val authApi = AuthApi(apiClient)

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun restoreSession() {
        val token = tokenStore.getToken()
        val id = tokenStore.getUserId()
        val username = tokenStore.getUsername()
        val name = tokenStore.getUserName()
        val role = tokenStore.getUserRole()
        _state.value = if (token != null && id != null && username != null && name != null && role != null) {
            AuthState.LoggedIn(SessionUser(id, username, name, role, tokenStore.getUserDepartmentId()))
        } else {
            AuthState.LoggedOut
        }
    }

    suspend fun login(username: String, password: String): Result<Unit> = runCatching {
        val response = authApi.login(username, password)
        tokenStore.saveToken(response.token)
        tokenStore.saveUser(
            id = response.user.id,
            username = response.user.username,
            name = response.user.name,
            role = response.user.role,
            departmentId = response.user.departmentId,
        )
        _state.value = AuthState.LoggedIn(
            SessionUser(
                id = response.user.id,
                username = response.user.username,
                name = response.user.name,
                role = response.user.role,
                departmentId = response.user.departmentId,
            )
        )
    }

    fun logout() {
        tokenStore.clear()
        _state.value = AuthState.LoggedOut
    }

    fun getServerUrl(): String = tokenStore.getServerUrl() ?: ApiConfig.DEFAULT_BASE_URL

    fun setServerUrl(url: String) {
        tokenStore.saveServerUrl(url)
    }
}
