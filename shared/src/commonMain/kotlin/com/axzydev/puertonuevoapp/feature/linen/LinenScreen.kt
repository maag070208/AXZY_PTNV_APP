package com.axzydev.puertonuevoapp.feature.linen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.linen.QueueItemDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.SessionUser
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoActionButton
import com.axzydev.puertonuevoapp.core.ui.BentoStatusRow
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.util.currentTimeMillis
import com.axzydev.puertonuevoapp.core.util.formatLocalTime
import com.axzydev.puertonuevoapp.core.util.parseIsoToEpochMillis
import kotlinx.coroutines.delay

/** Intervalo del polling de los pedidos mientras la pantalla está visible. */
private const val POLL_MILLIS = 20_000L

/** ViewModel compartido por las pantallas de blancos (Inicio, Blancos, Habitaciones, Historial). */
@Composable
internal fun linenViewModel(): LinenViewModel = viewModel {
    val user = (AppContainer.authRepository.state.value as? AuthState.LoggedIn)?.user
    LinenViewModel(
        AppContainer.linenApi,
        canRequest = user?.canRequestLinen == true,
        canChangeStatus = user?.canChangeRoomStatus == true,
    )
}

@Composable
internal fun currentLinenUser(): SessionUser? =
    (AppContainer.authRepository.state.collectAsState().value as? AuthState.LoggedIn)?.user

/** Refresca al entrar y cada ~20 s mientras la pantalla está en composición (se cancela al salir). */
@Composable
internal fun LinenPolling(viewModel: LinenViewModel) {
    LaunchedEffect(Unit) {
        viewModel.refresh(silent = true)
        while (true) {
            delay(POLL_MILLIS)
            viewModel.refresh(silent = true)
        }
    }
}

/** Pantalla "Blancos": habitaciones de hoy con filtros, búsqueda y la acción que le toca al rol. */
@Composable
fun LinenScreen(viewModel: LinenViewModel = linenViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val user = currentLinenUser()
    val navigator = LocalNavigator.current
    val canRequest = user?.canRequestLinen == true
    val canFulfill = user?.canFulfillLinen == true
    val canCarry = user?.canCarryLinen == true

    LinenPolling(viewModel)

    Column(
        modifier = Modifier.fillMaxSize().background(AppBento.canvas).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            !canRequest && !canFulfill && !canCarry ->
                ErrorState("Tu puesto no tiene pantalla de pedido de blancos.")
            state.loading && !state.loaded -> LoadingState()
            state.error != null && !state.loaded -> ErrorState(state.error ?: "Error", onRetry = viewModel::load)
            else -> {
                AppSearchField(
                    value = state.search,
                    onValueChange = viewModel::setSearch,
                    placeholder = "Buscar habitación",
                    testTag = "linen-search",
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        null to "Todas",
                        LinenBucket.Pending to "Pendientes",
                        LinenBucket.InProgress to "En proceso",
                        LinenBucket.Done to "Listas",
                    ).forEach { (bucket, label) ->
                        AppFilterChip(
                            label = "$label · ${state.count(bucket)}",
                            selected = state.filter == bucket,
                            onClick = { viewModel.setFilter(bucket) },
                        )
                    }
                }
                if (canCarry && state.today.any { it.status == "READY_FOR_PICKUP" && it.carrier == null }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppFilterChip(
                            label = if (state.selectMode) "Cancelar selección" else "Recoger varias",
                            selected = state.selectMode,
                            onClick = viewModel::toggleSelectMode,
                        )
                        if (state.selectMode) {
                            BentoActionButton(
                                label = "Recoger (${state.selectedIds.size})",
                                enabled = state.selectedIds.isNotEmpty() && state.busyId == null,
                                onClick = viewModel::pickupSelected,
                            )
                        }
                    }
                }
                PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val visible = state.visibleToday
                    if (visible.isEmpty()) {
                        EmptyState(if (state.today.isEmpty()) "Sin pedidos de blancos hoy." else "Sin resultados.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(visible, key = { it.id }) { item ->
                                LinenRoomRow(item, state, user, viewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    state.detail?.let { LinenDetailDialog(it, state, user, viewModel) }
    state.extras?.let { ExtrasDialog(it, state, viewModel) }
    state.deliver?.let { DeliverDialog(it, viewModel) }
    state.difference?.let { DifferenceDialog(it, viewModel) }
}

// --- Fila de habitación ---------------------------------------------------------------

internal fun LinenBucket.tone(): BentoTone = when (this) {
    LinenBucket.Pending -> BentoTone.Rose
    LinenBucket.InProgress -> BentoTone.Sun
    LinenBucket.Done -> BentoTone.Mint
}

/** Línea de estado de la fila: "Se desocupó" / "En proceso · hace X min" / "Lista". */
internal fun linenStatusLine(item: QueueItemDto): String = when (item.bucket) {
    LinenBucket.Pending -> "Se desocupó"
    LinenBucket.InProgress -> "En proceso · ${sinceText(lastTransition(item))}"
    LinenBucket.Done -> if (item.status == "RECEIVED" || item.status == "FULFILLED") "Entregado" else "Lista"
}

/** Último cambio de estado: recogido > listo > última actualización. */
internal fun lastTransition(item: QueueItemDto): String? =
    item.pickedUpAt ?: item.readyAt ?: item.updatedAt ?: item.createdAt.takeIf { it.isNotBlank() }

/** "hace 12 min" / "hace 2 h"; menos de un minuto = "ahora". */
internal fun sinceText(iso: String?): String {
    val millis = iso?.let(::parseIsoToEpochMillis) ?: return "hace un momento"
    val minutes = ((currentTimeMillis() - millis) / 60_000L).coerceAtLeast(0)
    return when {
        minutes < 1 -> "ahora"
        minutes < 60 -> "hace $minutes min"
        minutes < 1_440 -> "hace ${minutes / 60} h"
        else -> "hace ${minutes / 1_440} d"
    }
}

@Composable
private fun LinenRoomRow(item: QueueItemDto, state: LinenUiState, user: SessionUser?, vm: LinenViewModel) {
    val bucket = item.bucket
    val busy = state.busyId != null
    val selectable = state.selectMode && item.status == "READY_FOR_PICKUP" && item.carrier == null
    BentoStatusRow(
        title = item.room?.number ?: item.folio,
        status = linenStatusLine(item),
        icon = Icons.Filled.Bed,
        tone = bucket.tone(),
        muted = bucket == LinenBucket.Done,
        onClick = if (selectable) ({ vm.toggleSelected(item.id) }) else ({ vm.openDetail(item) }),
        trailing = {
            val action = if (state.selectMode) null else linenRowAction(item, user)
            when {
                selectable -> Checkbox(checked = item.id in state.selectedIds, onCheckedChange = { vm.toggleSelected(item.id) })
                action != null -> BentoActionButton(
                    label = action.label,
                    enabled = !busy,
                    onClick = { action.run(vm, item) },
                )
                bucket == LinenBucket.Done -> Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = AppBento.tone(BentoTone.Mint).content,
                    modifier = Modifier.size(22.dp).padding(end = 4.dp),
                )
                else -> Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = AppBento.tone(bucket.tone()).content,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
    )
}

/** Acción de un solo toque según el rol y el estado: nadie ve ni hace más de lo que su rol permite. */
private class RowAction(val label: String, val run: (LinenViewModel, QueueItemDto) -> Unit)

private fun linenRowAction(item: QueueItemDto, user: SessionUser?): RowAction? {
    if (user == null) return null
    return when (item.status) {
        // Lavandería: surte todo lo pendiente (el detalle permite ajustar cantidades).
        "SUBMITTED", "PARTIALLY_FULFILLED" -> when {
            user.canFulfillLinen -> RowAction("Listo") { vm, i -> vm.fulfill(i) }
            item.canAddExtras && user.canRequestLinen -> RowAction("Agregar extras") { vm, i -> vm.openExtras(i) }
            else -> null
        }
        "READY_FOR_PICKUP" ->
            if (user.canCarryLinen && item.carrier == null) RowAction("Recogí") { vm, i -> vm.pickup(i) } else null
        // Auxiliar de piso: antes de entregar confirma el sucio recogido.
        "IN_TRANSIT" ->
            if (user.canCarryLinen && item.carrier?.id == user.id) RowAction("Entregado") { vm, i -> vm.openDeliver(i) } else null
        // Solo los pedidos MANUAL se confirman; los de habitación cierran solos al entregar.
        "FULFILLED" ->
            if (user.canRequestLinen && !item.isTurnover) RowAction("Confirmar") { vm, i -> vm.confirm(i) } else null
        else -> null
    }
}

/** Pedido generado por el check-out (cierra solo al entregar). */
internal val QueueItemDto.isTurnover: Boolean get() = origin == "ROOM_TURNOVER"

/** Extras: solo sobre un pedido de habitación que lavandería aún no atiende. */
internal val QueueItemDto.canAddExtras: Boolean get() = isTurnover && status == "SUBMITTED"

// --- Detalle --------------------------------------------------------------------------

/** Detalle del pedido: renglones (lavandería ajusta cantidades) y quién/cuándo. */
@Composable
private fun LinenDetailDialog(item: QueueItemDto, state: LinenUiState, user: SessionUser?, vm: LinenViewModel) {
    val canAdjust = user?.canFulfillLinen == true && item.bucket == LinenBucket.Pending
    AppModal(
        title = "Hab. ${item.room?.number ?: item.folio}",
        onDismiss = vm::closeDetail,
        icon = Icons.Filled.Bed,
        confirmLabel = if (canAdjust) "Listo" else null,
        onConfirm = if (canAdjust) ({ vm.fulfill(item) }) else null,
        confirmEnabled = canAdjust && state.busyId == null && item.lines.any { state.fulfillQtyOf(it) > 0 },
    ) {
        Column(
            modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(linenStatusLine(item), style = MaterialTheme.typography.labelLarge, color = AppBento.tone(item.bucket.tone()).content)
            item.lines.forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(line.product.name, style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
                        Text(
                            if (canAdjust) "Pedido: ${qtyText(line.pendingQty)}"
                            else "Pedido ${qtyText(line.requestedQty)} · surtido ${qtyText(line.deliveredQty)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextMuted,
                        )
                    }
                    if (canAdjust) {
                        QtyStepper(
                            value = state.fulfillQtyOf(line),
                            onMinus = { vm.changeFulfillQty(item, line.id, -1.0) },
                            onPlus = { vm.changeFulfillQty(item, line.id, 1.0) },
                        )
                    }
                }
            }
            val who = buildList {
                item.requestedBy?.name?.takeIf { it.isNotBlank() }?.let { add("Pidió: $it · ${formatLocalTime(item.createdAt)}") }
                item.readyAt?.let { add("Listo en lavandería: ${formatLocalTime(it)}") }
                item.pickedUpAt?.let { add(listOfNotNull("Recogió:", item.carrier?.name?.takeIf { n -> n.isNotBlank() }, "· ${formatLocalTime(it)}").joinToString(" ")) }
                item.deliveredAt?.let { add("Entregado: ${formatLocalTime(it)}") }
            }
            val extra = buildList {
                item.dirtyCollectedAt?.let { at ->
                    val dirty = item.lines.filter { (it.dirtyCollectedQty ?: 0.0) > 0 }
                        .joinToString(", ") { "${it.product.name} ${qtyText(it.dirtyCollectedQty ?: 0.0)}" }
                    add("Sucio recogido: ${formatLocalTime(at)}" + if (dirty.isNotEmpty()) " · $dirty" else "")
                }
                item.differences.forEach { d ->
                    add("Diferencia ${d.folio} · ${d.status}" + (d.notes?.takeIf { n -> n.isNotBlank() }?.let { n -> " · $n" } ?: ""))
                }
            }
            (who + extra).forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted) }
            if (user?.canRequestLinen == true && item.canAddExtras) {
                BentoActionButton(label = "Agregar extras", onClick = { vm.openExtras(item) }, modifier = Modifier.fillMaxWidth())
            }
            if (user?.canRequestLinen == true && item.isTurnover && item.status == "RECEIVED" && item.id in state.deliveredIds) {
                BentoActionButton(label = "Reportar diferencia", onClick = { vm.openDifference(item) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
internal fun QtyStepper(value: Double, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMinus, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.Remove, contentDescription = "Quitar", tint = AppColors.TextPrimary)
        }
        Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
            Text(qtyText(value), style = MaterialTheme.typography.titleLarge, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onPlus, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "Agregar", tint = AppColors.TextPrimary)
        }
    }
}

/** "Agregar extras": artículos adicionales para un pedido de habitación aún por surtir. */
@Composable
private fun ExtrasDialog(dialog: ExtrasDialogState, state: LinenUiState, vm: LinenViewModel) {
    val options = state.catalog
        .filter { p -> dialog.rows.none { it.product.id == p.id } }
        .map { it.id to listOfNotNull(it.name, it.size).joinToString(" · ") }
    AppModal(
        title = "Extras · Hab. ${dialog.item.room?.number ?: dialog.item.folio}",
        onDismiss = vm::closeExtras,
        icon = Icons.Filled.Bed,
        confirmLabel = "Agregar",
        onConfirm = vm::submitExtras,
        confirmEnabled = dialog.canSubmit,
        saving = dialog.submitting,
    ) {
        Column(
            modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Se suman al pedido que ya está en lavandería", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
            dialog.rows.forEach { row ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(row.product.name, style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
                        row.product.size?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted) }
                    }
                    QtyStepper(
                        value = row.quantity,
                        onMinus = { vm.changeExtraQty(row.product.id, -1.0) },
                        onPlus = { vm.changeExtraQty(row.product.id, 1.0) },
                    )
                }
            }
            if (options.isNotEmpty()) {
                SimpleDropdownField(label = "Agregar artículo", value = "", options = options, onSelect = vm::addExtra)
            }
        }
    }
}

/** Paso "Sucio recogido": parte de lo entregado; el auxiliar corrige lo que realmente se llevó. */
@Composable
private fun DeliverDialog(dialog: DeliverDialogState, vm: LinenViewModel) {
    AppModal(
        title = "Sucio recogido · Hab. ${dialog.item.room?.number ?: dialog.item.folio}",
        onDismiss = vm::closeDeliver,
        icon = Icons.Filled.Bed,
        confirmLabel = "Entregado",
        onConfirm = vm::confirmDeliver,
        saving = dialog.submitting,
    ) {
        Column(
            modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Cantidad de sucio que recoges de la habitación", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
            dialog.item.lines.filter { it.deliveredQty > 0 }.forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(line.product.name, style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
                        Text("Entregado: ${qtyText(line.deliveredQty)}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                    }
                    QtyStepper(
                        value = dialog.dirtyQty[line.product.id] ?: 0.0,
                        onMinus = { vm.changeDirtyQty(line.product.id, -1.0) },
                        onPlus = { vm.changeDirtyQty(line.product.id, 1.0) },
                    )
                }
            }
        }
    }
}

/** "Reportar diferencia": faltante por producto entregado y nota obligatoria. */
@Composable
private fun DifferenceDialog(dialog: DifferenceDialogState, vm: LinenViewModel) {
    AppModal(
        title = "Diferencia · Hab. ${dialog.item.room?.number ?: dialog.item.folio}",
        onDismiss = vm::closeDifference,
        icon = Icons.Filled.Bed,
        confirmLabel = "Reportar",
        onConfirm = vm::submitDifference,
        confirmEnabled = dialog.canSubmit,
        saving = dialog.submitting,
    ) {
        Column(
            modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("¿Cuánto faltó de lo entregado?", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
            dialog.item.lines.filter { it.receivedQty > 0 }.forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(line.product.name, style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
                        Text("Recibido: ${qtyText(line.receivedQty)}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                    }
                    QtyStepper(
                        value = dialog.missingQty[line.product.id] ?: 0.0,
                        onMinus = { vm.changeMissingQty(line, -1.0) },
                        onPlus = { vm.changeMissingQty(line, 1.0) },
                    )
                }
            }
            AppTextField(
                value = dialog.notes,
                onValueChange = vm::setDifferenceNotes,
                label = { Text("Notas (obligatorio)") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// --- Utilidades de texto -------------------------------------------------------------

internal fun qtyText(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

internal fun linenStatusLabel(status: String, origin: String = "MANUAL"): String = when (status) {
    "SUBMITTED" -> "Por surtir"
    "PARTIALLY_FULFILLED" -> "Surtido parcial"
    "READY_FOR_PICKUP" -> "Listo para recoger"
    "IN_TRANSIT" -> "En camino"
    // Los pedidos de habitación cierran al entregar (RECEIVED; FULFILLED = datos previos).
    "FULFILLED" -> if (origin == "ROOM_TURNOVER") "Entregado" else "Por confirmar"
    "DRAFT" -> "Borrador"
    "RECEIVED" -> if (origin == "ROOM_TURNOVER") "Entregado" else "Cerrado"
    "CANCELLED" -> "Cancelado"
    else -> status
}

internal fun linenStatusLabel(item: QueueItemDto): String = linenStatusLabel(item.status, item.origin)
