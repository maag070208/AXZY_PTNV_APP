package com.axzydev.puertonuevoapp.core.network.inventory

import com.axzydev.puertonuevoapp.core.network.common.UserRefDto
import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.Serializable

/**
 * Adaptador al contrato REAL del API (`/inventario`). La app conserva su
 * modelo ([InventoryMovementDto]/[InventorySummaryDto]) y aquí se traduce
 * la respuesta del backend (`Movimiento`, `Dashboard`).
 */
class InventoryApi(private val client: ApiClient) {
    suspend fun movements(
        deviceId: String? = null,
        locationId: String? = null,
        start: String? = null,
        end: String? = null,
    ): List<InventoryMovementDto> {
        val params = buildList {
            deviceId?.takeIf { it.isNotBlank() }?.let { add("dispositivoId=$it") }
            start?.takeIf { it.isNotBlank() }?.let { add("start=${it.encodeURLParameter()}") }
            end?.takeIf { it.isNotBlank() }?.let { add("end=${it.encodeURLParameter()}") }
        }
        val qs = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
        return client.get<List<ApiMovimiento>>("/inventario/movimientos$qs").map { it.toDto() }
    }

    suspend fun registerMovement(input: MovementInput): InventoryMovementDto {
        val body = ApiCreateMovimiento(
            tipo = input.tipo,
            departamentoId = input.locationId,
            motivo = input.motivoBaja,
            observaciones = input.notas,
            prestamoId = input.prestamoId,
            detalles = listOf(
                ApiMovimientoDetalleInput(
                    dispositivoId = input.deviceId,
                    cantidad = 1,
                    condicion = input.condicion,
                )
            ),
        )
        return client.post<ApiCreateMovimiento, ApiMovimiento>("/inventario/movimientos", body).toDto()
    }

    suspend fun summary(): InventorySummaryDto {
        val dash = client.get<ApiDashboard>("/inventario/dashboard")
        return InventorySummaryDto(
            locations = emptyList(),
            stats = InventoryStatsDto(
                totalDevices = dash.stats.dispositivos,
                locatedDevices = 0,
                unlocatedDevices = dash.stats.dispositivos,
            ),
        )
    }
}

private fun ApiMovimiento.toDto(): InventoryMovementDto {
    val detalle = detalles.firstOrNull()
    return InventoryMovementDto(
        id = id,
        deviceId = detalle?.dispositivoId ?: "",
        tipo = tipo,
        notas = observaciones,
        userId = usuarioId,
        user = usuario?.let { UserRefDto(id = it.id, name = it.name, username = "") },
        prestamoId = prestamoId,
        prestadoA = responsable?.name,
        condicion = detalle?.condicion,
        motivoBaja = motivo,
        createdAt = fecha,
    )
}

@Serializable
private data class ApiUserRef(val id: String, val name: String)

@Serializable
private data class ApiMovimientoDetalle(
    val id: String = "",
    val dispositivoId: String = "",
    val cantidad: Int = 0,
    val condicion: String? = null,
    val observaciones: String? = null,
)

@Serializable
private data class ApiMovimiento(
    val id: String,
    val tipo: String,
    val fecha: String = "",
    val usuarioId: String = "",
    val usuario: ApiUserRef? = null,
    val responsable: ApiUserRef? = null,
    val departamentoId: String? = null,
    val motivo: String? = null,
    val observaciones: String? = null,
    val status: String = "ACTIVO",
    val prestamoId: String? = null,
    val detalles: List<ApiMovimientoDetalle> = emptyList(),
)

@Serializable
private data class ApiCreateMovimiento(
    val tipo: String,
    val responsableId: String? = null,
    val departamentoId: String? = null,
    val subareaId: String? = null,
    val motivo: String? = null,
    val observaciones: String? = null,
    val prestamoId: String? = null,
    val detalles: List<ApiMovimientoDetalleInput> = emptyList(),
)

@Serializable
private data class ApiMovimientoDetalleInput(
    val dispositivoId: String,
    val cantidad: Int = 1,
    val condicion: String? = null,
)

@Serializable
private data class ApiDashboardStats(
    val tipos: Int = 0,
    val dispositivos: Int = 0,
    val unidadesActivas: Int = 0,
    val disponible: Int = 0,
    val prestado: Int = 0,
    val danado: Int = 0,
    val mantenimiento: Int = 0,
    val baja: Int = 0,
)

@Serializable
private data class ApiDashboard(val stats: ApiDashboardStats = ApiDashboardStats())
