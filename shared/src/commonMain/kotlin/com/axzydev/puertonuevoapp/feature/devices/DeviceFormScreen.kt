package com.axzydev.puertonuevoapp.feature.devices

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.axzydev.puertonuevoapp.core.network.DeviceCreateDto
import com.axzydev.puertonuevoapp.core.network.DeviceTypeDto
import com.axzydev.puertonuevoapp.core.network.DeviceUpdateDto
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import kotlinx.coroutines.launch

/**
 * Alta/edición de UN dispositivo (paridad con DeviceFormPage del web, sin el
 * modo de alta/edición por lote ni la importación por CSV — flujos de
 * escritorio para cientos de unidades a la vez que no aportan en móvil;
 * se puede sumar después si hace falta).
 */
@Composable
fun DeviceFormScreen(deviceId: String?) {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val isEdit = deviceId != null

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var types by remember { mutableStateOf<List<DeviceTypeDto>>(emptyList()) }

    var typeId by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("SISTEMAS") }
    var numeroSerie by remember { mutableStateOf("") }
    var nombreEquipo by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var macAddress by remember { mutableStateOf("") }
    var sistemaOp by remember { mutableStateOf("") }
    var ram by remember { mutableStateOf("") }
    var almacenamiento by remember { mutableStateOf("") }
    var blocked by remember { mutableStateOf(false) }
    var blockedCode by remember { mutableStateOf("") }

    suspend fun load() {
        loading = true
        error = null
        try {
            val loadedTypes = AppContainer.deviceTypesApi.list()
            types = loadedTypes
            if (isEdit && deviceId != null) {
                val d = AppContainer.devicesApi.get(deviceId)
                typeId = d.typeId
                descripcion = d.descripcion
                marca = d.marca
                modelo = d.modelo
                area = d.area
                numeroSerie = d.numeroSerie ?: ""
                nombreEquipo = d.nombreEquipo ?: ""
                ip = d.ip ?: ""
                macAddress = d.macAddress ?: ""
                sistemaOp = d.sistemaOp ?: ""
                ram = d.ram ?: ""
                almacenamiento = d.almacenamiento ?: ""
                if (d.estado == "ASIGNADO") {
                    blocked = true
                    blockedCode = d.controlActivos
                }
            } else if (loadedTypes.isNotEmpty() && typeId.isBlank()) {
                typeId = loadedTypes.first().id
            }
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar la información"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(deviceId) { load() }

    val selectedType = types.firstOrNull { it.id == typeId }
    fun showField(key: String): Boolean = when (key) {
        "numeroSerie" -> selectedType?.fieldConfig?.numeroSerie?.enabled ?: true
        "nombreEquipo" -> selectedType?.fieldConfig?.nombreEquipo?.enabled ?: true
        "ip" -> selectedType?.fieldConfig?.ip?.enabled ?: false
        "macAddress" -> selectedType?.fieldConfig?.macAddress?.enabled ?: false
        "sistemaOp" -> selectedType?.fieldConfig?.sistemaOp?.enabled ?: false
        "ram" -> selectedType?.fieldConfig?.ram?.enabled ?: false
        "almacenamiento" -> selectedType?.fieldConfig?.almacenamiento?.enabled ?: false
        else -> false
    }
    val showSpecs = listOf("ip", "macAddress", "sistemaOp", "ram", "almacenamiento").any { showField(it) }
    val isValid = typeId.isNotBlank() && descripcion.isNotBlank() && marca.isNotBlank() && modelo.isNotBlank()

    fun submit() {
        if (!isValid || saving || blocked) return
        saving = true
        error = null
        scope.launch {
            try {
                if (isEdit && deviceId != null) {
                    AppContainer.devicesApi.update(
                        deviceId,
                        DeviceUpdateDto(
                            typeId = typeId,
                            descripcion = descripcion.trim(),
                            marca = marca.trim(),
                            modelo = modelo.trim(),
                            area = area.trim().ifBlank { null },
                            numeroSerie = if (showField("numeroSerie")) numeroSerie.trim().ifBlank { null } else null,
                            nombreEquipo = if (showField("nombreEquipo")) nombreEquipo.trim().ifBlank { null } else null,
                            ip = if (showField("ip")) ip.trim().ifBlank { null } else null,
                            macAddress = if (showField("macAddress")) macAddress.trim().ifBlank { null } else null,
                            sistemaOp = if (showField("sistemaOp")) sistemaOp.trim().ifBlank { null } else null,
                            ram = if (showField("ram")) ram.trim().ifBlank { null } else null,
                            almacenamiento = if (showField("almacenamiento")) almacenamiento.trim().ifBlank { null } else null,
                        ),
                    )
                } else {
                    AppContainer.devicesApi.create(
                        DeviceCreateDto(
                            typeId = typeId,
                            descripcion = descripcion.trim(),
                            marca = marca.trim(),
                            modelo = modelo.trim(),
                            area = area.trim().ifBlank { null },
                            numeroSerie = if (showField("numeroSerie")) numeroSerie.trim().ifBlank { null } else null,
                            nombreEquipo = if (showField("nombreEquipo")) nombreEquipo.trim().ifBlank { null } else null,
                            ip = if (showField("ip")) ip.trim().ifBlank { null } else null,
                            macAddress = if (showField("macAddress")) macAddress.trim().ifBlank { null } else null,
                            sistemaOp = if (showField("sistemaOp")) sistemaOp.trim().ifBlank { null } else null,
                            ram = if (showField("ram")) ram.trim().ifBlank { null } else null,
                            almacenamiento = if (showField("almacenamiento")) almacenamiento.trim().ifBlank { null } else null,
                        ),
                    )
                }
                navigator.pop()
            } catch (e: Exception) {
                error = e.message ?: "No se pudo guardar el dispositivo"
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
        if (blocked) {
            AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Text("Bloqueado", style = MaterialTheme.typography.titleSmall, color = AppColors.Warning)
                Spacer(Modifier.height(4.dp))
                Text(
                    "El dispositivo (activo $blockedCode) está asignado y no se puede editar. Registra su devolución primero.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                )
            }
            Spacer(Modifier.height(14.dp))
        }
        if (error != null) {
            Text(error ?: "", color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
        }

        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Datos del dispositivo")
            SimpleDropdownField(
                label = "Tipo",
                value = typeId,
                options = types.map { it.id to "${it.name} (${it.prefix})" },
                onSelect = { typeId = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción") },
                enabled = !blocked,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = marca,
                onValueChange = { marca = it },
                label = { Text("Marca") },
                enabled = !blocked,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = modelo,
                onValueChange = { modelo = it },
                label = { Text("Modelo") },
                enabled = !blocked,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = area,
                onValueChange = { area = it },
                label = { Text("Área") },
                enabled = !blocked,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            if (showField("numeroSerie")) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = numeroSerie,
                    onValueChange = { numeroSerie = it },
                    label = { Text("No. de serie") },
                    enabled = !blocked,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (showField("nombreEquipo")) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = nombreEquipo,
                    onValueChange = { nombreEquipo = it },
                    label = { Text("Nombre del equipo") },
                    enabled = !blocked,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (showSpecs) {
            Spacer(Modifier.height(14.dp))
            AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                SectionLabel("Especificaciones técnicas")
                if (showField("ip")) {
                    OutlinedTextField(
                        value = ip,
                        onValueChange = { ip = it },
                        label = { Text("IP") },
                        placeholder = { Text("192.168.0.1") },
                        enabled = !blocked,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (showField("macAddress")) {
                    OutlinedTextField(
                        value = macAddress,
                        onValueChange = { macAddress = it },
                        label = { Text("MAC") },
                        placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                        enabled = !blocked,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (showField("sistemaOp")) {
                    OutlinedTextField(
                        value = sistemaOp,
                        onValueChange = { sistemaOp = it },
                        label = { Text("Sistema operativo") },
                        enabled = !blocked,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (showField("ram")) {
                    OutlinedTextField(
                        value = ram,
                        onValueChange = { ram = it },
                        label = { Text("RAM") },
                        enabled = !blocked,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (showField("almacenamiento")) {
                    OutlinedTextField(
                        value = almacenamiento,
                        onValueChange = { almacenamiento = it },
                        label = { Text("Almacenamiento") },
                        enabled = !blocked,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { submit() },
            enabled = isValid && !saving && !blocked,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            if (saving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AppColors.Surface, strokeWidth = 2.dp)
            } else {
                Text(if (isEdit) "Guardar cambios" else "Dar de alta", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
