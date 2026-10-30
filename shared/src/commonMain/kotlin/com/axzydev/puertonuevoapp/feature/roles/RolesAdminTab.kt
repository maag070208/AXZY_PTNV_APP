package com.axzydev.puertonuevoapp.feature.roles

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.permissions.AccessMemberDto
import com.axzydev.puertonuevoapp.core.network.permissions.MatrixCellDto
import com.axzydev.puertonuevoapp.core.network.permissions.PermissionCatalogDto
import com.axzydev.puertonuevoapp.core.network.permissions.PermissionsApi
import com.axzydev.puertonuevoapp.core.network.permissions.PolicyDto
import com.axzydev.puertonuevoapp.core.network.permissions.RoleAdminDto
import com.axzydev.puertonuevoapp.core.network.permissions.RoleCreateInput
import com.axzydev.puertonuevoapp.core.network.permissions.RoleUpdateInput
import com.axzydev.puertonuevoapp.core.session.PermissionScopes
import com.axzydev.puertonuevoapp.core.session.permissionScopeLabel
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Formulario de alta/edición de un rol (duplicar = alta con `copyFrom`). */
data class RoleFormState(
    val editingKey: String? = null,
    val copyFrom: String? = null,
    val key: String = "",
    val name: String = "",
    val description: String = "",
    val module: String = "",
    val staff: Boolean = false,
    val sortOrder: String = "0",
) {
    val isEdit: Boolean get() = editingKey != null
    val isDuplicate: Boolean get() = copyFrom != null

    val error: String?
        get() = when {
            !isEdit && !ROLE_KEY_REGEX.matches(key.trim()) ->
                "La clave del rol usa solo MAYÚSCULAS, dígitos y guion bajo (2 a 40)"
            name.isBlank() -> "El nombre es obligatorio"
            sortOrder.trim().toIntOrNull()?.takeIf { it >= 0 } == null ->
                "El orden debe ser un entero mayor o igual a 0"
            else -> null
        }

    companion object {
        val ROLE_KEY_REGEX = Regex("^[A-Z][A-Z0-9_]{1,39}$")

        fun from(role: RoleAdminDto) = RoleFormState(
            editingKey = role.key,
            key = role.key,
            name = role.name,
            description = role.description.orEmpty(),
            module = role.module.orEmpty(),
            staff = role.staff,
            sortOrder = role.sortOrder.toString(),
        )

        fun duplicateFrom(role: RoleAdminDto) = RoleFormState(
            copyFrom = role.key,
            key = "",
            name = "${role.name} (copia)",
            description = role.description.orEmpty(),
            module = role.module.orEmpty(),
            staff = role.staff,
            sortOrder = role.sortOrder.toString(),
        )
    }
}

data class RolesAdminUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val roles: List<RoleAdminDto> = emptyList(),
    val catalog: List<PermissionCatalogDto> = emptyList(),
    val matrix: List<MatrixCellDto> = emptyList(),
    val members: List<AccessMemberDto> = emptyList(),
    val policies: List<PolicyDto> = emptyList(),
    val selectedKey: String? = null,
    val query: String = "",
    val form: RoleFormState? = null,
    val saving: Boolean = false,
    val actionError: String? = null,
    val deleteTarget: RoleAdminDto? = null,
    val deleting: Boolean = false,
    val memberPickerOpen: Boolean = false,
    val memberQuery: String = "",
) {
    val selected: RoleAdminDto? get() = roles.firstOrNull { it.key == selectedKey }

    val filteredRoles: List<RoleAdminDto>
        get() {
            val needle = query.trim()
            if (needle.isEmpty()) return roles
            return roles.filter {
                it.key.contains(needle, ignoreCase = true) ||
                    it.name.contains(needle, ignoreCase = true) ||
                    it.module.orEmpty().contains(needle, ignoreCase = true)
            }
        }

    /** Roles agrupados por módulo, en el orden del API (sin módulo → "General"). */
    val rolesByModule: List<Pair<String, List<RoleAdminDto>>>
        get() = filteredRoles.groupBy { it.module?.takeIf { m -> m.isNotBlank() } ?: "General" }.toList()

    fun scopeOf(permission: String): String =
        matrix.firstOrNull { it.role == selectedKey && it.permission == permission }?.scope ?: PermissionScopes.NONE

    /** Permisos del catálogo agrupados por módulo (mismo orden del API). */
    val modules: List<Pair<String, List<PermissionCatalogDto>>> get() = catalog.groupBy { it.module }.toList()

    val grantedPermissions: List<Pair<PermissionCatalogDto, String>>
        get() = catalog.mapNotNull { permission ->
            val scope = scopeOf(permission.key)
            if (scope == PermissionScopes.NONE) null else permission to scope
        }

    /** Cuántos permisos concedidos hay con cada alcance. */
    val scopeCounts: Map<String, Int>
        get() = grantedPermissions.groupingBy { it.second }.eachCount()

    /** Personas que tienen el rol seleccionado (principal o adicional). */
    val roleMembers: List<AccessMemberDto>
        get() = selectedKey?.let { key -> members.filter { it.role == key || key in it.extraRoles } }.orEmpty()

    /** Políticas que aplican al rol (las vacías aplican a todos). */
    val rolePolicies: List<PolicyDto>
        get() = selectedKey?.let { key -> policies.filter { it.roles.isEmpty() || key in it.roles } }.orEmpty()

    /** Personas a las que se les puede agregar el rol (sin él y no inactivas primero). */
    val memberCandidates: List<AccessMemberDto>
        get() {
            val key = selectedKey ?: return emptyList()
            val needle = memberQuery.trim()
            return members
                .filter { it.role != key && key !in it.extraRoles }
                .filter {
                    needle.isEmpty() ||
                        it.name.contains(needle, ignoreCase = true) ||
                        it.username.contains(needle, ignoreCase = true)
                }
                .sortedByDescending { it.active }
        }

    fun grantedCountFor(roleKey: String): Int =
        matrix.count { it.role == roleKey && it.scope != PermissionScopes.NONE }
}

/** Roles de la consola: lista por módulo, detalle (niveles, personas, reglas) y CRUD. */
class RolesAdminViewModel(
    private val permissionsApi: PermissionsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RolesAdminUiState())
    val uiState: StateFlow<RolesAdminUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                coroutineScope {
                    val roles = async { permissionsApi.roles() }
                    val admin = async { permissionsApi.admin() }
                    val members = async { permissionsApi.members() }
                    val policies = async { permissionsApi.policies() }
                    Quad(roles.await(), admin.await(), members.await(), policies.await())
                }
            }
            result.fold(
                onSuccess = { data ->
                    _uiState.update { state ->
                        val selected = state.selectedKey?.takeIf { key -> data.roles.any { it.key == key } }
                            ?: data.roles.firstOrNull()?.key
                        state.copy(
                            loading = false,
                            error = null,
                            roles = data.roles,
                            catalog = data.admin.catalog,
                            matrix = data.admin.matrix,
                            members = data.members,
                            policies = data.policies.policies,
                            selectedKey = selected,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(loading = false, error = networkMessage(error)) }
                },
            )
        }
    }

    fun selectRole(key: String) = _uiState.update { it.copy(selectedKey = key, actionError = null) }

    fun showRoleList() = _uiState.update { it.copy(selectedKey = null, actionError = null) }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }

    fun openCreate() = _uiState.update { it.copy(form = RoleFormState(), actionError = null) }

    fun openEdit(role: RoleAdminDto) = _uiState.update { it.copy(form = RoleFormState.from(role), actionError = null) }

    fun openDuplicate(role: RoleAdminDto) =
        _uiState.update { it.copy(form = RoleFormState.duplicateFrom(role), actionError = null) }

    fun dismissForm() = _uiState.update { if (it.saving) it else it.copy(form = null) }

    fun updateForm(transform: (RoleFormState) -> RoleFormState) =
        _uiState.update { state -> state.form?.let { state.copy(form = transform(it)) } ?: state }

    fun submitForm() {
        val form = _uiState.value.form ?: return
        if (form.error != null || _uiState.value.saving) return
        _uiState.update { it.copy(saving = true, actionError = null) }
        viewModelScope.launch {
            val sortOrder = form.sortOrder.trim().toIntOrNull() ?: 0
            val result = runCatching {
                if (form.isEdit) {
                    permissionsApi.updateRole(
                        form.editingKey!!,
                        RoleUpdateInput(
                            name = form.name.trim(),
                            description = form.description.trim(),
                            module = form.module.trim(),
                            staff = form.staff,
                            sortOrder = sortOrder,
                        ),
                    )
                } else {
                    permissionsApi.createRole(
                        RoleCreateInput(
                            key = form.key.trim(),
                            name = form.name.trim(),
                            description = form.description.trim().ifBlank { null },
                            module = form.module.trim().ifBlank { null },
                            staff = form.staff,
                            sortOrder = sortOrder,
                            copyFrom = form.copyFrom,
                        ),
                    )
                }
            }
            result.fold(
                onSuccess = { role ->
                    _uiState.update { it.copy(saving = false, form = null, selectedKey = role.key) }
                    AppSnackbar.show(
                        when {
                            form.isEdit -> "Rol actualizado"
                            form.isDuplicate -> "Rol duplicado con su matriz"
                            else -> "Rol creado"
                        },
                    )
                    load()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(saving = false, actionError = networkMessage(error)) }
                },
            )
        }
    }

    fun toggleActive(role: RoleAdminDto) {
        if (_uiState.value.saving) return
        _uiState.update { it.copy(actionError = null) }
        viewModelScope.launch {
            val result = runCatching {
                permissionsApi.updateRole(role.key, RoleUpdateInput(active = !role.active))
            }
            result.fold(
                onSuccess = {
                    AppSnackbar.show(if (role.active) "Rol desactivado" else "Rol activado")
                    load()
                },
                onFailure = { error -> _uiState.update { it.copy(actionError = networkMessage(error)) } },
            )
        }
    }

    fun requestDelete(role: RoleAdminDto) =
        _uiState.update { it.copy(deleteTarget = role, actionError = null) }

    fun dismissDelete() = _uiState.update { it.copy(deleteTarget = null) }

    fun confirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        if (_uiState.value.deleting) return
        _uiState.update { it.copy(deleting = true, actionError = null) }
        viewModelScope.launch {
            val result = runCatching { permissionsApi.deleteRole(target.key) }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(deleting = false, deleteTarget = null, selectedKey = null) }
                    AppSnackbar.show("Rol eliminado")
                    load()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(deleting = false, actionError = networkMessage(error)) }
                },
            )
        }
    }

    fun openMemberPicker() = _uiState.update { it.copy(memberPickerOpen = true, memberQuery = "") }

    fun closeMemberPicker() = _uiState.update { it.copy(memberPickerOpen = false, memberQuery = "") }

    fun onMemberQueryChange(value: String) = _uiState.update { it.copy(memberQuery = value) }

    fun addMember(member: AccessMemberDto) {
        val roleKey = _uiState.value.selectedKey ?: return
        _uiState.update { it.copy(actionError = null) }
        viewModelScope.launch {
            val result = runCatching { permissionsApi.addRoleMember(roleKey, member.id) }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(memberPickerOpen = false, memberQuery = "") }
                    AppSnackbar.show("Rol adicional asignado a ${member.name}")
                    load()
                },
                onFailure = { error -> _uiState.update { it.copy(actionError = networkMessage(error)) } },
            )
        }
    }

    fun removeMember(member: AccessMemberDto) {
        val roleKey = _uiState.value.selectedKey ?: return
        _uiState.update { it.copy(actionError = null) }
        viewModelScope.launch {
            val result = runCatching { permissionsApi.removeRoleMember(roleKey, member.id) }
            result.fold(
                onSuccess = {
                    AppSnackbar.show("Rol adicional retirado a ${member.name}")
                    load()
                },
                onFailure = { error -> _uiState.update { it.copy(actionError = networkMessage(error)) } },
            )
        }
    }

    private data class Quad(
        val roles: List<RoleAdminDto>,
        val admin: com.axzydev.puertonuevoapp.core.network.permissions.RolesAdminDto,
        val members: List<AccessMemberDto>,
        val policies: com.axzydev.puertonuevoapp.core.network.permissions.PoliciesResponseDto,
    )
}

/**
 * Pestaña Roles: lista por módulo con personas y permisos, y detalle con nivel
 * por módulo, alcance en línea, personas del rol (adicionales) y reglas.
 */
@Composable
internal fun RolesAdminTab(modifier: Modifier = Modifier) {
    val viewModel: RolesAdminViewModel = viewModel { RolesAdminViewModel(AppContainer.permissionsApi) }
    val state by viewModel.uiState.collectAsState()

    when {
        state.loading -> BentoState(
            message = "Cargando roles y personas…",
            loading = true,
            modifier = modifier.padding(16.dp),
        )
        state.error != null -> Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            BentoErrorState(message = state.error ?: "No se pudieron cargar los roles", onRetry = viewModel::load)
        }
        state.selected != null -> RoleDetail(
            state = state,
            viewModel = viewModel,
            role = state.selected!!,
            modifier = modifier,
        )
        else -> RoleList(state = state, viewModel = viewModel, modifier = modifier)
    }

    state.form?.let { form -> RoleFormModal(form, state.saving, state.actionError, viewModel) }
    state.deleteTarget?.let { role ->
        AppModal(
            title = "Eliminar rol",
            icon = Icons.Filled.DeleteOutline,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissDelete,
            confirmLabel = "Eliminar",
            onConfirm = viewModel::confirmDelete,
            saving = state.deleting,
        ) {
            Text(
                "¿Eliminar «${role.name}»? Solo se puede si no tiene personas asignadas y no es un rol del sistema.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
            state.actionError?.let { message ->
                Spacer(Modifier.height(10.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
            }
        }
    }
    if (state.memberPickerOpen) {
        AppModal(
            title = "Agregar persona al rol",
            icon = Icons.Filled.GroupAdd,
            onDismiss = viewModel::closeMemberPicker,
        ) {
            Text(
                "El rol se agrega como adicional; el principal se cambia en la cuenta de la persona.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
            Spacer(Modifier.height(10.dp))
            AppSearchField(
                value = state.memberQuery,
                onValueChange = viewModel::onMemberQueryChange,
                placeholder = "Buscar por nombre o usuario",
            )
            Spacer(Modifier.height(8.dp))
            Column(modifier = Modifier.height(320.dp)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.memberCandidates, key = { it.id }) { member ->
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            onClick = { viewModel.addMember(member) },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        member.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AppColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "@${member.username} · ${member.department ?: "Sin departamento"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.TextFaint,
                                        maxLines = 1,
                                    )
                                }
                                Icon(Icons.Filled.Add, contentDescription = null, tint = AppColors.EmeraldPrimary)
                            }
                        }
                    }
                    if (state.memberCandidates.isEmpty()) {
                        item { BentoState(message = "Sin personas para agregar con esa búsqueda.") }
                    }
                }
            }
            state.actionError?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
            }
        }
    }
}

@Composable
private fun RoleList(
    state: RolesAdminUiState,
    viewModel: RolesAdminViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${state.roles.size} rol(es) · ${state.members.size} persona(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                    modifier = Modifier.weight(1f),
                )
                PillButton(
                    text = "Nuevo rol",
                    onClick = viewModel::openCreate,
                    variant = PillButtonVariant.Secondary,
                    leadingIcon = Icons.Filled.Add,
                )
            }
            Spacer(Modifier.height(10.dp))
            AppSearchField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Buscar rol por nombre, clave o módulo",
            )
            state.actionError?.let { message ->
                Spacer(Modifier.height(6.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.rolesByModule.forEach { (module, roles) ->
                item(key = "module-$module") { SectionLabel(module) }
                items(roles, key = { it.key }) { role ->
                    RoleCard(
                        role = role,
                        granted = state.grantedCountFor(role.key),
                        onClick = { viewModel.selectRole(role.key) },
                    )
                }
            }
            if (state.rolesByModule.isEmpty()) {
                item { BentoState(message = "No hay roles con esa búsqueda.") }
            }
        }
    }
}

@Composable
private fun RoleCard(role: RoleAdminDto, granted: Int, onClick: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        role.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(6.dp))
                    StatusChip(
                        label = if (role.active) "Activo" else "Inactivo",
                        tone = if (role.active) BentoTone.Mint else BentoTone.Rose,
                    )
                }
                Text(
                    role.key,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = AppColors.TextFaint,
                    maxLines = 1,
                )
                Text(
                    "${role.userCount} persona(s) · $granted permiso(s)" +
                        if (role.staff) " · con expediente" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                    maxLines = 1,
                )
            }
            if (role.system) {
                Icon(Icons.Filled.Lock, contentDescription = "Rol del sistema", tint = AppColors.TextFaint)
            }
        }
    }
}

@Composable
private fun RoleDetail(
    state: RolesAdminUiState,
    viewModel: RolesAdminViewModel,
    role: RoleAdminDto,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TextButton(onClick = viewModel::showRoleList) {
                    Text("← Todos los roles", color = AppColors.EmeraldPrimary)
                }
            }
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                role.name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = AppColors.TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                role.key + (role.module?.let { " · $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = AppColors.TextFaint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            role.description?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextMuted,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusChip(
                                    label = if (role.active) "Activo" else "Inactivo",
                                    tone = if (role.active) BentoTone.Mint else BentoTone.Rose,
                                )
                                if (role.staff) {
                                    Spacer(Modifier.width(6.dp))
                                    StatusChip(label = "Con expediente", tone = BentoTone.Sea)
                                }
                                if (role.system) {
                                    Spacer(Modifier.width(6.dp))
                                    StatusChip(label = "Del sistema", tone = BentoTone.Plain)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${role.userCount} persona(s) · ${state.grantedPermissions.size} permiso(s) concedido(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        TextButton(onClick = { viewModel.openEdit(role) }) { Text("Editar", color = AppColors.EmeraldPrimary) }
                        TextButton(onClick = { viewModel.openDuplicate(role) }) { Text("Duplicar", color = AppColors.EmeraldPrimary) }
                        TextButton(onClick = { viewModel.toggleActive(role) }) {
                            Text(if (role.active) "Desactivar" else "Activar", color = AppColors.EmeraldPrimary)
                        }
                        if (!role.system) {
                            TextButton(onClick = { viewModel.requestDelete(role) }) {
                                Text("Eliminar", color = AppColors.Danger)
                            }
                        }
                    }
                    state.actionError?.let { message ->
                        Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
                    }
                }
            }
            item { SectionLabel("Nivel por módulo") }
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    state.modules.forEach { (module, permissions) ->
                        val level = moduleLevel(permissions) { state.scopeOf(it) }
                        val (granted, total) = moduleCoverage(permissions) { state.scopeOf(it) }
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(module, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                                Text(
                                    "$granted de $total permisos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextFaint,
                                )
                            }
                            StatusChip(label = level.label, tone = level.tone)
                        }
                    }
                }
            }
            item { SectionLabel("Alcance en línea") }
            item {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(PermissionScopes.ALL, PermissionScopes.AREA, PermissionScopes.OWN).forEach { scope ->
                        StatusChip(
                            label = "${permissionScopeLabel(scope)}: ${state.scopeCounts[scope] ?: 0}",
                            tone = when (scope) {
                                PermissionScopes.ALL -> BentoTone.Mint
                                PermissionScopes.AREA -> BentoTone.Sea
                                else -> BentoTone.Lavender
                            },
                        )
                    }
                }
            }
            item { SectionLabel("Personas del rol (${state.roleMembers.size})") }
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "El rol principal se cambia en la cuenta de la persona.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextMuted,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = viewModel::openMemberPicker) {
                            Text("Agregar", color = AppColors.EmeraldPrimary)
                        }
                    }
                    state.roleMembers.forEach { member ->
                        val isPrimary = member.role == role.key
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(member.name, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary, maxLines = 1)
                                Text(
                                    "@${member.username} · ${member.department ?: "Sin departamento"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextFaint,
                                    maxLines = 1,
                                )
                            }
                            if (isPrimary) {
                                StatusChip(label = "Principal", tone = BentoTone.Sea)
                            } else {
                                IconButton(onClick = { viewModel.removeMember(member) }) {
                                    Icon(
                                        Icons.Filled.PersonRemove,
                                        contentDescription = "Quitar rol adicional",
                                        tint = AppColors.Danger,
                                    )
                                }
                            }
                        }
                    }
                    if (state.roleMembers.isEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Nadie tiene este rol todavía.", style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
                    }
                }
            }
            item { SectionLabel("Permisos concedidos (${state.grantedPermissions.size})") }
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    state.grantedPermissions.groupBy { it.first.module }.forEach { (module, rows) ->
                        Text(module, style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
                        rows.forEach { (permission, scope) ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    permission.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextPrimary,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                StatusChip(label = permissionScopeLabel(scope), tone = BentoTone.Plain)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    if (state.grantedPermissions.isEmpty()) {
                        Text("Este rol no tiene permisos concedidos.", style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
                    }
                }
            }
            item { SectionLabel("Reglas que le aplican (${state.rolePolicies.size})") }
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    state.rolePolicies.forEach { policy ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatusChip(
                                label = if (policy.effect == "ALLOW") "Permite" else "Niega",
                                tone = if (policy.effect == "ALLOW") BentoTone.Mint else BentoTone.Rose,
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    policy.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${policy.action} · prioridad ${policy.priority}" +
                                        if (policy.roles.isEmpty()) " · todas las personas" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextFaint,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    if (state.rolePolicies.isEmpty()) {
                        Text("Ninguna regla ABAC aplica a este rol.", style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun RoleFormModal(
    form: RoleFormState,
    saving: Boolean,
    actionError: String?,
    viewModel: RolesAdminViewModel,
) {
    AppModal(
        title = when {
            form.isEdit -> "Editar rol"
            form.isDuplicate -> "Duplicar rol"
            else -> "Nuevo rol"
        },
        icon = if (form.isEdit) Icons.Filled.Edit else Icons.Filled.Add,
        onDismiss = viewModel::dismissForm,
        confirmLabel = if (form.isEdit) "Guardar" else "Crear",
        onConfirm = viewModel::submitForm,
        confirmEnabled = form.error == null && !saving,
        saving = saving,
    ) {
        Text(
            when {
                form.isEdit -> "Cambia nombre, módulo, expediente y orden del rol."
                form.isDuplicate -> "Se crea un rol nuevo con la misma matriz de permisos que «${form.copyFrom}»."
                else -> "El rol nuevo arranca sin permisos; se ajustan en la Matriz."
            },
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        Spacer(Modifier.height(12.dp))
        AppTextField(
            value = form.key,
            onValueChange = { value -> viewModel.updateForm { it.copy(key = value.uppercase()) } },
            label = { Text("Clave (MAYÚSCULAS)") },
            placeholder = { Text("EJ. SUPERVISOR") },
            singleLine = true,
            enabled = !form.isEdit,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = form.name,
            onValueChange = { value -> viewModel.updateForm { it.copy(name = value) } },
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = form.module,
            onValueChange = { value -> viewModel.updateForm { it.copy(module = value) } },
            label = { Text("Módulo (para agrupar en la lista)") },
            placeholder = { Text("Ej. Operación") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = form.description,
            onValueChange = { value -> viewModel.updateForm { it.copy(description = value) } },
            label = { Text("Descripción (opcional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = form.sortOrder,
            onValueChange = { value -> viewModel.updateForm { it.copy(sortOrder = value.filter(Char::isDigit)) } },
            label = { Text("Orden") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Rol con expediente de personal",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = form.staff, onCheckedChange = { value -> viewModel.updateForm { it.copy(staff = value) } })
        }
        form.error?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
        }
        actionError?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
        }
    }
}
