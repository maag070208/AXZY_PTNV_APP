package com.axzydev.puertonuevoapp.feature.cartas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.network.CartaInputDto
import com.axzydev.puertonuevoapp.core.network.CartaItemInputDto
import com.axzydev.puertonuevoapp.core.network.DeviceDto
import com.axzydev.puertonuevoapp.core.network.DeviceTypeDto
import com.axzydev.puertonuevoapp.core.network.UserDto
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.util.roleLabel
import kotlinx.coroutines.launch

/**
 * Alta/edición de carta responsiva. Réplica simplificada de CartaForm.tsx:
 * el "Recurso TIC" solo puede venir de un dispositivo DISPONIBLE existente
 * (el web tampoco permite capturar descripcion/marca/modelo/controlActivos
 * a mano — siempre se derivan del Device elegido), así que aquí igual.
 * El PDF de la carta (generado 100% client-side en el web con
 * @react-pdf/renderer, sin endpoint en el backend) queda fuera de esta
 * pasada — ver CartaDetailScreen para la vista previa en pantalla.
 */
@Composable
fun CartaFormScreen(cartaId: String? = null) {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val isEdit = cartaId != null

    var loading by remember { mutableStateOf(isEdit) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Cabecera / recurso TIC
    var deviceTypes by remember { mutableStateOf<List<DeviceTypeDto>>(emptyList()) }
    var selectedTypeId by remember { mutableStateOf<String?>(null) }
    var previewFolio by remember { mutableStateOf<String?>(null) }
    var devices by remember { mutableStateOf<List<DeviceDto>>(emptyList()) }
    var selectedDeviceId by remember { mutableStateOf("") }
    var changingDevice by remember { mutableStateOf(!isEdit) }

    // Item actual (solo lectura, cuando se edita y no se está cambiando el device)
    var currentItemSummary by remember { mutableStateOf<String?>(null) }

    // Empleado (responsable / quien recibe)
    var empleados by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    var empleadoQuery by remember { mutableStateOf("") }
    var selectedEmpleadoId by remember { mutableStateOf("") }

    // Firmantes
    var jefes by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    var jefeQuery by remember { mutableStateOf("") }
    var selectedEncargadoId by remember { mutableStateOf("") }
    var deliveryBy by remember { mutableStateOf("Departamento de Mantenimiento") }

    var numeroEmpleado by remember { mutableStateOf("") }
    var empresa by remember { mutableStateOf("") }
    var departamento by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            deviceTypes = AppContainer.deviceTypesApi.list()
        } catch (_: Exception) { /* el picker de tipo simplemente queda vacío */ }
        try {
            empleados = AppContainer.usersApi.empleados()
        } catch (_: Exception) { }
        try {
            jefes = AppContainer.usersApi.empleados(roles = listOf("ADMIN", "GERENTE", "JEFE_DE_AREA"))
        } catch (_: Exception) { }

        if (cartaId != null) {
            loading = true
            error = null
            try {
                val c = AppContainer.cartasApi.get(cartaId)
                numeroEmpleado = c.numeroEmpleado
                empresa = c.empresa ?: ""
                departamento = c.departamento
                selectedEmpleadoId = c.responsableId ?: ""
                selectedEncargadoId = c.encargadoId ?: ""
                deliveryBy = c.deliveryBy ?: "Departamento de Mantenimiento"
                val item = c.items.firstOrNull()
                area = item?.area ?: ""
                currentItemSummary = item?.let {
                    listOfNotNull(
                        it.descripcion,
                        listOfNotNull(it.marca, it.modelo).joinToString(" ").ifBlank { null },
                        it.controlActivos?.let { ca -> "Activo: $ca" },
                    ).joinToString(" · ")
                }
            } catch (e: Exception) {
                error = e.message ?: "No se pudo cargar la carta"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(selectedTypeId, changingDevice) {
        if (!changingDevice) return@LaunchedEffect
        selectedDeviceId = ""
        try {
            devices = AppContainer.devicesApi.list(estado = "DISPONIBLE", typeId = selectedTypeId).data
        } catch (_: Exception) {
            devices = emptyList()
        }
        previewFolio = null
        if (!isEdit) {
            val typeId = selectedTypeId
            if (typeId != null) {
                try {
                    previewFolio = AppContainer.deviceTypesApi.peekCarta(typeId)
                } catch (_: Exception) { }
            }
        }
    }

    val filteredEmpleados = remember(empleados, empleadoQuery) {
        if (empleadoQuery.isBlank()) empleados else empleados.filter { u ->
            listOfNotNull(u.name, u.numeroEmpleado, u.puesto, u.department?.name)
                .any { it.contains(empleadoQuery, ignoreCase = true) }
        }
    }
    val empleadoOptions = filteredEmpleados.map { u ->
        u.id to listOfNotNull(
            u.name,
            u.numeroEmpleado?.let { "#$it" },
            u.puesto,
            u.department?.name,
        ).joinToString(" · ")
    }

    val filteredJefes = remember(jefes, jefeQuery) {
        if (jefeQuery.isBlank()) jefes else jefes.filter { u ->
            listOfNotNull(u.name, u.numeroEmpleado, u.puesto, u.department?.name)
                .any { it.contains(jefeQuery, ignoreCase = true) }
        }
    }
    val jefeOptions = filteredJefes.map { u ->
        u.id to listOfNotNull(
            u.name,
            roleLabel(u.role),
            u.numeroEmpleado?.let { "#$it" },
            u.department?.name,
        ).joinToString(" · ")
    }

    val tipoOptions = deviceTypes.map { it.id to "${it.name} (${it.prefix})" }
    val deviceOptions = devices.map { d ->
        d.id to "${d.controlActivos} — ${d.descripcion} (${d.marca} ${d.modelo})"
    }

    fun onEmpleadoSelect(id: String) {
        selectedEmpleadoId = id
        val u = empleados.firstOrNull { it.id == id } ?: return
        numeroEmpleado = u.numeroEmpleado ?: numeroEmpleado
        empresa = u.empresa ?: empresa
        departamento = u.department?.name ?: departamento
        if (area.isBlank()) area = u.department?.name ?: area
    }

    fun onDeviceSelect(id: String) {
        selectedDeviceId = id
        val d = devices.firstOrNull { it.id == id } ?: return
        if (area.isBlank()) area = d.area
    }

    val isValid = numeroEmpleado.isNotBlank() && (isEdit || selectedDeviceId.isNotBlank())

    fun submit() {
        if (!isValid || saving) return
        saving = true
        error = null
        scope.launch {
            try {
                val itemInput = if (changingDevice && selectedDeviceId.isNotBlank()) {
                    CartaItemInputDto(deviceId = selectedDeviceId, area = area.trim().ifBlank { null })
                } else {
                    null
                }
                val input = CartaInputDto(
                    consecutivo = previewFolio,
                    numeroEmpleado = numeroEmpleado.trim(),
                    empresa = empresa.trim().ifBlank { null },
                    departamento = departamento.trim().ifBlank { null },
                    deliveryBy = deliveryBy.trim().ifBlank { null },
                    responsableId = selectedEmpleadoId.ifBlank { null },
                    encargadoId = selectedEncargadoId.ifBlank { null },
                    item = itemInput,
                )
                if (isEdit && cartaId != null) {
                    AppContainer.cartasApi.update(cartaId, input)
                } else {
                    AppContainer.cartasApi.create(input)
                }
                navigator.pop()
            } catch (e: Exception) {
                error = e.message ?: "No se pudo guardar la carta"
            } finally {
                saving = false
            }
        }
    }

    if (loading) {
        LoadingState(modifier = Modifier.fillMaxSize())
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(16.dp),
    ) {
        if (error != null) {
            Text(error ?: "", color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
        }

        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Recurso TIC")
            if (!changingDevice) {
                Text(
                    currentItemSummary ?: "Sin dispositivo asignado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { changingDevice = true }) { Text("Cambiar dispositivo") }
            } else {
                SimpleDropdownField(
                    label = "Tipo de dispositivo",
                    value = selectedTypeId ?: "",
                    options = tipoOptions,
                    onSelect = { selectedTypeId = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                previewFolio?.let {
                    Spacer(Modifier.height(6.dp))
                    Text("Folio: $it", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                }
                Spacer(Modifier.height(10.dp))
                SimpleDropdownField(
                    label = if (selectedTypeId == null) "Selecciona primero el tipo" else "Dispositivo disponible",
                    value = selectedDeviceId,
                    options = deviceOptions,
                    onSelect = ::onDeviceSelect,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isEdit) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        changingDevice = false
                        selectedDeviceId = ""
                        selectedTypeId = null
                    }) { Text("Conservar dispositivo actual") }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Empleado (quien recibe)")
            AppSearchField(empleadoQuery, { empleadoQuery = it }, "Buscar por nombre, número, puesto…")
            Spacer(Modifier.height(8.dp))
            SimpleDropdownField(
                label = "Empleado",
                value = selectedEmpleadoId,
                options = empleadoOptions,
                onSelect = ::onEmpleadoSelect,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = numeroEmpleado,
                onValueChange = { numeroEmpleado = it },
                label = { Text("No. de empleado") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = empresa,
                    onValueChange = { empresa = it },
                    label = { Text("Empresa") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = departamento,
                    onValueChange = { departamento = it },
                    label = { Text("Departamento") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = area,
                onValueChange = { area = it },
                label = { Text("Área (del recurso)") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(14.dp))
        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Firmantes")
            AppSearchField(jefeQuery, { jefeQuery = it }, "Buscar administrador, gerente o jefe…")
            Spacer(Modifier.height(8.dp))
            SimpleDropdownField(
                label = "Jefe de área (encargado)",
                value = selectedEncargadoId,
                options = jefeOptions,
                onSelect = { selectedEncargadoId = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = deliveryBy,
                onValueChange = { deliveryBy = it },
                label = { Text("Entrega (quien entrega)") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { submit() },
            enabled = isValid && !saving,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            if (saving) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), color = AppColors.Surface, strokeWidth = 2.dp)
            } else {
                Text(if (isEdit) "Guardar cambios" else "Crear carta", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
