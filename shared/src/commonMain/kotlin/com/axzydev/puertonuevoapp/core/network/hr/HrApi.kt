package com.axzydev.puertonuevoapp.core.network.hr

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import com.axzydev.puertonuevoapp.core.network.users.UserDto

/**
 * Expedientes de personal (RH). Endpoints ADMIN/HUMAN_RESOURCES.
 *
 * La tabla usa `POST /hr/query` — el mismo contrato server-side que la
 * web (`EmployeesTable.tsx`): filtros `name`, `departmentId`, `active`, `role`
 * ordenados por nombre. Devuelve los campos de `UserDto` más los del
 * expediente (se descartan con `ignoreUnknownKeys`).
 */
class HrApi(private val client: ApiClient) {

    suspend fun query(request: TableRequest): TableResponse<UserDto> =
        client.post("/hr/query", request)

    suspend fun get(id: String): EmployeeProfileDto =
        client.get("/hr/$id")

    suspend fun documents(id: String): List<EmployeeDocumentDto> =
        client.get("/hr/$id/documents")

    suspend fun documentTypes(): List<DocumentTypeDto> =
        client.get("/hr/catalogs/document-types")
}