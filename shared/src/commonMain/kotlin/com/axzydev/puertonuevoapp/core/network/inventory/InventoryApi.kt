package com.axzydev.puertonuevoapp.core.network.inventory

import com.axzydev.puertonuevoapp.core.network.common.UserRefDto
import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * Adaptador al contrato REAL del API (`/inventory`). La app conserva su
 * modelo ([InventoryMovementDto]/[InventorySummaryDto]) y aquí se traduce
 * la respuesta del backend (`Movement`, `Dashboard`).
 *
 * Desde la Fase 1 del plan de paridad también expone las lecturas y escrituras
 * pieza a pieza del inventario: unidades físicas, existencias, kardex,
 * movimientos, devoluciones, auditor y detalle de préstamo.
 *
 * **Paginación:** el API de inventario NO tiene `POST /<recurso>/query`: los
 * listados (`GET /inventory/movements`, `GET /inventory/returns`) devuelven TODO
 * y no aceptan `page`/`limit`. [queryMovements] y [returns] reciben el
 * [TableRequest] estándar y paginan/ordenan **en el cliente** para que las
 * pantallas usen el mismo contrato de tabla que el resto de la app; queda
 * documentado aquí porque no es paginación server-side.
 */
class InventoryApi(private val client: ApiClient) {
    // -----------------------------------------------------------------------
    // Movimientos (modelo histórico de la app)
    // -----------------------------------------------------------------------

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

    /**
     * Registra un movimiento. Para `STOCK_IN` con identidades, [MovementInput]
     * lleva `quantity` (piezas nuevas a crear) y `units` (serie/MAC/IP/hostname
     * por pieza, en el orden de creación); la API crea las que falten vacías.
     *
     * @param idempotencyKey `Idempotency-Key` opcional: repetir la misma petición
     *   devuelve el mismo movimiento en vez de duplicarlo.
     */
    suspend fun registerMovement(
        input: MovementInput,
        idempotencyKey: String? = null,
    ): InventoryMovementDto {
        val body = ApiCreateMovement(
            type = input.type,
            departmentId = input.locationId,
            reason = input.retirementReason,
            notes = input.notes,
            loanId = input.loanId,
            items = listOf(
                ApiMovementItemInput(
                    deviceId = input.deviceId,
                    quantity = input.quantity,
                    condition = input.condition,
                    units = input.units,
                )
            ),
        )
        val raw = if (idempotencyKey.isNullOrBlank()) {
            client.post<ApiCreateMovement, ApiMovement>("/inventory/movements", body)
        } else {
            client.decode<ApiMovement>(
                postJsonWithIdempotency(
                    "/inventory/movements",
                    client.json.encodeToString(body),
                    idempotencyKey,
                )
            )
        }
        return raw.toDto()
    }

    // -----------------------------------------------------------------------
    // Unidades físicas, existencias y kardex
    // -----------------------------------------------------------------------

    /** Piezas físicas del dispositivo (`GET /inventory/devices/:id/units`). */
    suspend fun units(deviceId: String): List<DeviceUnitDto> =
        client.get("/inventory/devices/$deviceId/units")

    /** Existencias por estado del dispositivo (`GET /inventory/devices/:id/stock`). */
    suspend fun stock(deviceId: String): StockDto =
        client.get("/inventory/devices/$deviceId/stock")

    /**
     * Kardex del dispositivo (`GET /inventory/devices/:id/ledger`): dispositivo,
     * existencias y TODOS los renglones con su saldo. El endpoint NO pagina.
     */
    suspend fun ledger(deviceId: String): DeviceLedgerDto =
        client.get("/inventory/devices/$deviceId/ledger")

    /** Edita la identificación de una pieza (`PUT /inventory/units/:id`). */
    suspend fun updateUnit(unitId: String, input: UnitUpdateInput): DeviceUnitDto =
        client.put<UnitUpdateInput, DeviceUnitDto>("/inventory/units/$unitId", input)

    /**
     * Busca piezas por activo fijo, serie o hostname (`GET /inventory/units?q=`).
     * Devuelve ya resuelto el dispositivo y su departamento. Vacío si no hay
     * término (el API también devuelve `[]`).
     */
    suspend fun searchUnits(query: String, limit: Int = 20): List<DeviceUnitDto> {
        val term = query.trim()
        if (term.isEmpty()) return emptyList()
        val take = limit.coerceIn(1, 50)
        return client.get("/inventory/units?q=${term.encodeURLParameter()}&limit=$take")
    }

    // -----------------------------------------------------------------------
    // Movimientos (contrato crudo del API)
    // -----------------------------------------------------------------------

    /**
     * Listado de movimientos para tablas server-side.
     *
     * NOTA: `POST /inventory/movements/query` no existe; se usa
     * `GET /inventory/movements` (que devuelve todo) y se pagina/ordena en el
     * cliente. Filtros que viajan al servidor: `type` y `deviceId` (las demás
     * claves de [TableRequest.filters] se ignoran). Orden soportado:
     * `date`/`fecha`/`createdAt` y `type`/`tipo`.
     */
    suspend fun queryMovements(request: TableRequest): TableResponse<MovementDto> {
        val params = buildList {
            request.filters["type"]?.takeIf { it.isNotBlank() }?.let { add("type=${it.encodeURLParameter()}") }
            request.filters["deviceId"]?.takeIf { it.isNotBlank() }?.let { add("deviceId=${it.encodeURLParameter()}") }
        }
        val qs = if (params.isEmpty()) "" else "?" + params.joinToString("&")
        val all = client.get<List<MovementDto>>("/inventory/movements$qs")
        val sort = request.sort
        val sorted = when (sort?.key) {
            "date", "fecha", "createdAt" ->
                if (sort.direction.equals("asc", ignoreCase = true)) all.sortedBy { it.date }
                else all.sortedByDescending { it.date }

            "type", "tipo" ->
                if (sort.direction.equals("asc", ignoreCase = true)) all.sortedBy { it.type }
                else all.sortedByDescending { it.type }

            else -> all
        }
        return sorted.toTablePage(request)
    }

    /** Detalle de un movimiento con sus renglones y piezas (`GET /inventory/movements/:id`). */
    suspend fun movement(id: String): MovementDto =
        client.get("/inventory/movements/$id")

    /** Revierte un movimiento reversible (`POST /inventory/movements/:id/revert`). */
    suspend fun revertMovement(id: String): MovementDto =
        client.postNoBody("/inventory/movements/$id/revert")

    // -----------------------------------------------------------------------
    // Devoluciones
    // -----------------------------------------------------------------------

    /**
     * Bandeja de devoluciones para tablas.
     *
     * NOTA: `GET /inventory/returns` tampoco pagina; sólo filtra por `loanId`.
     * Se pagina/ordena en el cliente (orden por `date`, el de la API).
     */
    suspend fun returns(request: TableRequest): TableResponse<LoanReturnDto> {
        val loanId = request.filters["loanId"]?.takeIf { it.isNotBlank() }
        val qs = loanId?.let { "?loanId=${it.encodeURLParameter()}" } ?: ""
        val all = client.get<List<LoanReturnDto>>("/inventory/returns$qs")
        val sort = request.sort
        val sorted = when (sort?.key) {
            "date", "fecha" ->
                if (sort.direction.equals("asc", ignoreCase = true)) all.sortedBy { it.date }
                else all.sortedByDescending { it.date }

            else -> all.sortedByDescending { it.date }
        }
        return sorted.toTablePage(request)
    }

    /**
     * Registra una devolución (parcial) de un préstamo (`POST /inventory/returns`).
     * Cada renglón regresa piezas exactas (`unitIds`) o una cantidad del renglón
     * (`quantity`), con su condición.
     *
     * @param idempotencyKey `Idempotency-Key` opcional.
     */
    suspend fun createReturn(
        input: ReturnCreateInput,
        idempotencyKey: String? = null,
    ): MovementDto =
        if (idempotencyKey.isNullOrBlank()) {
            client.post<ReturnCreateInput, MovementDto>("/inventory/returns", input)
        } else {
            client.decode<MovementDto>(
                postJsonWithIdempotency(
                    "/inventory/returns",
                    client.json.encodeToString(input),
                    idempotencyKey,
                )
            )
        }

    // -----------------------------------------------------------------------
    // Auditor de consistencia
    // -----------------------------------------------------------------------

    /** Corre el auditor de inventario (`GET /inventory/audit`, 8 reglas). */
    suspend fun audit(): InventoryAuditDto =
        client.get("/inventory/audit")

    /** Resuelve un descuadre del auditor (`POST /inventory/audit/mismatches/resolve`). */
    suspend fun resolveMismatch(input: ResolveMismatchInput): ResolveMismatchResultDto =
        client.post<ResolveMismatchInput, ResolveMismatchResultDto>(
            "/inventory/audit/mismatches/resolve",
            input,
        )

    // -----------------------------------------------------------------------
    // Préstamos (detalle para la devolución parcial por pieza)
    // -----------------------------------------------------------------------

    /**
     * Detalle de un préstamo/carta (`GET /inventory/loans/:id`) con sus renglones,
     * piezas exactas (`units[].returned`) y devoluciones ya registradas.
     */
    suspend fun loan(id: String): LoanDetailDto =
        client.get("/inventory/loans/$id")

    // -----------------------------------------------------------------------
    // Dashboard / resumen (histórico)
    // -----------------------------------------------------------------------

    /** Dispositivos por categoría (tipo) para el inicio; el API recorta por permiso. */
    suspend fun deviceCategories(): List<DeviceCategoryDto> =
        client.get<ApiDashboard>("/inventory/dashboard").byType.map { type ->
            DeviceCategoryDto(type.name, type.devices.sumOf { it.total })
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

    // -----------------------------------------------------------------------
    // Utilidades internas
    // -----------------------------------------------------------------------

    /**
     * POST con cuerpo JSON y header `Idempotency-Key`. [ApiClient] todavía no
     * expone un `post` con headers (lo está ampliando el dueño de ese archivo);
     * mientras tanto se usa su cliente Ktor interno desde el mismo módulo. Al
     * llegar el soporte de headers en `ApiClient`, este helper se reemplaza por
     * una llamada directa.
     */
    private suspend fun postJsonWithIdempotency(
        path: String,
        bodyJson: String,
        idempotencyKey: String?,
    ): HttpResponse = with(client) {
        httpClient.post(buildUrl(path)) {
            authHeader()
            contentType(ContentType.Application.Json)
            if (!idempotencyKey.isNullOrBlank()) header(IDEMPOTENCY_HEADER, idempotencyKey)
            setBody(bodyJson)
        }
    }

    /** Recorta en el cliente una lista completa a la página pedida (ver KDoc de la clase). */
    private fun <T> List<T>.toTablePage(request: TableRequest): TableResponse<T> {
        val page = request.page.coerceAtLeast(1)
        val limit = request.limit.coerceAtLeast(1)
        val from = (page - 1) * limit
        return TableResponse(
            data = if (from >= size) emptyList() else drop(from).take(limit),
            total = size,
        )
    }
}

private const val IDEMPOTENCY_HEADER = "Idempotency-Key"

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
        deviceLabel = item?.device?.let { device ->
            listOfNotNull(
                device.code?.takeIf { it.isNotBlank() },
                device.name.takeIf { it.isNotBlank() },
            ).joinToString(" · ").ifBlank { null }
        },
        unitsLabel = item?.let { row ->
            if (row.units.isNotEmpty()) {
                row.units.joinToString(", ") { unit ->
                    unit.deviceUnit?.assetTag ?: unit.deviceUnit?.serialNumber ?: "—"
                }
            } else if (row.quantity > 1) {
                "×${row.quantity}"
            } else {
                null
            }
        },
    )
}

@Serializable
private data class ApiUserRef(val id: String, val name: String)

@Serializable
private data class ApiMovementDevice(
    val id: String = "",
    val code: String? = null,
    val name: String = "",
    val brand: String = "",
    val model: String = "",
)

@Serializable
private data class ApiMovementDeviceUnit(
    val assetTag: String? = null,
    val serialNumber: String? = null,
)

@Serializable
private data class ApiMovementUnit(val deviceUnit: ApiMovementDeviceUnit? = null)

@Serializable
private data class ApiMovementItem(
    val id: String = "",
    val deviceId: String = "",
    val device: ApiMovementDevice? = null,
    val quantity: Int = 0,
    val condition: String? = null,
    val notes: String? = null,
    val units: List<ApiMovementUnit> = emptyList(),
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
    val units: List<UnitIdentityInput> = emptyList(),
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
private data class ApiDashboardDevice(val total: Int = 0)

@Serializable
private data class ApiDashboardType(val name: String = "", val devices: List<ApiDashboardDevice> = emptyList())

@Serializable
private data class ApiDashboard(
    val stats: ApiDashboardStats = ApiDashboardStats(),
    val byType: List<ApiDashboardType> = emptyList(),
)

/** Total de dispositivos por categoría (tipo) del inicio. */
data class DeviceCategoryDto(val name: String, val total: Int)
