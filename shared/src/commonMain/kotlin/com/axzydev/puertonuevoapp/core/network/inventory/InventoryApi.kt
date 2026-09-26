package com.axzydev.puertonuevoapp.core.network.inventory

import com.axzydev.puertonuevoapp.core.network.common.UserRefDto
import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.Serializable

/**
 * Adaptador al contrato REAL del API (`/inventory`). La app conserva su
 * modelo ([InventoryMovementDto]/[InventorySummaryDto]) y aquí se traduce
 * la respuesta del backend (`Movement`, `Dashboard`).
 */
class InventoryApi(private val client: ApiClient) {
    suspend fun movements(
        deviceId: String? = null,
        locationId: String? = null,
        start: String? = null,
        end: String? = null,
    ): List<InventoryMovementDto> {
        val params = buildList {
            deviceId?.takeIf { it.isNotBlank() }?.let { add("deviceId=$it") }
            start?.takeIf { it.isNotBlank() }?.let { add("start=${it.encodeURLParameter()}") }
            end?.takeIf { it.isNotBlank() }?.let { add("end=${it.encodeURLParameter()}") }
        }
        val qs = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
        return client.get<List<ApiMovement>>("/inventory/movements$qs").map { it.toDto() }
    }

    suspend fun registerMovement(input: MovementInput): InventoryMovementDto {
        val body = ApiCreateMovement(
            type = input.type,
            departmentId = input.locationId,
            reason = input.retirementReason,
            notes = input.notes,
            loanId = input.loanId,
            items = listOf(
                ApiMovementItemInput(
                    deviceId = input.deviceId,
                    quantity = 1,
                    condition = input.condition,
                )
            ),
        )
        return client.post<ApiCreateMovement, ApiMovement>("/inventory/movements", body).toDto()
    }

    suspend fun summary(): InventorySummaryDto {
        val dash = client.get<ApiDashboard>("/inventory/dashboard")
        return InventorySummaryDto(
            locations = emptyList(),
            stats = InventoryStatsDto(
                totalDevices = dash.stats.devices,
                locatedDevices = 0,
                unlocatedDevices = dash.stats.devices,
            ),
        )
    }
}

private fun ApiMovement.toDto(): InventoryMovementDto {
    val item = items.firstOrNull()
    return InventoryMovementDto(
        id = id,
        deviceId = item?.deviceId ?: "",
        type = type,
        notes = notes,
        userId = createdById,
        user = createdBy?.let { UserRefDto(id = it.id, name = it.name, username = "") },
        loanId = loanId,
        loanedTo = custodian?.name,
        condition = item?.condition,
        retirementReason = reason,
        createdAt = date,
    )
}

@Serializable
private data class ApiUserRef(val id: String, val name: String)

@Serializable
private data class ApiMovementItem(
    val id: String = "",
    val deviceId: String = "",
    val quantity: Int = 0,
    val condition: String? = null,
    val notes: String? = null,
)

@Serializable
private data class ApiMovement(
    val id: String,
    val type: String,
    val date: String = "",
    val createdById: String = "",
    val createdBy: ApiUserRef? = null,
    val custodian: ApiUserRef? = null,
    val departmentId: String? = null,
    val reason: String? = null,
    val notes: String? = null,
    val status: String = "ACTIVE",
    val loanId: String? = null,
    val items: List<ApiMovementItem> = emptyList(),
)

@Serializable
private data class ApiCreateMovement(
    val type: String,
    val custodianId: String? = null,
    val departmentId: String? = null,
    val subareaId: String? = null,
    val reason: String? = null,
    val notes: String? = null,
    val loanId: String? = null,
    val items: List<ApiMovementItemInput> = emptyList(),
)

@Serializable
private data class ApiMovementItemInput(
    val deviceId: String,
    val quantity: Int = 1,
    val condition: String? = null,
)

@Serializable
private data class ApiDashboardStats(
    val types: Int = 0,
    val devices: Int = 0,
    val activeUnits: Int = 0,
    val available: Int = 0,
    val loaned: Int = 0,
    val damaged: Int = 0,
    val maintenance: Int = 0,
    val retirement: Int = 0,
)

@Serializable
private data class ApiDashboard(val stats: ApiDashboardStats = ApiDashboardStats())
