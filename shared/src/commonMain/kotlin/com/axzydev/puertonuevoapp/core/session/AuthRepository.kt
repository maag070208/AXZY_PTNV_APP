package com.axzydev.puertonuevoapp.core.session

import com.axzydev.puertonuevoapp.core.network.auth.AuthApi
import com.axzydev.puertonuevoapp.core.network.auth.AuthUserDto
import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.ApiConfig
import com.axzydev.puertonuevoapp.core.network.http.apiJson
import com.axzydev.puertonuevoapp.core.util.currentTimeMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.io.encoding.Base64

/** Margen para renovar el access token antes de que venza (evita un 401 a media navegación). */
private const val REFRESH_WINDOW_SECONDS = 60L

/**
 * Dueño de la sesión del usuario: restaura la sesión guardada al abrir la
 * app, hace login/logout contra el API, renueva el access token con el
 * refresh token (`POST /auth/refresh`) y expone el estado actual para que la
 * UI decida qué mostrar (login vs. shell de la app).
 *
 * Los permisos efectivos solo los trae `GET /auth/me`, así que se piden tras
 * iniciar sesión y se refrescan al abrir la app: un cambio en la matriz de
 * roles aplica sin volver a iniciar sesión.
 *
 * **Sesión endurecida (paridad con la web):**
 * - El login guarda `token` + `refreshToken`; el API rota el refresh en cada
 *   `POST /auth/refresh`.
 * - Al arrancar, [ensureFreshSession] renueva el access token si está vencido
 *   (o a punto de vencer) y después relee `/auth/me`; si el refresh ya no es
 *   válido el API responde 401 y el `ApiClient` dispara el logout global.
 * - `GET /auth/me` trae `mustChangePassword` y `roles`; ambos viven en
 *   [SessionUser] para que la UI muestre `ChangePasswordScreen` mientras la
 *   contraseña sea provisional.
 */
class AuthRepository(
    private val tokenStore: TokenStore,
    apiClient: ApiClient,
) {
    private val authApi = AuthApi(apiClient)
    private val permissionsSerializer = MapSerializer(String.serializer(), String.serializer())
    private val rolesSerializer = ListSerializer(String.serializer())

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
                SessionUser(
                    id = id,
                    username = username,
                    name = name,
                    role = role,
                    departmentId = tokenStore.getUserDepartmentId(),
                    permissions = readPermissions(),
                    roles = readRoles(),
                    mustChangePassword = tokenStore.getMustChangePassword(),
                )
            )
        } else {
            AuthState.LoggedOut
        }
    }

    /**
     * Arranque de sesión con renovación: si el access token guardado está
     * vencido o vence dentro de [REFRESH_WINDOW_SECONDS], lo renueva con el
     * refresh token (`POST /auth/refresh`, que además rota el refresh) y
     * luego relee `/auth/me`; si el token todavía es fresco, solo relee
     * `/auth/me`.
     *
     * Un refresh inválido/vencido termina en logout (el 401 del API dispara
     * el `onUnauthorized` del `ApiClient`). Un fallo de red conserva la
     * sesión persistida —igual que la web al recargar sin conexión— y la
     * siguiente petición autenticada decide. Llamarla después de
     * [restoreSession] y en lugar de [refreshSession] al abrir la app.
     */
    suspend fun ensureFreshSession() {
        if (_state.value !is AuthState.LoggedIn) return
        val accessToken = tokenStore.getToken() ?: run {
            logout()
            return
        }

        if (isAccessTokenStale(accessToken)) {
            // Sin refresh token (sesión anterior a este soporte) no hay forma de renovar.
            val refreshToken = tokenStore.getRefreshToken() ?: run {
                logout()
                return
            }
            val renewed = try {
                rotateTokens(refreshToken)
                true
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Refresh inválido: el ApiClient ya llamó a logout() al ver el 401.
                false
            }
            if (!renewed) return
        }

        refreshSession()
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
        // El refresh token se guarda junto al access: es lo que permite renovar sin password.
        response.refreshToken?.let { tokenStore.saveRefreshToken(it) }
        val me = try {
            authApi.me()
        } catch (e: Exception) {
            // Sin permisos no se puede armar la sesión: no dejar un token a medias.
            tokenStore.clear()
            throw e
        }
        saveSession(me)
    }

    /**
     * Cambia la propia contraseña (`POST /auth/change-password`). Es la salida
     * de la compuerta `mustChangePassword`; en éxito el API la deja en false.
     *
     * Además de llamar al endpoint, apaga el flag en el estado local —así el
     * shell se muestra al instante— y relee `/auth/me` para sincronizar la
     * sesión. Devuelve `Result.failure` con el error del API (cuerpo ya en
     * español) para que la UI lo muestre con `networkMessage()`.
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            authApi.changePassword(currentPassword, newPassword)
            tokenStore.saveMustChangePassword(false)
            (_state.value as? AuthState.LoggedIn)?.let { logged ->
                _state.value = logged.copy(user = logged.user.copy(mustChangePassword = false))
            }
            refreshSession()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        tokenStore.clear()
        _state.value = AuthState.LoggedOut
    }

    fun getServerUrl(): String = tokenStore.getServerUrl() ?: ApiConfig.DEFAULT_BASE_URL

    fun setServerUrl(url: String) {
        tokenStore.saveServerUrl(url)
    }

    /** Canjea el refresh token por un access nuevo y guarda el refresh rotado. */
    private suspend fun rotateTokens(refreshToken: String) {
        val response = authApi.refresh(refreshToken)
        tokenStore.saveToken(response.token)
        response.refreshToken?.let { tokenStore.saveRefreshToken(it) }
    }

    private fun saveSession(user: AuthUserDto) {
        val permissions = user.permissions.orEmpty()
        val roles = user.roles.orEmpty()
        tokenStore.saveUser(
            id = user.id,
            username = user.username,
            name = user.name,
            role = user.role,
            departmentId = user.departmentId,
        )
        tokenStore.savePermissions(apiJson.encodeToString(permissionsSerializer, permissions))
        tokenStore.saveRoles(apiJson.encodeToString(rolesSerializer, roles))
        tokenStore.saveMustChangePassword(user.mustChangePassword)
        _state.value = AuthState.LoggedIn(
            SessionUser(
                id = user.id,
                username = user.username,
                name = user.name,
                role = user.role,
                departmentId = user.departmentId,
                permissions = permissions,
                roles = roles,
                mustChangePassword = user.mustChangePassword,
            )
        )
    }

    private fun readPermissions(): Map<String, String> =
        tokenStore.getPermissions()
            ?.let { runCatching { apiJson.decodeFromString(permissionsSerializer, it) }.getOrNull() }
            .orEmpty()

    private fun readRoles(): List<String> =
        tokenStore.getRoles()
            ?.let { runCatching { apiJson.decodeFromString(rolesSerializer, it) }.getOrNull() }
            .orEmpty()

    /**
     * ¿El access token hay que renovarlo ya? Se lee el `exp` (segundos epoch)
     * del payload base64url del JWT, sin verificar la firma: el API valida el
     * token, aquí solo se decide si conviene pedir uno nuevo antes de usarlo.
     * Si el token no se puede leer se devuelve `false` y el 401 del API manda.
     */
    private fun isAccessTokenStale(token: String): Boolean {
        val expiresAt = jwtExpiresAtEpochSeconds(token) ?: return false
        val nowSeconds = currentTimeMillis() / 1000
        return expiresAt - nowSeconds <= REFRESH_WINDOW_SECONDS
    }

    /** `exp` del JWT en segundos epoch, o null si el token no tiene payload legible. */
    private fun jwtExpiresAtEpochSeconds(token: String): Long? = runCatching {
        val payload = token.split('.').getOrNull(1)?.takeIf { it.isNotEmpty() } ?: return@runCatching null
        // JWT = base64url sin padding; se normaliza al alfabeto estándar de Base64.
        val normalized = payload.replace('-', '+').replace('_', '/')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        val json = Base64.decode(padded).decodeToString()
        apiJson.parseToJsonElement(json).jsonObject["exp"]?.jsonPrimitive?.longOrNull
    }.getOrNull()
}
