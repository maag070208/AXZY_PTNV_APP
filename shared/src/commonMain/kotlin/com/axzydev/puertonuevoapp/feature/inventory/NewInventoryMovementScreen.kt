package com.axzydev.puertonuevoapp.feature.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.axzydev.puertonuevoapp.core.network.DeviceDto
import com.axzydev.puertonuevoapp.core.network.LocationDto
import com.axzydev.puertonuevoapp.core.network.RegisterMovementDto
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.util.condicionLabel
import com.axzydev.puertonuevoapp.core.util.deviceEstadoLabel
import com.axzydev.puertonuevoapp.core.util.formatLocation
import com.axzydev.puertonuevoapp.core.util.movementTypeLabel
import kotlinx.coroutines.launch

private val tipoOptions = listOf(
    "ENTRADA" to "Entrada (alta en inventario)",
    "SALIDA" to "Salida (retirar de ubicación)",
    "TRASLADO" to "Traslado (mover a otra ubicación)",
    "BAJA" to "Baja (dar de baja el dispositivo)",
    "PRESTAMO" to "Asignado (equipo entregado a alguien)",
    "DEVOLUCION" to "Devolución (equipo regresado de asignación)",
)

private val condicionOptions = listOf("BUENO", "ACEPTABLE", "MALO", "ROTO")

/**
 * Registrar movimiento de inventario (paridad con NewInventoryMovementPage
 * del web), incluyendo el flujo especial de "malas condiciones" al hacer una
 * devolución: da de baja el equipo (dos movimientos: BAJA + DEVOLUCION) o
 * solo registra la devolución con la condición (el caso "crear ticket" del
 * web tampoco crea un ticket automáticamente ahí — se replica igual aquí).
 */
@Composable
fun NewInventoryMovementScreen(deviceId: String? = null) {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()

    var devices by remember { mutableStateOf<List<DeviceDto>>(emptyList()) }
    var locations by remember { mutableStateOf<List<LocationDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    var deviceIdSel by remember { mutableStateOf(deviceId ?: "") }
    var tipo by remember { mutableStateOf("") }
    var locationIdSel by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }
    var prestadoA by remember { mutableStateOf("") }
    var fechaRetorno by remember { mutableStateOf("") }
    var condicion by remember { mutableStateOf("") }
    var motivoBaja by remember { mutableStateOf("") }
    var accionMalasCondiciones by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        try {
            devices = AppContainer.devicesApi.list().data
            locations = AppContainer.locationsApi.list()
            if (deviceId != null) {
                devices.firstOrNull { it.id == deviceId }?.let { d ->
                    deviceIdSel = d.id
                    locationIdSel = d.locationId ?: ""
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar la información"
        } finally {
            loading = false
        }
    }

    val selectedDevice = devices.firstOrNull { it.id == deviceIdSel }
    val requiresLocation = tipo == "ENTRADA" || tipo == "TRASLADO"
    val requiresPrestamo = tipo == "PRESTAMO"
    val requiresDevolucion = tipo == "DEVOLUCION"
    val isMalasCondiciones = condicion == "MALO" || condicion == "ROTO"

    val isValid = deviceIdSel.isNotBlank() && tipo.isNotBlank() &&
        (!requiresLocation || locationIdSel.isNotBlank()) &&
        (!requiresPrestamo || (prestadoA.isNotBlank() && fechaRetorno.isNotBlank())) &&
        (!requiresDevolucion || condicion.isNotBlank()) &&
        (!isMalasCondiciones || accionMalasCondiciones.isNotBlank())

    fun submit() {
        if (!isValid || saving) return
        saving = true
        error = null
        scope.launch {
            try {
                when {
                    isMalasCondiciones && accionMalasCondiciones == "BAJA" -> {
                        AppContainer.inventoryApi.registerMovement(
                            RegisterMovementDto(
                                deviceId = deviceIdSel,
                                tipo = "BAJA",
                                notas = if (notas.isNotBlank()) "$notas | Condición: $condicion" else "Condición: $condicion",
                                motivoBaja = "Equipo devuelto en condiciones ${condicion.lowercase()}",
                            ),
                        )
                        AppContainer.inventoryApi.registerMovement(
                            RegisterMovementDto(
                                deviceId = deviceIdSel,
                                tipo = "DEVOLUCION",
                                notas = notas.ifBlank { null },
                                condicion = condicion,
                            ),
                        )
                    }
                    isMalasCondiciones && accionMalasCondiciones == "TICKET" -> {
                        AppContainer.inventoryApi.registerMovement(
                            RegisterMovementDto(
                                deviceId = deviceIdSel,
                                tipo = "DEVOLUCION",
                                notas = notas.ifBlank { null },
                                condicion = condicion,
                            ),
                        )
                    }
                    else -> {
                        AppContainer.inventoryApi.registerMovement(
                            RegisterMovementDto(
                                deviceId = deviceIdSel,
                                tipo = tipo,
                                locationId = if (requiresLocation) locationIdSel else null,
                                notas = notas.trim().ifBlank { null },
                                prestadoA = if (requiresPrestamo) prestadoA.trim() else null,
                                fechaRetornoEsperado = if (requiresPrestamo) fechaRetorno.trim() else null,
                                condicion = if (requiresDevolucion) condicion else null,
                                motivoBaja = if (tipo == "BAJA") motivoBaja.trim().ifBlank { null } else null,
                            ),
                        )
                    }
                }
                navigator.pop()
            } catch (e: Exception) {
                error = e.message ?: "No se pudo registrar el movimiento"
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
            SectionLabel("Datos del movimiento")
            SimpleDropdownField(
                label = "Dispositivo",
                value = deviceIdSel,
                options = devices.map { it.id to "${it.controlActivos} - ${it.descripcion}" },
                onSelect = { id ->
                    deviceIdSel = id
                    locationIdSel = devices.firstOrNull { it.id == id }?.locationId ?: ""
                },
                modifier = Modifier.fillMaxWidth(),
            )
            selectedDevice?.let { d ->
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.SurfaceVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    Text("${d.controlActivos} · ${deviceEstadoLabel(d.estado)}", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                    Text(
                        "Ubicación actual: ${d.location?.let { formatLocation(it.lugar, it.subLugar, it.numero) } ?: "Sin ubicación"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            SimpleDropdownField(
                label = "Tipo de movimiento",
                value = tipo,
                options = tipoOptions,
                onSelect = { tipo = it; condicion = ""; accionMalasCondiciones = "" },
                modifier = Modifier.fillMaxWidth(),
            )

            if (requiresLocation) {
                Spacer(Modifier.height(10.dp))
                SimpleDropdownField(
                    label = "Ubicación destino",
                    value = locationIdSel,
                    options = locations.map { it.id to formatLocation(it.lugar, it.subLugar, it.numero) },
                    onSelect = { locationIdSel = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (requiresPrestamo) {
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.Info.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                ) {
                    Text("Datos de la asignación", style = MaterialTheme.typography.labelSmall, color = AppColors.Info)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = prestadoA,
                        onValueChange = { prestadoA = it },
                        label = { Text("Asignado a") },
                        placeholder = { Text("Nombre de quien recibe el equipo…") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = fechaRetorno,
                        onValueChange = { fechaRetorno = it },
                        label = { Text("Fecha de retorno esperada (AAAA-MM-DD)") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (requiresDevolucion) {
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.Success.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                ) {
                    Text("Condición del equipo al regresar", style = MaterialTheme.typography.labelSmall, color = AppColors.Success)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        condicionOptions.forEach { c ->
                            val selected = condicion == c
                            Text(
                                condicionLabel(c),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) AppColors.Surface else AppColors.TextMuted,
                                modifier = Modifier
                                    .background(if (selected) AppColors.condicionColor(c) else AppColors.SurfaceVariant, RoundedCornerShape(20.dp))
                                    .clickable { condicion = c; accionMalasCondiciones = "" }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }

            if (isMalasCondiciones) {
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.Danger.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                ) {
                    Text("El equipo está en malas condiciones. ¿Qué acción deseas tomar?", style = MaterialTheme.typography.labelSmall, color = AppColors.Danger)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { accionMalasCondiciones = "BAJA" },
                            colors = if (accionMalasCondiciones == "BAJA") ButtonDefaults.buttonColors(containerColor = AppColors.Danger, contentColor = AppColors.Surface) else ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Danger),
                        ) { Text("Dar de baja") }
                        OutlinedButton(
                            onClick = { accionMalasCondiciones = "TICKET" },
                            colors = if (accionMalasCondiciones == "TICKET") ButtonDefaults.buttonColors(containerColor = AppColors.Warning, contentColor = AppColors.Surface) else ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Warning),
                        ) { Text("Solo registrar") }
                    }
                    if (accionMalasCondiciones == "BAJA") {
                        Spacer(Modifier.height(8.dp))
                        Text("El dispositivo será dado de baja permanentemente.", style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
                    }
                }
            }

            val currentDeviceLocation = selectedDevice?.location
            if (tipo == "SALIDA" && currentDeviceLocation != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "El dispositivo se retirará de: ${formatLocation(currentDeviceLocation.lugar, currentDeviceLocation.subLugar, currentDeviceLocation.numero)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Danger,
                )
            }

            if (tipo == "BAJA") {
                Spacer(Modifier.height(10.dp))
                Text("Esta acción marcará el dispositivo como BAJA. No podrá ser usado nuevamente.", style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = motivoBaja,
                    onValueChange = { motivoBaja = it },
                    label = { Text("Motivo de la baja (opcional)") },
                    placeholder = { Text("Ej. Equipo en mal estado, robado, etc.") },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = notas,
                onValueChange = { notas = it },
                label = { Text("Notas (opcional)") },
                placeholder = { Text("Observaciones adicionales…") },
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
                Text("Registrar movimiento", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
