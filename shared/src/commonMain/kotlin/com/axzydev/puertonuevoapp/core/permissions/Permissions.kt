package com.axzydev.puertonuevoapp.core.permissions

import androidx.compose.runtime.Composable

/** Permisos de runtime que la app necesita para el escaneo de credenciales. */
enum class AppPermission { CAMERA, LOCATION }

/**
 * Estado de un permiso de runtime + acciones para solicitarlo o abrir los
 * ajustes del sistema. `isSupported = false` significa que la plataforma no
 * lo implementa todavía (iOS en esta fase).
 */
class PermissionController(
    val isSupported: Boolean,
    val granted: Boolean,
    val denied: Boolean,
    val request: () -> Unit,
    val openSettings: () -> Unit,
)

/** Observa/solicita un permiso de runtime con el launcher de Activity Result. */
@Composable
expect fun rememberPermissionController(permission: AppPermission): PermissionController
