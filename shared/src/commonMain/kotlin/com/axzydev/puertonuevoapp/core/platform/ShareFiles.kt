package com.axzydev.puertonuevoapp.core.platform

/** MIME de los CSV que exporta el API (`Content-Type: text/csv; charset=utf-8`). */
const val CSV_MIME_TYPE: String = "text/csv"

/**
 * Comparte un archivo con la hoja de compartir del sistema: correo, WhatsApp,
 * Drive, "Guardar en Archivos", etc. Es la vía de la app para las
 * exportaciones CSV (que la web descarga con un `<a download>`).
 *
 * Los bytes ya están en memoria: p. ej. los de una exportación del API
 * (`ApiClient.postBytes`/`getBytes`) o un archivo elegido con
 * `rememberFilePicker`.
 *
 * Android: escribe en `cacheDir/shared/` y comparte por `FileProvider`
 * (`ACTION_SEND`); la autoridad está declarada en el `AndroidManifest.xml` de
 * `androidApp`. iOS: pendiente (Fase 4, Android-first) — stub no-op.
 */
expect fun shareFile(name: String, mime: String, bytes: ByteArray)

/** Comparte un archivo recién elegido con [rememberFilePicker]. */
fun shareFile(file: PickedFile) {
    shareFile(file.name, file.mime, file.bytes)
}

/**
 * Comparte un CSV generado en la app a partir de su texto (UTF-8, sin BOM).
 * Si [name] no trae extensión se le agrega `.csv`.
 *
 * Para compartir una exportación del API tal cual, usar
 * [shareFile] con los bytes crudos: así el archivo queda idéntico al que
 * descarga la web (no se re-codifica).
 */
fun shareCsv(name: String, content: String) {
    val fileName = if ('.' in name.substringAfterLast('/')) name else "$name.csv"
    shareFile(fileName, CSV_MIME_TYPE, content.encodeToByteArray())
}
