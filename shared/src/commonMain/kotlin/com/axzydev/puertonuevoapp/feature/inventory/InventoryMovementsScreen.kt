package com.axzydev.puertonuevoapp.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.RegisterTopBarAction
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.ui.table.AppTableScreen
import com.axzydev.puertonuevoapp.core.ui.table.FilterControl
import com.axzydev.puertonuevoapp.core.ui.table.FilterOption
import com.axzydev.puertonuevoapp.core.ui.table.TableFilterSpec
import com.axzydev.puertonuevoapp.core.ui.table.TableSortSpec
import com.axzydev.puertonuevoapp.core.util.formatDateTime
import com.axzydev.puertonuevoapp.core.util.movementTypeLabel

/**
 * Kardex / Movimientos: pantalla piloto del estándar de tablas (`AppTableScreen`
 * + [InventoryMovementsViewModel]). Columnas: fecha, tipo, dispositivo,
 * unidades/cantidad, motivo y usuario; filtros por columna (tipo catálogo, rango
 * de fechas, dispositivo y usuario) y orden por fecha/tipo. Tocar una fila abre el
 * detalle del dispositivo cuando el movimiento trae uno.
 *
 * Los movimientos de mantenimiento se pueden **revertir** desde la fila (con
 * confirmación y solo con `devices.edit`); el API decide siempre y, si el
 * movimiento ya no es reversible, su mensaje se muestra en un snackbar.
 *
 * El botón "Nuevo movimiento" (solo con `devices.edit`) va en la barra superior del
 * shell; la tabla ya no necesita el icono de filtros ahí porque los filtros viven
 * en el chip de la cabecera.
 */
@Composable
fun InventoryMovementsScreen(
    viewModel: InventoryMovementsViewModel = viewModel {
        InventoryMovementsViewModel(AppContainer.inventoryApi)
    },
) {
    val state by viewModel.uiState.collectAsState()
    val revertingId by viewModel.revertingId.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canRegister = user?.canRegisterMovement == true
    val canRevert = user?.canRegisterMovement == true

    // Fila pendiente de confirmar la reversión (el modal nombra el movimiento).
    var pendingRevert by remember { mutableStateOf<InventoryMovementRow?>(null) }

    if (canRegister) {
        RegisterTopBarAction(Icons.Filled.Add, "Nuevo movimiento") {
            navigator.push(Screen.NewInventoryMovement())
        }
    }

    AppTableScreen(
        state = state,
        heading = "Kardex",
        filterSpecs = movementFilterSpecs,
        onSearchChange = viewModel::onSearchChange,
        onFiltersChange = viewModel::onFiltersChange,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onRetry = viewModel::retry,
        searchPlaceholder = "Buscar por dispositivo, motivo o usuario",
        emptyMessage = "No hay movimientos con estos filtros",
        searchKey = MovementFilterKeys.Search,
        sortSpecs = movementSortSpecs,
        onSortChange = viewModel::onSortChange,
        itemKey = { it.id },
    ) { row ->
        MovementCard(
            row = row,
            onOpenDevice = if (row.deviceId.isNotBlank()) {
                { navigator.push(Screen.DeviceDetail(row.deviceId)) }
            } else {
                null
            },
            canRevert = canRevert,
            reverting = revertingId == row.id,
            onRevert = { pendingRevert = row },
        )
    }

    pendingRevert?.let { row ->
        RevertMovementModal(
            row = row,
            onConfirm = {
                pendingRevert = null
                viewModel.revert(row)
            },
            onDismiss = { pendingRevert = null },
        )
    }
}

/** Confirmación de la reversión: nombra tipo, fecha y dispositivo del movimiento. */
@Composable
private fun RevertMovementModal(
    row: InventoryMovementRow,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppModal(
        title = "Revertir movimiento",
        icon = Icons.AutoMirrored.Filled.Undo,
        tone = AppModalTone.Danger,
        confirmLabel = "Revertir",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    ) {
        Text(
            text = "¿Revertir ${movementTypeLabelOf(row.type).lowercase()} del " +
                "${formatDateTime(row.date)} sobre ${row.deviceLabel}?",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "El movimiento quedará cancelado y la reversión se registrará en el kardex. " +
                "Solo los movimientos de mantenimiento se pueden revertir; el API rechaza el resto.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
    }
}

private val movementSortSpecs: List<TableSortSpec> = listOf(
    TableSortSpec("date", "Fecha"),
    TableSortSpec("type", "Tipo"),
)

/** Catálogo completo de tipos del API (`MovementType` del schema) con su etiqueta. */
private val movementTypeOptions: List<Pair<String, String>> = listOf(
    "STOCK_IN" to "Entrada",
    "ADJUSTMENT_IN" to "Ajuste de entrada",
    "STOCK_OUT" to "Salida",
    "ADJUSTMENT_OUT" to "Ajuste de salida",
    "LOAN" to "Asignado",
    "RETURN" to "Devolución",
    "TRANSFER" to "Traslado",
    "MAINTENANCE_IN" to "Mantenimiento entrada",
    "MAINTENANCE_OUT" to "Mantenimiento salida",
    "RETIREMENT" to "Baja",
    "REVERSAL" to "Reversión",
)

private val movementFilterSpecs: List<TableFilterSpec> = listOf(
    TableFilterSpec(
        MovementFilterKeys.Type,
        "Tipo",
        FilterControl.Select(movementTypeOptions.map { FilterOption(it.first, it.second) }),
    ),
    TableFilterSpec(MovementFilterKeys.Date, "Fecha", FilterControl.DateRange),
    TableFilterSpec(MovementFilterKeys.Device, "Dispositivo", FilterControl.Text),
    TableFilterSpec(MovementFilterKeys.User, "Usuario", FilterControl.Text),
)

private fun movementTypeLabelOf(type: String): String =
    movementTypeOptions.firstOrNull { it.first == type }?.second ?: movementTypeLabel(type)

/** Color por tipo; `AppColors.movementTypeColor` no cubre ajustes/mantenimiento/reversión. */
private fun movementTypeColorOf(type: String): Color = when (type) {
    "STOCK_IN", "ADJUSTMENT_IN", "RETURN" -> AppColors.Success
    "STOCK_OUT", "ADJUSTMENT_OUT", "LOAN", "MAINTENANCE_OUT" -> AppColors.Warning
    "RETIREMENT", "REVERSAL" -> AppColors.Danger
    "TRANSFER", "MAINTENANCE_IN" -> AppColors.Info
    else -> AppColors.TextFaint
}

@Composable
private fun MovementCard(
    row: InventoryMovementRow,
    onOpenDevice: (() -> Unit)?,
    canRevert: Boolean,
    reverting: Boolean,
    onRevert: () -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenDevice,
        borderColor = AppColors.Outline.copy(alpha = 0.5f),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(movementTypeLabelOf(row.type), movementTypeColorOf(row.type))
            if (row.status == "CANCELLED") {
                Spacer(Modifier.width(6.dp))
                StatusChip("Cancelado", AppColors.Danger)
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = formatDateTime(row.date),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = row.deviceLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Unidades: ${row.unitsLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        row.reason?.takeIf { it.isNotBlank() }?.let { reason ->
            Text(
                text = "Motivo: $reason",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Registró: ${row.userLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (onOpenDevice != null) {
                Text(
                    text = "Ver dispositivo",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.EmeraldPrimary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        if (canRevert && row.isReversible) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (reverting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = AppColors.Danger,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Revirtiendo…", style = MaterialTheme.typography.labelSmall, color = AppColors.Danger)
                } else {
                    AppFilterChip(
                        label = "Revertir",
                        selected = true,
                        selectedColor = AppColors.Danger,
                        onClick = onRevert,
                    )
                }
            }
        }
    }
}
