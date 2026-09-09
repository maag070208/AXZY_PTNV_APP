package com.axzydev.puertonuevoapp.core.network

class AuthApi(private val client: ApiClient) {
    suspend fun login(username: String, password: String): LoginResponseDto =
        client.post("/auth/login", LoginRequest(username, password))

    suspend fun me(): AuthUserDto = client.get("/auth/me")
}
