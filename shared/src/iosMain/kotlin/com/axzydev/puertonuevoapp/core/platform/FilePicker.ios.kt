package com.axzydev.puertonuevoapp.core.platform

import androidx.compose.runtime.Composable

/**
 * Stub de iOS (Fase 1, MVP Android-first): el selector de archivos del sistema
 * todavía no está implementado, así que la acción devuelta no hace nada. La UI
 * debe tratar el caso como "no disponible", igual que cámara/ubicación (ver
 * `Permissions.ios.kt` y `QrScannerView.ios.kt`).
 *
 * TODO(Fase 4): presentar `UIDocumentPickerViewController` desde la
 * `UIViewController` de Compose (`LocalUIViewController` de Compose
 * Multiplatform), implementar `UIDocumentPickerDelegate`, leer el `NSData` del
 * `NSURL` con `NSFileCoordinator` y construir el [PickedFile] con su nombre y
 * MIME. Mismos [mimeTypes] (`UTType` en iOS) y mismo contrato que Android.
 */
@Composable
actual fun rememberFilePicker(
    mimeTypes: List<String>,
    onPicked: (PickedFile) -> Unit,
): () -> Unit = {}
