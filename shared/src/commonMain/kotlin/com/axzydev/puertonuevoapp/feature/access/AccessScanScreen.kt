package com.axzydev.puertonuevoapp.feature.access

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.camera.QrScannerView
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.network.access.AccessEventDto
import com.axzydev.puertonuevoapp.core.network.access.AccessEventType
import com.axzydev.puertonuevoapp.core.network.access.AccessLookupResultDto
import com.axzydev.puertonuevoapp.core.permissions.AppPermission
import com.axzydev.puertonuevoapp.core.permissions.PermissionController
import com.axzydev.puertonuevoapp.core.permissions.rememberPermissionController
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.RemoteImage
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.accessEventTypeLabel
import com.axzydev.puertonuevoapp.core.util.accessLocationSourceLabel
import com.axzydev.puertonuevoapp.core.util.formatDateTime

/**
 * Pantalla de portería: elegir sitio → escanear QR → confirmar ENTRY/EXIT.
 * La cámara se pausa al resolver/confirmar para no leer códigos en bucle.
 */
@Composable
fun AccessScanScreen(
    viewModel: AccessScanViewModel = viewModel {
        AccessScanViewModel(AppContainer.accessApi, AppContainer.locationProvider)
    },
) {
    val state by viewModel.uiState.collectAsState()
    val cameraPermission = rememberPermissionController(AppPermission.CAMERA)
    val locationPermission = rememberPermissionController(AppPermission.LOCATION)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SiteSelector(
            state = state,
            onSelect = viewModel::selectSite,
            onRetry = viewModel::loadSites,
        )
        Spacer(Modifier.height(12.dp))

        if (locationPermission.isSupported && !locationPermission.granted) {
            LocationPermissionBanner(locationPermission)
            Spacer(Modifier.height(12.dp))
        }

        when (state.phase) {
            ScanPhase.REVIEW -> ReviewCard(state = state, viewModel = viewModel)
            ScanPhase.SUCCESS -> SuccessCard(state = state, onNext = viewModel::scanNext)
            else -> CameraArea(
                state = state,
                cameraPermission = cameraPermission,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun SiteSelector(
    state: AccessScanUiState,
    onSelect: (String) -> Unit,
    onRetry: () -> Unit,
) {
    SectionLabel("Portería")
    when {
        state.loadingSites -> Text(
            "Cargando porterías…",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )

        state.sitesError != null -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                state.sitesError,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Danger,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) { Text("Reintentar") }
        }

        !state.hasSites -> Text(
            "No hay porterías configuradas. Contacta al administrador.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )

        else -> SimpleDropdownField(
            label = "Sitio / portería",
            value = state.selectedSiteId ?: "",
            options = state.sites.map { it.id to it.name },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun CameraArea(
    state: AccessScanUiState,
    cameraPermission: PermissionController,
    viewModel: AccessScanViewModel,
) {
    when {
        !cameraPermission.isSupported -> NotAvailableCard()

        !cameraPermission.granted -> CameraPermissionCard(cameraPermission)

        else -> {
            if (!state.hasSiteSelected) {
                InfoBanner("Selecciona una portería para comenzar a escanear.")
                Spacer(Modifier.height(12.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black),
            ) {
                QrScannerView(
                    modifier = Modifier.matchParentSize(),
                    isActive = state.hasSiteSelected && state.phase == ScanPhase.SCANNING,
                    onQrScanned = viewModel::onQrScanned,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(210.dp)
                        .border(3.dp, Color.White, RoundedCornerShape(18.dp)),
                )
                if (state.phase == ScanPhase.LOOKING_UP) {
                    Box(
                        modifier = Modifier.matchParentSize().background(Color(0x99000000)),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(color = Color.White) }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "Apunta el código QR de la credencial al recuadro.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )

            state.scanError?.let { message ->
                Spacer(Modifier.height(12.dp))
                ErrorBanner(message = message, onDismiss = viewModel::dismissScanError)
            }
        }
    }
}

@Composable
private fun ReviewCard(
    state: AccessScanUiState,
    viewModel: AccessScanViewModel,
) {
    val employee = state.employee ?: return
    AppSurfaceCard(Modifier.fillMaxWidth()) {
        EmployeeHeader(employee)
        Spacer(Modifier.height(18.dp))
        SectionLabel("Registrar")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccessTypeChip(
                label = accessEventTypeLabel(AccessEventType.ENTRY),
                selected = state.selectedType == AccessEventType.ENTRY,
                onClick = { viewModel.setType(AccessEventType.ENTRY) },
            )
            AccessTypeChip(
                label = accessEventTypeLabel(AccessEventType.EXIT),
                selected = state.selectedType == AccessEventType.EXIT,
                onClick = { viewModel.setType(AccessEventType.EXIT) },
            )
        }

        employee.lastEvent?.let { last ->
            Spacer(Modifier.height(10.dp))
            Text(
                "Último registro: ${accessEventTypeLabel(last.type)} · ${formatDateTime(last.occurredAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
        }

        state.errorMessage?.let { message ->
            Spacer(Modifier.height(12.dp))
            ErrorBanner(message = message, onDismiss = viewModel::dismissError)
        }

        Spacer(Modifier.height(18.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = viewModel::cancelReview, enabled = !state.submitting) {
                Text("Cancelar")
            }
            Spacer(Modifier.width(10.dp))
            if (state.errorMessage != null) {
                OutlinedButton(onClick = viewModel::retryRegistration, enabled = !state.submitting) {
                    Text("Reintentar")
                }
                Spacer(Modifier.width(10.dp))
            }
            Button(
                onClick = viewModel::confirm,
                enabled = !state.submitting,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
            ) {
                if (state.submitting) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Confirmar")
                }
            }
        }
    }
}

@Composable
private fun EmployeeHeader(employee: AccessLookupResultDto) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RemoteImage(
            path = employee.fotoUrl,
            loadBytes = { AppContainer.accessApi.photoBytes(it) },
            modifier = Modifier.size(64.dp).clip(CircleShape),
            contentDescription = employee.name,
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                employee.name,
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.TextPrimary,
            )
            Text(
                employee.numeroEmpleado ?: "Sin número de empleado",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
            Text(
                listOfNotNull(employee.puesto, employee.department).joinToString(" · ").ifBlank { "—" },
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
        }
        StatusChip(
            label = if (employee.active) "Activo" else "Inactivo",
            color = if (employee.active) AppColors.Success else AppColors.Danger,
        )
    }
}

@Composable
private fun SuccessCard(state: AccessScanUiState, onNext: () -> Unit) {
    val event: AccessEventDto = state.result ?: return
    AppSurfaceCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AppColors.Success, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "Registro exitoso",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.TextPrimary,
                )
                Text(
                    "${accessEventTypeLabel(event.type)} · ${event.employeeNameSnapshot ?: "Empleado"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextMuted,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Hora del servidor: ${formatDateTime(event.occurredAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        Text(
            "Ubicación: ${accessLocationSourceLabel(state.locationSource)}",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
        ) { Text("Escanear siguiente") }
    }
}

@Composable
private fun AccessTypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AppColors.EmeraldPrimary,
            selectedLabelColor = Color.White,
        ),
    )
}

@Composable
private fun NotAvailableCard() {
    AppSurfaceCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = AppColors.TextMuted)
            Spacer(Modifier.width(12.dp))
            Text(
                "El escaneo de credenciales no está disponible en iOS todavía. Usa un dispositivo Android.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
        }
    }
}

@Composable
private fun CameraPermissionCard(permission: PermissionController) {
    AppSurfaceCard(Modifier.fillMaxWidth()) {
        Text(
            "Permiso de cámara requerido",
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (permission.denied) {
                "El permiso de cámara está bloqueado. Habilítalo en los ajustes del sistema para escanear credenciales."
            } else {
                "Necesitamos acceso a la cámara para leer el código QR de la credencial."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextMuted,
        )
        Spacer(Modifier.height(14.dp))
        if (permission.denied) {
            Button(onClick = permission.openSettings) { Text("Abrir ajustes") }
        } else {
            Button(
                onClick = permission.request,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
            ) { Text("Permitir cámara") }
        }
    }
}

@Composable
private fun LocationPermissionBanner(permission: PermissionController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Warning.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.LocationOff, contentDescription = null, tint = AppColors.Warning, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Sin permiso de ubicación: los registros se guardarán solo con el sitio.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = permission.request) { Text("Permitir") }
    }
}

@Composable
private fun InfoBanner(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Info.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Danger.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = AppColors.Danger, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.Danger,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDismiss) { Text("Cerrar") }
    }
}
