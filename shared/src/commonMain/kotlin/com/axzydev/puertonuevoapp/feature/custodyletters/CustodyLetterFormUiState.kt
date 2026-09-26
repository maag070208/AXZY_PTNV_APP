package com.axzydev.puertonuevoapp.feature.custodyletters

import com.axzydev.puertonuevoapp.core.network.devices.DeviceDto
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypeDto
import com.axzydev.puertonuevoapp.core.network.users.UserDto

data class CustodyLetterFormUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false,

    // Recurso TIC
    val deviceTypes: List<DeviceTypeDto> = emptyList(),
    val selectedTypeId: String? = null,
    val previewFolio: String? = null,
    val devices: List<DeviceDto> = emptyList(),
    val selectedDeviceId: String = "",
    val changingDevice: Boolean = true,
    val currentItemSummary: String? = null,

    // Empleado (quien recibe)
    val employees: List<UserDto> = emptyList(),
    val employeeQuery: String = "",
    val selectedEmployeeId: String = "",

    // Firmantes
    val areaHeads: List<UserDto> = emptyList(),
    val areaHeadQuery: String = "",
    val selectedSupervisorId: String = "",
    val deliveryBy: String = "Departamento de Mantenimiento",

    val employeeNumber: String = "",
    val company: String = "",
    val department: String = "",
    val area: String = "",

    val saved: Boolean = false,
) {
    val isValid: Boolean get() = employeeNumber.isNotBlank() && (isEditMode || selectedDeviceId.isNotBlank())

    val filteredEmployees: List<UserDto>
        get() = if (employeeQuery.isBlank()) employees else employees.filter { u ->
            listOfNotNull(u.name, u.employeeNumber, u.jobTitle, u.department?.name).any { it.contains(employeeQuery, ignoreCase = true) }
        }

    val filteredAreaHeads: List<UserDto>
        get() = if (areaHeadQuery.isBlank()) areaHeads else areaHeads.filter { u ->
            listOfNotNull(u.name, u.employeeNumber, u.jobTitle, u.department?.name).any { it.contains(areaHeadQuery, ignoreCase = true) }
        }

    val typeOptions: List<Pair<String, String>> get() = deviceTypes.map { it.id to "${it.name} (${it.prefix})" }
    val deviceOptions: List<Pair<String, String>> get() = devices.map { it.id to "${it.assetTag} — ${it.description} (${it.brand} ${it.model})" }
}
