package com.axzydev.puertonuevoapp.core.network.users

import com.axzydev.puertonuevoapp.core.network.common.DeleteSuccessResponse
import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import com.axzydev.puertonuevoapp.core.platform.PickedFile
import com.axzydev.puertonuevoapp.core.platform.toMultipartPart

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

    /**
     * Historial de la cuenta (`GET /users/:id/history`, permiso `users.view`):
     * eventos de inventario, tickets y altas/bajas, del más nuevo al más viejo.
     */
    suspend fun history(id: String): List<UserHistoryEntryDto> = client.get("/users/$id/history")

    /**
     * Da de baja la cuenta con su motivo (`PATCH /users/:id/deactivate`). El API
     * exige `users.edit` (o `hr.records`), audita el cambio y, con
     * [notifyUser] (por defecto), avisa a la persona.
     */
    suspend fun deactivate(id: String, reason: String, notifyUser: Boolean = true): UserActiveStateDto =
        client.patch("/users/$id/deactivate", UserDeactivateInput(reason = reason, notifyUser = notifyUser))

    /** Reactiva una cuenta dada de baja (`PATCH /users/:id/reactivate`). */
    suspend fun reactivate(id: String): UserActiveStateDto =
        client.patch("/users/$id/reactivate", UserReactivateInput())

    /**
     * Carga masiva de cuentas por Excel (`POST /users/import`, multipart, campo
     * `file`, permiso `users.create`). Cada fila crea una cuenta con rol
     * EMPLEADO; las filas inválidas o repetidas vuelven en `skipped` sin
     * detener la carga. La API no tiene endpoint de plantilla ni de
     * previsualización: la pantalla revisa el archivo elegido antes de subirlo.
     */
    suspend fun importUsers(file: PickedFile): UserImportResultDto =
        client.postMultipart(path = "/users/import", parts = listOf(file.toMultipartPart()))
}
