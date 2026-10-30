package com.axzydev.puertonuevoapp.feature.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.devices.DevicesApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.inventory.DeviceUnitDto
import com.axzydev.puertonuevoapp.core.network.inventory.InventoryApi
import com.axzydev.puertonuevoapp.core.network.inventory.UnitUpdateInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Detalle de un dispositivo: ficha, existencias, piezas físicas (con edición de
 * su identidad) y kardex. La ficha carga primero; las secciones de inventario
 * cargan en paralelo y su fallo no tumba la pantalla.
 */
class DeviceDetailViewModel(
    private val deviceId: String,
    private val devicesApi: DevicesApi,
    private val inventoryApi: InventoryApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceDetailUiState())
    val uiState: StateFlow<DeviceDetailUiState> = _uiState.asStateFlow()

    /** La pantalla se fue a otra (p. ej. "Agregar unidades"): al volver, recargar inventario. */
    private var refreshOnReturnArmed = false

    init {
        load()
    }

    /** La pantalla salió de composición; si vuelve, recarga piezas/kardex. */
    fun armRefreshOnReturn() {
        refreshOnReturnArmed = true
    }

    /** Llamado al entrar a composición: si la pantalla viene de regreso, recarga la ficha completa. */
    fun refreshOnReturn() {
        if (!refreshOnReturnArmed) return
        refreshOnReturnArmed = false
        // Se recarga TODO: al volver de "Agregar unidades" cambian las piezas y el
        // kardex, y al volver de "Editar" también los datos del dispositivo.
        load()
    }

    /** Carga la ficha y, si sale bien, las secciones de inventario. */
    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { devicesApi.get(deviceId) }
            result.fold(
                onSuccess = { device ->
                    _uiState.update { it.copy(loading = false, device = device) }
                    loadExtras()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(loading = false, error = networkMessage(e)) }
                },
            )
        }
    }

    /** Recarga unidades, existencias y kardex (reintento o al volver de "Agregar unidades"). */
    fun loadExtras() {
        _uiState.update { it.copy(extrasLoading = true, extrasError = null) }
        viewModelScope.launch {
            val units = runCatching { inventoryApi.units(deviceId) }
            val stock = runCatching { inventoryApi.stock(deviceId) }
            val ledger = runCatching { inventoryApi.ledger(deviceId) }
            val firstError = units.exceptionOrNull()
                ?: stock.exceptionOrNull()
                ?: ledger.exceptionOrNull()
            _uiState.update { state ->
                state.copy(
                    extrasLoading = false,
                    extrasError = firstError?.let(::networkMessage),
                    units = units.getOrNull() ?: state.units,
                    stock = stock.getOrNull() ?: state.stock,
                    ledger = ledger.getOrNull()?.rows ?: state.ledger,
                )
            }
        }
    }

    fun onUnitFilterChange(status: String) = _uiState.update {
        it.copy(unitFilter = if (status == DEVICE_UNIT_FILTER_ALL || status in deviceUnitStatusOrder) status else DEVICE_UNIT_FILTER_ALL)
    }

    // ---------------------------------------------------------------------
    // Edición de la identificación de una pieza (PUT /inventory/units/:id)
    // ---------------------------------------------------------------------

    fun startEditUnit(unit: DeviceUnitDto) = _uiState.update {
        it.copy(
            editingUnit = unit,
            editSerialNumber = unit.serialNumber.orEmpty(),
            editMacAddress = unit.macAddress.orEmpty(),
            editIp = unit.ip.orEmpty(),
            editHostname = unit.hostname.orEmpty(),
            editArea = unit.area,
            editError = null,
            editSaving = false,
        )
    }

    fun dismissEditUnit() = _uiState.update {
        it.copy(editingUnit = null, editError = null, editSaving = false)
    }

    fun onEditSerialNumberChange(value: String) = _uiState.update { it.copy(editSerialNumber = value) }
    fun onEditMacAddressChange(value: String) = _uiState.update { it.copy(editMacAddress = value) }
    fun onEditIpChange(value: String) = _uiState.update { it.copy(editIp = value) }
    fun onEditHostnameChange(value: String) = _uiState.update { it.copy(editHostname = value) }
    fun onEditAreaChange(value: String) = _uiState.update { it.copy(editArea = value) }

    fun saveUnit() {
        val state = _uiState.value
        val unit = state.editingUnit ?: return
        if (state.editSaving) return
        _uiState.update { it.copy(editSaving = true, editError = null) }
        viewModelScope.launch {
            val result = runCatching {
                inventoryApi.updateUnit(
                    unitId = unit.id,
                    input = UnitUpdateInput(
                        serialNumber = state.editSerialNumber.trim(),
                        macAddress = state.editMacAddress.trim(),
                        ip = state.editIp.trim(),
                        hostname = state.editHostname.trim(),
                        area = state.editArea.trim(),
                    ),
                )
            }
            result.fold(
                onSuccess = { updated ->
                    _uiState.update { current ->
                        current.copy(
                            editSaving = false,
                            editingUnit = null,
                            units = current.units.map { if (it.id == updated.id) updated else it },
                            unitSaved = true,
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(editSaving = false, editError = networkMessage(e)) }
                },
            )
        }
    }

    /** La pantalla ya mostró el aviso de identidad actualizada. */
    fun consumeUnitSaved() = _uiState.update { it.copy(unitSaved = false) }
}
