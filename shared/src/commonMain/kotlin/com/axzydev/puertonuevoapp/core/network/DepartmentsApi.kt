package com.axzydev.puertonuevoapp.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class DepartmentsApi(private val client: ApiClient) {
    suspend fun list(includeInactive: Boolean = false): List<DepartmentDto> =
        client.get("/departments${if (includeInactive) "?includeInactive=true" else ""}")

    suspend fun get(id: String): DepartmentDto = client.get("/departments/$id")

    suspend fun create(name: String): DepartmentDto =
        client.post("/departments", DepartmentCreateDto(name))

    suspend fun update(id: String, input: DepartmentUpdateDto): DepartmentDto =
        client.put("/departments/$id", input)

    suspend fun remove(id: String): DepartmentDeleteResultDto = client.delete("/departments/$id")

    suspend fun addSubarea(departmentId: String, name: String): SubareaDto =
        client.post("/departments/$departmentId/subareas", SubareaCreateDto(name))

    suspend fun removeSubarea(subareaId: String): SubareaDeleteResultDto =
        client.delete("/departments/subareas/$subareaId")
}

@Serializable
data class DeptCountDto(
    val users: Int = 0,
)

@Serializable
data class DepartmentDto(
    val id: String,
    val name: String,
    val active: Boolean = true,
    val subareas: List<SubareaDto> = emptyList(),
    @SerialName("_count") val count: DeptCountDto? = null,
)

@Serializable
data class SubareaDto(
    val id: String,
    val departmentId: String,
    val name: String,
    val active: Boolean = true,
)

@Serializable
data class DepartmentCreateDto(
    val name: String,
)

@Serializable
data class DepartmentUpdateDto(
    val name: String? = null,
    val active: Boolean? = null,
)

@Serializable
data class SubareaCreateDto(
    val name: String,
)

@Serializable
data class DepartmentDeleteResultDto(
    val soft: Boolean,
    val data: DepartmentDto,
)

@Serializable
data class SubareaDeleteResultDto(
    val soft: Boolean,
    val data: SubareaDto,
)
