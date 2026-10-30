package com.axzydev.puertonuevoapp.feature.inventory

import com.axzydev.puertonuevoapp.core.network.http.TableSort
import com.axzydev.puertonuevoapp.core.network.inventory.InventoryApi
import com.axzydev.puertonuevoapp.core.ui.table.ClientTableViewModel

/**
 * Kardex / Movimientos sobre el estándar de tablas (`AppTableScreen`).
 *
 * El API **no** expone `POST /inventory/movements/query`: `GET /inventory/movements`
 * devuelve la lista completa (sin paginar ni filtrar por fecha; solo acepta `type`
 * y `deviceId`). Por eso el piloto usa [ClientTableViewModel], espejo de
 * `makeClientTableFetch` de la web: trae la lista una vez y este ViewModel aplica
 * filtros y orden en memoria, mientras el estándar pagina el resultado.
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

    override suspend fun fetchAll(): List<InventoryMovementRow> =
        inventoryApi.movements().map { it.toRow() }

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
