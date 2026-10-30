package com.axzydev.puertonuevoapp.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypesApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.inventory.InventoryImportApi
import com.axzydev.puertonuevoapp.core.platform.PickedFile
import com.axzydev.puertonuevoapp.core.platform.shareFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** MIME de un libro de Excel (.xlsx): plantilla descargable y archivo a subir. */
const val XlsxMime: String = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

/**
 * Asistente de carga masiva de dispositivos (Excel).
 *
 * Tres pasos sobre un mismo archivo: subirlo, revisar exactamente lo que va a
 * pasar y confirmar. La previsualización no escribe nada y la confirmación es
 * todo-o-nada (una sola transacción en la API), así que no puede quedar una
 * carga a medias descuadrando el inventario.
 *
 * La clave de idempotencia se deriva del contenido del archivo y de las
 * elecciones revisadas (tipos por fila): reenviar lo mismo no duplica unidades;
 * cambiar el archivo o un tipo es una carga distinta.
 */
class InventoryImportViewModel(
    private val importApi: InventoryImportApi,
    private val deviceTypesApi: DeviceTypesApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryImportUiState())
    val uiState: StateFlow<InventoryImportUiState> = _uiState.asStateFlow()

    init {
        loadTypes()
    }

    /** Catálogo de tipos activos para el selector de tipo por fila. */
    fun loadTypes() {
        viewModelScope.launch {
            val result = runCatching { deviceTypesApi.list(includeInactive = false) }
            _uiState.update { state ->
                state.copy(types = result.getOrDefault(state.types))
            }
        }
    }

    /** Paso 1: el usuario eligió un archivo → se previsualiza con el tipo del propio archivo. */
    fun onFilePicked(file: PickedFile) {
        _uiState.update {
            it.copy(
                file = file,
                preview = null,
                result = null,
                typeOverrides = emptyMap(),
                loadingPreview = true,
                error = null,
            )
        }
        runPreview(file, emptyMap())
    }

    /**
     * Cambia (o quita, con `typeId` vacío) el tipo de una fila y recalcula la
     * previsualización: el servidor resuelve el plan otra vez con la elección,
     * así lo revisado es lo que se carga.
     */
    fun setTypeOverride(row: Int, typeId: String) {
        val file = _uiState.value.file ?: return
        val next = if (typeId.isBlank()) _uiState.value.typeOverrides - row else _uiState.value.typeOverrides + (row to typeId)
        _uiState.update { it.copy(typeOverrides = next, loadingPreview = true, error = null) }
        runPreview(file, next)
    }

    /** Reintenta la previsualización del archivo actual (tras un fallo de red). */
    fun retryPreview() {
        val state = _uiState.value
        val file = state.file ?: return
        _uiState.update { it.copy(loadingPreview = true, error = null) }
        runPreview(file, state.typeOverrides)
    }

    private fun runPreview(file: PickedFile, overrides: Map<Int, String>) {
        viewModelScope.launch {
            val result = runCatching { importApi.preview(file, overrides) }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { preview ->
                        state.copy(preview = preview, step = ImportStep.Review, loadingPreview = false, error = null)
                    },
                    onFailure = { error ->
                        // Sin preview no hay nada que revisar: se vuelve a subir.
                        state.copy(preview = null, step = ImportStep.Upload, loadingPreview = false, error = networkMessage(error))
                    },
                )
            }
        }
    }

    /**
     * Paso 2: confirma la carga (todo o nada). `allowRepeat` solo lo manda el
     * botón "cargar de todos modos" tras avisar que el archivo ya se cargó.
     */
    fun confirm(allowRepeat: Boolean = false) {
        val state = _uiState.value
        val file = state.file ?: return
        if (state.hasErrors) return
        _uiState.update { it.copy(confirming = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                importApi.import(
                    file = file,
                    typeOverrides = state.typeOverrides,
                    allowRepeatFile = allowRepeat,
                    idempotencyKey = requestKeyOf(file, state.typeOverrides, allowRepeat),
                )
            }
            _uiState.update { current ->
                result.fold(
                    onSuccess = { result -> current.copy(result = result, step = ImportStep.Done, confirming = false, error = null) },
                    onFailure = { error -> current.copy(confirming = false, error = networkMessage(error)) },
                )
            }
        }
    }

    /** Descarga la plantilla .xlsx y la comparte (el móvil no la guarda en una carpeta fija). */
    fun downloadTemplate() {
        if (_uiState.value.downloadingTemplate) return
        _uiState.update { it.copy(downloadingTemplate = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { importApi.template() }
            result.getOrNull()?.let { bytes ->
                runCatching { shareFile("plantilla-dispositivos.xlsx", XlsxMime, bytes) }
            }
            _uiState.update {
                it.copy(
                    downloadingTemplate = false,
                    error = result.exceptionOrNull()?.let(::networkMessage),
                )
            }
        }
    }

    /** Vuelve al paso 1 con el selector limpio. */
    fun reset() = _uiState.update {
        it.copy(
            step = ImportStep.Upload,
            file = null,
            preview = null,
            result = null,
            typeOverrides = emptyMap(),
            loadingPreview = false,
            confirming = false,
            error = null,
        )
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    /**
     * Clave estable por archivo + elecciones revisadas. Un doble tap en
     * "Confirmar" con los mismos datos devuelve el MISMO resultado del API en
     * vez de crear otro movimiento; cambiar archivo o tipo genera otra clave.
     */
    private fun requestKeyOf(file: PickedFile, overrides: Map<Int, String>, allowRepeat: Boolean): String {
        val overridesKey = overrides.entries.sortedBy { it.key }.joinToString(",") { "${it.key}=${it.value}" }
        val seed = "device-import:${file.name}:${file.bytes.size}:${file.bytes.contentHashCode()}:$overridesKey:$allowRepeat"
        return "app-import-${seed.hashCode().toUInt().toString(16)}"
    }
}
