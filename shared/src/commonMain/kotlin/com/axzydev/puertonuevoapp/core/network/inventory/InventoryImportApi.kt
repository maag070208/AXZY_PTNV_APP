package com.axzydev.puertonuevoapp.core.network.inventory

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.platform.PickedFile
import com.axzydev.puertonuevoapp.core.platform.toMultipartPart
import kotlinx.serialization.Serializable

/**
 * Carga masiva de dispositivos por Excel (`/inventory/devices/import`).
 *
 * Es un adaptador aparte de [InventoryApi] porque estos tres endpoints son
 * `multipart/form-data`, no JSON:
 * - `GET  /inventory/devices/import/template` → plantilla .xlsx con encabezados y catálogo de tipos.
 * - `POST /inventory/devices/import/preview` → qué haría la carga (no escribe nada).
 * - `POST /inventory/devices/import`        → la carga real, en UNA transacción (todo o nada).
 *
 * El archivo se manda en los dos pasos porque la API lo vuelve a leer y a
 * resolver en la confirmación en vez de confiar en lo que la pantalla revisó:
 * así lo revisado es exactamente lo que se ejecuta.
 */
class InventoryImportApi(private val client: ApiClient) {

    /** Plantilla .xlsx (bytes) para compartirla con `shareFile`. */
    suspend fun template(): ByteArray = client.getBytes("/inventory/devices/import/template")

    /**
     * Paso 1: previsualiza la carga. Los tipos elegidos a mano por fila viajan
     * como JSON `{ "<fila>": "<typeId>" }` en el campo `typeOverrides`.
     */
    suspend fun preview(
        file: PickedFile,
        typeOverrides: Map<Int, String> = emptyMap(),
    ): DeviceImportPreviewDto = client.postMultipart(
        path = "/inventory/devices/import/preview",
        parts = listOf(file.toMultipartPart()),
        fields = typeOverrideFields(typeOverrides),
    )

    /**
     * Paso 2: confirma la carga. `allowRepeatFile` solo se manda cuando el
     * usuario vio el aviso de archivo repetido y decidió cargarlo de todos
     * modos; la clave de idempotencia evita que un doble envío duplique unidades.
     */
    suspend fun import(
        file: PickedFile,
        typeOverrides: Map<Int, String> = emptyMap(),
        allowRepeatFile: Boolean = false,
        idempotencyKey: String? = null,
    ): DeviceImportResultDto {
        val fields = buildMap<String, String> {
            putAll(typeOverrideFields(typeOverrides))
            if (allowRepeatFile) put("allowRepeatFile", "true")
        }
        val headers = idempotencyKey
            ?.takeIf { it.isNotBlank() }
            ?.let { mapOf("Idempotency-Key" to it) }
            ?: emptyMap()
        return client.postMultipart(
            path = "/inventory/devices/import",
            parts = listOf(file.toMultipartPart()),
            fields = fields,
            headers = headers,
        )
    }
}

/**
 * Campo `typeOverrides`: JSON `{ "<fila Excel>": "<typeId>" }` con las filas en
 * orden. Se omite si no hay elecciones (la API usa el tipo del archivo).
 */
private fun typeOverrideFields(typeOverrides: Map<Int, String>): Map<String, String> {
    val cleaned = typeOverrides.filterValues { it.isNotBlank() }
    if (cleaned.isEmpty()) return emptyMap()
    val json = cleaned.entries
        .sortedBy { it.key }
        .joinToString(prefix = "{", postfix = "}") { (row, typeId) -> "\"$row\":\"${typeId.escapeJson()}\"" }
    return mapOf("typeOverrides" to json)
}

private fun String.escapeJson(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")

/** Previsualización: lo que la carga haría, sin escribir nada. */
@Serializable
data class DeviceImportPreviewDto(
    val rows: List<DeviceImportRowDto> = emptyList(),
    val summary: DeviceImportSummaryDto = DeviceImportSummaryDto(),
    /** Ya se cargó antes un archivo con exactamente este contenido (null = no). */
    val repeatedImport: RepeatedImportDto? = null,
)

/** Un renglón de Excel ya resuelto contra la base. */
@Serializable
data class DeviceImportRowDto(
    /** Número de fila en Excel (2 = primer renglón de datos). */
    val row: Int = 0,
    val typeName: String = "",
    val resolvedTypeName: String = "",
    val resolvedTypeCode: String = "",
    /** El tipo del archivo no está en el catálogo → se fue al genérico. */
    val typeUnknown: Boolean = false,
    /** La fila no trae tipo → se fue al genérico. */
    val typeMissing: Boolean = false,
    /** El usuario eligió el tipo de esta fila en la previsualización. */
    val typeOverridden: Boolean = false,
    /** Código del modelo: el del catálogo si ya existe; el del archivo si es alta; null = se genera. */
    val code: String? = null,
    /** Dispositivo existente al que se suman las unidades (null = alta). */
    val deviceId: String? = null,
    val typeId: String? = null,
    val name: String = "",
    val brand: String = "",
    val model: String = "",
    val quantity: Int = 0,
    /** `CREATE` (alta) o `ADD_UNITS` (suma unidades a un dispositivo existente). */
    val action: String = "CREATE",
    /** Filas del mismo archivo que caen en este mismo dispositivo. */
    val mergedRows: List<Int> = emptyList(),
    /** Unidades que ya tiene el dispositivo (solo en `ADD_UNITS`). */
    val currentUnits: Int = 0,
    /** Folios que se crearían (solo en altas/entradas). */
    val assetTagFrom: String? = null,
    val assetTagTo: String? = null,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
)

/** Totales de la previsualización. */
@Serializable
data class DeviceImportSummaryDto(
    val rows: Int = 0,
    val valid: Int = 0,
    val invalid: Int = 0,
    val units: Int = 0,
    val newDevices: Int = 0,
    val existingDevices: Int = 0,
    val genericRows: Int = 0,
    val unknownTypeRows: Int = 0,
    /** Tipos que la carga tendría que crear; hoy solo el genérico si falta. */
    val typesToCreate: List<String> = emptyList(),
)

/** Aviso de archivo ya cargado: la confirmación exige permiso explícito para repetirlo. */
@Serializable
data class RepeatedImportDto(
    val movementId: String = "",
    val importedAt: String = "",
    val fileName: String? = null,
)

/** Resultado de la carga (movimiento STOCK_IN que la respalda). */
@Serializable
data class DeviceImportResultDto(
    val movementId: String = "",
    val devicesCreated: Int = 0,
    val devicesReused: Int = 0,
    val unitsCreated: Int = 0,
    val rows: Int = 0,
    val fileName: String? = null,
    /** La petición ya se había registrado: no se duplicó nada. */
    val repeated: Boolean = false,
)
