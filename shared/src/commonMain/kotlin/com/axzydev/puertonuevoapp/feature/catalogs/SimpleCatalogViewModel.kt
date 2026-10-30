package com.axzydev.puertonuevoapp.feature.catalogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de una pestaña de catálogo simple: lista (con inactivos), diálogo de
 * alta/edición y confirmación de baja. Las acciones de escritura solo se
 * ofrecen desde la UI cuando la sesión tiene `catalogs.manage`.
 */
data class SimpleCatalogUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<SimpleCatalogEntry> = emptyList(),
    val actionError: String? = null,
    val showForm: Boolean = false,
    val editing: SimpleCatalogEntry? = null,
    val name: String = "",
    val saving: Boolean = false,
    val formError: String? = null,
    val deleteTarget: SimpleCatalogEntry? = null,
    val deleting: Boolean = false,
) {
    val canSave: Boolean get() = name.trim().isNotEmpty() && !saving
}

/**
 * CRUD de un catálogo simple sobre [SimpleCatalogSource]. Se comparte entre los
 * cuatro catálogos con la misma forma; el ViewModel se instancia con la clave
 * de su pestaña para que cada uno conserve su estado.
 */
class SimpleCatalogViewModel(
    private val source: SimpleCatalogSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SimpleCatalogUiState())
    val uiState: StateFlow<SimpleCatalogUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Lista completa, incluidos los inactivos (para poder reactivarlos). */
    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { source.list(true) }
            _uiState.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let(::networkMessage),
                    items = result.getOrDefault(it.items),
                )
            }
        }
    }

    fun openCreate() =
        _uiState.update { it.copy(showForm = true, editing = null, name = "", formError = null) }

    fun openEdit(item: SimpleCatalogEntry) =
        _uiState.update { it.copy(showForm = true, editing = item, name = item.name, formError = null) }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, formError = null) }

    fun dismissForm() =
        _uiState.update { it.copy(showForm = false, editing = null, name = "", formError = null) }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        val name = state.name.trim()
        _uiState.update { it.copy(saving = true, formError = null) }
        viewModelScope.launch {
            val result = runCatching {
                val editing = state.editing
                if (editing == null) {
                    source.create(name)
                } else {
                    source.update(editing.id, name, null)
                }
            }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(saving = false, showForm = false, editing = null, name = "") }
                    AppSnackbar.show(if (state.editing == null) "${source.label}: agregado" else "${source.label}: guardado")
                    load()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(saving = false, formError = networkMessage(error)) }
                },
            )
        }
    }

    /** Activar/desactivar sin pasar por el formulario (misma acción que la web). */
    fun toggleActive(item: SimpleCatalogEntry) {
        if (_uiState.value.saving) return
        _uiState.update { it.copy(actionError = null) }
        viewModelScope.launch {
            val result = runCatching { source.update(item.id, null, !item.active) }
            result.fold(
                onSuccess = {
                    AppSnackbar.show(
                        if (item.active) "${item.name} desactivado" else "${item.name} activado",
                    )
                    load()
                },
                onFailure = { error -> _uiState.update { it.copy(actionError = networkMessage(error)) } },
            )
        }
    }

    fun requestDelete(item: SimpleCatalogEntry) =
        _uiState.update { it.copy(deleteTarget = item, actionError = null) }

    fun dismissDelete() = _uiState.update { it.copy(deleteTarget = null) }

    fun confirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        if (_uiState.value.deleting) return
        _uiState.update { it.copy(deleting = true, actionError = null) }
        viewModelScope.launch {
            val result = runCatching { source.remove(target.id) }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(deleting = false, deleteTarget = null) }
                    AppSnackbar.show("\"${target.name}\" eliminado")
                    load()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(deleting = false, actionError = networkMessage(error)) }
                },
            )
        }
    }

    fun dismissActionError() = _uiState.update { it.copy(actionError = null) }
}
