package com.axzydev.puertonuevoapp.core.platform

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.axzydev.puertonuevoapp.core.session.AndroidAppContext
import java.io.File

/**
 * Autoridad del `FileProvider` de la app. Debe coincidir con el `<provider>`
 * declarado en `androidApp/src/main/AndroidManifest.xml` y con el
 * `applicationId` (`com.axzydev.puertonuevoapp`).
 */
const val ANDROID_FILE_PROVIDER_AUTHORITY: String = "com.axzydev.puertonuevoapp.fileprovider"

/**
 * Escribe el archivo en `cacheDir/shared/` y lo comparte con `ACTION_SEND` a
 * través de un `FileProvider` (obligatorio desde Android 7: exponer un
 * `file://` lanza `FileUriExposedException`). El `Intent` se abre con el
 * contexto de aplicación, así que necesita `FLAG_ACTIVITY_NEW_TASK`.
 */
actual fun shareFile(name: String, mime: String, bytes: ByteArray) {
    val context = appContextOrNull() ?: return
    val file = File(sharedCacheDir(context), safeFileName(name))
    file.writeBytes(bytes)

    val uri = FileProvider.getUriForFile(context, ANDROID_FILE_PROVIDER_AUTHORITY, file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mime.ifBlank { "application/octet-stream" }
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, "Compartir archivo").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(chooser)
}

/**
 * `AndroidAppContext` se inicializa en `MainActivity`; si `shareFile` se
 * llamara antes (no debería: hay UI de por medio) se ignora en vez de
 * reventar con `UninitializedPropertyAccessException`.
 */
private fun appContextOrNull(): Context? =
    runCatching { AndroidAppContext.appContext }.getOrNull()

private fun sharedCacheDir(context: Context): File =
    File(context.cacheDir, "shared").apply { if (!exists()) mkdirs() }

/**
 * Nombre de archivo seguro dentro de `cacheDir/shared/`: sin rutas ni
 * caracteres de control (una barra escribiría fuera del directorio y un NUL
 * rompe el `File`).
 */
private fun safeFileName(name: String): String {
    val cleaned = name
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .replace("\u0000", "")
        .trim()
    return if (cleaned.isBlank() || cleaned == "." || cleaned == "..") "archivo" else cleaned
}
