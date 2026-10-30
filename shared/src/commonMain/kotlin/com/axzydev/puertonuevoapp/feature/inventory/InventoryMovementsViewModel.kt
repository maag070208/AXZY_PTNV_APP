package com.axzydev.puertonuevoapp.feature.inventory

import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.TableSort
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.inventory.InventoryApi
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import com.axzydev.puertonuevoapp.core.ui.table.ClientTableViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Kardex / Movimientos sobre el estándar de tablas (`AppTableScreen`).
 *
 * El API **no** expone `POST /inventory/movements/query`: `GET /inventory/movements`
 * devuelve la lista completa (sin paginar ni filtrar por fecha; solo acepta `type`
 * y `deviceId`). Por eso el piloto usa [ClientTableViewModel], espejo de
 * `makeClientTableFetch` de la web: trae la lista una vez y este ViewModel aplica
 * filtros y orden en memoria, mientras el estándar pagina el resultado.
 *
 * Además de la tabla expone "Revertir" (`POST /inventory/movements/:id/revert`):
 * la API solo revierte movimientos de mantenimiento y responde 409
 * (`MOVEMENT_NOT_REVERSIBLE` / `MOVEMENT_ALREADY_REVERSED`) en el resto; su
 * mensaje se muestra con `networkMessage`.
 *
 * Cuando el API agregue el endpoint de tabla, esta clase pasa a
 * `PaginatedTableViewModel` (con `TableRequest`/`TableResponse` y `sort`) sin tocar
 * la pantalla: `AppTableScreen` es idéntico en ambos casos.
 */
class InventoryMovementsViewModel(
    private val inventoryApi: InventoryApi,
) : ClientTableViewModel<InventoryMovementRow>() {

    override val pageSize: Int = 10
    override val searchKey: String? = MovementFilterKeys.Search

    private val _revertingId = MutableStateFlow<String?>(null)

    /** Id del movimiento cuya reversión está en curso (para el spinner de su fila). */
    val revertingId: StateFlow<String?> = _revertingId.asStateFlow()

    override suspend fun fetchAll(): List<InventoryMovementRow> =
        inventoryApi.movements().map { it.toRow() }

    /**
     * Revierte un movimiento de mantenimiento. La fila se valida aquí y el API
     * revalida tipo/estado; al terminar refresca el kardex.
     */
    fun revert(row: InventoryMovementRow) {
        if (_revertingId.value != null) return
        if (!row.isReversible) {
            AppSnackbar.showError("Solo se pueden revertir movimientos de mantenimiento.")
            return
        }
        _revertingId.value = row.id
        viewModelScope.launch {
            val result = runCatching { inventoryApi.revertMovement(row.id) }
            result.fold(
                onSuccess = {
                    AppSnackbar.show("Movimiento revertido.")
                    refresh()
                },
                onFailure = { e -> AppSnackbar.showError(networkMessage(e)) },
            )
            _revertingId.value = null
        }
    }

    override fun applyFilters(
        items: List<InventoryMovementRow>,
        filters: Map<String, String>,
    ): List<InventoryMovementRow> {
        val query = filters[MovementFilterKeys.Search]?.trim().orEmpty().lowercase()
        val type = filters[MovementFilterKeys.Type]?.takeIf { it.isNotBlank() }
        val device = filters[MovementFilterKeys.Device]?.trim().orEmpty().lowercase()
        val user = filters[MovementFilterKeys.User]?.trim().orEmpty().lowercase()
        val from = filters[MovementFilterKeys.DateFrom]?.takeIf { it.isNotBlank() }
        val to = filters[MovementFilterKeys.DateTo]?.takeIf { it.isNotBlank() }

        return items.filter { row ->
            (type == null || row.type == type) &&
                (query.isEmpty() || row.searchText.contains(query)) &&
                (
                    device.isEmpty() ||
                        row.deviceLabel.lowercase().contains(device) ||
                        row.deviceId.lowercase().contains(device)
                    ) &&
                (user.isEmpty() || row.userLabel.lowercase().contains(user)) &&
                row.inDayRange(from, to)
        }
    }

    override fun applySort(
        items: List<InventoryMovementRow>,
        sort: TableSort?,
    ): List<InventoryMovementRow> {
        // Sin orden elegido se conserva el del API (fecha descendente).
        if (sort == null) return items
        val ascending = sort.direction != "desc"
        return when (sort.key) {
            "date" -> if (ascending) items.sortedBy { it.date } else items.sortedByDescending { it.date }
            "type" -> if (ascending) items.sortedBy { it.type } else items.sortedByDescending { it.type }
            else -> items
        }
    }
}
