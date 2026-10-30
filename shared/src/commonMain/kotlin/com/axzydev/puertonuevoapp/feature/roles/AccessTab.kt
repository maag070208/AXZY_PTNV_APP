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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
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
import com.axzydev.puertonuevoapp.core.network.permissions.PermissionCatalogDto
import com.axzydev.puertonuevoapp.core.network.permissions.PermissionsApi
import com.axzydev.puertonuevoapp.core.network.permissions.PolicyActionDefDto
import com.axzydev.puertonuevoapp.core.network.permissions.RoleAdminDto
import com.axzydev.puertonuevoapp.core.network.permissions.SimulationDto
import com.axzydev.puertonuevoapp.core.network.permissions.SimulationInput
import com.axzydev.puertonuevoapp.core.network.permissions.SimulationRuleTraceDto
import com.axzydev.puertonuevoapp.core.network.permissions.UserAccessDto
import com.axzydev.puertonuevoapp.core.session.PermissionScopes
import com.axzydev.puertonuevoapp.core.session.permissionScopeLabel
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pestaña "Acceso por persona": visor de acceso efectivo y probador. */
data class AccessTabUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val members: List<AccessMemberDto> = emptyList(),
    val catalog: List<PermissionCatalogDto> = emptyList(),
    val roles: List<RoleAdminDto> = emptyList(),
    val policyActions: List<PolicyActionDefDto> = emptyList(),
    val query: String = "",
    val selectedUserId: String? = null,
    val loadingAccess: Boolean = false,
    val accessError: String? = null,
    val access: UserAccessDto? = null,
    val permissionQuery: String = "",
    val showOnlyGranted: Boolean = true,
    val simulatePermission: String = "",
    val resourceValues: Map<String, String> = emptyMap(),
    val simulating: Boolean = false,
    val simulation: SimulationDto? = null,
    val simulateError: String? = null,
) {
    val selectedMember: AccessMemberDto? get() = members.firstOrNull { it.id == selectedUserId }

    val filteredMembers: List<AccessMemberDto>
        get() {
            val needle = query.trim()
            val base = if (needle.isEmpty()) {
                members
            } else {
                members.filter {
                    it.name.contains(needle, ignoreCase = true) ||
                        it.username.contains(needle, ignoreCase = true) ||
                        it.department.orEmpty().contains(needle, ignoreCase = true)
                }
            }
            return base.sortedByDescending { it.active }
        }

    val activePermissions: List<PermissionCatalogDto> get() = catalog.filter { it.active }

    fun permissionName(key: String): String =
        catalog.firstOrNull { it.key == key }?.name ?: key

    /** Acción de política de la selección del probador (define los campos del registro). */
    val simulateAction: PolicyActionDefDto?
        get() = policyActions.firstOrNull { it.key == simulatePermission }

    /** Permisos del acceso efectivo filtrados por la búsqueda y el filtro. */
    val visibleAccessPermissions: List<com.axzydev.puertonuevoapp.core.network.permissions.AccessPermissionDto>
        get() {
            val all = access?.permissions.orEmpty()
            val needle = permissionQuery.trim()
            return all.filter { permission ->
                (!showOnlyGranted || permission.effective != PermissionScopes.NONE) &&
                    (needle.isEmpty() ||
                        permission.key.contains(needle, ignoreCase = true) ||
                        permissionName(permission.key).contains(needle, ignoreCase = true))
            }
        }
}

/** Personas, su acceso efectivo explicado por origen y el probador RBAC → ABAC. */
class AccessTabViewModel(
    private val permissionsApi: PermissionsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccessTabUiState())
    val uiState: StateFlow<AccessTabUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                coroutineScope {
                    val members = async { permissionsApi.members() }
                    val admin = async { permissionsApi.admin() }
                    val roles = async { permissionsApi.roles() }
                    val policies = async { permissionsApi.policies() }
                    Data(members.await(), admin.await().catalog, roles.await(), policies.await().actions)
                }
            }
            result.fold(
                onSuccess = { data ->
                    _uiState.update { it.copy(loading = false, error = null, members = data.members, catalog = data.catalog, roles = data.roles, policyActions = data.actions) }
                },
                onFailure = { error -> _uiState.update { it.copy(loading = false, error = networkMessage(error)) } },
            )
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }

    fun selectMember(member: AccessMemberDto) {
        _uiState.update {
            it.copy(
                selectedUserId = member.id,
                access = null,
                accessError = null,
                simulation = null,
                simulateError = null,
                simulatePermission = "",
                resourceValues = emptyMap(),
            )
        }
        loadAccess(member.id)
    }

    fun showMemberList() = _uiState.update { it.copy(selectedUserId = null, access = null) }

    fun refreshAccess() {
        _uiState.value.selectedUserId?.let { loadAccess(it) }
    }

    private fun loadAccess(userId: String) {
        _uiState.update { it.copy(loadingAccess = true, accessError = null) }
        viewModelScope.launch {
            val result = runCatching { permissionsApi.memberAccess(userId) }
            result.fold(
                onSuccess = { access ->
                    _uiState.update { it.copy(loadingAccess = false, access = access) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(loadingAccess = false, accessError = networkMessage(error)) }
                },
            )
        }
    }

    fun onPermissionQueryChange(value: String) = _uiState.update { it.copy(permissionQuery = value) }

    fun setShowOnlyGranted(value: Boolean) = _uiState.update { it.copy(showOnlyGranted = value) }

    // ── Probador ─────────────────────────────────────────────────────────────

    fun onSimulatePermissionChange(permission: String) =
        _uiState.update {
            it.copy(
                simulatePermission = permission,
                resourceValues = emptyMap(),
                simulation = null,
                simulateError = null,
            )
        }

    fun onResourceValueChange(field: String, value: String) =
        _uiState.update { it.copy(resourceValues = it.resourceValues + (field to value), simulation = null) }

    fun clearSimulation() = _uiState.update { it.copy(simulation = null, simulateError = null) }

    fun simulate() {
        val state = _uiState.value
        val userId = state.selectedUserId ?: return
        if (state.simulatePermission.isBlank() || state.simulating) return
        val resource = state.resourceValues.filterValues { it.isNotBlank() }.takeIf { it.isNotEmpty() }
        _uiState.update { it.copy(simulating = true, simulateError = null, simulation = null) }
        viewModelScope.launch {
            val result = runCatching {
                permissionsApi.simulate(
                    SimulationInput(userId = userId, permission = state.simulatePermission, resource = resource),
                )
            }
            result.fold(
                onSuccess = { simulation ->
                    _uiState.update { it.copy(simulating = false, simulation = simulation) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(simulating = false, simulateError = networkMessage(error)) }
                    AppSnackbar.showError(networkMessage(error))
                },
            )
        }
    }

    private data class Data(
        val members: List<AccessMemberDto>,
        val catalog: List<PermissionCatalogDto>,
        val roles: List<RoleAdminDto>,
        val actions: List<PolicyActionDefDto>,
    )
}

@Composable
internal fun AccessTab(modifier: Modifier = Modifier) {
    val viewModel: AccessTabViewModel = viewModel { AccessTabViewModel(AppContainer.permissionsApi) }
    val state by viewModel.uiState.collectAsState()

    when {
        state.loading -> BentoState(message = "Cargando personas y permisos…", loading = true, modifier = modifier.padding(16.dp))
        state.error != null -> Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            BentoErrorState(message = state.error ?: "No se pudo cargar el acceso", onRetry = viewModel::load)
        }
        state.selectedMember != null -> AccessDetail(state, viewModel, state.selectedMember!!, modifier)
        else -> AccessMemberList(state, viewModel, modifier)
    }
}

@Composable
private fun AccessMemberList(state: AccessTabUiState, viewModel: AccessTabViewModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "Elige una persona para ver su acceso efectivo y probar una acción.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
            Spacer(Modifier.height(10.dp))
            AppSearchField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Buscar por nombre, usuario o departamento",
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.filteredMembers, key = { it.id }) { member ->
                AppCard(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.selectMember(member) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                member.name,
                                style = MaterialTheme.typography.titleSmall,
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
                            Text(
                                "Rol: ${roleDisplayName(member.role, state.roles)}" +
                                    if (member.extraRoles.isNotEmpty()) " +${member.extraRoles.size}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextMuted,
                                maxLines = 1,
                            )
                        }
                        if (member.exceptions > 0) {
                            StatusChip(label = "${member.exceptions} excepción(es)", tone = BentoTone.Sun)
                        }
                        if (!member.active) {
                            Spacer(Modifier.width(6.dp))
                            StatusChip(label = "Inactiva", tone = BentoTone.Rose)
                        }
                    }
                }
            }
            if (state.filteredMembers.isEmpty()) {
                item { BentoState(message = "No hay personas con esa búsqueda.") }
            }
        }
    }
}

@Composable
private fun AccessDetail(
    state: AccessTabUiState,
    viewModel: AccessTabViewModel,
    member: AccessMemberDto,
    modifier: Modifier = Modifier,
) {
    val access = state.access
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TextButton(onClick = viewModel::showMemberList) {
                    Text("← Todas las personas", color = AppColors.EmeraldPrimary)
                }
            }
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        member.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = AppColors.TextPrimary,
                    )
                    Text(
                        "@${member.username} · ${member.department ?: "Sin departamento"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextFaint,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        (access?.roles ?: emptyList()).forEach { role ->
                            StatusChip(
                                label = roleDisplayName(role.key, state.roles) + if (role.primary) " · principal" else " · adicional",
                                tone = if (role.primary) BentoTone.Sea else BentoTone.Lavender,
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        if (!member.active) StatusChip(label = "Inactiva", tone = BentoTone.Rose)
                    }
                }
            }
            item { SectionLabel("Probador: ¿puede…?") }
            item { SimulatorCard(state, viewModel, modifier = Modifier) }
            item { SectionLabel("Acceso efectivo (${access?.permissions?.size ?: 0} permisos)") }
            item {
                Column {
                    AppSearchField(
                        value = state.permissionQuery,
                        onValueChange = viewModel::onPermissionQueryChange,
                        placeholder = "Buscar permiso por clave o nombre",
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppFilterChip(
                            label = "Con acceso",
                            selected = state.showOnlyGranted,
                            onClick = { viewModel.setShowOnlyGranted(true) },
                        )
                        AppFilterChip(
                            label = "Todos",
                            selected = !state.showOnlyGranted,
                            onClick = { viewModel.setShowOnlyGranted(false) },
                        )
                    }
                }
            }
            if (state.loadingAccess) {
                item { BentoState(message = "Cargando acceso efectivo…", loading = true) }
            } else if (state.accessError != null) {
                item { BentoErrorState(message = state.accessError, onRetry = viewModel::refreshAccess) }
            } else {
                val rows = state.visibleAccessPermissions
                if (rows.isEmpty()) {
                    item { BentoState(message = "Sin permisos para mostrar con ese filtro.") }
                } else {
                    items(rows, key = { it.key }) { permission ->
                        AccessPermissionRow(permission, state)
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun AccessPermissionRow(
    permission: com.axzydev.puertonuevoapp.core.network.permissions.AccessPermissionDto,
    state: AccessTabUiState,
) {
    AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    state.permissionName(permission.key),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    permission.key,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = AppColors.TextFaint,
                )
                val origin = buildList {
                    permission.byRole.forEach { (role, scope) ->
                        add("${roleDisplayName(role, state.roles)}: ${permissionScopeLabel(scope)}")
                    }
                    permission.exception?.let { add("Excepción: ${permissionScopeLabel(it.scope)}") }
                }.joinToString(" · ")
                if (origin.isNotBlank()) {
                    Text(origin, style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted, maxLines = 2)
                }
            }
            StatusChip(
                label = permissionScopeLabel(permission.effective),
                tone = when (permission.effective) {
                    PermissionScopes.ALL -> BentoTone.Mint
                    PermissionScopes.AREA -> BentoTone.Sea
                    PermissionScopes.OWN -> BentoTone.Lavender
                    else -> BentoTone.Plain
                },
            )
        }
    }
}

@Composable
private fun SimulatorCard(state: AccessTabUiState, viewModel: AccessTabViewModel, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Text(
            "Recorre las mismas capas que una petición real: identidad → RBAC → ABAC.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        Spacer(Modifier.height(10.dp))
        SimpleDropdownField(
            label = "Permiso / acción",
            value = state.simulatePermission,
            options = listOf("" to "Elige un permiso") +
                state.activePermissions.map { it.key to "${it.module} · ${it.name}" },
            onSelect = viewModel::onSimulatePermissionChange,
            modifier = Modifier.fillMaxWidth(),
        )
        val action = state.simulateAction
        if (action != null && action.fields.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Campos del registro para evaluar las reglas (opcionales):",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
            action.fields.forEach { field ->
                Spacer(Modifier.height(8.dp))
                val options = when (field.type) {
                    "enum" -> listOf("" to "Sin valor") + field.options.map { it to it }
                    "user" -> listOf("" to "Sin valor") + policyUserReferences
                    else -> null
                }
                if (options != null) {
                    SimpleDropdownField(
                        label = field.label,
                        value = state.resourceValues[field.key].orEmpty(),
                        options = options,
                        onSelect = { viewModel.onResourceValueChange(field.key, it) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    AppTextField(
                        value = state.resourceValues[field.key].orEmpty(),
                        onValueChange = { viewModel.onResourceValueChange(field.key, it) },
                        label = { Text(field.label) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PillButton(
                text = "Probar",
                onClick = viewModel::simulate,
                enabled = state.simulatePermission.isNotBlank(),
                loading = state.simulating,
                leadingIcon = Icons.Filled.PlayArrow,
            )
            if (state.simulation != null) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = viewModel::clearSimulation) { Text("Limpiar") }
            }
        }
        state.simulateError?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
        }
        state.simulation?.let { simulation ->
            Spacer(Modifier.height(12.dp))
            SimulationResult(simulation, state)
        }
    }
}

@Composable
private fun SimulationResult(simulation: SimulationDto, state: AccessTabUiState) {
    Column {
        StatusChip(
            label = if (simulation.allowed) "Puede hacerlo" else "No puede",
            tone = if (simulation.allowed) BentoTone.Mint else BentoTone.Rose,
        )
        Spacer(Modifier.height(8.dp))
        LayerRow(
            label = "Identidad",
            ok = simulation.identity.ok,
            detail = if (simulation.identity.ok) "Cuenta activa" else "La cuenta está inactiva",
        )
        LayerRow(
            label = "RBAC",
            ok = simulation.rbac.ok,
            detail = "Alcance ${permissionScopeLabel(simulation.rbac.scope)}" +
                if (simulation.rbac.permissionActive) "" else " · permiso inactivo",
        )
        val decidedRule = simulation.abac.explanation?.decidedBy
        LayerRow(
            label = "ABAC",
            ok = simulation.abac.ok,
            detail = when {
                !simulation.abac.evaluated -> "No se evaluó (sin reglas o RBAC denegado)"
                decidedRule != null -> "Decidió «${decidedRule.name}»"
                else -> "Ninguna regla aplicó: se permite"
            },
        )
        if (simulation.rbac.byRole.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                simulation.rbac.byRole.entries.joinToString(" · ") {
                    "${roleDisplayName(it.key, state.roles)}: ${permissionScopeLabel(it.value)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
        }
        val explanation = simulation.abac.explanation
        if (explanation != null && explanation.rules.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Reglas evaluadas (en orden):", style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
            explanation.rules.forEach { rule -> SimulationRuleRow(rule) }
        }
    }
}

@Composable
private fun LayerRow(label: String, ok: Boolean, detail: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        StatusChip(
            label = if (ok) "Sí" else "No",
            tone = if (ok) BentoTone.Mint else BentoTone.Rose,
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
        }
    }
}

@Composable
private fun SimulationRuleRow(rule: SimulationRuleTraceDto) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        StatusChip(
            label = if (rule.matches) "Casa" else "No casa",
            tone = if (rule.matches) (if (rule.effect == "DENY") BentoTone.Rose else BentoTone.Mint) else BentoTone.Plain,
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${rule.name} · ${if (rule.effect == "ALLOW") "permite" else "niega"} · prioridad ${rule.priority}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextPrimary,
            )
            if (rule.conditions.isNotEmpty()) {
                Text(
                    rule.conditions.joinToString("; ") {
                        "${it.field} ${policyOperatorLabel(it.operator)} ${it.expected?.toString() ?: ""}".trim()
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextFaint,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun policyOperatorLabel(operator: String): String =
    policyOperatorLabels.firstOrNull { it.first == operator }?.second ?: operator
