package com.axzydev.puertonuevoapp.core.network.linen

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import io.ktor.http.encodeURLParameter

/**
 * Puerto de datos del pedido de blancos por habitación. La UI/ViewModel
 * depende de la interfaz para poder sustituirla por un fake en pruebas.
 */
interface LinenApi {
    /** Habitaciones activas (`GET /hotel/rooms`). */
    suspend fun rooms(): List<HotelRoomDto>

    /** Catálogo de productos del hotel (el filtrado a blancos activos lo hace el ViewModel). */
    suspend fun products(): List<HotelProductDto>

    suspend fun suggestion(roomId: String): TurnoverSuggestionDto

    /** "Se desocupó": crea y envía el pedido; si ya hay uno abierto lo devuelve (`created = false`). */
    suspend fun createTurnover(input: TurnoverInput): TurnoverResultDto

    suspend fun queue(): TurnoverQueueDto

    /** Pedidos por habitación de hoy (cualquier estado visible) en las zonas del usuario. */
    suspend fun today(): List<QueueItemDto>

    /** Historial paginado de pedidos por habitación (últimos 60 días). */
    suspend fun history(page: Int): LinenHistoryPageDto

    /** Lavandería surte (queda LISTO PARA RECOGER). */
    suspend fun fulfill(id: String, input: FulfillInput)

    /** Supervisora confirma lo recibido. */
    suspend fun confirm(id: String)

    /** Auxiliar de piso recoge; 409 si otra persona lo tomó primero. */
    suspend fun pickup(id: String)

    /** Entrega con el sucio recogido por producto (por omisión, lo entregado). */
    suspend fun deliver(id: String, input: DeliverInput)

    /** Recepción: check-out de una habitación (`OCCUPIED` -> `CLEANING`); el servidor crea el pedido. */
    suspend fun changeRoomStatus(roomId: String, to: String)

    /** Supervisora: agrega extras a un pedido aún `SUBMITTED`. */
    suspend fun addLines(id: String, input: AddLinesInput)

    /** Supervisora: reporta faltante sobre un pedido ya entregado. */
    suspend fun reportDifference(id: String, input: DifferenceInput): DifferenceResultDto

    /** Recoge varios pedidos; los que otra persona tomó primero vuelven en `conflicts`. */
    suspend fun pickupMany(ids: List<String>): PickupManyResultDto
}

/** Implementación real contra `/hotel/linen` (patrón `core/network/access`). */
class LinenApiClient(private val client: ApiClient) : LinenApi {
    private val base = "/hotel/linen"

    override suspend fun rooms(): List<HotelRoomDto> = client.get("/hotel/rooms?includeInactive=false")

    override suspend fun products(): List<HotelProductDto> = client.get("/hotel/products")

    override suspend fun suggestion(roomId: String): TurnoverSuggestionDto =
        client.get("$base/rooms/${roomId.encodeURLParameter()}/turnover-suggestion")

    override suspend fun createTurnover(input: TurnoverInput): TurnoverResultDto =
        client.post("$base/supply-requests/room-turnover", input)

    override suspend fun queue(): TurnoverQueueDto = client.get("$base/supply-requests/queue")

    override suspend fun today(): List<QueueItemDto> = client.get("$base/supply-requests/today")

    override suspend fun history(page: Int): LinenHistoryPageDto =
        client.get("$base/supply-requests/history?page=$page&pageSize=30")

    override suspend fun fulfill(id: String, input: FulfillInput) =
        client.postNoContent("$base/supply-requests/${id.encodeURLParameter()}/fulfill", input)

    override suspend fun confirm(id: String) =
        client.postNoContent("$base/supply-requests/${id.encodeURLParameter()}/confirm", EmptyBody())

    override suspend fun pickup(id: String) =
        client.postNoContent("$base/supply-requests/${id.encodeURLParameter()}/pickup", EmptyBody())

    override suspend fun deliver(id: String, input: DeliverInput) =
        client.postNoContent("$base/supply-requests/${id.encodeURLParameter()}/deliver", input)

    override suspend fun changeRoomStatus(roomId: String, to: String) {
        client.patch<RoomStatusInput, RoomStatusResultDto>(
            "/hotel/rooms/${roomId.encodeURLParameter()}/status",
            RoomStatusInput(to),
        )
    }

    override suspend fun addLines(id: String, input: AddLinesInput) =
        client.postNoContent("$base/supply-requests/${id.encodeURLParameter()}/lines", input)

    override suspend fun reportDifference(id: String, input: DifferenceInput): DifferenceResultDto =
        client.post("$base/supply-requests/${id.encodeURLParameter()}/difference", input)

    override suspend fun pickupMany(ids: List<String>): PickupManyResultDto =
        client.post("$base/supply-requests/pickup", PickupManyInput(ids))
}
