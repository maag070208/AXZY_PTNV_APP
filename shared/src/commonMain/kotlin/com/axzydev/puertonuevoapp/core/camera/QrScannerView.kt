package com.axzydev.puertonuevoapp.core.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Preview de cámara con lectura de QR. Emite el texto crudo de cada código
 * detectado vía [onQrScanned]; el consumidor decide qué hacer con él
 * (parsear, deduplicar, llamar al API). [isActive] permite pausar la cámara
 * cuando la pantalla no está en primer plano o ya hay un resultado.
 *
 * Android: CameraX + ML Kit (modelo empaquetado, sin Play Services en runtime).
 * iOS: stub (Fase 4) — no renderiza cámara.
 */
@Composable
expect fun QrScannerView(
    modifier: Modifier,
    isActive: Boolean,
    onQrScanned: (String) -> Unit,
)
