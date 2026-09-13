package com.axzydev.puertonuevoapp.core.network

import io.ktor.http.encodeURLParameter

class UsersApi(private val client: ApiClient) {
    suspend fun list(): List<UserDto> = client.get("/users")

    suspend fun empleados(
        departmentId: String? = null,
        q: String? = null,
        roles: List<String>? = null,
    ): List<UserDto> {
        val params = buildList {
            departmentId?.takeIf { it.isNotBlank() }?.let { add("departmentId=$it") }
            q?.takeIf { it.isNotBlank() }?.let { add("q=${it.encodeURLParameter()}") }
            roles?.takeIf { it.isNotEmpty() }?.let { add("roles=${it.joinToString(",")}") }
        }
        val qs = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
        return client.get("/users/empleados$qs")
    }

    suspend fun get(id: String): UserDto = client.get("/users/$id")

    suspend fun create(request: UserCreateRequest): UserDto = client.post("/users", request)

    suspend fun update(id: String, request: UserUpdateRequest): UserDto = client.put("/users/$id", request)

    suspend fun setActive(id: String, active: Boolean): UserDto = client.put("/users/$id", UserActiveRequest(active))

    suspend fun changePassword(id: String, password: String) {
        client.putNoContent("/users/$id/password", ChangePasswordRequest(password))
    }

    suspend fun delete(id: String, force: Boolean = false): UserDeleteResponse =
        client.delete("/users/$id${if (force) "?force=true" else ""}")

    suspend fun history(id: String): List<UserHistoryEntry> = client.get("/users/$id/history")
}
