package com.axzydev.puertonuevoapp.feature.materialoutputs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.MaterialTheme
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
import com.axzydev.puertonuevoapp.core.network.materialoutputs.materialOutputReasonOptions
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField

/**
 * Alta/edición de una salida de material. Formulario en bloques de una columna
 * (qué sale → destino → detalle) y un único botón primario al final.
 */
@Composable
fun MaterialOutputFormScreen(materialOutputId: String? = null) {
    val navigator = LocalNavigator.current
    val viewModel: MaterialOutputFormViewModel = viewModel(key = "material-output-form-${materialOutputId ?: "new"}") {
        MaterialOutputFormViewModel(materialOutputId, AppContainer.materialOutputsApi)
    }
    val state by viewModel.uiState.collectAsState()
    val isEdit = materialOutputId != null

    LaunchedEffect(state.saved) { if (state.saved) navigator.pop() }

    if (state.loading) {
        LoadingState(modifier = Modifier.fillMaxSize())
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(16.dp),
    ) {
        state.error?.let {
            BentoState(
                message = it,
                tone = BentoTone.Rose,
                icon = Icons.Filled.ErrorOutline,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
        }

        // Bloque 1: qué sale.
        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Qué sale")
            AppTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = state.brand,
                onValueChange = viewModel::onBrandChange,
                label = { Text("Marca") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = state.model,
                onValueChange = viewModel::onModelChange,
                label = { Text("Modelo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(12.dp))

        // Bloque 2: a dónde va.
        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Destino")
            AppTextField(
                value = state.departmentName,
                onValueChange = viewModel::onDepartmentNameChange,
                label = { Text("Departamento") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = state.userName,
                onValueChange = viewModel::onUserNameChange,
                label = { Text("Usuario") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = state.area,
                onValueChange = viewModel::onAreaChange,
                label = { Text("Área") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = state.project,
                onValueChange = viewModel::onProjectChange,
                label = { Text("Proyecto") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(12.dp))

        // Bloque 3: detalle de la salida.
        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Detalle")
            AppTextField(
                value = state.quantity,
                onValueChange = viewModel::onQuantityChange,
                label = { Text("Cantidad") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            SimpleDropdownField(
                label = "Motivo (si aplica)",
                value = state.reason,
                options = materialOutputReasonOptions,
                onSelect = viewModel::onReasonChange,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Observaciones") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(20.dp))
        PillButton(
            text = if (isEdit) "Guardar cambios" else "Registrar salida",
            onClick = viewModel::submit,
            enabled = state.isValid,
            loading = state.saving,
            variant = PillButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
    }
}
