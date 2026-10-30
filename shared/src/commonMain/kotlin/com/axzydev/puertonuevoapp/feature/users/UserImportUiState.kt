package com.axzydev.puertonuevoapp.feature.users

import com.axzydev.puertonuevoapp.core.network.users.UserImportResultDto
import com.axzydev.puertonuevoapp.core.platform.PickedFile

/** Pasos del asistente: elegir el Excel, revisarlo, confirmar y ver el resultado. */
enum class UserImportStep {
    /** Sin archivo: solo se ve el paso de elegir. */
    Choose,

    /** Archivo elegido: se revisa (nombre, tamaño y requisitos) antes de subirlo. */
    Review,

    /** La API respondió: creados y filas omitidas. */
    Done,
}

/**
 * Estado del asistente de carga masiva de cuentas.
 *
 * El archivo se conserva en memoria hasta confirmar porque `POST /users/import`
 * lo recibe en el cuerpo multipart; la API no tiene previsualización ni
 * plantilla, así que revisar es verificar el archivo elegido y los requisitos
 * del formato antes de mandarlo.
 */
data class UserImportUiState(
    val step: UserImportStep = UserImportStep.Choose,
    val file: PickedFile? = null,
    val result: UserImportResultDto? = null,
    val uploading: Boolean = false,
    val error: String? = null,
) {
    /** Hay archivo y no hay otra subida en vuelo. */
    val canImport: Boolean get() = file != null && !uploading

    /** Filas omitidas del último resultado. */
    val skippedCount: Int get() = result?.skipped?.size ?: 0

    /** Cuentas creadas en el último resultado. */
    val createdCount: Int get() = result?.created ?: 0
}
