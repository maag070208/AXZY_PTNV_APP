package com.axzydev.puertonuevoapp.core.session

import com.axzydev.puertonuevoapp.core.network.auth.AuthApi
import com.axzydev.puertonuevoapp.core.network.auth.AuthUserDto
import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.ApiConfig
import com.axzydev.puertonuevoapp.core.network.http.apiJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/**
 * Dueño de la sesión del usuario: restaura el token guardado al abrir la
 * app, hace login/logout contra el API y expone el estado actual para que
 * la UI decida qué mostrar (login vs. shell de la app).
 *
 * Los permisos efectivos solo los trae `GET /auth/me` (el login no), así que
 * se piden tras iniciar sesión y se refrescan al abrir la app: un cambio en la
 * matriz de roles aplica sin volver a iniciar sesión.
 */
class AuthRepository(
    private val tokenStore: TokenStore,
    apiClient: ApiClient,
) {
    private val authApi = AuthApi(apiClient)
    private val permissionsSerializer = MapSerializer(String.serializer(), String.serializer())

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun restoreSession() {
        val token = tokenStore.getToken()
        val id = tokenStore.getUserId()
        val username = tokenStore.getUsername()
        val name = tokenStore.getUserName()
        val role = tokenStore.getUserRole()
        _state.value = if (token != null && id != null && username != null && name != null && role != null) {
            AuthState.LoggedIn(
                SessionUser(id, username, name, role, tokenStore.getUserDepartmentId(), readPermissions())
            )
        } else {
            AuthState.LoggedOut
        }
    }

    /**
     * Relee `GET /auth/me` para traer los permisos y datos vigentes. Si falla
     * por red se conserva la sesión guardada; un 401 ya dispara el logout
     * global desde el `ApiClient`.
     */
    suspend fun refreshSession() {
        if (_state.value !is AuthState.LoggedIn) return
        try {
            val me = authApi.me()
            // Si la sesión se cerró mientras tanto, no revivirla.
            if (_state.value is AuthState.LoggedIn && tokenStore.getToken() != null) saveSession(me)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Sin red: se queda con los permisos persistidos.
        }
    }

    suspend fun login(username: String, password: String): Result<Unit> = runCatching {
        val response = authApi.login(username, password)
        tokenStore.saveToken(response.token)
        val me = try {
            authApi.me()
        } catch (e: Exception) {
            // Sin permisos no se puede armar la sesión: no dejar un token a medias.
            tokenStore.clear()
            throw e
        }
        saveSession(me)
    }

    fun logout() {
        tokenStore.clear()
        _state.value = AuthState.LoggedOut
    }

    fun getServerUrl(): String = tokenStore.getServerUrl() ?: ApiConfig.DEFAULT_BASE_URL

    fun setServerUrl(url: String) {
        tokenStore.saveServerUrl(url)
    }

    private fun saveSession(user: AuthUserDto) {
        val permissions = user.permissions.orEmpty()
        tokenStore.saveUser(
            id = user.id,
            username = user.username,
            name = user.name,
            role = user.role,
            departmentId = user.departmentId,
        )
        tokenStore.savePermissions(apiJson.encodeToString(permissionsSerializer, permissions))
        _state.value = AuthState.LoggedIn(
            SessionUser(
                id = user.id,
                username = user.username,
                name = user.name,
                role = user.role,
                departmentId = user.departmentId,
                permissions = permissions,
            )
        )
    }

    private fun readPermissions(): Map<String, String> =
        tokenStore.getPermissions()
            ?.let { runCatching { apiJson.decodeFromString(permissionsSerializer, it) }.getOrNull() }
            .orEmpty()
}
