package com.axzydev.puertonuevoapp.feature.roles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.permissions.PermissionCatalogCreateInput
import com.axzydev.puertonuevoapp.core.network.permissions.PermissionCatalogDto
import com.axzydev.puertonuevoapp.core.network.permissions.PermissionCatalogUpdateInput
import com.axzydev.puertonuevoapp.core.network.permissions.PermissionsApi
import com.axzydev.puertonuevoapp.core.session.AuthRepository
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Administración de roles y permisos (`roles.manage`), espejo de
 * `web/src/features/roles`: la matriz rol → permiso → alcance con un borrador
 * local y su diff contra lo persistido, y el catálogo de permisos (alta,
 * edición y activación; no se eliminan, solo se desactivan).
 */
class RolesPermissionsViewModel(
    private val permissionsApi: PermissionsApi,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RolesPermissionsUiState())
    val uiState: StateFlow<RolesPermissionsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { permissionsApi.admin() }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { data ->
                        val baseline = data.baseline()
                        // Recargar (p. ej. tras editar el catálogo) no descarta los cambios
                        // pendientes de la matriz, salvo los de permisos ya inactivos.
                        val active = data.catalog.filter { it.active }.map { it.key }.toSet()
                        val pending = state.changes
                            .filter { it.permission in active && it.role in data.roles }
                            .associate { cellKey(it.role, it.permission) to it.scope }
                        state.copy(
                            loading = false,
                            data = data,
                            baseline = baseline,
                            draft = baseline + pending,
                            selectedRole = state.selectedRole?.takeIf { it in data.roles } ?: data.roles.firstOrNull(),
                        )
                    },
                    onFailure = { e -> state.copy(loading = false, error = networkMessage(e)) },
                )
            }
        }
    }

    fun onTabChange(tab: Int) = _uiState.update { it.copy(tab = tab) }

    // --- Matriz ---

    fun selectRole(role: String) = _uiState.update { it.copy(selectedRole = role) }

    fun setScope(role: String, permission: String, scope: String) =
        _uiState.update { it.copy(draft = it.draft + (cellKey(role, permission) to scope)) }

    fun discard() = _uiState.update { it.copy(draft = it.baseline) }

    fun saveMatrix() {
        val state = _uiState.value
        val changes = state.changes
        if (changes.isEmpty() || state.savingMatrix) return
        _uiState.update { it.copy(savingMatrix = true) }
        viewModelScope.launch {
            val result = runCatching { permissionsApi.saveMatrix(changes) }
            _uiState.update { it.copy(savingMatrix = false) }
            result.fold(
                onSuccess = {
                    AppSnackbar.show("Matriz guardada correctamente")
                    load()
                    // Si cambió el rol propio, la sesión adopta los permisos nuevos.
                    authRepository.refreshSession()
                },
                onFailure = { AppSnackbar.showError(networkMessage(it)) },
            )
        }
    }

    // --- Catálogo ---

    fun onCatalogQueryChange(value: String) = _uiState.update { it.copy(catalogQuery = value) }

    fun openCreate() = _uiState.update { it.copy(form = PermissionForm()) }

    fun openEdit(permission: PermissionCatalogDto) = _uiState.update { it.copy(form = PermissionForm.from(permission)) }

    fun dismissForm() = _uiState.update { if (it.savingCatalog) it else it.copy(form = null) }

    fun updateForm(transform: (PermissionForm) -> PermissionForm) =
        _uiState.update { state -> state.form?.let { state.copy(form = transform(it)) } ?: state }

    fun toggleFormScope(scope: String) = updateForm { form ->
        form.copy(scopes = if (scope in form.scopes) form.scopes - scope else form.scopes + scope)
    }

    fun submitForm() {
        val form = _uiState.value.form ?: return
        if (form.error != null || _uiState.value.savingCatalog) return
        _uiState.update { it.copy(savingCatalog = true) }
        viewModelScope.launch {
            val result = runCatching {
                val sortOrder = form.sortOrder.trim().toInt()
                if (form.editingKey == null) {
                    permissionsApi.createCatalog(
                        PermissionCatalogCreateInput(
                            key = form.key.trim(),
                            module = form.module.trim(),
                            name = form.name.trim(),
                            description = form.description.trim().ifBlank { null },
                            scopes = form.orderedScopes,
                            sensitive = form.sensitive,
                            sortOrder = sortOrder,
                        )
                    )
                } else {
                    permissionsApi.updateCatalog(
                        form.editingKey,
                        PermissionCatalogUpdateInput(
                            module = form.module.trim(),
                            name = form.name.trim(),
                            // "" limpia la descripción (un null no viajaría).
                            description = form.description.trim(),
                            scopes = form.orderedScopes,
                            sensitive = form.sensitive,
                            sortOrder = sortOrder,
                        ),
                    )
                }
            }
            _uiState.update { it.copy(savingCatalog = false) }
            result.fold(
                onSuccess = {
                    AppSnackbar.show(if (form.isEdit) "Permiso actualizado correctamente" else "Permiso creado correctamente")
                    _uiState.update { it.copy(form = null) }
                    load()
                },
                onFailure = { AppSnackbar.showError(networkMessage(it)) },
            )
        }
    }

    fun toggleActive(permission: PermissionCatalogDto) {
        if (_uiState.value.togglingKey != null) return
        _uiState.update { it.copy(togglingKey = permission.key) }
        viewModelScope.launch {
            val result = runCatching {
                permissionsApi.updateCatalog(permission.key, PermissionCatalogUpdateInput(active = !permission.active))
            }
            _uiState.update { it.copy(togglingKey = null) }
            result.fold(
                onSuccess = {
                    AppSnackbar.show(if (permission.active) "Permiso desactivado" else "Permiso activado")
                    load()
                    authRepository.refreshSession()
                },
                onFailure = { AppSnackbar.showError(networkMessage(it)) },
            )
        }
    }
}
