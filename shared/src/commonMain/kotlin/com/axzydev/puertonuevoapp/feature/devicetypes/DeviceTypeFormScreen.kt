package com.axzydev.puertonuevoapp.feature.devicetypes

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.network.DeviceFieldConfigDto
import com.axzydev.puertonuevoapp.core.network.DeviceFieldSettingDto
import com.axzydev.puertonuevoapp.core.network.DeviceTypeCreateDto
import com.axzydev.puertonuevoapp.core.network.DeviceTypeUpdateDto
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import kotlinx.coroutines.launch

private data class FieldDef(val key: String, val label: String)

private val fieldDefs = listOf(
    FieldDef("numeroSerie", "Número de serie"),
    FieldDef("nombreEquipo", "Nombre de equipo"),
    FieldDef("ip", "Dirección IP"),
    FieldDef("macAddress", "MAC Address"),
    FieldDef("sistemaOp", "Sistema operativo"),
    FieldDef("ram", "RAM"),
    FieldDef("almacenamiento", "Almacenamiento"),
)

private fun DeviceFieldConfigDto.settingFor(key: String): DeviceFieldSettingDto = when (key) {
    "numeroSerie" -> numeroSerie
    "nombreEquipo" -> nombreEquipo
    "ip" -> ip
    "macAddress" -> macAddress
    "sistemaOp" -> sistemaOp
    "ram" -> ram
    "almacenamiento" -> almacenamiento
    else -> DeviceFieldSettingDto()
}

private fun DeviceFieldConfigDto.withSetting(key: String, setting: DeviceFieldSettingDto): DeviceFieldConfigDto = when (key) {
    "numeroSerie" -> copy(numeroSerie = setting)
    "nombreEquipo" -> copy(nombreEquipo = setting)
    "ip" -> copy(ip = setting)
    "macAddress" -> copy(macAddress = setting)
    "sistemaOp" -> copy(sistemaOp = setting)
    "ram" -> copy(ram = setting)
    "almacenamiento" -> copy(almacenamiento = setting)
    else -> this
}

@Composable
fun DeviceTypeFormScreen(typeId: String? = null) {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val isEdit = typeId != null

    var loading by remember { mutableStateOf(isEdit) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var prefix by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(true) }
    var fieldConfig by remember { mutableStateOf(DeviceFieldConfigDto()) }

    suspend fun load() {
        if (typeId == null) return
        loading = true
        error = null
        try {
            val t = AppContainer.deviceTypesApi.get(typeId)
            code = t.code
            name = t.name
            prefix = t.prefix
            active = t.active
            fieldConfig = t.fieldConfig
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el tipo"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(typeId) { load() }

    val isValid = code.isNotBlank() && name.isNotBlank() && prefix.isNotBlank()

    fun submit() {
        if (!isValid || saving) return
        saving = true
        error = null
        scope.launch {
            try {
                if (isEdit && typeId != null) {
                    AppContainer.deviceTypesApi.update(
                        typeId,
                        DeviceTypeUpdateDto(
                            name = name.trim(),
                            prefix = prefix.trim().uppercase(),
                            active = active,
                            fieldConfig = fieldConfig,
                        ),
                    )
                } else {
                    AppContainer.deviceTypesApi.create(
                        DeviceTypeCreateDto(
                            code = code.trim(),
                            name = name.trim(),
                            prefix = prefix.trim().uppercase(),
                            fieldConfig = fieldConfig,
                        ),
                    )
                }
                navigator.pop()
            } catch (e: Exception) {
                error = e.message ?: "No se pudo guardar el tipo de dispositivo"
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
            SectionLabel("Datos del tipo")
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.uppercase() },
                label = { Text("Código interno") },
                placeholder = { Text("LAPTOP") },
                enabled = !isEdit,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                placeholder = { Text("Laptop") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = prefix,
                onValueChange = { prefix = it.uppercase() },
                label = { Text("Prefijo (consecutivo)") },
                placeholder = { Text("LPT") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Ej: ${prefix.ifBlank { "LPT" }}-0001, ${prefix.ifBlank { "LPT" }}-0002…",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (isEdit) {
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Activo", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Campos de este tipo")
            Text(
                "Define qué datos aparecen al dar de alta cada dispositivo y cuáles son obligatorios.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
            )
            Spacer(Modifier.height(8.dp))
            fieldDefs.forEach { def ->
                val setting = fieldConfig.settingFor(def.key)
                FieldConfigRow(
                    label = def.label,
                    enabled = setting.enabled,
                    required = setting.required,
                    onEnabledChange = { checked ->
                        fieldConfig = fieldConfig.withSetting(def.key, setting.copy(enabled = checked, required = checked && setting.required))
                    },
                    onRequiredChange = { checked ->
                        fieldConfig = fieldConfig.withSetting(def.key, setting.copy(required = checked))
                    },
                )
            }
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
                Text(if (isEdit) "Guardar cambios" else "Crear tipo", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun FieldConfigRow(
    label: String,
    enabled: Boolean,
    required: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onRequiredChange: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = enabled, onCheckedChange = onEnabledChange)
            Text("Mostrar", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
            Spacer(Modifier.width(16.dp))
            Checkbox(checked = required, onCheckedChange = onRequiredChange, enabled = enabled)
            Text("Obligatorio", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
        }
    }
}
