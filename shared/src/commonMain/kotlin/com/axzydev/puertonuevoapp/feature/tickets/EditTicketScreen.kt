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
import com.axzydev.puertonuevoapp.core.network.TicketEditDto
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.util.ticketPriorityLabel
import kotlinx.coroutines.launch

private val priorityOptions = listOf("BAJA", "MEDIA", "ALTA", "URGENTE").map { it to ticketPriorityLabel(it) }

@Composable
fun EditTicketScreen(ticketId: String) {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIA") }

    suspend fun load() {
        loading = true
        error = null
        try {
            val t = AppContainer.ticketsApi.get(ticketId)
            titulo = t.titulo
            descripcion = t.descripcion
            priority = t.priority
            loaded = true
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el ticket"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(ticketId) { load() }

    fun submit() {
        if (titulo.isBlank() || descripcion.isBlank() || saving) return
        saving = true
        error = null
        scope.launch {
            try {
                AppContainer.ticketsApi.update(
                    ticketId,
                    TicketEditDto(titulo = titulo.trim(), descripcion = descripcion.trim(), priority = priority),
                )
                navigator.pop()
            } catch (e: Exception) {
                error = e.message ?: "No se pudo actualizar el ticket"
            } finally {
                saving = false
            }
        }
    }

    when {
        loading -> LoadingState(modifier = Modifier.fillMaxSize())
        !loaded -> ErrorState(
            message = error ?: "No se pudo cargar el ticket",
            modifier = Modifier.fillMaxSize(),
            onRetry = { scope.launch { load() } },
        )
        else -> Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Título") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción") },
                minLines = 4,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            SimpleDropdownField(
                label = "Prioridad",
                value = priority,
                options = priorityOptions,
                onSelect = { priority = it },
                modifier = Modifier.fillMaxWidth(),
            )

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error ?: "", color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { submit() },
                enabled = titulo.isNotBlank() && descripcion.isNotBlank() && !saving,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AppColors.Surface, strokeWidth = 2.dp)
                } else {
                    Text("Guardar cambios", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
