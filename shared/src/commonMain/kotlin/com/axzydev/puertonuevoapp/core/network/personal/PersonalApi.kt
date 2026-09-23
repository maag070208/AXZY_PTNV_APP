package com.axzydev.puertonuevoapp.core.network.personal

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import com.axzydev.puertonuevoapp.core.network.users.UserDto

/**
 * Expedientes de personal (RH). Endpoints ADMIN/RECURSOS_HUMANOS.
 *
 * La tabla usa `POST /personal/query` — el mismo contrato server-side que la
 * web (`EmployeesTable.tsx`): filtros `name`, `departmentId`, `active`, `role`
 * ordenados por nombre. Devuelve los campos de `UserDto` más los del
 * expediente (se descartan con `ignoreUnknownKeys`).
 */
class PersonalApi(private val client: ApiClient) {

    suspend fun query(request: TableRequest): TableResponse<UserDto> =
        client.post("/personal/query", request)

    suspend fun get(id: String): PersonalProfileDto =
        client.get("/personal/$id")

    suspend fun documents(id: String): List<EmployeeDocumentDto> =
        client.get("/personal/$id/documentos")

    suspend fun documentTypes(): List<DocumentTypeDto> =
        client.get("/personal/catalogos/tipos-documento")
}