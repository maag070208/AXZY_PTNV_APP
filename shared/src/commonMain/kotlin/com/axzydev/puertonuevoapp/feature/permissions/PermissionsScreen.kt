package com.axzydev.puertonuevoapp.feature.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.permissions.AppPermission
import com.axzydev.puertonuevoapp.core.permissions.PermissionController
import com.axzydev.puertonuevoapp.core.permissions.rememberPermissionController
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.BrandLogoBadge
import com.axzydev.puertonuevoapp.core.ui.OceanBackdrop
import com.axzydev.puertonuevoapp.core.ui.StatusChip

/**
 * Pantalla post-login: lista los permisos que la app necesita y permite
 * solicitarlos en el mismo lugar. La solicitud es version-aware (ver
 * `rememberPermissionController`): en Android 13+ pide POST_NOTIFICATIONS,
 * en Android 12+ la ubicación puede ser aproximada, etc.
 */
@Composable
fun PermissionsScreen(onContinue: () -> Unit) {
    val camera = rememberPermissionController(AppPermission.CAMERA)
    val location = rememberPermissionController(AppPermission.LOCATION)
    val notifications = rememberPermissionController(AppPermission.NOTIFICATIONS)
    var showSkipConfirm by remember { mutableStateOf(false) }

    // Cámara y ubicación son las necesarias para el flujo operativo; las
    // notificaciones son opcionales. Se puede continuar sin ellas, pero se
    // avisa.
    val requiredGranted = camera.granted && location.granted

    val items = listOf(
        PermissionItem(
            title = "Cámara",
            description = "Para escanear el código QR de las credenciales en portería.",
            icon = Icons.Filled.CameraAlt,
            controller = camera,
        ),
        PermissionItem(
            title = "Ubicación",
            description = "Para dejar constancia del sitio al registrar entradas y salidas.",
            icon = Icons.Filled.LocationOn,
            controller = location,
        ),
        PermissionItem(
            title = "Notificaciones",
            description = "Para recibir avisos de tickets y tareas asignadas.",
            icon = Icons.Filled.Notifications,
            controller = notifications,
        ),
    )

    OceanBackdrop(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandLogoBadge(size = 72.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Permisos de la aplicación",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                "Para funcionar correctamente, Puerto Nuevo necesita los siguientes accesos.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
            )

            AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, item ->
                    PermissionRow(item)
                    if (index < items.lastIndex) Spacer(Modifier.height(14.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { if (requiredGranted) onContinue() else showSkipConfirm = true },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(
                    if (requiredGranted) "Continuar" else "Continuar sin permisos",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }

    if (showSkipConfirm) {
        AppModal(
            title = "Permisos pendientes",
            onDismiss = { showSkipConfirm = false },
            confirmLabel = "Continuar de todos modos",
            onConfirm = {
                showSkipConfirm = false
                onContinue()
            },
        ) {
            Text(
                "Aún no concedes la cámara y/o la ubicación. Algunas funciones " +
                    "(escaneo de credenciales, registro de sitio) no estarán " +
                    "disponibles hasta que las concedas en los ajustes.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
        }
    }
}

private data class PermissionItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val controller: PermissionController,
)

@Composable
private fun PermissionRow(item: PermissionItem) {
    val controller = item.controller
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp).padding(top = 2.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                item.description,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.height(6.dp))
            when {
                !controller.isSupported -> StatusChip("No disponible", AppColors.TextFaint)
                controller.granted -> StatusChip("Concedido", AppColors.Success)
                controller.denied -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatusChip("Denegado", AppColors.Danger)
                        SmallAction("Abrir ajustes", controller.openSettings)
                    }
                }
                else -> SmallAction("Permitir", controller.request)
            }
        }
    }
}

@Composable
private fun SmallAction(label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = Modifier.height(28.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}
