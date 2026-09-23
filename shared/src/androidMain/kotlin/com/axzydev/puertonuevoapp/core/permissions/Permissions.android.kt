package com.axzydev.puertonuevoapp.core.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Solicita un permiso de runtime de forma version-aware:
 *
 * - CAMERA: permiso normal de runtime (todas las versiones).
 * - LOCATION: se piden FINE + COARSE juntos. En Android 12+ (API 31) el usuario
 *   puede elegir "aproximada"; basta con que se conceda cualquiera de las dos.
 * - NOTIFICATIONS: en Android 13+ (API 33) es un permiso de runtime
 *   (`POST_NOTIFICATIONS`); en versiones anteriores no existe y se considera
 *   concedido.
 */
@Composable
actual fun rememberPermissionController(permission: AppPermission): PermissionController {
    val context = LocalContext.current

    val androidPermissions: List<String> = remember(permission) {
        when (permission) {
            AppPermission.CAMERA -> listOf(Manifest.permission.CAMERA)
            AppPermission.LOCATION -> listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
            AppPermission.NOTIFICATIONS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    emptyList()
                }
        }
    }

    fun isGrantedNow(): Boolean = when (permission) {
        AppPermission.CAMERA -> context.hasPermission(Manifest.permission.CAMERA)
        AppPermission.LOCATION ->
            context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        AppPermission.NOTIFICATIONS ->
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    }

    var granted by remember(permission) { mutableStateOf(isGrantedNow()) }
    var denied by remember(permission) { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val now = isGrantedNow()
        granted = now
        if (!now) denied = true
    }

    // Revalida al volver de los ajustes del sistema.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, permission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val now = isGrantedNow()
                granted = now
                if (now) denied = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return PermissionController(
        isSupported = true,
        granted = granted,
        denied = denied,
        request = {
            if (androidPermissions.isNotEmpty()) {
                launcher.launch(androidPermissions.toTypedArray())
            }
        },
        openSettings = { context.openAppSettings() },
    )
}

private fun Context.hasPermission(permission: String): Boolean =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}
