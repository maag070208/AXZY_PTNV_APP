package com.axzydev.puertonuevoapp.feature.inventory

import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import com.axzydev.puertonuevoapp.core.network.inventory.InventoryApi
import com.axzydev.puertonuevoapp.core.network.inventory.LoanReturnDto
import com.axzydev.puertonuevoapp.core.ui.table.PaginatedTableViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Bandeja de devoluciones (`GET /inventory/returns`): la bitácora completa,
 * ordenada de la más reciente a la más antigua. El endpoint no pagina; el
 * adaptador de la API recorta la página y ordena, así que la pantalla usa la
 * tabla estándar (`PaginatedTableViewModel` + `AppTableScreen`).
 */
class InventoryReturnsViewModel(
    private val inventoryApi: InventoryApi,
) : PaginatedTableViewModel<LoanReturnDto>() {

    override val pageSize: Int = 25

    private val _detail = MutableStateFlow(InventoryReturnsUiState())
    val detail: StateFlow<InventoryReturnsUiState> = _detail.asStateFlow()

    override suspend fun fetch(page: Int, limit: Int, filters: Map<String, String>): TableResponse<LoanReturnDto> =
        inventoryApi.returns(TableRequest(page = page, limit = limit, filters = filters))

    /** Abre el detalle de una devolución (renglones, condición y piezas que volvieron). */
    fun openDetail(returned: LoanReturnDto) = _detail.update { it.copy(selected = returned) }

    fun closeDetail() = _detail.update { it.copy(selected = null) }
}
