package com.axzydev.puertonuevoapp.feature.inventory

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.LoadingState

@Composable
fun LocationFormScreen(locationId: String? = null) {
    val navigator = LocalNavigator.current
    val viewModel: LocationFormViewModel = viewModel(key = "location-form-${locationId ?: "new"}") {
        LocationFormViewModel(locationId, AppContainer.locationsApi)
    }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saved) { if (state.saved) navigator.pop() }

    if (state.loading) {
        LoadingState(Modifier.fillMaxSize())
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(16.dp),
    ) {
        OutlinedTextField(
            value = state.lugar,
            onValueChange = viewModel::onLugarChange,
            label = { Text("Lugar") },
            placeholder = { Text("Ej. OFICINA, BODEGA") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.descripcion,
            onValueChange = viewModel::onDescripcionChange,
            label = { Text("Descripción") },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = viewModel::submit,
            enabled = state.isValid && !state.saving,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            if (state.saving) CircularProgressIndicator(Modifier.size(20.dp), color = AppColors.Surface, strokeWidth = 2.dp)
            else Text(if (locationId == null) "Crear ubicación" else "Guardar cambios", style = MaterialTheme.typography.titleMedium)
        }
    }
}
