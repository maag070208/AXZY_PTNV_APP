package com.axzydev.puertonuevoapp.feature.linen

import com.axzydev.puertonuevoapp.core.network.linen.HotelProductDto
import com.axzydev.puertonuevoapp.core.network.linen.HotelRoomDto
import com.axzydev.puertonuevoapp.core.network.linen.LinenProductRefDto
import com.axzydev.puertonuevoapp.core.network.linen.QueueItemDto
import com.axzydev.puertonuevoapp.core.network.linen.QueueLineDto

/** Renglón editable (extras de la supervisora). */
data class TurnoverRow(val product: LinenProductRefDto, val quantity: Double)

/** Diálogo "Agregar extras" sobre un pedido por habitación aún por surtir. */
data class ExtrasDialogState(
    val item: QueueItemDto,
    val rows: List<TurnoverRow> = emptyList(),
    val submitting: Boolean = false,
) {
    val canSubmit: Boolean get() = !submitting && rows.any { it.quantity > 0 }
}

/** Paso "Sucio recogido" antes de entregar: cantidades por producto (por omisión, lo entregado). */
data class DeliverDialogState(
    val item: QueueItemDto,
    val dirtyQty: Map<String, Double>,
    val submitting: Boolean = false,
)

/** "Reportar diferencia": faltante por producto + nota obligatoria. */
data class DifferenceDialogState(
    val item: QueueItemDto,
    val missingQty: Map<String, Double> = emptyMap(),
    val notes: String = "",
    val submitting: Boolean = false,
) {
    val canSubmit: Boolean get() = !submitting && notes.isNotBlank() && missingQty.values.any { it > 0 }
}

/** Grupo visual de un pedido por habitación en la app (pendiente / en proceso / lista). */
enum class LinenBucket { Pending, InProgress, Done }

/** Estado del pedido -> grupo (misma regla que `turnoverBucket` del API). */
val QueueItemDto.bucket: LinenBucket
    get() = when (status) {
        "READY_FOR_PICKUP", "IN_TRANSIT" -> LinenBucket.InProgress
        "FULFILLED", "RECEIVED" -> LinenBucket.Done
        else -> LinenBucket.Pending
    }

data class LinenUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    /** Pedidos por habitación de hoy (más los aún abiertos de días previos). */
    val today: List<QueueItemDto> = emptyList(),
    val loaded: Boolean = false,
    val search: String = "",
    val filter: LinenBucket? = null,
    /** Pedido abierto en la hoja de detalle. */
    val detailId: String? = null,
    val history: List<QueueItemDto> = emptyList(),
    val historyPage: Int = 0,
    val historyHasMore: Boolean = false,
    val historyLoading: Boolean = false,
    val historyError: String? = null,
    val rooms: List<HotelRoomDto> = emptyList(),
    /** Supervisora: false = solo habitaciones OCUPADAS (flujo rápido); true = todas. */
    val showAllRooms: Boolean = false,
    /** Catálogo de blancos activos para "agregar algo más" (se carga al abrir el diálogo). */
    val catalog: List<HotelProductDto> = emptyList(),
    /** Cantidad a surtir por renglón (lineId -> cantidad); lo no editado usa `pendingQty`. */
    val fulfillQty: Map<String, Double> = emptyMap(),
    /** Pedido con una acción en curso (evita doble toque). */
    val busyId: String? = null,
    val extras: ExtrasDialogState? = null,
    val deliver: DeliverDialogState? = null,
    val difference: DifferenceDialogState? = null,
    /** Recepción: habitación pendiente de confirmar el check-out. */
    val checkOutRoom: HotelRoomDto? = null,
    val checkingOut: Boolean = false,
    /** Auxiliar de piso: modo "recoger varias" y pedidos marcados. */
    val selectMode: Boolean = false,
    /** Pedidos entregados por este usuario (últimas 12 h) a los que puede reportar diferencia. */
    val deliveredIds: Set<String> = emptySet(),
    val selectedIds: Set<String> = emptySet(),
) {
    fun fulfillQtyOf(line: QueueLineDto): Double =
        fulfillQty[line.id] ?: line.pendingQty

    /** Habitaciones a mostrar: solo ocupadas por defecto, o todas. */
    val visibleRooms: List<HotelRoomDto>
        get() = if (showAllRooms) rooms else rooms.filter { it.status == "OCCUPIED" }

    /** Habitación -> pedido abierto (aún sin cerrar). */
    val openByRoom: Map<String, QueueItemDto>
        get() = today.filter { it.status != "RECEIVED" }.mapNotNull { item -> item.room?.let { it.id to item } }.toMap()

    fun count(bucket: LinenBucket?): Int = if (bucket == null) today.size else today.count { it.bucket == bucket }

    /** Lista de la pantalla Blancos: filtro + búsqueda, pendientes primero y por número de habitación. */
    val visibleToday: List<QueueItemDto>
        get() {
            val q = search.trim()
            return today
                .filter { filter == null || it.bucket == filter }
                .filter { q.isEmpty() || it.room?.number.orEmpty().contains(q, ignoreCase = true) }
                .sortedWith(
                    compareBy<QueueItemDto> { it.bucket.ordinal }
                        .thenBy { it.room?.number?.filter(Char::isDigit)?.toIntOrNull() ?: Int.MAX_VALUE }
                        .thenBy { it.room?.number.orEmpty() },
                )
        }

    val detail: QueueItemDto? get() = detailId?.let { id -> today.firstOrNull { it.id == id } }
}
