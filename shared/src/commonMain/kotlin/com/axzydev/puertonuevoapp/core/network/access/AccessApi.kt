package com.axzydev.puertonuevoapp.core.network.access

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import io.ktor.http.encodeURLParameter

/**
 * Puerto de datos del control de acceso. La UI/ViewModel depende de esta
 * interfaz (no de la implementación concreta), lo que permite sustituirla
 * por un fake en los tests sin montar un cliente HTTP real.
 */
interface AccessApi {
    /** Resuelve un QR crudo a resumen del empleado. No registra. */
    suspend fun lookup(qr: String): AccessLookupResultDto

    /** Registra un evento (idempotente por `clientEventId`). */
    suspend fun createEvent(input: AccessEventInput): AccessEventDto

    /** Último evento de un empleado y sugerencia ENTRY/EXIT. */
    suspend fun status(employeeId: String): AccessStatusDto

    /** Tabla server-side de eventos (roles de consulta). */
    suspend fun query(request: TableRequest): TableResponse<AccessEventDto>

    /** Catálogo de sitios/porterías activos. */
    suspend fun sites(): List<SiteDto>

    /** Escaneos registrados por el guardia autenticado hoy. */
    suspend fun meToday(): AccessTodayResponseDto

    /** Bytes de la foto del empleado (ruta relativa al API). */
    suspend fun photoBytes(path: String): ByteArray
}

/** Implementación real contra `/access` (patrón `core/network/inventory`). */
class AccessApiClient(private val client: ApiClient) : AccessApi {

    override suspend fun lookup(qr: String): AccessLookupResultDto =
        client.post<AccessLookupInput, AccessLookupResultDto>("/access/lookup", AccessLookupInput(qr))

    override suspend fun createEvent(input: AccessEventInput): AccessEventDto =
        client.post<AccessEventInput, AccessEventDto>("/access/events", input)

    override suspend fun status(employeeId: String): AccessStatusDto =
        client.get("/access/status/${employeeId.encodeURLParameter()}")

    override suspend fun query(request: TableRequest): TableResponse<AccessEventDto> =
        client.post("/access/query", request)

    override suspend fun sites(): List<SiteDto> = client.get("/access/sites")

    override suspend fun meToday(): AccessTodayResponseDto = client.get("/access/me/today")

    override suspend fun photoBytes(path: String): ByteArray = client.getBytes(path)
}
