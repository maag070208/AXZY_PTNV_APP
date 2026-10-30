package com.axzydev.puertonuevoapp.core.network.auth

import com.axzydev.puertonuevoapp.core.network.http.ApiClient

/**
 * Endpoints de autenticación del API (bajo `/auth`). Contrato verificado en
 * `api/src/modules/auth/models/dto/auth.dto.ts`:
 * - `POST /auth/login` → `{ token, refreshToken, user }`.
 * - `GET /auth/me` → usuario de la sesión con `permissions`, `roles` y
 *   `mustChangePassword`.
 * - `POST /auth/refresh` → `{ token, refreshToken }` (sin usuario; el refresh
 *   token rotado invalida el anterior).
 * - `POST /auth/change-password` → 204 sin cuerpo.
 */
class AuthApi(private val client: ApiClient) {
    suspend fun login(username: String, password: String): LoginResponseDto =
        client.post("/auth/login", LoginRequest(username, password))

    suspend fun me(): AuthUserDto = client.get("/auth/me")

    /** Renueva el access token (y rota el refresh token) a partir del refresh vigente. */
    suspend fun refresh(refreshToken: String): RefreshResponseDto =
        client.post("/auth/refresh", RefreshRequest(refreshToken))

    /** Cambia la propia contraseña; en éxito el API responde 204 (sin cuerpo). */
    suspend fun changePassword(currentPassword: String, newPassword: String) {
        client.postNoContent("/auth/change-password", ChangePasswordRequest(currentPassword, newPassword))
    }
}
