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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.network.salidas.salidaMotivoOptions
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField

@Composable
fun SalidaFormScreen(salidaId: String? = null) {
    val navigator = LocalNavigator.current
    val viewModel: SalidaFormViewModel = viewModel(key = "salida-form-${salidaId ?: "new"}") {
        SalidaFormViewModel(salidaId, AppContainer.salidasApi)
    }
    val state by viewModel.uiState.collectAsState()
    val isEdit = salidaId != null

    LaunchedEffect(state.saved) { if (state.saved) navigator.pop() }

    if (state.loading) {
        LoadingState(modifier = Modifier.fillMaxSize())
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(16.dp),
    ) {
        state.error?.let {
            Text(it, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
        }

        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Datos de la salida")
            OutlinedTextField(
                value = state.descripcion,
                onValueChange = viewModel::onDescripcionChange,
                label = { Text("Descripción") },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.marca,
                    onValueChange = viewModel::onMarcaChange,
                    label = { Text("Marca") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.modelo,
                    onValueChange = viewModel::onModeloChange,
                    label = { Text("Modelo") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.proyecto,
                    onValueChange = viewModel::onProyectoChange,
                    label = { Text("Proyecto") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(2f),
                )
                OutlinedTextField(
                    value = state.cantidad,
                    onValueChange = viewModel::onCantidadChange,
                    label = { Text("Cantidad") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.departamento,
                onValueChange = viewModel::onDepartamentoChange,
                label = { Text("Departamento") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.usuario,
                onValueChange = viewModel::onUsuarioChange,
                label = { Text("Usuario") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.area,
                onValueChange = viewModel::onAreaChange,
                label = { Text("Área") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            SimpleDropdownField(
                label = "Motivo (si aplica)",
                value = state.motivo,
                options = salidaMotivoOptions,
                onSelect = viewModel::onMotivoChange,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.observaciones,
                onValueChange = viewModel::onObservacionesChange,
                label = { Text("Observaciones") },
                minLines = 2,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = viewModel::submit,
            enabled = state.isValid && !state.saving,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            if (state.saving) CircularProgressIndicator(modifier = Modifier.height(20.dp), color = AppColors.Surface, strokeWidth = 2.dp)
            else Text(if (isEdit) "Guardar cambios" else "Registrar salida", style = MaterialTheme.typography.titleMedium)
        }
    }
}
