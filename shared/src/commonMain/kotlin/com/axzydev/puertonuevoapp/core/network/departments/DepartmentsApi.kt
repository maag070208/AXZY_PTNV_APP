package com.axzydev.puertonuevoapp.core.network.departments

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse

class DepartmentsApi(private val client: ApiClient) {
    suspend fun list(includeInactive: Boolean = false): List<DepartmentDto> =
        client.get("/departments${if (includeInactive) "?includeInactive=true" else ""}")

    suspend fun query(request: TableRequest): TableResponse<DepartmentDto> = client.post("/departments/query", request)

    suspend fun get(id: String): DepartmentDto = client.get("/departments/$id")

    suspend fun create(name: String): DepartmentDto = client.post("/departments", DepartmentCreateInput(name))

    suspend fun update(id: String, input: DepartmentUpdateInput): DepartmentDto = client.put("/departments/$id", input)

    suspend fun remove(id: String): DepartmentDeleteResultDto = client.delete("/departments/$id")

    suspend fun addSubarea(departmentId: String, name: String): SubareaDto =
        client.post("/departments/$departmentId/subareas", SubareaCreateInput(name))

    suspend fun removeSubarea(subareaId: String): SubareaDeleteResultDto =
        client.delete("/departments/subareas/$subareaId")
}
