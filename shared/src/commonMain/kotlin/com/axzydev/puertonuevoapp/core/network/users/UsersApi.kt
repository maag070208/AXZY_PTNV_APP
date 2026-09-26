package com.axzydev.puertonuevoapp.core.network.users

import com.axzydev.puertonuevoapp.core.network.common.DeleteSuccessResponse
import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse

class UsersApi(private val client: ApiClient) {
    /** Selector liviano para firmas/asignaciones — cualquier autenticado. */
    suspend fun employees(): List<UserDto> = client.get("/users/employees")

    suspend fun query(request: TableRequest): TableResponse<UserDto> = client.post("/users/query", request)

    suspend fun list(): List<UserDto> = client.get("/users")

    suspend fun get(id: String): UserDto = client.get("/users/$id")

    suspend fun create(input: UserCreateInput): UserDto = client.post("/users", input)

    suspend fun update(id: String, input: UserUpdateInput): UserDto = client.put("/users/$id", input)

    suspend fun delete(id: String): DeleteSuccessResponse = client.delete("/users/$id")

    suspend fun changePassword(id: String, password: String) =
        client.putNoContent("/users/$id/password", UserPasswordInput(password))
}
