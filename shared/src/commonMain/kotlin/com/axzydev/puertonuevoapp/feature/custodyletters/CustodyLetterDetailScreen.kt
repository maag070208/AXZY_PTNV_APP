package com.axzydev.puertonuevoapp.feature.custodyletters

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import com.axzydev.puertonuevoapp.core.ui.AppTextField

/**
 * Vista de la carta responsiva en pantalla (no PDF): el PDF se genera
 * 100% client-side en el web con @react-pdf/renderer, sin endpoint en el
 * backend, así que aquí se ofrece esta vista de solo lectura en su lugar.
 */
@Composable
fun CustodyLetterDetailScreen(custodyLetterId: String) {
    val viewModel: CustodyLetterDetailViewModel = viewModel(key = "custody-letter-$custodyLetterId") { CustodyLetterDetailViewModel(custodyLetterId, AppContainer.custodyLettersApi) }
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val canDelete = (authState as? AuthState.LoggedIn)?.user?.role != "EMPLOYEE"

    LaunchedEffect(state.deleted) { if (state.deleted) navigator.pop() }

    if (state.loading) {
        LoadingState(modifier = Modifier.fillMaxSize())
        return
    }
    val c = state.custodyLetter
    if (c == null) {
        ErrorState(state.error ?: "Carta no encontrada", Modifier.fillMaxSize(), onRetry = viewModel::load)
        return
    }

    val item = c.items.firstOrNull()
    val isReturned = c.returnDate != null

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { navigator.push(Screen.CustodyLetterForm(c.id)) }, modifier = Modifier.weight(1f)) {
                IconLabel(Icons.Filled.Edit)
                Text("Editar")
            }
            if (canDelete) {
                OutlinedButton(
                    onClick = viewModel::requestDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Danger),
                    modifier = Modifier.weight(1f),
                ) {
                    IconLabel(Icons.Filled.DeleteOutline)
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
                OutlinedButton(onClick = viewModel::requestUndoReturn) {
                    IconLabel(Icons.Filled.Undo)
                    Text("Cancelar devolución")
                }
            }
        } else {
            Button(
                onClick = viewModel::openReturnModal,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconLabel(Icons.Filled.Undo)
                Text("Marcar devolución")
            }
        }

        Spacer(Modifier.height(14.dp))

        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(c.consecutive, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = AppColors.TextPrimary)
                Column(horizontalAlignment = Alignment.End) {
                    Text("Fecha: ${formatShortDate(c.date)}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                    Text("No. empleado: ${c.employeeNumber.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Recibí autorización de uso y/o acceso al siguiente Recurso de TIC (Tecnología de la Información y la Comunicación) propiedad de ${c.company ?: "Puerto Nuevo Hotel y Villas"}, siendo éste para uso exclusivo de las actividades laborales de la empresa, por lo que tomo responsabilidad sobre el mismo y me comprometo a no divulgarlo, no facilitar su acceso, no utilizarlo en perjuicio de la empresa, devolverlo cuando sea solicitado y prevenir su robo o daño.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
            Spacer(Modifier.height(14.dp))
            SectionLabel("Recurso TIC")
            InfoRow("Descripción", item?.description ?: "—")
            InfoRow("Marca", item?.brand ?: "—")
            InfoRow("Modelo", item?.model ?: "—")
            InfoRow("No. de serie", item?.serialNumber ?: "—")
            InfoRow("Nombre del equipo", item?.hostname ?: "—")
            InfoRow("Control de activos", item?.assetTag ?: "—")
            InfoRow("Área", item?.area ?: "—")

            val dev = item?.device
            val fc = dev?.type?.fieldConfig
            val hasSpecs = fc != null && listOfNotNull(
                fc.ip.takeIf { it.enabled },
                fc.macAddress.takeIf { it.enabled },
                fc.operatingSystem.takeIf { it.enabled },
                fc.ram.takeIf { it.enabled },
                fc.storage.takeIf { it.enabled },
            ).isNotEmpty()
            if (dev != null && hasSpecs && fc != null) {
                Spacer(Modifier.height(10.dp))
                SectionLabel("Especificaciones técnicas")
                if (fc.ip.enabled) InfoRow("IP", dev.ip ?: "N/A")
                if (fc.macAddress.enabled) InfoRow("MAC Address", dev.macAddress ?: "N/A")
                if (fc.operatingSystem.enabled) InfoRow("Sistema Operativo", dev.operatingSystem ?: "N/A")
                if (fc.ram.enabled) InfoRow("RAM", dev.ram ?: "N/A")
                if (fc.storage.enabled) InfoRow("Almacenamiento", dev.storage ?: "N/A")
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(c.custodian?.name ?: "—", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                    Text("Responsable", style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(c.supervisor?.name ?: "—", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                    Text("Encargado del área", style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (state.showDeleteConfirm) {
        AppModal(
            title = "Eliminar carta",
            icon = Icons.Filled.DeleteOutline,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissDelete,
            confirmLabel = "Eliminar",
            saving = state.actionSaving,
            onConfirm = viewModel::confirmDelete,
        ) {
            Text("¿Eliminar la carta ${c.consecutive} del historial? Esta acción no se puede deshacer.", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
        }
    }

    if (state.showReturnModal) {
        AppModal(
            title = "Marcar devolución",
            icon = Icons.Filled.Undo,
            onDismiss = viewModel::dismissReturnModal,
            confirmLabel = "Marcar devuelto",
            confirmEnabled = state.returnedBy.isNotBlank() && state.returnCondition.isNotBlank(),
            saving = state.actionSaving,
            onConfirm = viewModel::confirmReturn,
        ) {
            Column {
                AppTextField(
                    value = state.returnedBy,
                    onValueChange = viewModel::onReturnedByChange,
                    label = { Text("Nombre de quien resguarda") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(
                    value = state.returnCondition,
                    onValueChange = viewModel::onReturnConditionChange,
                    label = { Text("Condiciones en las que se devuelve") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (state.showUndoConfirm) {
        AppModal(
            title = "Cancelar devolución",
            icon = Icons.Filled.Undo,
            tone = AppModalTone.Warning,
            onDismiss = viewModel::dismissUndoReturn,
            confirmLabel = "Cancelar devolución",
            saving = state.actionSaving,
            onConfirm = viewModel::confirmUndoReturn,
        ) {
            Text("¿Cancelar la devolución? El dispositivo volverá a ASIGNADO.", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
        }
    }

    state.actionError?.let { message ->
        AppModal(
            title = "No se pudo completar la acción",
            icon = Icons.Filled.ErrorOutline,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissActionError,
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
private fun IconLabel(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
    Spacer(Modifier.width(6.dp))
}
