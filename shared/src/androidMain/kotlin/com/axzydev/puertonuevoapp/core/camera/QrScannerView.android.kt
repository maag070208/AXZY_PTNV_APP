package com.axzydev.puertonuevoapp.core.camera

import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

private const val SAME_CODE_THROTTLE_MS = 2_500L

/**
 * Cámara + lectura de QR con CameraX y ML Kit (variante *bundled*: el modelo
 * viaja en el APK, sin depender de Play Services en runtime). El preview se
 * monta con [AndroidView] sobre un `PreviewView`.
 */
@Composable
actual fun QrScannerView(
    modifier: Modifier,
    isActive: Boolean,
    onQrScanned: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build(),
        )
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val lastEmitted = remember { AtomicReference<Pair<String, Long>?>(null) }
    val onScannedRef = remember { AtomicReference<(String) -> Unit> { } }
    SideEffect { onScannedRef.set(onQrScanned) }

    DisposableEffect(lifecycleOwner, isActive) {
        if (!isActive) return@DisposableEffect onDispose {}
        val future = ProcessCameraProvider.getInstance(context)
        var analysis: ImageAnalysis? = null
        val listener = Runnable {
            val provider = runCatching { future.get() }.getOrNull() ?: return@Runnable
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                processFrame(imageProxy, scanner, lastEmitted) { raw -> onScannedRef.get()(raw) }
            }
            analysis = imageAnalysis
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
            }
        }
        future.addListener(listener, mainExecutor())

        onDispose {
            runCatching { analysis?.clearAnalyzer() }
            runCatching { future.get().unbindAll() }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { scanner.close() }
            executor.shutdown()
        }
    }

    AndroidView(modifier = modifier, factory = { previewView })
}

@OptIn(ExperimentalGetImage::class)
private fun processFrame(
    imageProxy: ImageProxy,
    scanner: BarcodeScanner,
    lastEmitted: AtomicReference<Pair<String, Long>?>,
    onScanned: (String) -> Unit,
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(input)
        .addOnSuccessListener { barcodes ->
            val raw = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
            if (raw != null) {
                val now = System.currentTimeMillis()
                val last = lastEmitted.get()
                if (last == null || last.first != raw || now - last.second > SAME_CODE_THROTTLE_MS) {
                    lastEmitted.set(raw to now)
                    onScanned(raw)
                }
            }
        }
        .addOnCompleteListener { imageProxy.close() }
}

private fun mainExecutor(): Executor {
    val handler = Handler(Looper.getMainLooper())
    return Executor { command -> handler.post(command) }
}
