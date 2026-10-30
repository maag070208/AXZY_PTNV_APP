package com.axzydev.puertonuevoapp.feature.catalogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentDto
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentUpdateInput
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentsApi
import com.axzydev.puertonuevoapp.core.network.departments.SubareaDto
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de la pestaña Departamentos/Subáreas: lista (con inactivos), alta y
 * edición del departamento y la gestión de sus subáreas en un modal.
 */
data class DepartmentsCatalogUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val departments: List<DepartmentDto> = emptyList(),
    val actionError: String? = null,
    val showForm: Boolean = false,
    val editing: DepartmentDto? = null,
    val name: String = "",
    val formActive: Boolean = true,
    val saving: Boolean = false,
    val formError: String? = null,
    val deleteTarget: DepartmentDto? = null,
    val deleting: Boolean = false,
    /** Departamento cuyas subáreas se están administrando (modal abierto). */
    val subareasOf: DepartmentDto? = null,
    val subareaName: String = "",
    val subareaSaving: Boolean = false,
) {
    val canSaveForm: Boolean get() = name.trim().isNotEmpty() && !saving
    val canAddSubarea: Boolean get() = subareaName.trim().isNotEmpty() && !subareaSaving
}

/**
 * Departamentos y subáreas (`departments.manage` para escribir; la lectura es
 * de sesión). La app no tiene endpoint propio para renombrar una subárea, así
 * que desde aquí solo se agregan y se quitan (el API sí lo soporta).
 */
class DepartmentsCatalogViewModel(
    private val departmentsApi: DepartmentsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DepartmentsCatalogUiState())
    val uiState: StateFlow<DepartmentsCatalogUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { departmentsApi.list(includeInactive = true) }
            _uiState.update { state ->
                val fresh = result.getOrNull()
                state.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let(::networkMessage),
                    departments = fresh ?: state.departments,
                    // Mantiene fresco el modal de subáreas si estaba abierto.
                    subareasOf = state.subareasOf?.let { open -> fresh?.firstOrNull { it.id == open.id } },
                )
            }
        }
    }

    // ── Alta/edición del departamento ────────────────────────────────────────

    fun openCreate() =
        _uiState.update { it.copy(showForm = true, editing = null, name = "", formActive = true, formError = null) }

    fun openEdit(department: DepartmentDto) =
        _uiState.update {
            it.copy(showForm = true, editing = department, name = department.name, formActive = department.active, formError = null)
        }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, formError = null) }

    fun onFormActiveChange(active: Boolean) = _uiState.update { it.copy(formActive = active) }

    fun dismissForm() =
        _uiState.update { it.copy(showForm = false, editing = null, name = "", formError = null) }

    fun saveDepartment() {
        val state = _uiState.value
        if (!state.canSaveForm) return
        val name = state.name.trim()
        _uiState.update { it.copy(saving = true, formError = null) }
        viewModelScope.launch {
            val result = runCatching {
                val editing = state.editing
                if (editing == null) {
                    departmentsApi.create(name)
                } else {
                    departmentsApi.update(editing.id, DepartmentUpdateInput(name = name, active = state.formActive))
                }
            }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(saving = false, showForm = false, editing = null, name = "") }
                    AppSnackbar.show(if (state.editing == null) "Departamento creado" else "Departamento guardado")
                    load()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(saving = false, formError = networkMessage(error)) }
                },
            )
        }
    }

    // ── Baja del departamento ────────────────────────────────────────────────

    fun requestDelete(department: DepartmentDto) =
        _uiState.update { it.copy(deleteTarget = department, actionError = null) }

    fun dismissDelete() = _uiState.update { it.copy(deleteTarget = null) }

    fun confirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        if (_uiState.value.deleting) return
        _uiState.update { it.copy(deleting = true, actionError = null) }
        viewModelScope.launch {
            val result = runCatching { departmentsApi.remove(target.id) }
            result.fold(
                onSuccess = { response ->
                    _uiState.update { it.copy(deleting = false, deleteTarget = null) }
                    AppSnackbar.show(
                        if (response.soft) "${target.name} desactivado" else "${target.name} eliminado",
                    )
                    load()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(deleting = false, actionError = networkMessage(error)) }
                },
            )
        }
    }

    // ── Subáreas ─────────────────────────────────────────────────────────────

    fun openSubareas(department: DepartmentDto) =
        _uiState.update { it.copy(subareasOf = department, subareaName = "", actionError = null) }

    fun closeSubareas() = _uiState.update { it.copy(subareasOf = null, subareaName = "") }

    fun onSubareaNameChange(value: String) = _uiState.update { it.copy(subareaName = value) }

    fun addSubarea() {
        val state = _uiState.value
        val department = state.subareasOf ?: return
        if (!state.canAddSubarea) return
        val name = state.subareaName.trim()
        _uiState.update { it.copy(subareaSaving = true, actionError = null) }
        viewModelScope.launch {
            val result = runCatching { departmentsApi.addSubarea(department.id, name) }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(subareaSaving = false, subareaName = "") }
                    load()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(subareaSaving = false, actionError = networkMessage(error)) }
                },
            )
        }
    }

    fun removeSubarea(subarea: SubareaDto) {
        if (_uiState.value.subareaSaving) return
        _uiState.update { it.copy(subareaSaving = true, actionError = null) }
        viewModelScope.launch {
            val result = runCatching { departmentsApi.removeSubarea(subarea.id) }
            result.fold(
                onSuccess = {
                    AppSnackbar.show("Subárea ${subarea.name} eliminada")
                    _uiState.update { it.copy(subareaSaving = false) }
                    load()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(subareaSaving = false, actionError = networkMessage(error)) }
                },
            )
        }
    }

    fun dismissActionError() = _uiState.update { it.copy(actionError = null) }
}
