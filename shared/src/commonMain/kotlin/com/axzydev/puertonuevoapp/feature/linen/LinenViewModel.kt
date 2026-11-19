package com.axzydev.puertonuevoapp.feature.linen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.ApiException
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.linen.AddLinesInput
import com.axzydev.puertonuevoapp.core.network.linen.DeliverInput
import com.axzydev.puertonuevoapp.core.network.linen.DifferenceInput
import com.axzydev.puertonuevoapp.core.network.linen.FulfillInput
import com.axzydev.puertonuevoapp.core.network.linen.MissingLineInput
import com.axzydev.puertonuevoapp.core.network.linen.ProductQtyInput
import com.axzydev.puertonuevoapp.core.network.linen.QueueLineDto
import com.axzydev.puertonuevoapp.core.network.linen.FulfillLineInput
import com.axzydev.puertonuevoapp.core.network.linen.HotelRoomDto
import com.axzydev.puertonuevoapp.core.network.linen.LinenApi
import com.axzydev.puertonuevoapp.core.network.linen.LinenProductRefDto
import com.axzydev.puertonuevoapp.core.network.linen.QueueItemDto
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Pedido de blancos por habitación (tablet/teléfono). Una sola pantalla con
 * secciones según los permisos del usuario: extras/diferencias (supervisora), check-out (recepción), surtir
 * (lavandería) y llevar (auxiliar de piso). La cola se refresca por polling
 * desde la pantalla (cada ~20 s mientras está visible) y con pull to refresh.
 */
class LinenViewModel(
    private val api: LinenApi,
    private val canRequest: Boolean,
    private val canChangeStatus: Boolean = false,
) : ViewModel() {

    private companion object {
        const val MULTI_BUSY = "__multi__"
    }

    private val _uiState = MutableStateFlow(LinenUiState())
    val uiState: StateFlow<LinenUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Carga inicial: cola + habitaciones (solo quien puede pedir las usa). */
    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { fetchQueue() }
                .onFailure { e -> _uiState.update { it.copy(loading = false, error = networkMessage(e)) } }
            if (canChangeStatus) loadRooms()
        }
    }

    /** Pull to refresh / polling. En el polling (`silent`) un fallo no pisa lo ya mostrado. */
    fun refresh(silent: Boolean = false) {
        if (!silent) _uiState.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            runCatching { fetchQueue() }
                .onFailure { e ->
                    if (!silent) _uiState.update { it.copy(error = networkMessage(e)) }
                }
            // Tras el check-out la habitación pasa a limpieza: hay que recargar también las habitaciones.
            if (canChangeStatus) loadRooms(silent)
            _uiState.update { it.copy(refreshing = false) }
        }
    }

    private suspend fun fetchQueue() {
        val today = api.today()
        // `delivered` solo alimenta "Reportar diferencia"; si falla no debe tumbar la lista.
        val deliveredIds = runCatching { api.queue().delivered.map { it.id }.toSet() }.getOrNull()
        _uiState.update { state ->
            // Conserva los ajustes de cantidad solo de renglones que siguen pendientes.
            val live = today.filter { it.bucket == LinenBucket.Pending }.flatMap { it.lines }.map { it.id }.toSet()
            state.copy(
                loading = false,
                error = null,
                today = today,
                loaded = true,
                deliveredIds = deliveredIds ?: state.deliveredIds,
                fulfillQty = state.fulfillQty.filterKeys { it in live },
            )
        }
    }

    fun setSearch(value: String) = _uiState.update { it.copy(search = value) }

    fun setFilter(value: LinenBucket?) = _uiState.update { it.copy(filter = value) }

    fun openDetail(item: QueueItemDto) = _uiState.update { it.copy(detailId = item.id) }

    fun closeDetail() = _uiState.update { it.copy(detailId = null) }

    // --- Historial --------------------------------------------------------------------

    /** Carga la primera página del historial (o la siguiente con `more`). */
    fun loadHistory(more: Boolean = false) {
        val state = _uiState.value
        if (state.historyLoading) return
        val page = if (more) state.historyPage + 1 else 1
        _uiState.update { it.copy(historyLoading = true, historyError = null) }
        viewModelScope.launch {
            runCatching { api.history(page) }
                .onSuccess { result ->
                    _uiState.update {
                        val items = if (more) it.history + result.data else result.data
                        it.copy(
                            history = items,
                            historyPage = result.page,
                            historyHasMore = items.size < result.total,
                            historyLoading = false,
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(historyLoading = false, historyError = networkMessage(e)) } }
        }
    }

    private suspend fun loadRooms(silent: Boolean = false) {
        runCatching { api.rooms() }.onSuccess { all ->
            val sorted = all
                .filter { it.active }
                .sortedWith(compareBy<HotelRoomDto> { it.number.toIntOrNull() ?: Int.MAX_VALUE }.thenBy { it.number })
            _uiState.update { it.copy(rooms = sorted) }
        }.onFailure { e -> if (!silent) AppSnackbar.show(networkMessage(e), isError = true) }
    }

    fun toggleShowAllRooms() {
        _uiState.update { it.copy(showAllRooms = !it.showAllRooms) }
    }

    // --- Recepción: check-out ---------------------------------------------------------

    fun askCheckOut(room: HotelRoomDto) = _uiState.update { it.copy(checkOutRoom = room) }

    fun cancelCheckOut() {
        if (!_uiState.value.checkingOut) _uiState.update { it.copy(checkOutRoom = null) }
    }

    /** `PATCH /hotel/rooms/:id/status {to: CLEANING}`: el servidor abre la limpieza y crea el pedido de blancos. */
    fun confirmCheckOut() {
        val room = _uiState.value.checkOutRoom ?: return
        if (_uiState.value.checkingOut) return
        _uiState.update { it.copy(checkingOut = true) }
        viewModelScope.launch {
            runCatching { api.changeRoomStatus(room.id, "CLEANING") }
                .onSuccess { AppSnackbar.show("Check-out registrado · Hab. ${room.number}") }
                .onFailure { e -> AppSnackbar.show(networkMessage(e), isError = true) }
            _uiState.update { it.copy(checkingOut = false, checkOutRoom = null) }
            refresh(silent = true)
        }
    }

    // --- Supervisora: agregar extras a un pedido SUBMITTED --------------------------

    fun openExtras(item: QueueItemDto) {
        _uiState.update { it.copy(extras = ExtrasDialogState(item), detailId = null) }
        if (_uiState.value.catalog.isNotEmpty()) return
        viewModelScope.launch {
            runCatching { api.products() }
                .onSuccess { all -> _uiState.update { s -> s.copy(catalog = all.filter { it.kind == "LINEN" && it.active }) } }
                .onFailure { e -> AppSnackbar.show(networkMessage(e), isError = true) }
        }
    }

    fun closeExtras() {
        if (_uiState.value.extras?.submitting != true) _uiState.update { it.copy(extras = null) }
    }

    fun changeExtraQty(productId: String, delta: Double) {
        _uiState.update { state ->
            val dialog = state.extras ?: return@update state
            val rows = dialog.rows.map {
                if (it.product.id == productId) it.copy(quantity = (it.quantity + delta).coerceAtLeast(0.0)) else it
            }
            state.copy(extras = dialog.copy(rows = rows))
        }
    }

    fun addExtra(productId: String) {
        _uiState.update { state ->
            val dialog = state.extras ?: return@update state
            val product = state.catalog.firstOrNull { it.id == productId } ?: return@update state
            if (dialog.rows.any { it.product.id == productId }) return@update state
            val row = TurnoverRow(LinenProductRefDto(product.id, product.code, product.name, product.size), 1.0)
            state.copy(extras = dialog.copy(rows = dialog.rows + row))
        }
    }

    fun submitExtras() {
        val dialog = _uiState.value.extras ?: return
        if (!dialog.canSubmit) return
        _uiState.update { it.copy(extras = dialog.copy(submitting = true)) }
        viewModelScope.launch {
            val lines = dialog.rows.filter { it.quantity > 0 }.map { ProductQtyInput(it.product.id, it.quantity) }
            runCatching { api.addLines(dialog.item.id, AddLinesInput(lines)) }
                .onSuccess {
                    AppSnackbar.show("Extras agregados · Hab. ${dialog.item.room?.number.orEmpty()}")
                    _uiState.update { it.copy(extras = null) }
                    refresh(silent = true)
                }
                .onFailure { e ->
                    AppSnackbar.show(networkMessage(e), isError = true)
                    _uiState.update { s -> s.copy(extras = s.extras?.copy(submitting = false)) }
                }
        }
    }

    // --- Supervisora: reportar diferencia -------------------------------------------

    fun openDifference(item: QueueItemDto) =
        _uiState.update { it.copy(difference = DifferenceDialogState(item), detailId = null) }

    fun closeDifference() {
        if (_uiState.value.difference?.submitting != true) _uiState.update { it.copy(difference = null) }
    }

    fun changeMissingQty(line: QueueLineDto, delta: Double) {
        _uiState.update { state ->
            val dialog = state.difference ?: return@update state
            val next = ((dialog.missingQty[line.product.id] ?: 0.0) + delta).coerceIn(0.0, line.receivedQty)
            state.copy(difference = dialog.copy(missingQty = dialog.missingQty + (line.product.id to next)))
        }
    }

    fun setDifferenceNotes(value: String) =
        _uiState.update { s -> s.copy(difference = s.difference?.copy(notes = value)) }

    fun submitDifference() {
        val dialog = _uiState.value.difference ?: return
        if (!dialog.canSubmit) return
        _uiState.update { it.copy(difference = dialog.copy(submitting = true)) }
        viewModelScope.launch {
            val lines = dialog.missingQty.filterValues { it > 0 }.map { (id, qty) -> MissingLineInput(id, qty) }
            runCatching { api.reportDifference(dialog.item.id, DifferenceInput(lines, dialog.notes.trim())) }
                .onSuccess { result ->
                    AppSnackbar.show(
                        if (result.adjustment?.status == "PENDING_APPROVAL") "Diferencia reportada (pendiente de aprobación)"
                        else "Diferencia reportada",
                    )
                    _uiState.update { it.copy(difference = null) }
                    refresh(silent = true)
                }
                .onFailure { e ->
                    AppSnackbar.show(networkMessage(e), isError = true)
                    _uiState.update { s -> s.copy(difference = s.difference?.copy(submitting = false)) }
                }
        }
    }

    // --- Supervisora: confirmar recibido (solo pedidos MANUAL) ----------------------

    fun confirm(item: QueueItemDto) = runAction(item.id, "Pedido confirmado") { api.confirm(item.id) }

    // --- Lavandería: surtir ---------------------------------------------------------

    fun changeFulfillQty(item: QueueItemDto, lineId: String, delta: Double) {
        val line = item.lines.firstOrNull { it.id == lineId } ?: return
        _uiState.update { state ->
            val next = (state.fulfillQtyOf(line) + delta).coerceIn(0.0, line.pendingQty)
            state.copy(fulfillQty = state.fulfillQty + (lineId to next))
        }
    }

    fun fulfill(item: QueueItemDto) {
        val state = _uiState.value
        val lines = item.lines.filter { it.pendingQty > 0 }.map { FulfillLineInput(it.id, state.fulfillQtyOf(it)) }
        if (lines.none { it.deliveredQty > 0 }) return
        _uiState.update { it.copy(detailId = null) }
        runAction(item.id, "Pedido listo para recoger") { api.fulfill(item.id, FulfillInput(lines)) }
    }

    // --- Auxiliar de piso -----------------------------------------------------------

    fun pickup(item: QueueItemDto) = runAction(item.id, "Pedido recogido") { api.pickup(item.id) }

    fun toggleSelectMode() = _uiState.update { it.copy(selectMode = !it.selectMode, selectedIds = emptySet()) }

    fun toggleSelected(id: String) = _uiState.update {
        it.copy(selectedIds = if (id in it.selectedIds) it.selectedIds - id else it.selectedIds + id)
    }

    /** Recoge todos los marcados; si otra persona tomó alguno primero se avisa cuáles. */
    fun pickupSelected() {
        val state = _uiState.value
        val ids = state.selectedIds.toList()
        if (ids.isEmpty() || state.busyId != null) return
        _uiState.update { it.copy(busyId = MULTI_BUSY) }
        viewModelScope.launch {
            runCatching { api.pickupMany(ids) }
                .onSuccess { result ->
                    val rooms = result.conflicts.mapNotNull { id -> _uiState.value.today.firstOrNull { it.id == id }?.room?.number }
                    if (result.conflicts.isEmpty()) {
                        AppSnackbar.show("${result.picked.size} pedidos recogidos")
                    } else {
                        val which = if (rooms.isEmpty()) "" else " (Hab. ${rooms.joinToString(", ")})"
                        AppSnackbar.show(
                            "Recogidos ${result.picked.size}. ${result.conflicts.size} ya los tomó otra persona$which.",
                            isError = true,
                        )
                    }
                }
                .onFailure { e -> AppSnackbar.show(networkMessage(e), isError = true) }
            _uiState.update { it.copy(busyId = null, selectMode = false, selectedIds = emptySet()) }
            refresh(silent = true)
        }
    }

    /** Paso "Sucio recogido": parte de lo entregado de cada producto. */
    fun openDeliver(item: QueueItemDto) {
        val qty = item.lines.filter { it.deliveredQty > 0 }.associate { it.product.id to it.deliveredQty }
        _uiState.update { it.copy(deliver = DeliverDialogState(item, qty)) }
    }

    fun closeDeliver() {
        if (_uiState.value.deliver?.submitting != true) _uiState.update { it.copy(deliver = null) }
    }

    fun changeDirtyQty(productId: String, delta: Double) {
        _uiState.update { state ->
            val dialog = state.deliver ?: return@update state
            val next = ((dialog.dirtyQty[productId] ?: 0.0) + delta).coerceAtLeast(0.0)
            state.copy(deliver = dialog.copy(dirtyQty = dialog.dirtyQty + (productId to next)))
        }
    }

    fun confirmDeliver() {
        val dialog = _uiState.value.deliver ?: return
        if (dialog.submitting) return
        _uiState.update { it.copy(deliver = dialog.copy(submitting = true)) }
        viewModelScope.launch {
            val lines = dialog.dirtyQty.map { (id, qty) -> ProductQtyInput(id, qty) }
            runCatching { api.deliver(dialog.item.id, DeliverInput(lines)) }
                .onSuccess { AppSnackbar.show("Pedido entregado") }
                .onFailure { e -> AppSnackbar.show(networkMessage(e), isError = true) }
            _uiState.update { it.copy(deliver = null) }
            refresh(silent = true)
        }
    }

    /** Ejecuta una acción sobre un pedido y siempre refresca la cola (otro usuario pudo tomarlo). */
    private fun runAction(id: String, successMessage: String, call: suspend () -> Unit) {
        if (_uiState.value.busyId != null) return
        _uiState.update { it.copy(busyId = id) }
        viewModelScope.launch {
            runCatching { call() }
                .onSuccess { AppSnackbar.show(successMessage) }
                .onFailure { e ->
                    // 409: otra persona recogió/gestionó el pedido primero.
                    val message = if (e is ApiException && e.status == 409) {
                        "Otra persona ya tomó este pedido. Se actualizó la lista."
                    } else {
                        networkMessage(e)
                    }
                    AppSnackbar.show(message, isError = true)
                }
            _uiState.update { it.copy(busyId = null) }
            refresh(silent = true)
        }
    }
}
