package com.axzydev.puertonuevoapp.feature.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.devices.DeviceCreateInput
import com.axzydev.puertonuevoapp.core.network.devices.DeviceUpdateInput
import com.axzydev.puertonuevoapp.core.network.devices.DevicesApi
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypesApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Alta/edición de UN dispositivo. [deviceId] null = alta. */
class DeviceFormViewModel(
    private val deviceId: String?,
    private val devicesApi: DevicesApi,
    private val deviceTypesApi: DeviceTypesApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceFormUiState())
    val uiState: StateFlow<DeviceFormUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                val types = deviceTypesApi.list()
                val device = deviceId?.let { devicesApi.get(it) }
                types to device
            }
            result.fold(
                onSuccess = { (types, device) ->
                    _uiState.update {
                        if (device != null) {
                            it.copy(
                                loading = false,
                                types = types,
                                typeId = device.typeId,
                                descripcion = device.descripcion,
                                marca = device.marca,
                                modelo = device.modelo,
                                area = device.area,
                                numeroSerie = device.numeroSerie ?: "",
                                nombreEquipo = device.nombreEquipo ?: "",
                                ip = device.ip ?: "",
                                macAddress = device.macAddress ?: "",
                                sistemaOp = device.sistemaOp ?: "",
                                ram = device.ram ?: "",
                                almacenamiento = device.almacenamiento ?: "",
                                blocked = device.estado == "ASIGNADO",
                                blockedCode = device.controlActivos,
                            )
                        } else {
                            it.copy(loading = false, types = types, typeId = it.typeId.ifBlank { types.firstOrNull()?.id ?: "" })
                        }
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(loading = false, error = networkMessage(e)) } },
            )
        }
    }

    fun onTypeChange(value: String) = _uiState.update { it.copy(typeId = value) }
    fun onDescripcionChange(value: String) = _uiState.update { it.copy(descripcion = value) }
    fun onMarcaChange(value: String) = _uiState.update { it.copy(marca = value) }
    fun onModeloChange(value: String) = _uiState.update { it.copy(modelo = value) }
    fun onAreaChange(value: String) = _uiState.update { it.copy(area = value) }
    fun onNumeroSerieChange(value: String) = _uiState.update { it.copy(numeroSerie = value) }
    fun onNombreEquipoChange(value: String) = _uiState.update { it.copy(nombreEquipo = value) }
    fun onIpChange(value: String) = _uiState.update { it.copy(ip = value) }
    fun onMacAddressChange(value: String) = _uiState.update { it.copy(macAddress = value) }
    fun onSistemaOpChange(value: String) = _uiState.update { it.copy(sistemaOp = value) }
    fun onRamChange(value: String) = _uiState.update { it.copy(ram = value) }
    fun onAlmacenamientoChange(value: String) = _uiState.update { it.copy(almacenamiento = value) }

    fun submit() {
        val state = _uiState.value
        if (!state.isValid || state.saving || state.blocked) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                if (deviceId == null) {
                    devicesApi.create(
                        DeviceCreateInput(
                            typeId = state.typeId,
                            descripcion = state.descripcion.trim(),
                            marca = state.marca.trim(),
                            modelo = state.modelo.trim(),
                            area = state.area.trim().ifBlank { null },
                            numeroSerie = if (state.showField("numeroSerie")) state.numeroSerie.trim().ifBlank { null } else null,
                            nombreEquipo = if (state.showField("nombreEquipo")) state.nombreEquipo.trim().ifBlank { null } else null,
                            ip = if (state.showField("ip")) state.ip.trim().ifBlank { null } else null,
                            macAddress = if (state.showField("macAddress")) state.macAddress.trim().ifBlank { null } else null,
                            sistemaOp = if (state.showField("sistemaOp")) state.sistemaOp.trim().ifBlank { null } else null,
                            ram = if (state.showField("ram")) state.ram.trim().ifBlank { null } else null,
                            almacenamiento = if (state.showField("almacenamiento")) state.almacenamiento.trim().ifBlank { null } else null,
                        ),
                    )
                } else {
                    devicesApi.update(
                        deviceId,
                        DeviceUpdateInput(
                            typeId = state.typeId,
                            descripcion = state.descripcion.trim(),
                            marca = state.marca.trim(),
                            modelo = state.modelo.trim(),
                            area = state.area.trim().ifBlank { null },
                            numeroSerie = if (state.showField("numeroSerie")) state.numeroSerie.trim().ifBlank { null } else null,
                            nombreEquipo = if (state.showField("nombreEquipo")) state.nombreEquipo.trim().ifBlank { null } else null,
                            ip = if (state.showField("ip")) state.ip.trim().ifBlank { null } else null,
                            macAddress = if (state.showField("macAddress")) state.macAddress.trim().ifBlank { null } else null,
                            sistemaOp = if (state.showField("sistemaOp")) state.sistemaOp.trim().ifBlank { null } else null,
                            ram = if (state.showField("ram")) state.ram.trim().ifBlank { null } else null,
                            almacenamiento = if (state.showField("almacenamiento")) state.almacenamiento.trim().ifBlank { null } else null,
                        ),
                    )
                }
            }
            _uiState.update {
                it.copy(
                    saving = false,
                    error = result.exceptionOrNull()?.let(::networkMessage),
                    saved = result.isSuccess,
                )
            }
        }
    }
}
