package com.axzydev.puertonuevoapp.core.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Stub de iOS (Fase 1, MVP Android-first): el escaneo de QR con cámara no
 * está disponible todavía. La Fase 4 implementa `AVCaptureSession` +
 * `AVCaptureMetadataOutput`.
 */
@Composable
actual fun QrScannerView(
    modifier: Modifier,
    isActive: Boolean,
    onQrScanned: (String) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "El escaneo de credenciales aún no está disponible en iOS.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp),
        )
    }
}
