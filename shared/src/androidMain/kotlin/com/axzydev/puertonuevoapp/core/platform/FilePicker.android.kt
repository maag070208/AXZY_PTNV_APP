package com.axzydev.puertonuevoapp.core.platform

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Selector de archivos en Android con Activity Result:
 *
 * - Sin filtro o con un solo MIME → `ActivityResultContracts.GetContent`
 *   (selector de contenido: fotos, WhatsApp, descargas…).
 * - Con varios MIME → `ActivityResultContracts.OpenDocument`, que sí acepta
 *   una lista de tipos (documentos del proveedor de archivos).
 *
 * Los bytes se leen con `ContentResolver.openInputStream` en
 * `Dispatchers.IO` (nunca en el hilo principal) y `onPicked` se invoca de
 * vuelta en el hilo principal, con el `PickedFile` completo en memoria.
 */
@Composable
actual fun rememberFilePicker(
    mimeTypes: List<String>,
    onPicked: (PickedFile) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnPicked by rememberUpdatedState(onPicked)

    fun handleUri(uri: Uri?) {
        if (uri == null) return // el usuario canceló
        scope.launch {
            val picked = withContext(Dispatchers.IO) {
                runCatching { readPickedFile(context, uri) }.getOrNull()
            }
            if (picked != null) currentOnPicked(picked)
        }
    }

    val single = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { handleUri(it) }
    val multiple = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { handleUri(it) }

    return remember(single, multiple, mimeTypes) {
        {
            if (mimeTypes.size > 1) {
                multiple.launch(mimeTypes.toTypedArray())
            } else {
                single.launch(mimeTypes.firstOrNull() ?: FileMimeTypes.ANY)
            }
        }
    }
}

/** Lee nombre, MIME y bytes de un `content://` uri (llamar fuera del hilo principal). */
private fun readPickedFile(context: Context, uri: Uri): PickedFile {
    val resolver = context.contentResolver

    val name = resolver
        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        ?.takeIf { it.isNotBlank() }
        ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
        ?: "archivo"

    val mime = resolver.getType(uri)?.takeIf { it.isNotBlank() } ?: "application/octet-stream"

    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
        ?: error("No se pudo leer el archivo elegido")

    return PickedFile(name = name, mime = mime, bytes = bytes)
}
