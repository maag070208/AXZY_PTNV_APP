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
                                description = device.description,
                                brand = device.brand,
                                model = device.model,
                                area = device.area,
                                serialNumber = device.serialNumber ?: "",
                                hostname = device.hostname ?: "",
                                ip = device.ip ?: "",
                                macAddress = device.macAddress ?: "",
                                operatingSystem = device.operatingSystem ?: "",
                                ram = device.ram ?: "",
                                storage = device.storage ?: "",
                                blocked = device.status == "ASSIGNED",
                                blockedCode = device.assetTag,
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
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun onBrandChange(value: String) = _uiState.update { it.copy(brand = value) }
    fun onModelChange(value: String) = _uiState.update { it.copy(model = value) }
    fun onAreaChange(value: String) = _uiState.update { it.copy(area = value) }
    fun onSerialNumberChange(value: String) = _uiState.update { it.copy(serialNumber = value) }
    fun onHostnameChange(value: String) = _uiState.update { it.copy(hostname = value) }
    fun onIpChange(value: String) = _uiState.update { it.copy(ip = value) }
    fun onMacAddressChange(value: String) = _uiState.update { it.copy(macAddress = value) }
    fun onOperatingSystemChange(value: String) = _uiState.update { it.copy(operatingSystem = value) }
    fun onRamChange(value: String) = _uiState.update { it.copy(ram = value) }
    fun onStorageChange(value: String) = _uiState.update { it.copy(storage = value) }

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
                            description = state.description.trim(),
                            brand = state.brand.trim(),
                            model = state.model.trim(),
                            area = state.area.trim().ifBlank { null },
                            serialNumber = if (state.showField("serialNumber")) state.serialNumber.trim().ifBlank { null } else null,
                            hostname = if (state.showField("hostname")) state.hostname.trim().ifBlank { null } else null,
                            ip = if (state.showField("ip")) state.ip.trim().ifBlank { null } else null,
                            macAddress = if (state.showField("macAddress")) state.macAddress.trim().ifBlank { null } else null,
                            operatingSystem = if (state.showField("operatingSystem")) state.operatingSystem.trim().ifBlank { null } else null,
                            ram = if (state.showField("ram")) state.ram.trim().ifBlank { null } else null,
                            storage = if (state.showField("storage")) state.storage.trim().ifBlank { null } else null,
                        ),
                    )
                } else {
                    devicesApi.update(
                        deviceId,
                        DeviceUpdateInput(
                            typeId = state.typeId,
                            description = state.description.trim(),
                            brand = state.brand.trim(),
                            model = state.model.trim(),
                            area = state.area.trim().ifBlank { null },
                            serialNumber = if (state.showField("serialNumber")) state.serialNumber.trim().ifBlank { null } else null,
                            hostname = if (state.showField("hostname")) state.hostname.trim().ifBlank { null } else null,
                            ip = if (state.showField("ip")) state.ip.trim().ifBlank { null } else null,
                            macAddress = if (state.showField("macAddress")) state.macAddress.trim().ifBlank { null } else null,
                            operatingSystem = if (state.showField("operatingSystem")) state.operatingSystem.trim().ifBlank { null } else null,
                            ram = if (state.showField("ram")) state.ram.trim().ifBlank { null } else null,
                            storage = if (state.showField("storage")) state.storage.trim().ifBlank { null } else null,
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
