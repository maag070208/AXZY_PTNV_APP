package com.axzydev.puertonuevoapp.feature.salidas

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
import com.axzydev.puertonuevoapp.core.network.SalidaInputDto
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import kotlinx.coroutines.launch

@Composable
fun SalidaFormScreen(salidaId: String? = null) {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val isEdit = salidaId != null

    var loading by remember { mutableStateOf(isEdit) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var area by remember { mutableStateOf("Sistemas") }
    var descripcion by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var proyecto by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("1") }
    var departamento by remember { mutableStateOf("") }
    var usuario by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }

    LaunchedEffect(salidaId) {
        if (salidaId == null) return@LaunchedEffect
        loading = true
        error = null
        try {
            val s = AppContainer.salidasApi.get(salidaId)
            area = s.area
            descripcion = s.descripcion
            modelo = s.modelo ?: ""
            marca = s.marca ?: ""
            proyecto = s.proyecto ?: ""
            cantidad = s.cantidad.toString()
            departamento = s.departamento
            usuario = s.usuario
            observaciones = s.observaciones ?: ""
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el registro"
        } finally {
            loading = false
        }
    }

    val isValid = descripcion.isNotBlank() && departamento.isNotBlank() && usuario.isNotBlank()

    fun submit() {
        if (!isValid || saving) return
        saving = true
        error = null
        scope.launch {
            try {
                val input = SalidaInputDto(
                    descripcion = descripcion.trim(),
                    modelo = modelo.trim().ifBlank { null },
                    marca = marca.trim().ifBlank { null },
                    proyecto = proyecto.trim().ifBlank { null },
                    cantidad = cantidad.toIntOrNull()?.takeIf { it > 0 } ?: 1,
                    departamento = departamento.trim(),
                    usuario = usuario.trim(),
                    observaciones = observaciones.trim().ifBlank { null },
                    area = area.trim().ifBlank { null },
                )
                if (isEdit && salidaId != null) {
                    AppContainer.salidasApi.update(salidaId, input)
                } else {
                    AppContainer.salidasApi.create(input)
                }
                navigator.pop()
            } catch (e: Exception) {
                error = e.message ?: "No se pudo guardar el registro"
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
            SectionLabel("Datos de la salida")
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción") },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = marca,
                    onValueChange = { marca = it },
                    label = { Text("Marca") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = modelo,
                    onValueChange = { modelo = it },
                    label = { Text("Modelo") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = proyecto,
                    onValueChange = { proyecto = it },
                    label = { Text("Proyecto") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(2f),
                )
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { cantidad = it.filter { c -> c.isDigit() } },
                    label = { Text("Cantidad") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = departamento,
                onValueChange = { departamento = it },
                label = { Text("Departamento") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = usuario,
                onValueChange = { usuario = it },
                label = { Text("Usuario") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = area,
                onValueChange = { area = it },
                label = { Text("Área") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = observaciones,
                onValueChange = { observaciones = it },
                label = { Text("Observaciones") },
                minLines = 2,
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
                Text(if (isEdit) "Guardar cambios" else "Registrar salida", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
