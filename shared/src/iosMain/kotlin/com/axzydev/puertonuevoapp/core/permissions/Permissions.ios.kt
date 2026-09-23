package com.axzydev.puertonuevoapp.core.permissions

import androidx.compose.runtime.Composable

/**
 * Stub de iOS (Fase 1, MVP Android-first). No se solicitan permisos de cámara
 * ni de ubicación: el escaneo de credenciales no está disponible en iOS hasta
 * la Fase 4. La UI muestra el mensaje de "no disponible".
 */
@Composable
actual fun rememberPermissionController(permission: AppPermission): PermissionController =
    PermissionController(
        isSupported = false,
        granted = false,
        denied = false,
        request = {},
        openSettings = {},
    )
