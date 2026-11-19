package com.axzydev.puertonuevoapp.core.network.linen

import kotlinx.serialization.Serializable

/**
 * DTOs del pedido de blancos por habitación (`/hotel/linen`). Espejo de
 * `web/src/entities/hotel-linen/model/turnoverTypes.ts`; las cantidades llegan
 * como número (piezas) y se manejan como `Double` para tolerar decimales.
 */

/** Habitación del hotel (`GET /hotel/rooms`); solo lo que usa la cuadrícula. */
@Serializable
data class HotelRoomDto(
    val id: String,
    val number: String,
    val active: Boolean = true,
    val status: String = "",
)

/** Producto del catálogo del hotel (`GET /hotel/products`); solo lo necesario para "agregar algo más". */
@Serializable
data class HotelProductDto(
    val id: String,
    val code: String = "",
    val name: String,
    val kind: String = "",
    val size: String? = null,
    val active: Boolean = true,
)

@Serializable
data class LinenProductRefDto(
    val id: String,
    val code: String = "",
    val name: String,
    val size: String? = null,
)

@Serializable
data class LinenRoomRefDto(
    val id: String,
    val number: String,
)

@Serializable
data class LinenAreaRefDto(
    val id: String,
    val code: String = "",
    val name: String = "",
)

@Serializable
data class LinenUserRefDto(
    val id: String,
    val name: String = "",
)

@Serializable
data class SuggestionLineDto(
    val product: LinenProductRefDto,
    val quantity: Double,
)

@Serializable
data class OpenRequestRefDto(
    val id: String,
    val folio: String = "",
    val status: String = "",
)

/** `GET /hotel/linen/rooms/:id/turnover-suggestion`. */
@Serializable
data class TurnoverSuggestionDto(
    val room: LinenRoomRefDto,
    val area: LinenAreaRefDto? = null,
    val lines: List<SuggestionLineDto> = emptyList(),
    val openRequest: OpenRequestRefDto? = null,
)

@Serializable
data class TurnoverLineInput(
    val productId: String,
    val requestedQty: Double,
)

/** Cuerpo de `POST /hotel/linen/supply-requests/room-turnover`. */
@Serializable
data class TurnoverInput(
    val roomId: String,
    val lines: List<TurnoverLineInput>? = null,
)

@Serializable
data class TurnoverRequestRefDto(
    val id: String,
    val folio: String = "",
    val status: String = "",
)

/** 201 = creado (`created = true`); 200 = ya existía un pedido abierto. */
@Serializable
data class TurnoverResultDto(
    val created: Boolean = true,
    val request: TurnoverRequestRefDto,
)

@Serializable
data class QueueLineDto(
    val id: String,
    val product: LinenProductRefDto,
    val requestedQty: Double = 0.0,
    val deliveredQty: Double = 0.0,
    /** Lo que la habitación ya recibió (tope del faltante en "Reportar diferencia"). */
    val receivedQty: Double = 0.0,
    val dirtyCollectedQty: Double? = null,
    val pendingQty: Double = 0.0,
)

@Serializable
data class DifferenceLineDto(val productId: String = "", val quantity: Double = 0.0)

/** Diferencia ya reportada sobre el pedido. */
@Serializable
data class DifferenceRefDto(
    val id: String = "",
    val folio: String = "",
    val status: String = "",
    val notes: String? = null,
    val createdAt: String = "",
    val lines: List<DifferenceLineDto> = emptyList(),
)

@Serializable
data class QueueItemDto(
    val id: String,
    val folio: String = "",
    val status: String,
    val origin: String = "MANUAL",
    val room: LinenRoomRefDto? = null,
    val area: LinenAreaRefDto? = null,
    val requestedBy: LinenUserRefDto? = null,
    val carrier: LinenUserRefDto? = null,
    val createdAt: String = "",
    val readyAt: String? = null,
    val pickedUpAt: String? = null,
    val deliveredAt: String? = null,
    val dirtyCollectedAt: String? = null,
    val differences: List<DifferenceRefDto> = emptyList(),
    val updatedAt: String? = null,
    val ageMinutes: Int = 0,
    val lines: List<QueueLineDto> = emptyList(),
)

/** `GET /hotel/linen/supply-requests/history`: una página de pedidos por habitación (últimos 60 días). */
@Serializable
data class LinenHistoryPageDto(
    val data: List<QueueItemDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 30,
)

/** Cola por rol (`GET /hotel/linen/supply-requests/queue`): cada sección llega vacía si no hay permiso. */
@Serializable
data class TurnoverQueueDto(
    val toFulfill: List<QueueItemDto> = emptyList(),
    val requested: List<QueueItemDto> = emptyList(),
    val toPickUp: List<QueueItemDto> = emptyList(),
    val carrying: List<QueueItemDto> = emptyList(),
    /** Pedidos de habitación de este usuario entregados en las últimas 12 h (aquí se reporta diferencia). */
    val delivered: List<QueueItemDto> = emptyList(),
)

@Serializable
data class AdjustmentRefDto(val id: String = "", val folio: String = "", val status: String = "")

/** Respuesta de `POST .../difference`; el ajuste puede quedar `PENDING_APPROVAL`. */
@Serializable
data class DifferenceResultDto(val adjustment: AdjustmentRefDto? = null)

@Serializable
data class FulfillLineInput(
    val lineId: String,
    val deliveredQty: Double,
)

/** Cuerpo de `POST /hotel/linen/supply-requests/:id/fulfill`. */
@Serializable
data class FulfillInput(
    val lines: List<FulfillLineInput>,
)

/** Cuerpo vacío (`{}`) de las acciones sin datos (confirm, pickup, deliver). */
@Serializable
class EmptyBody


/** Cuerpo de `PATCH /hotel/rooms/:id/status` (check-out: `CLEANING`). */
@Serializable
data class RoomStatusInput(val to: String)

/** Respuesta del cambio de estado; solo interesa que sea exitoso. */
@Serializable
data class RoomStatusResultDto(val id: String = "")

@Serializable
data class ProductQtyInput(val productId: String, val quantity: Double)

/** Cuerpo de `POST /hotel/linen/supply-requests/:id/lines` (extras de la supervisora). */
@Serializable
data class AddLinesInput(val lines: List<ProductQtyInput>)

@Serializable
data class MissingLineInput(val productId: String, val missingQty: Double)

/** Cuerpo de `POST /hotel/linen/supply-requests/:id/difference`. */
@Serializable
data class DifferenceInput(val lines: List<MissingLineInput>, val notes: String)

/** Cuerpo de `POST /hotel/linen/supply-requests/pickup` (recoger varios). */
@Serializable
data class PickupManyInput(val ids: List<String>)

@Serializable
data class PickupManyResultDto(
    val picked: List<String> = emptyList(),
    val conflicts: List<String> = emptyList(),
)

/** Cuerpo de `POST /hotel/linen/supply-requests/:id/deliver` con el sucio recogido. */
@Serializable
data class DeliverInput(val dirtyLines: List<ProductQtyInput>)
