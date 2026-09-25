package com.axzydev.puertonuevoapp.feature.tickets

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
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.AppTextField

@Composable
fun NewTicketScreen(viewModel: TicketFormViewModel = viewModel(key = "ticket-form-new") { TicketFormViewModel(null, AppContainer.ticketsApi) }) {
    val navigator = LocalNavigator.current
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.savedTicketId) {
        state.savedTicketId?.let { id ->
            navigator.pop()
            navigator.push(Screen.TicketDetail(id))
        }
    }

    TicketFormContent(state = state, viewModel = viewModel, submitLabel = "Crear ticket")
}

@Composable
fun EditTicketScreen(ticketId: String) {
    val navigator = LocalNavigator.current
    val viewModel: TicketFormViewModel = viewModel(key = "ticket-form-$ticketId") {
        TicketFormViewModel(ticketId, AppContainer.ticketsApi)
    }
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.savedTicketId) {
        if (state.savedTicketId != null) navigator.pop()
    }

    if (state.loading) {
        com.axzydev.puertonuevoapp.core.ui.LoadingState(modifier = Modifier.fillMaxSize())
    } else {
        TicketFormContent(state = state, viewModel = viewModel, submitLabel = "Guardar cambios")
    }
}

@Composable
private fun TicketFormContent(
    state: TicketFormUiState,
    viewModel: TicketFormViewModel,
    submitLabel: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(16.dp),
    ) {
        AppTextField(
            value = state.titulo,
            onValueChange = viewModel::onTituloChange,
            label = { Text("Título") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        AppTextField(
            value = state.descripcion,
            onValueChange = viewModel::onDescripcionChange,
            label = { Text("Descripción") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        SimpleDropdownField(
            label = "Prioridad",
            value = state.priority,
            options = ticketPriorityOptions,
            onSelect = viewModel::onPriorityChange,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        SimpleDropdownField(
            label = "Categoría",
            value = state.categoryId,
            options = state.categoryOptions,
            onSelect = viewModel::onCategoryChange,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.error != null) {
            Spacer(Modifier.height(12.dp))
            Text(state.error, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = viewModel::submit,
            enabled = state.isValid && !state.saving,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            if (state.saving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AppColors.Surface, strokeWidth = 2.dp)
            } else {
                Text(submitLabel, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
