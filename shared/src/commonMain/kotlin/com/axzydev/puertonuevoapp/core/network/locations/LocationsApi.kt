package com.axzydev.puertonuevoapp.core.network.locations

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Adaptador al contrato REAL del API. El backend eliminó el concepto de
 * "ubicación" (`/locations`) y ahora las ubicaciones son **departamentos**
 * (`/departments`) con **subáreas**. La app conserva su modelo [LocationDto]
 * y aquí se mapea `Department` → `LocationDto`.
 */
class LocationsApi(private val client: ApiClient) {
    suspend fun list(includeInactive: Boolean = false): List<LocationDto> =
        client.get<List<ApiDepartment>>("/departments")
            .map { it.toDto() }
            .filter { includeInactive || it.active }

    suspend fun get(id: String): LocationDto =
        client.get<ApiDepartment>("/departments/$id").toDto()

    suspend fun create(input: LocationCreateInput): LocationDto =
        client.post<ApiDepartmentCreate, ApiDepartment>("/departments", ApiDepartmentCreate(name = input.name)).toDto()

    suspend fun update(id: String, input: LocationUpdateInput): LocationDto =
        client.put<ApiDepartmentUpdate, ApiDepartment>(
            "/departments/$id",
            ApiDepartmentUpdate(name = input.name, active = input.active),
        ).toDto()

    suspend fun remove(id: String): LocationDeleteResultDto {
        val res = client.delete<ApiDeleteResult>("/departments/$id")
        return LocationDeleteResultDto(soft = res.soft, data = res.data?.toDto())
    }

    suspend fun addSubLocation(locationId: String, name: String): SubLocationDto =
        client.post<ApiSubareaCreate, ApiSubarea>("/departments/$locationId/subareas", ApiSubareaCreate(name)).toDto(locationId)

    suspend fun removeSubLocation(subLocationId: String): SubLocationDeleteResultDto {
        val res = client.delete<ApiSubareaDeleteResult>("/departments/subareas/$subLocationId")
        return SubLocationDeleteResultDto(soft = res.soft, data = res.data?.toDto(res.data.departmentId))
    }
}

private fun ApiDepartment.toDto(): LocationDto = LocationDto(
    id = id,
    name = name,
    active = active,
    departmentId = id,
    subLocations = subareas.map { it.toDto(id) },
    count = count?.let { LocationCountDto(devices = 0, custodyLetters = 0) },
)

private fun ApiSubarea.toDto(departmentId: String): SubLocationDto = SubLocationDto(
    id = id,
    locationId = this.departmentId.ifBlank { departmentId },
    name = name,
    active = active,
)

@Serializable
private data class ApiSubarea(
    val id: String,
    val departmentId: String = "",
    val name: String,
    val active: Boolean = true,
)

@Serializable
private data class ApiDeptCount(val users: Int = 0)

@Serializable
private data class ApiDepartment(
    val id: String,
    val name: String,
    val active: Boolean = true,
    val subareas: List<ApiSubarea> = emptyList(),
    @SerialName("_count") val count: ApiDeptCount? = null,
)

@Serializable
private data class ApiDepartmentCreate(val name: String)

@Serializable
private data class ApiDepartmentUpdate(val name: String? = null, val active: Boolean? = null)

@Serializable
private data class ApiSubareaCreate(val name: String)

@Serializable
private data class ApiDeleteResult(val soft: Boolean = false, val data: ApiDepartment? = null)

@Serializable
private data class ApiSubareaDeleteResult(val soft: Boolean = false, val data: ApiSubarea? = null)
