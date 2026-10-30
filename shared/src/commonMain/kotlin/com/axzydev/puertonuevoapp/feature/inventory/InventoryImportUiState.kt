package com.axzydev.puertonuevoapp.feature.inventory

import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypeDto
import com.axzydev.puertonuevoapp.core.network.inventory.DeviceImportPreviewDto
import com.axzydev.puertonuevoapp.core.network.inventory.DeviceImportResultDto
import com.axzydev.puertonuevoapp.core.platform.PickedFile

/** Paso del asistente de carga masiva: subir el archivo, revisar, confirmar. */
enum class ImportStep { Upload, Review, Done }

/**
 * Estado del asistente de carga de dispositivos por Excel.
 *
 * El archivo se conserva en memoria entre los dos pasos porque la confirmación
 * lo vuelve a mandar a la API: el servidor revalida el archivo real en vez de
 * confiar en lo que la pantalla revisó.
 */
data class InventoryImportUiState(
    val step: ImportStep = ImportStep.Upload,
    val file: PickedFile? = null,
    val preview: DeviceImportPreviewDto? = null,
    val result: DeviceImportResultDto? = null,
    /** Tipos de dispositivo activos para el selector "tipo por fila". */
    val types: List<DeviceTypeDto> = emptyList(),
    /** Tipo elegido a mano por número de fila de Excel. */
    val typeOverrides: Map<Int, String> = emptyMap(),
    val loadingPreview: Boolean = false,
    val confirming: Boolean = false,
    val downloadingTemplate: Boolean = false,
    val error: String? = null,
) {
    /** Filas que impiden confirmar: mientras haya errores, no se carga nada. */
    val hasErrors: Boolean get() = (preview?.summary?.invalid ?: 0) > 0

    /** ¿Se puede confirmar? Hay preview sin errores y no hay otra confirmación en vuelo. */
    val canConfirm: Boolean get() = preview != null && !hasErrors && !confirming

    /** El archivo ya se cargó antes: hay que confirmarlo explícitamente. */
    val repeatedImport get() = preview?.repeatedImport
}
