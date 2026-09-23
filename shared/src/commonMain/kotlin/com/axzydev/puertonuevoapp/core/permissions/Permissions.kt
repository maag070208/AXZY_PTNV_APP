package com.axzydev.puertonuevoapp.core.permissions

import androidx.compose.runtime.Composable

/**
 * Permisos de runtime que la app necesita.
 *
 * - [CAMERA]: escaneo del QR de credenciales (portería).
 * - [LOCATION]: constancia del sitio al registrar entradas/salidas.
 * - [NOTIFICATIONS]: avisos del sistema. Solo se pide en Android 13+ (API 33);
 *   en versiones anteriores el permiso se concede en instalación.
 */
enum class AppPermission { CAMERA, LOCATION, NOTIFICATIONS }

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
