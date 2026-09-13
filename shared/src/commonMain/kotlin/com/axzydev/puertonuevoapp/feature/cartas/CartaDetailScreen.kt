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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.CartaDto
import com.axzydev.puertonuevoapp.core.network.CartaReturnDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import kotlinx.coroutines.launch

/**
 * Vista de la carta responsiva: réplica en pantalla (no PDF) del documento
 * legal que en el web genera CartaPreview.tsx / CartaPDF.tsx. El PDF en sí
 * se genera 100% client-side con @react-pdf/renderer y no tiene endpoint
 * en el backend, así que en móvil se ofrece esta vista de solo lectura en
 * vez de una descarga — el resto de las acciones (editar/eliminar/marcar
 * devolución) sí llaman a los mismos endpoints que el web.
 */
@Composable
fun CartaDetailScreen(cartaId: String) {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val authState by AppContainer.authRepository.state.collectAsState()
    val role = (authState as? AuthState.LoggedIn)?.user?.role
    val canDelete = role != "EMPLEADO"

    var carta by remember { mutableStateOf<CartaDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionSaving by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showReturnModal by remember { mutableStateOf(false) }
    var showUndoConfirm by remember { mutableStateOf(false) }
    var returnedBy by remember { mutableStateOf("") }
    var returnCondition by remember { mutableStateOf("") }

    suspend fun load() {
        loading = true
        error = null
        try {
            carta = AppContainer.cartasApi.get(cartaId)
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar la carta"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(cartaId) { load() }

    if (loading) {
        LoadingState(modifier = Modifier.fillMaxSize())
        return
    }
    val c = carta
    if (c == null) {
        ErrorState(error ?: "Carta no encontrada", Modifier.fillMaxSize(), onRetry = { scope.launch { load() } })
        return
    }

    val item = c.items.firstOrNull()
    val isReturned = c.returnDate != null

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { navigator.push(Screen.CartaForm(c.id)) }, modifier = Modifier.weight(1f)) {
                Icon2(Icons.Filled.Edit)
                Text("Editar")
            }
            if (canDelete) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Danger),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon2(Icons.Filled.DeleteOutline)
                    Text("Eliminar")
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (isReturned) {
            AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                SectionLabel("Devolución registrada")
                Text("Fecha: ${formatShortDate(c.returnDate)}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                Text("Resguardó: ${c.returnedBy ?: "—"}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                Text("Condiciones: ${c.returnCondition ?: "—"}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { showUndoConfirm = true }) {
                    Icon2(Icons.Filled.Undo)
                    Text("Cancelar devolución")
                }
            }
        } else {
            Button(
                onClick = { showReturnModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon2(Icons.Filled.Undo)
                Text("Marcar devolución")
            }
        }

        Spacer(Modifier.height(14.dp))

        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(c.consecutivo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = AppColors.TextPrimary)
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Text("Fecha: ${formatShortDate(c.fecha)}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                    Text("No. empleado: ${c.numeroEmpleado.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Recibí autorización de uso y/o acceso al siguiente Recurso de TIC (Tecnología de la Información y la Comunicación) propiedad de ${c.empresa ?: "Puerto Nuevo Hotel y Villas"}, siendo éste para uso exclusivo de las actividades laborales de la empresa, por lo que tomo responsabilidad sobre el mismo y me comprometo a no divulgarlo, no facilitar su acceso, no utilizarlo en perjuicio de la empresa, devolverlo cuando sea solicitado y prevenir su robo o daño.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
            Spacer(Modifier.height(14.dp))
            SectionLabel("Recurso TIC")
            InfoRow("Descripción", item?.descripcion ?: "—")
            InfoRow("Marca", item?.marca ?: "—")
            InfoRow("Modelo", item?.modelo ?: "—")
            InfoRow("No. de serie", item?.numeroSerie ?: "—")
            InfoRow("Nombre del equipo", item?.nombreEquipo ?: "—")
            InfoRow("Control de activos", item?.controlActivos ?: "—")
            InfoRow("Área", item?.area ?: "—")

            val dev = item?.device
            val fc = dev?.type?.fieldConfig
            val hasSpecs = fc != null && listOfNotNull(
                fc.ip.takeIf { it.enabled },
                fc.macAddress.takeIf { it.enabled },
                fc.sistemaOp.takeIf { it.enabled },
                fc.ram.takeIf { it.enabled },
                fc.almacenamiento.takeIf { it.enabled },
            ).isNotEmpty()
            if (dev != null && hasSpecs && fc != null) {
                Spacer(Modifier.height(10.dp))
                SectionLabel("Especificaciones técnicas")
                if (fc.ip.enabled) InfoRow("IP", dev.ip ?: "N/A")
                if (fc.macAddress.enabled) InfoRow("MAC Address", dev.macAddress ?: "N/A")
                if (fc.sistemaOp.enabled) InfoRow("Sistema Operativo", dev.sistemaOp ?: "N/A")
                if (fc.ram.enabled) InfoRow("RAM", dev.ram ?: "N/A")
                if (fc.almacenamiento.enabled) InfoRow("Almacenamiento", dev.almacenamiento ?: "N/A")
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(c.responsable?.name ?: "—", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                    Text("Responsable", style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(c.encargado?.name ?: "—", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                    Text("Encargado del área", style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showDeleteConfirm) {
        AppModal(
            title = "Eliminar carta",
            icon = Icons.Filled.DeleteOutline,
            tone = AppModalTone.Danger,
            onDismiss = { showDeleteConfirm = false },
            confirmLabel = "Eliminar",
            saving = actionSaving,
            onConfirm = {
                scope.launch {
                    actionSaving = true
                    try {
                        AppContainer.cartasApi.remove(c.id)
                        navigator.pop()
                    } catch (e: Exception) {
                        showDeleteConfirm = false
                        actionError = e.message ?: "No se pudo eliminar la carta"
                    } finally {
                        actionSaving = false
                    }
                }
            },
        ) {
            Text("¿Eliminar la carta ${c.consecutivo} del historial? Esta acción no se puede deshacer.", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
        }
    }

    if (showReturnModal) {
        AppModal(
            title = "Marcar devolución",
            icon = Icons.Filled.Undo,
            onDismiss = { showReturnModal = false },
            confirmLabel = "Marcar devuelto",
            confirmEnabled = returnedBy.isNotBlank() && returnCondition.isNotBlank(),
            saving = actionSaving,
            onConfirm = {
                scope.launch {
                    actionSaving = true
                    try {
                        carta = AppContainer.cartasApi.returnCarta(c.id, CartaReturnDto(returnedBy.trim(), returnCondition.trim()))
                        showReturnModal = false
                    } catch (e: Exception) {
                        actionError = e.message ?: "No se pudo registrar la devolución"
                    } finally {
                        actionSaving = false
                    }
                }
            },
        ) {
            Column {
                OutlinedTextField(
                    value = returnedBy,
                    onValueChange = { returnedBy = it },
                    label = { Text("Nombre de quien resguarda") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = returnCondition,
                    onValueChange = { returnCondition = it },
                    label = { Text("Condiciones en las que se devuelve") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showUndoConfirm) {
        AppModal(
            title = "Cancelar devolución",
            icon = Icons.Filled.Undo,
            tone = AppModalTone.Warning,
            onDismiss = { showUndoConfirm = false },
            confirmLabel = "Cancelar devolución",
            saving = actionSaving,
            onConfirm = {
                scope.launch {
                    actionSaving = true
                    try {
                        carta = AppContainer.cartasApi.undoReturn(c.id)
                        showUndoConfirm = false
                    } catch (e: Exception) {
                        actionError = e.message ?: "No se pudo cancelar la devolución"
                    } finally {
                        actionSaving = false
                    }
                }
            },
        ) {
            Text("¿Cancelar la devolución? El dispositivo volverá a ASIGNADO.", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
        }
    }

    actionError?.let { message ->
        AppModal(
            title = "No se pudo completar la acción",
            icon = Icons.Filled.ErrorOutline,
            tone = AppModalTone.Danger,
            onDismiss = { actionError = null },
        ) { Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted) }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint, modifier = Modifier.weight(0.45f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = AppColors.TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.55f))
    }
}

@Composable
private fun Icon2(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    androidx.compose.material3.Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
    Spacer(Modifier.width(6.dp))
}
