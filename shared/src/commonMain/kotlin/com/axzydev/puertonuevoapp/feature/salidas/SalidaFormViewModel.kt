package com.axzydev.puertonuevoapp.feature.salidas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.salidas.SalidaInput
import com.axzydev.puertonuevoapp.core.network.salidas.SalidasApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SalidaFormViewModel(
    private val salidaId: String?,
    private val salidasApi: SalidasApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalidaFormUiState(loading = salidaId != null))
    val uiState: StateFlow<SalidaFormUiState> = _uiState.asStateFlow()

    init {
        if (salidaId != null) load(salidaId)
    }

    private fun load(id: String) {
        viewModelScope.launch {
            val result = runCatching { salidasApi.get(id) }
            result.fold(
                onSuccess = { s ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            area = s.area,
                            descripcion = s.descripcion,
                            modelo = s.modelo ?: "",
                            marca = s.marca ?: "",
                            proyecto = s.proyecto ?: "",
                            cantidad = s.cantidad.toString(),
                            departamento = s.departamento,
                            usuario = s.usuario,
                            observaciones = s.observaciones ?: "",
                            motivo = s.motivo ?: "",
                        )
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(loading = false, error = networkMessage(e)) } },
            )
        }
    }

    fun onAreaChange(value: String) = _uiState.update { it.copy(area = value) }
    fun onDescripcionChange(value: String) = _uiState.update { it.copy(descripcion = value) }
    fun onModeloChange(value: String) = _uiState.update { it.copy(modelo = value) }
    fun onMarcaChange(value: String) = _uiState.update { it.copy(marca = value) }
    fun onProyectoChange(value: String) = _uiState.update { it.copy(proyecto = value) }
    fun onCantidadChange(value: String) = _uiState.update { it.copy(cantidad = value.filter { c -> c.isDigit() }) }
    fun onDepartamentoChange(value: String) = _uiState.update { it.copy(departamento = value) }
    fun onUsuarioChange(value: String) = _uiState.update { it.copy(usuario = value) }
    fun onObservacionesChange(value: String) = _uiState.update { it.copy(observaciones = value) }
    fun onMotivoChange(value: String) = _uiState.update { it.copy(motivo = value) }

    fun submit() {
        val state = _uiState.value
        if (!state.isValid || state.saving) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val input = SalidaInput(
                descripcion = state.descripcion.trim(),
                modelo = state.modelo.trim().ifBlank { null },
                marca = state.marca.trim().ifBlank { null },
                proyecto = state.proyecto.trim().ifBlank { null },
                cantidad = state.cantidad.toIntOrNull()?.takeIf { it > 0 } ?: 1,
                departamento = state.departamento.trim(),
                usuario = state.usuario.trim(),
                observaciones = state.observaciones.trim().ifBlank { null },
                area = state.area.trim().ifBlank { null },
                motivo = state.motivo.ifBlank { null },
            )
            val result = runCatching {
                if (salidaId == null) salidasApi.create(input) else salidasApi.update(salidaId, input)
            }
            _uiState.update { it.copy(saving = false, error = result.exceptionOrNull()?.let(::networkMessage), saved = result.isSuccess) }
        }
    }
}
