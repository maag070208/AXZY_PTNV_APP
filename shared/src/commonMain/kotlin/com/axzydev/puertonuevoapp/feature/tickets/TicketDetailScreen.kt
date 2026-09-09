package com.axzydev.puertonuevoapp.feature.tickets

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.axzydev.puertonuevoapp.core.network.TicketDto
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatDateTime
import com.axzydev.puertonuevoapp.core.util.ticketCategoryLabel
import com.axzydev.puertonuevoapp.core.util.ticketPriorityLabel
import com.axzydev.puertonuevoapp.core.util.ticketStatusLabel
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width

private val statusOptions = listOf(
    "ABIERTO" to "Abierto",
    "EN_SEGUIMIENTO" to "En seguimiento",
    "CERRADO" to "Cerrado",
)

@Composable
fun TicketDetailScreen(ticketId: String) {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var ticket by remember { mutableStateOf<TicketDto?>(null) }
    var updatingStatus by remember { mutableStateOf(false) }
    var newComment by remember { mutableStateOf("") }
    var sendingComment by remember { mutableStateOf(false) }

    suspend fun load() {
        loading = true
        error = null
        try {
            ticket = AppContainer.ticketsApi.get(ticketId)
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el ticket"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(ticketId) { load() }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Ticket", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Surface),
            )
        },
    ) { padding ->
        when {
            loading -> LoadingState(modifier = Modifier.fillMaxSize().padding(padding))
            error != null || ticket == null -> ErrorState(
                message = error ?: "Ticket no encontrado",
                modifier = Modifier.fillMaxSize().padding(padding),
                onRetry = { scope.launch { load() } },
            )
            else -> {
                val t = ticket!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            t.titulo,
                            style = MaterialTheme.typography.headlineSmall,
                            color = AppColors.TextPrimary,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                        )
                        StatusChip(ticketPriorityLabel(t.priority), AppColors.ticketPriorityColor(t.priority))
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${ticketCategoryLabel(t.category)} · ${t.department?.name ?: "Sin depto."} · #${t.id.take(8).uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextFaint,
                    )

                    Spacer(Modifier.height(16.dp))
                    InfoCard {
                        SectionLabel("Descripción")
                        Text(t.descripcion, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                    }

                    Spacer(Modifier.height(14.dp))
                    InfoCard {
                        SectionLabel("Estado")
                        if (updatingStatus) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AppColors.EmeraldPrimary)
                        } else {
                            SimpleDropdownField(
                                label = "Cambiar estado",
                                value = t.status,
                                options = statusOptions,
                                onSelect = { newStatus ->
                                    if (newStatus != t.status) {
                                        scope.launch {
                                            updatingStatus = true
                                            try {
                                                ticket = AppContainer.ticketsApi.updateStatus(t.id, newStatus)
                                            } catch (e: Exception) {
                                                error = e.message
                                            } finally {
                                                updatingStatus = false
                                            }
                                        }
                                    }
                                },
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Creado por ${t.creadoPor.name} · ${formatDateTime(t.creadoEn)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextFaint,
                        )
                        Text(
                            "Asignado a: ${t.asignadoA?.name ?: "Sin asignar"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextFaint,
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    InfoCard {
                        SectionLabel("Comentarios (${t.comments.size})")
                        t.comments.forEach { c ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(
                                    "${c.autor.name} · ${formatDateTime(c.creadoEn)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextFaint,
                                )
                                Text(c.texto, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newComment,
                                onValueChange = { newComment = it },
                                placeholder = { Text("Escribe un comentario…") },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium,
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                enabled = newComment.isNotBlank() && !sendingComment,
                                onClick = {
                                    val text = newComment
                                    scope.launch {
                                        sendingComment = true
                                        try {
                                            AppContainer.ticketsApi.addComment(t.id, text)
                                            newComment = ""
                                            ticket = AppContainer.ticketsApi.get(t.id)
                                        } catch (e: Exception) {
                                            error = e.message
                                        } finally {
                                            sendingComment = false
                                        }
                                    }
                                },
                            ) {
                                Icon(Icons.Filled.Send, contentDescription = "Enviar", tint = AppColors.EmeraldPrimary)
                            }
                        }
                    }

                    if (t.history.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        InfoCard {
                            SectionLabel("Historial (${t.history.size})")
                            t.history.forEach { h ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(h.detail ?: h.type, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                                    Text(
                                        "${h.autor?.name ?: "Sistema"} · ${formatDateTime(h.createdAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.TextFaint,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(16.dp))
            .padding(14.dp),
        content = content,
    )
}
