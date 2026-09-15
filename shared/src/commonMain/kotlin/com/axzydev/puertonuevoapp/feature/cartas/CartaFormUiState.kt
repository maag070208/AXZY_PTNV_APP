package com.axzydev.puertonuevoapp.feature.cartas

import com.axzydev.puertonuevoapp.core.network.devices.DeviceDto
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypeDto
import com.axzydev.puertonuevoapp.core.network.users.UserDto

data class CartaFormUiState(
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
    val empleados: List<UserDto> = emptyList(),
    val empleadoQuery: String = "",
    val selectedEmpleadoId: String = "",

    // Firmantes
    val jefes: List<UserDto> = emptyList(),
    val jefeQuery: String = "",
    val selectedEncargadoId: String = "",
    val deliveryBy: String = "Departamento de Mantenimiento",

    val numeroEmpleado: String = "",
    val empresa: String = "",
    val departamento: String = "",
    val area: String = "",

    val saved: Boolean = false,
) {
    val isValid: Boolean get() = numeroEmpleado.isNotBlank() && (isEditMode || selectedDeviceId.isNotBlank())

    val filteredEmpleados: List<UserDto>
        get() = if (empleadoQuery.isBlank()) empleados else empleados.filter { u ->
            listOfNotNull(u.name, u.numeroEmpleado, u.puesto, u.department?.name).any { it.contains(empleadoQuery, ignoreCase = true) }
        }

    val filteredJefes: List<UserDto>
        get() = if (jefeQuery.isBlank()) jefes else jefes.filter { u ->
            listOfNotNull(u.name, u.numeroEmpleado, u.puesto, u.department?.name).any { it.contains(jefeQuery, ignoreCase = true) }
        }

    val tipoOptions: List<Pair<String, String>> get() = deviceTypes.map { it.id to "${it.name} (${it.prefix})" }
    val deviceOptions: List<Pair<String, String>> get() = devices.map { it.id to "${it.controlActivos} — ${it.descripcion} (${it.marca} ${it.modelo})" }
}
