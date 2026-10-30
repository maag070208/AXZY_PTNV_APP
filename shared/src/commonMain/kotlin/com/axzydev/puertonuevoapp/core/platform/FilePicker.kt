package com.axzydev.puertonuevoapp.core.platform

import androidx.compose.runtime.Composable
import com.axzydev.puertonuevoapp.core.network.http.MultipartPart

/**
 * MIME frecuentes para el selector y para validar/etiquetar archivos.
 *
 * Los endpoints de subida del API aceptan (según el módulo): imágenes, PDF,
 * video (adjuntos de ticket), `.xlsx` (cargas masivas) y CSV (exportaciones).
 */
object FileMimeTypes {
    /** Cualquier archivo (comodín `ANY`). */
    const val ANY: String = "*/*"

    /** Imágenes (cualquier subtipo de `image`): fotos de ticket, evidencia, foto de empleado. */
    const val IMAGE: String = "image/*"

    /** PDF (`application/pdf`): documentos de expediente, adjuntos. */
    const val PDF: String = "application/pdf"

    /** Excel moderno (`.xlsx`), el que sirven las plantillas de carga masiva. */
    const val EXCEL: String =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    /** CSV (`text/csv`) que devuelven las exportaciones del API. */
    const val CSV: String = "text/csv"
}

/**
 * Archivo elegido por el usuario con [rememberFilePicker].
 *
 * El contenido se trae completo en memoria ([bytes]) porque todos los
 * endpoints de subida del API reciben el archivo en el cuerpo multipart
 * (`multer.memoryStorage()`): el archivo es la petición. Para adjuntarlo se
 * convierte con [toMultipartPart]; para compartirlo/exportarlo se usa
 * `shareFile(name, mime, bytes)` (`core/platform/ShareFiles.kt`).
 */
data class PickedFile(
    /** Nombre del archivo tal como lo reporta el sistema (`acta.pdf`). */
    val name: String,
    /** MIME reportado por el sistema; el API valida los permitidos. */
    val mime: String,
    /** Contenido completo del archivo. */
    val bytes: ByteArray,
) {
    /** Tamaño en bytes (para mostrarlo con `formatFileSize` en la UI). */
    val sizeBytes: Long get() = bytes.size.toLong()
}

/**
 * Convierte el archivo elegido en una parte multipart para
 * `ApiClient.postMultipart`/`postMultipartNoContent`.
 *
 * [partName] es el nombre del campo del formulario: el API lee los archivos con
 * multer `upload.single("file")`, así que por defecto es `file`.
 */
fun PickedFile.toMultipartPart(partName: String = "file"): MultipartPart =
    MultipartPart(
        name = partName,
        fileName = name,
        mime = mime,
        bytes = bytes,
    )

/**
 * Selector de archivos del sistema, con estado de Compose.
 *
 * Devuelve la acción que abre el selector; al elegir un archivo se invoca
 * [onPicked] con nombre, MIME y bytes. Si el usuario cancela no se invoca
 * nada. La acción se puede invocar varias veces (es la misma para "Elegir" y
 * "Cambiar").
 *
 * @param mimeTypes filtro del selector; vacío = cualquier archivo (`FileMimeTypes.ANY`).
 *   Con un solo MIME se usa el selector de contenido del sistema; con varios,
 *   el de documentos (Android), que sí acepta una lista. Ejemplos:
 *   adjuntos de ticket (imagen, PDF o video),
 *   carga Excel `listOf(FileMimeTypes.EXCEL)`.
 * @param onPicked recibe el archivo elegido; se llama en el hilo principal.
 *
 * Android: `ActivityResultContracts.GetContent` (u `OpenDocument` con varios
 * MIME) + `ContentResolver`, leyendo los bytes fuera del hilo principal.
 *
 * iOS: pendiente (Fase 4, Android-first) — stub no-op documentado.
 */
@Composable
expect fun rememberFilePicker(
    mimeTypes: List<String> = emptyList(),
    onPicked: (PickedFile) -> Unit,
): () -> Unit
