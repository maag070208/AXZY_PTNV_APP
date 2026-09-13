package com.axzydev.puertonuevoapp.feature.tickets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.TicketInputDto
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.util.ticketCategoryLabel
import com.axzydev.puertonuevoapp.core.util.ticketPriorityLabel
import kotlinx.coroutines.launch

private val priorityOptions = listOf("BAJA", "MEDIA", "ALTA", "URGENTE").map { it to ticketPriorityLabel(it) }
private val categoryOptions = listOf("MANTENIMIENTO", "EQUIPO", "SISTEMA", "OTRO").map { it to ticketCategoryLabel(it) }

@Composable
fun NewTicketScreen() {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()

    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIA") }
    var category by remember { mutableStateOf("OTRO") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (titulo.isBlank() || descripcion.isBlank() || saving) return
        saving = true
        error = null
        scope.launch {
            try {
                val created = AppContainer.ticketsApi.create(
                    TicketInputDto(titulo = titulo.trim(), descripcion = descripcion.trim(), priority = priority, category = category),
                )
                navigator.pop()
                navigator.push(Screen.TicketDetail(created.id))
            } catch (e: Exception) {
                error = e.message ?: "No se pudo crear el ticket"
            } finally {
                saving = false
            }
        }
    }

    Column(
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
            Spacer(Modifier.height(12.dp))
            SimpleDropdownField(
                label = "Categoría",
                value = category,
                options = categoryOptions,
                onSelect = { category = it },
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
                    Text("Crear ticket", style = MaterialTheme.typography.titleMedium)
                }
            }
    }
}
