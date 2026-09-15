package com.axzydev.puertonuevoapp.feature.cartas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.cartas.CartaCreateInput
import com.axzydev.puertonuevoapp.core.network.cartas.CartaItemInput
import com.axzydev.puertonuevoapp.core.network.cartas.CartaUpdateInput
import com.axzydev.puertonuevoapp.core.network.cartas.CartasApi
import com.axzydev.puertonuevoapp.core.network.devices.DevicesApi
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypesApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.users.UserDto
import com.axzydev.puertonuevoapp.core.network.users.UsersApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Alta/edición de carta responsiva. El "Recurso TIC" solo puede venir de un
 * dispositivo DISPONIBLE existente (igual que CartaForm.tsx: no se captura
 * descripcion/marca/modelo a mano, siempre se deriva del Device elegido).
 * El PDF (100% client-side en el web, sin endpoint) queda fuera de alcance
 * — ver CartaDetailScreen para la vista previa en pantalla.
 */
class CartaFormViewModel(
    private val cartaId: String?,
    private val cartasApi: CartasApi,
    private val devicesApi: DevicesApi,
    private val deviceTypesApi: DeviceTypesApi,
    private val usersApi: UsersApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartaFormUiState(isEditMode = cartaId != null, changingDevice = cartaId == null))
    val uiState: StateFlow<CartaFormUiState> = _uiState.asStateFlow()

    private fun UserDto.isJefeLike() = role == "ADMIN" || role == "GERENTE" || role == "JEFE_DE_AREA"

    init {
        viewModelScope.launch {
            val deviceTypes = runCatching { deviceTypesApi.list() }.getOrDefault(emptyList())
            val empleados = runCatching { usersApi.empleados() }.getOrDefault(emptyList())
            _uiState.update { it.copy(deviceTypes = deviceTypes, empleados = empleados, jefes = empleados.filter { u -> u.isJefeLike() }) }

            if (cartaId != null) {
                val result = runCatching { cartasApi.get(cartaId) }
                result.fold(
                    onSuccess = { c ->
                        val item = c.items.firstOrNull()
                        _uiState.update {
                            it.copy(
                                loading = false,
                                numeroEmpleado = c.numeroEmpleado,
                                empresa = c.empresa ?: "",
                                departamento = c.departamento,
                                selectedEmpleadoId = c.responsableId ?: "",
                                selectedEncargadoId = c.encargadoId ?: "",
                                deliveryBy = c.deliveryBy ?: "Departamento de Mantenimiento",
                                area = item?.area ?: "",
                                currentItemSummary = item?.let { i ->
                                    listOfNotNull(
                                        i.descripcion,
                                        listOfNotNull(i.marca, i.modelo).joinToString(" ").ifBlank { null },
                                        i.controlActivos?.let { ca -> "Activo: $ca" },
                                    ).joinToString(" · ")
                                },
                            )
                        }
                    },
                    onFailure = { e -> _uiState.update { it.copy(loading = false, error = networkMessage(e)) } },
                )
            } else {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    private fun reloadDevicesForType() {
        val state = _uiState.value
        viewModelScope.launch {
            val devices = runCatching { devicesApi.list(estado = "DISPONIBLE", typeId = state.selectedTypeId).data }.getOrDefault(emptyList())
            val folio = if (cartaId == null && state.selectedTypeId != null) {
                runCatching { deviceTypesApi.peekCarta(state.selectedTypeId) }.getOrNull()
            } else null
            _uiState.update { it.copy(devices = devices, selectedDeviceId = "", previewFolio = folio) }
        }
    }

    fun onTypeChange(typeId: String) {
        _uiState.update { it.copy(selectedTypeId = typeId) }
        reloadDevicesForType()
    }

    fun startChangingDevice() {
        _uiState.update { it.copy(changingDevice = true, selectedTypeId = null, selectedDeviceId = "", devices = emptyList(), previewFolio = null) }
    }

    fun keepCurrentDevice() {
        _uiState.update { it.copy(changingDevice = false, selectedDeviceId = "", selectedTypeId = null) }
    }

    fun onDeviceChange(id: String) {
        val device = _uiState.value.devices.firstOrNull { it.id == id }
        _uiState.update { it.copy(selectedDeviceId = id, area = it.area.ifBlank { device?.area ?: "" }) }
    }

    fun onEmpleadoQueryChange(value: String) = _uiState.update { it.copy(empleadoQuery = value) }
    fun onJefeQueryChange(value: String) = _uiState.update { it.copy(jefeQuery = value) }

    fun onEmpleadoChange(id: String) {
        val u = _uiState.value.empleados.firstOrNull { it.id == id } ?: return
        _uiState.update {
            it.copy(
                selectedEmpleadoId = id,
                numeroEmpleado = u.numeroEmpleado ?: it.numeroEmpleado,
                empresa = u.empresa ?: it.empresa,
                departamento = u.department?.name ?: it.departamento,
                area = it.area.ifBlank { u.department?.name ?: "" },
            )
        }
    }

    fun onEncargadoChange(id: String) = _uiState.update { it.copy(selectedEncargadoId = id) }
    fun onNumeroEmpleadoChange(value: String) = _uiState.update { it.copy(numeroEmpleado = value) }
    fun onEmpresaChange(value: String) = _uiState.update { it.copy(empresa = value) }
    fun onDepartamentoChange(value: String) = _uiState.update { it.copy(departamento = value) }
    fun onAreaChange(value: String) = _uiState.update { it.copy(area = value) }
    fun onDeliveryByChange(value: String) = _uiState.update { it.copy(deliveryBy = value) }

    fun submit() {
        val state = _uiState.value
        if (!state.isValid || state.saving) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                if (cartaId == null) {
                    cartasApi.create(
                        CartaCreateInput(
                            consecutivo = state.previewFolio,
                            numeroEmpleado = state.numeroEmpleado.trim(),
                            empresa = state.empresa.trim().ifBlank { null },
                            departamento = state.departamento.trim().ifBlank { null },
                            deliveryBy = state.deliveryBy.trim().ifBlank { null },
                            responsableId = state.selectedEmpleadoId.ifBlank { null },
                            encargadoId = state.selectedEncargadoId.ifBlank { null },
                            item = CartaItemInput(deviceId = state.selectedDeviceId, area = state.area.trim().ifBlank { null }),
                        ),
                    )
                } else {
                    val itemInput = if (state.changingDevice && state.selectedDeviceId.isNotBlank()) {
                        CartaItemInput(deviceId = state.selectedDeviceId, area = state.area.trim().ifBlank { null })
                    } else {
                        null
                    }
                    cartasApi.update(
                        cartaId,
                        CartaUpdateInput(
                            numeroEmpleado = state.numeroEmpleado.trim(),
                            empresa = state.empresa.trim().ifBlank { null },
                            departamento = state.departamento.trim().ifBlank { null },
                            deliveryBy = state.deliveryBy.trim().ifBlank { null },
                            responsableId = state.selectedEmpleadoId.ifBlank { null },
                            encargadoId = state.selectedEncargadoId.ifBlank { null },
                            item = itemInput,
                        ),
                    )
                }
            }
            _uiState.update { it.copy(saving = false, error = result.exceptionOrNull()?.let(::networkMessage), saved = result.isSuccess) }
        }
    }
}
