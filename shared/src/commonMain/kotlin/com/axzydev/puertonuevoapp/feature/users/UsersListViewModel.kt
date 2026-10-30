package com.axzydev.puertonuevoapp.feature.users

import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentDto
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentsApi
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.users.UserDto
import com.axzydev.puertonuevoapp.core.network.users.UsersApi
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import com.axzydev.puertonuevoapp.core.ui.table.PaginatedTableViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Lista de cuentas de usuario (`POST /users/query`, server-side), con la tira
 * de totales de la web (todas / activas / inactivas), el catálogo de
 * departamentos para el filtro y las acciones de la fila: baja con motivo,
 * reactivación y cambio de contraseña.
 *
 * La edición y el alta no viven aquí: navegan a `Screen.UserForm`, la pantalla
 * que ya existe y reutiliza `UsersApi.create/update`.
 */
class UsersListViewModel(
    private val usersApi: UsersApi,
    private val departmentsApi: DepartmentsApi,
) : PaginatedTableViewModel<UserDto>() {

    override val pageSize: Int = 10

    /** La caja de búsqueda escribe en `filters["name"]` (el API busca en los 4 campos del nombre). */
    override val searchKey: String? = "name"

    private val _screen = MutableStateFlow(UsersListUiState())
    val screen: StateFlow<UsersListUiState> = _screen.asStateFlow()

    init {
        loadDepartments()
        loadCounts()
    }

    override suspend fun fetch(page: Int, limit: Int, filters: Map<String, String>): TableResponse<UserDto> =
        usersApi.query(TableRequest(page = page, limit = limit, filters = filters))

    /** Departamentos activos para el filtro del modal. */
    private fun loadDepartments() {
        viewModelScope.launch {
            runCatching { departmentsApi.list() }
                .onSuccess { list ->
                    _screen.update { state ->
                        state.copy(departments = list.filter { it.active }.sortedBy { it.name })
                    }
                }
        }
    }

    /**
     * Totales reales (no de la página), como la tira de la web: tres consultas
     * de una fila con y sin el filtro `active`. Si falla, el mosaico se oculta
     * y la lista sigue funcionando.
     */
    fun loadCounts() {
        viewModelScope.launch {
            val all = runCatching { count(mapOf()) }.getOrNull()
            val active = runCatching { count(mapOf("active" to "true")) }.getOrNull()
            val inactive = runCatching { count(mapOf("active" to "false")) }.getOrNull()
            if (all != null && active != null && inactive != null) {
                _screen.update { it.copy(counts = UsersCounts(total = all, active = active, inactive = inactive)) }
            }
        }
    }

    private suspend fun count(filters: Map<String, String>): Int =
        usersApi.query(TableRequest(page = 1, limit = 1, filters = filters)).total

    /** Vuelve a pedir la página vigente y los totales después de una acción. */
    private fun reloadAfterChange() {
        refresh()
        loadCounts()
    }

    // ── Baja con motivo ──────────────────────────────────────────────────────

    fun requestDeactivate(user: UserDto) =
        _screen.update { it.copy(deactivateTarget = user, deactivateReason = "", error = null) }

    fun dismissDeactivate() =
        _screen.update { it.copy(deactivateTarget = null, deactivateReason = "", error = null) }

    fun onDeactivateReasonChange(value: String) =
        _screen.update { it.copy(deactivateReason = value, error = null) }

    fun confirmDeactivate() {
        val state = _screen.value
        val target = state.deactivateTarget ?: return
        if (!state.canConfirmDeactivate) return
        _screen.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            runCatching { usersApi.deactivate(target.id, state.deactivateReason.trim()) }
                .onSuccess {
                    _screen.update { it.copy(saving = false, deactivateTarget = null, deactivateReason = "", error = null) }
                    AppSnackbar.show("La cuenta @${target.username} quedó desactivada")
                    reloadAfterChange()
                }
                .onFailure { error ->
                    _screen.update { it.copy(saving = false, error = networkMessage(error)) }
                }
        }
    }

    // ── Reactivación ─────────────────────────────────────────────────────────

    fun requestReactivate(user: UserDto) =
        _screen.update { it.copy(reactivateTarget = user, error = null) }

    fun dismissReactivate() =
        _screen.update { it.copy(reactivateTarget = null, error = null) }

    fun confirmReactivate() {
        val state = _screen.value
        val target = state.reactivateTarget ?: return
        if (state.saving) return
        _screen.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            runCatching { usersApi.reactivate(target.id) }
                .onSuccess {
                    _screen.update { it.copy(saving = false, reactivateTarget = null, error = null) }
                    AppSnackbar.show("La cuenta @${target.username} quedó activa")
                    reloadAfterChange()
                }
                .onFailure { error ->
                    _screen.update { it.copy(saving = false, error = networkMessage(error)) }
                }
        }
    }

    // ── Cambio de contraseña ─────────────────────────────────────────────────

    fun requestPassword(user: UserDto) =
        _screen.update { it.copy(passwordTarget = user, newPassword = "", error = null) }

    fun dismissPassword() =
        _screen.update { it.copy(passwordTarget = null, newPassword = "", error = null) }

    fun onNewPasswordChange(value: String) =
        _screen.update { it.copy(newPassword = value, error = null) }

    fun confirmPassword() {
        val state = _screen.value
        val target = state.passwordTarget ?: return
        if (!state.canConfirmPassword) return
        _screen.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            runCatching { usersApi.changePassword(target.id, state.newPassword.trim()) }
                .onSuccess {
                    _screen.update { it.copy(saving = false, passwordTarget = null, newPassword = "", error = null) }
                    AppSnackbar.show("Contraseña de @${target.username} actualizada")
                }
                .onFailure { error ->
                    _screen.update { it.copy(saving = false, error = networkMessage(error)) }
                }
        }
    }
}
