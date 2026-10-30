package com.axzydev.puertonuevoapp.feature.devices

import com.axzydev.puertonuevoapp.core.network.devices.DeviceDto
import com.axzydev.puertonuevoapp.core.network.inventory.DeviceUnitDto
import com.axzydev.puertonuevoapp.core.network.inventory.StockDto
import com.axzydev.puertonuevoapp.core.network.inventory.StockLedgerRowDto

/** Filtro "Todas" del listado de piezas. */
const val DEVICE_UNIT_FILTER_ALL = "all"

/** Estados de pieza del API, en el orden en que se muestran los filtros. */
val deviceUnitStatusOrder: List<String> = listOf(
    "AVAILABLE",
    "ON_LOAN",
    "DAMAGED",
    "IN_MAINTENANCE",
    "RETIRED",
)

data class DeviceDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val device: DeviceDto? = null,
    /** Existencias por estado (`GET /inventory/devices/:id/stock`). */
    val stock: StockDto? = null,
    /** Piezas físicas (`GET .../units`). */
    val units: List<DeviceUnitDto> = emptyList(),
    /** Kardex del dispositivo (`GET .../ledger`), con su saldo por renglón. */
    val ledger: List<StockLedgerRowDto> = emptyList(),
    val extrasLoading: Boolean = false,
    /** Fallo al cargar unidades/stock/kardex (la ficha del dispositivo sigue visible). */
    val extrasError: String? = null,
    /** Estado de pieza seleccionado en el filtro, o [DEVICE_UNIT_FILTER_ALL]. */
    val unitFilter: String = DEVICE_UNIT_FILTER_ALL,
    /** Pieza en edición de identidad (null = diálogo cerrado). */
    val editingUnit: DeviceUnitDto? = null,
    val editSerialNumber: String = "",
    val editMacAddress: String = "",
    val editIp: String = "",
    val editHostname: String = "",
    val editArea: String = "",
    val editSaving: Boolean = false,
    val editError: String? = null,
    /** Se actualizó una identidad: la pantalla avisa y limpia la bandera. */
    val unitSaved: Boolean = false,
) {
    val available: Int get() = stock?.AVAILABLE ?: 0
    val onLoan: Int get() = stock?.ON_LOAN ?: 0
    val damaged: Int get() = stock?.DAMAGED ?: 0
    val inMaintenance: Int get() = stock?.IN_MAINTENANCE ?: 0
    val retired: Int get() = stock?.RETIRED ?: 0
    val activeUnits: Int get() = stock?.active ?: 0
    val historicalUnits: Int get() = stock?.historical ?: 0

    /** Piezas visibles con el filtro actual. */
    val filteredUnits: List<DeviceUnitDto>
        get() = if (unitFilter == DEVICE_UNIT_FILTER_ALL) units else units.filter { it.status == unitFilter }

    /** Conteo por estado de las piezas cargadas (para las pestañas de filtro). */
    val unitCounts: Map<String, Int> get() = units.groupingBy { it.status }.eachCount()

    /** ¿El tipo del dispositivo pide ese dato de identificación por pieza? */
    fun showEditField(key: String): Boolean = when (key) {
        "serialNumber" -> device?.type?.fieldConfig?.serialNumber?.enabled ?: true
        "hostname" -> device?.type?.fieldConfig?.hostname?.enabled ?: true
        "ip" -> device?.type?.fieldConfig?.ip?.enabled ?: false
        "macAddress" -> device?.type?.fieldConfig?.macAddress?.enabled ?: false
        else -> false
    }
}
