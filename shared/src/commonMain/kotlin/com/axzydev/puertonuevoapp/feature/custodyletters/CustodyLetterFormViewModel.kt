package com.axzydev.puertonuevoapp.feature.custodyletters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.custodyletters.CustodyLetterCreateInput
import com.axzydev.puertonuevoapp.core.network.custodyletters.CustodyLetterItemInput
import com.axzydev.puertonuevoapp.core.network.custodyletters.CustodyLetterUpdateInput
import com.axzydev.puertonuevoapp.core.network.custodyletters.CustodyLettersApi
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
 * dispositivo AVAILABLE existente (igual que CartaForm.tsx: no se captura
 * description/brand/model a mano, siempre se deriva del Device elegido).
 * El PDF (100% client-side en el web, sin endpoint) queda fuera de alcance
 * — ver CartaDetailScreen para la vista previa en pantalla.
 */
class CustodyLetterFormViewModel(
    private val custodyLetterId: String?,
    private val custodyLettersApi: CustodyLettersApi,
    private val devicesApi: DevicesApi,
    private val deviceTypesApi: DeviceTypesApi,
    private val usersApi: UsersApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustodyLetterFormUiState(isEditMode = custodyLetterId != null, changingDevice = custodyLetterId == null))
    val uiState: StateFlow<CustodyLetterFormUiState> = _uiState.asStateFlow()

    private fun UserDto.isAreaHeadLike() = role == "ADMIN" || role == "MANAGER" || role == "AREA_HEAD"

    init {
        viewModelScope.launch {
            val deviceTypes = runCatching { deviceTypesApi.list() }.getOrDefault(emptyList())
            val employees = runCatching { usersApi.employees() }.getOrDefault(emptyList())
            _uiState.update { it.copy(deviceTypes = deviceTypes, employees = employees, areaHeads = employees.filter { u -> u.isAreaHeadLike() }) }

            if (custodyLetterId != null) {
                val result = runCatching { custodyLettersApi.get(custodyLetterId) }
                result.fold(
                    onSuccess = { c ->
                        val item = c.items.firstOrNull()
                        _uiState.update {
                            it.copy(
                                loading = false,
                                employeeNumber = c.employeeNumber,
                                company = c.company ?: "",
                                department = c.department,
                                selectedEmployeeId = c.custodianId ?: "",
                                selectedSupervisorId = c.supervisorId ?: "",
                                deliveryBy = c.deliveryBy ?: "Departamento de Mantenimiento",
                                area = item?.area ?: "",
                                currentItemSummary = item?.let { i ->
                                    listOfNotNull(
                                        i.description,
                                        listOfNotNull(i.brand, i.model).joinToString(" ").ifBlank { null },
                                        i.assetTag?.let { ca -> "Activo: $ca" },
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
            val devices = runCatching { devicesApi.list(status = "AVAILABLE", typeId = state.selectedTypeId).data }.getOrDefault(emptyList())
            val folio = if (custodyLetterId == null && state.selectedTypeId != null) {
                runCatching { deviceTypesApi.peekCustodyLetter(state.selectedTypeId) }.getOrNull()
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

    fun onEmployeeQueryChange(value: String) = _uiState.update { it.copy(employeeQuery = value) }
    fun onAreaHeadQueryChange(value: String) = _uiState.update { it.copy(areaHeadQuery = value) }

    fun onEmployeeChange(id: String) {
        val u = _uiState.value.employees.firstOrNull { it.id == id } ?: return
        _uiState.update {
            it.copy(
                selectedEmployeeId = id,
                employeeNumber = u.employeeNumber ?: it.employeeNumber,
                company = u.company ?: it.company,
                department = u.department?.name ?: it.department,
                area = it.area.ifBlank { u.department?.name ?: "" },
            )
        }
    }

    fun onSupervisorChange(id: String) = _uiState.update { it.copy(selectedSupervisorId = id) }
    fun onEmployeeNumberChange(value: String) = _uiState.update { it.copy(employeeNumber = value) }
    fun onCompanyChange(value: String) = _uiState.update { it.copy(company = value) }
    fun onDepartmentChange(value: String) = _uiState.update { it.copy(department = value) }
    fun onAreaChange(value: String) = _uiState.update { it.copy(area = value) }
    fun onDeliveryByChange(value: String) = _uiState.update { it.copy(deliveryBy = value) }

    fun submit() {
        val state = _uiState.value
        if (!state.isValid || state.saving) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                if (custodyLetterId == null) {
                    custodyLettersApi.create(
                        CustodyLetterCreateInput(
                            consecutive = state.previewFolio,
                            employeeNumber = state.employeeNumber.trim(),
                            company = state.company.trim().ifBlank { null },
                            department = state.department.trim().ifBlank { null },
                            deliveryBy = state.deliveryBy.trim().ifBlank { null },
                            custodianId = state.selectedEmployeeId.ifBlank { null },
                            supervisorId = state.selectedSupervisorId.ifBlank { null },
                            item = CustodyLetterItemInput(deviceId = state.selectedDeviceId, area = state.area.trim().ifBlank { null }),
                        ),
                    )
                } else {
                    val itemInput = if (state.changingDevice && state.selectedDeviceId.isNotBlank()) {
                        CustodyLetterItemInput(deviceId = state.selectedDeviceId, area = state.area.trim().ifBlank { null })
                    } else {
                        null
                    }
                    custodyLettersApi.update(
                        custodyLetterId,
                        CustodyLetterUpdateInput(
                            employeeNumber = state.employeeNumber.trim(),
                            company = state.company.trim().ifBlank { null },
                            department = state.department.trim().ifBlank { null },
                            deliveryBy = state.deliveryBy.trim().ifBlank { null },
                            custodianId = state.selectedEmployeeId.ifBlank { null },
                            supervisorId = state.selectedSupervisorId.ifBlank { null },
                            item = itemInput,
                        ),
                    )
                }
            }
            _uiState.update { it.copy(saving = false, error = result.exceptionOrNull()?.let(::networkMessage), saved = result.isSuccess) }
        }
    }
}
