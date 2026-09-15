package com.axzydev.puertonuevoapp.feature.cartas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip

@Composable
fun GenerateCartaScreen(
    viewModel: GenerateCartaViewModel = viewModel { GenerateCartaViewModel(AppContainer.cartasApi, AppContainer.deviceTypesApi) },
) {
    val navigator = LocalNavigator.current
    val state by viewModel.uiState.collectAsState()

    if (state.loading) {
        LoadingState(modifier = Modifier.fillMaxSize())
        return
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            SimpleDropdownField(
                label = "Tipo de dispositivo",
                value = state.typeId,
                options = state.types.map { it.id to "${it.name} · ${it.code}" },
                onSelect = viewModel::onTypeChange,
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { navigator.pop() }, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(
                    onClick = viewModel::generate,
                    enabled = state.typeId.isNotBlank() && !state.generating,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
                    modifier = Modifier.weight(1f),
                ) {
                    if (state.generating) CircularProgressIndicator(Modifier.size(18.dp), color = AppColors.Surface, strokeWidth = 2.dp)
                    else Text("Generar")
                }
            }
        }

        state.result?.let { generated ->
            Spacer(Modifier.height(16.dp))
            AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Text("Carta generada para ${generated.tipo.name}", style = MaterialTheme.typography.titleSmall, color = AppColors.Success)
                Spacer(Modifier.height(10.dp))
                StatusChip(generated.carta.consecutivo, AppColors.EmeraldPrimary)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { navigator.pop(); navigator.push(Screen.CartaDetail(generated.carta.id)) },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Ver carta generada") }
            }
        }
    }
}
