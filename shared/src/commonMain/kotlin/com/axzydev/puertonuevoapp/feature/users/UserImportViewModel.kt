package com.axzydev.puertonuevoapp.feature.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.users.UsersApi
import com.axzydev.puertonuevoapp.core.platform.PickedFile
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Asistente de carga masiva de cuentas (`POST /users/import`).
 *
 * La API procesa el Excel fila por fila: crea las cuentas válidas con rol
 * EMPLEADO y devuelve las omitidas (usuario repetido, datos faltantes o
 * contraseña de menos de 6 caracteres) sin detener la carga. Por eso el paso de
 * revisión es sobre el archivo elegido: el servidor no ofrece previsualización
 * ni plantilla.
 */
class UserImportViewModel(
    private val usersApi: UsersApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserImportUiState())
    val uiState: StateFlow<UserImportUiState> = _uiState.asStateFlow()

    /** El selector devolvió un archivo: se pasa a revisar antes de subirlo. */
    fun onFilePicked(file: PickedFile) {
        _uiState.update {
            it.copy(step = UserImportStep.Review, file = file, result = null, error = null)
        }
    }

    /** Vuelve a elegir archivo (descarta el anterior y el resultado). */
    fun pickAnother() {
        _uiState.update {
            it.copy(step = UserImportStep.Choose, file = null, result = null, uploading = false, error = null)
        }
    }

    /** Confirma la carga del archivo revisado. */
    fun confirm() {
        val state = _uiState.value
        val file = state.file ?: return
        if (!state.canImport) return
        _uiState.update { it.copy(uploading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { usersApi.importUsers(file) }
            result.fold(
                onSuccess = { created ->
                    _uiState.update {
                        it.copy(step = UserImportStep.Done, result = created, uploading = false, error = null)
                    }
                    val message = if (created.created == 1) "1 cuenta creada" else "${created.created} cuentas creadas"
                    AppSnackbar.show(message)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(uploading = false, error = networkMessage(error)) }
                },
            )
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }
}
