package com.axzydev.puertonuevoapp.feature.roles

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.permissions.PermissionsApi
import com.axzydev.puertonuevoapp.core.network.permissions.PolicyActionDefDto
import com.axzydev.puertonuevoapp.core.network.permissions.PolicyConditionDto
import com.axzydev.puertonuevoapp.core.network.permissions.PolicyCreateInput
import com.axzydev.puertonuevoapp.core.network.permissions.PolicyDto
import com.axzydev.puertonuevoapp.core.network.permissions.PolicyFieldDefDto
import com.axzydev.puertonuevoapp.core.network.permissions.PolicyUpdateInput
import com.axzydev.puertonuevoapp.core.network.permissions.RoleAdminDto
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoInsight
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
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

/** Condición en edición (`campo operador valor`). */
data class PolicyConditionForm(
    val field: String = "",
    val operator: String = "eq",
    val value: String = "",
)

/** Formulario de alta/edición de una regla ABAC. */
data class PolicyFormState(
    val editingId: String? = null,
    val name: String = "",
    val description: String = "",
    val action: String = "",
    val effect: String = "DENY",
    val priority: String = "100",
    val active: Boolean = true,
    val allRoles: Boolean = true,
    val roles: Set<String> = emptySet(),
    val conditions: List<PolicyConditionForm> = emptyList(),
) {
    val isEdit: Boolean get() = editingId != null
}

data class PoliciesTabUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val policies: List<PolicyDto> = emptyList(),
    val actions: List<PolicyActionDefDto> = emptyList(),
    val roles: List<RoleAdminDto> = emptyList(),
    val form: PolicyFormState? = null,
    val saving: Boolean = false,
    val actionError: String? = null,
    val deleteTarget: PolicyDto? = null,
    val deleting: Boolean = false,
) {
    fun actionOf(key: String): PolicyActionDefDto? = actions.firstOrNull { it.key == key }

    fun fieldOf(actionKey: String, fieldKey: String): PolicyFieldDefDto? =
        actionOf(actionKey)?.fields?.firstOrNull { it.key == fieldKey }

    /** Acciones con sus reglas, en el orden del catálogo de acciones (y las extra al final). */
    val groups: List<Pair<String, List<PolicyDto>>>
        get() {
            val keys = (actions.map { it.key } + policies.map { it.action }).distinct()
            return keys.map { key -> key to policies.filter { it.action == key }.sortedBy { it.priority } }
        }

    /** Siguiente prioridad para una regla nueva de la acción (después de las existentes). */
    fun nextPriority(action: String): Int {
        val used = policies.filter { it.action == action }.map { it.priority }
        return if (used.isEmpty()) 100 else used.max() + 10
    }

    fun roleName(key: String): String = roleDisplayName(key, roles)
}

/** Políticas ABAC (`/permissions/policies`): reglas por acción, editor y orden por prioridad. */
class PoliciesTabViewModel(
    private val permissionsApi: PermissionsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PoliciesTabUiState())
    val uiState: StateFlow<PoliciesTabUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                coroutineScope {
                    val data = async { permissionsApi.policies() }
                    val roles = async { permissionsApi.roles() }
                    data.await() to roles.await()
                }
            }
            result.fold(
                onSuccess = { (data, roles) ->
                    _uiState.update {
                        it.copy(loading = false, error = null, policies = data.policies, actions = data.actions, roles = roles)
                    }
                },
                onFailure = { error -> _uiState.update { it.copy(loading = false, error = networkMessage(error)) } },
            )
        }
    }

    fun openCreate(actionKey: String? = null) {
        val state = _uiState.value
        val action = actionKey ?: state.actions.firstOrNull()?.key.orEmpty()
        _uiState.update {
            it.copy(
                form = PolicyFormState(
                    action = action,
                    priority = state.nextPriority(action).toString(),
                ),
                actionError = null,
            )
        }
    }

    fun openEdit(policy: PolicyDto) {
        _uiState.update {
            it.copy(
                form = PolicyFormState(
                    editingId = policy.id,
                    name = policy.name,
                    description = policy.description.orEmpty(),
                    action = policy.action,
                    effect = policy.effect,
                    priority = policy.priority.toString(),
                    active = policy.active,
                    allRoles = policy.roles.isEmpty(),
                    roles = policy.roles.toSet(),
                    conditions = policy.conditions.map { PolicyConditionForm(it.field, it.operator, it.value.orEmpty()) },
                ),
                actionError = null,
            )
        }
    }

    fun dismissForm() = _uiState.update { if (it.saving) it else it.copy(form = null) }

    fun updateForm(transform: (PolicyFormState) -> PolicyFormState) =
        _uiState.update { state -> state.form?.let { state.copy(form = transform(it)) } ?: state }

    fun changeAction(actionKey: String) {
        val state = _uiState.value
        updateForm {
            it.copy(
                action = actionKey,
                conditions = emptyList(),
                priority = if (it.isEdit) it.priority else state.nextPriority(actionKey).toString(),
            )
        }
    }

    fun toggleRole(roleKey: String) = updateForm { form ->
        form.copy(roles = if (roleKey in form.roles) form.roles - roleKey else form.roles + roleKey)
    }

    fun addCondition() {
        val form = _uiState.value.form ?: return
        val action = _uiState.value.actionOf(form.action) ?: return
        val field = action.fields.firstOrNull() ?: return
        updateForm { current ->
            current.copy(
                conditions = current.conditions + PolicyConditionForm(
                    field = field.key,
                    operator = operatorsForType(field.type).firstOrNull()?.first ?: "eq",
                ),
            )
        }
    }

    fun updateCondition(index: Int, transform: (PolicyConditionForm) -> PolicyConditionForm) =
        updateForm { form ->
            form.copy(conditions = form.conditions.mapIndexed { i, condition -> if (i == index) transform(condition) else condition })
        }

    fun removeCondition(index: Int) = updateForm { form ->
        form.copy(conditions = form.conditions.filterIndexed { i, _ -> i != index })
    }

    /** Valida el formulario con las reglas del API; devuelve el mensaje de error o null. */
    private fun validate(form: PolicyFormState): String? {
        if (form.name.trim().isBlank()) return "El nombre es obligatorio"
        if (form.action.isBlank()) return "Elige una acción"
        if (form.priority.trim().toIntOrNull() == null) return "La prioridad debe ser un número entero"
        if (!form.allRoles && form.roles.isEmpty()) return "Elige al menos un rol o marca «todas las personas»"
        val action = _uiState.value.actionOf(form.action) ?: return "La acción no está en el catálogo"
        form.conditions.forEach { condition ->
            val field = action.fields.firstOrNull { it.key == condition.field }
                ?: return "Cada condición necesita un campo válido"
            if (condition.operator !in operatorsForType(field.type).map { it.first }) {
                return "Operador no válido para «${field.label}»"
            }
            if (condition.operator !in operatorsWithoutValue && condition.value.isBlank()) {
                return "«${field.label}» necesita un valor"
            }
        }
        return null
    }

    fun submitForm() {
        val form = _uiState.value.form ?: return
        val error = validate(form)
        if (error != null) {
            _uiState.update { it.copy(actionError = error) }
            return
        }
        if (_uiState.value.saving) return
        _uiState.update { it.copy(saving = true, actionError = null) }
        val conditions = form.conditions.map {
            PolicyConditionDto(
                field = it.field,
                operator = it.operator,
                value = if (it.operator in operatorsWithoutValue) null else it.value.trim(),
            )
        }
        val roles = if (form.allRoles) emptyList() else form.roles.toList()
        viewModelScope.launch {
            val result = runCatching {
                if (form.isEdit) {
                    permissionsApi.updatePolicy(
                        form.editingId!!,
                        PolicyUpdateInput(
                            name = form.name.trim(),
                            description = form.description.trim(),
                            action = form.action,
                            effect = form.effect,
                            priority = form.priority.trim().toInt(),
                            active = form.active,
                            roles = roles,
                            conditions = conditions,
                        ),
                    )
                } else {
                    permissionsApi.createPolicy(
                        PolicyCreateInput(
                            name = form.name.trim(),
                            description = form.description.trim().ifBlank { null },
                            action = form.action,
                            effect = form.effect,
                            priority = form.priority.trim().toInt(),
                            active = form.active,
                            roles = roles,
                            conditions = conditions,
                        ),
                    )
                }
            }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(saving = false, form = null) }
                    AppSnackbar.show(if (form.isEdit) "Regla actualizada" else "Regla creada")
                    load()
                },
                onFailure = { failure ->
                    _uiState.update { it.copy(saving = false, actionError = networkMessage(failure)) }
                },
            )
        }
    }

    fun toggleActive(policy: PolicyDto) {
        if (_uiState.value.saving) return
        _uiState.update { it.copy(actionError = null) }
        viewModelScope.launch {
            val result = runCatching { permissionsApi.updatePolicy(policy.id, PolicyUpdateInput(active = !policy.active)) }
            result.fold(
                onSuccess = {
                    AppSnackbar.show(if (policy.active) "Regla desactivada" else "Regla activada")
                    load()
                },
                onFailure = { failure -> _uiState.update { it.copy(actionError = networkMessage(failure)) } },
            )
        }
    }

    /** Reordena la regla intercambiando la prioridad con su vecina (el motor ordena por prioridad). */
    fun move(policy: PolicyDto, up: Boolean) {
        val state = _uiState.value
        val siblings = state.policies.filter { it.action == policy.action }.sortedBy { it.priority }
        val index = siblings.indexOfFirst { it.id == policy.id }
        val other = siblings.getOrNull(if (up) index - 1 else index + 1) ?: return
        if (state.saving) return
        _uiState.update { it.copy(actionError = null) }
        viewModelScope.launch {
            val result = runCatching {
                permissionsApi.updatePolicy(policy.id, PolicyUpdateInput(priority = other.priority))
                permissionsApi.updatePolicy(other.id, PolicyUpdateInput(priority = policy.priority))
            }
            result.fold(
                onSuccess = { load() },
                onFailure = { failure -> _uiState.update { it.copy(actionError = networkMessage(failure)) } },
            )
        }
    }

    fun requestDelete(policy: PolicyDto) = _uiState.update { it.copy(deleteTarget = policy, actionError = null) }

    fun dismissDelete() = _uiState.update { it.copy(deleteTarget = null) }

    fun confirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        if (_uiState.value.deleting) return
        _uiState.update { it.copy(deleting = true, actionError = null) }
        viewModelScope.launch {
            val result = runCatching { permissionsApi.deletePolicy(target.id) }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(deleting = false, deleteTarget = null) }
                    AppSnackbar.show("Regla eliminada")
                    load()
                },
                onFailure = { failure ->
                    _uiState.update { it.copy(deleting = false, actionError = networkMessage(failure)) }
                },
            )
        }
    }
}

@Composable
internal fun PoliciesTab(modifier: Modifier = Modifier) {
    val viewModel: PoliciesTabViewModel = viewModel { PoliciesTabViewModel(AppContainer.permissionsApi) }
    val state by viewModel.uiState.collectAsState()

    when {
        state.loading -> BentoState(message = "Cargando reglas…", loading = true, modifier = modifier.padding(16.dp))
        state.error != null -> Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            BentoErrorState(message = state.error ?: "No se pudieron cargar las reglas", onRetry = viewModel::load)
        }
        else -> Column(modifier = modifier.fillMaxSize()) {
            LazyPolicyList(state, viewModel, modifier = Modifier.weight(1f))
        }
    }

    state.form?.let { form -> PolicyFormModal(state, form, viewModel) }
    state.deleteTarget?.let { policy ->
        AppModal(
            title = "Eliminar regla",
            icon = Icons.Filled.DeleteOutline,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissDelete,
            confirmLabel = "Eliminar",
            onConfirm = viewModel::confirmDelete,
            saving = state.deleting,
        ) {
            Text(
                "¿Eliminar «${policy.name}»? Las reglas base del sistema no se pueden borrar (solo desactivar).",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
            state.actionError?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
            }
        }
    }
}

@Composable
private fun LazyPolicyList(state: PoliciesTabUiState, viewModel: PoliciesTabViewModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "Las reglas ABAC afinan el permiso con contexto: la primera que casa por prioridad decide. Sin reglas, el permiso RBAC ya autorizó la acción.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.actionError != null) {
                    Text(
                        state.actionError,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Danger,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                PillButton(
                    text = "Nueva regla",
                    onClick = { viewModel.openCreate() },
                    variant = PillButtonVariant.Secondary,
                    leadingIcon = Icons.Filled.Add,
                )
            }
        }
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.groups.forEach { (actionKey, rules) ->
                item(key = "action-$actionKey") {
                    PolicyActionCard(actionKey, rules, state, viewModel)
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun PolicyActionCard(
    actionKey: String,
    rules: List<PolicyDto>,
    state: PoliciesTabUiState,
    viewModel: PoliciesTabViewModel,
) {
    val action = state.actionOf(actionKey)
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    action?.label ?: actionKey,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                )
                val allows = rules.count { it.effect == "ALLOW" }
                val denies = rules.count { it.effect == "DENY" }
                Text(
                    listOfNotNull(
                        action?.module ?: actionKey,
                        "${rules.size} regla(s)",
                        "$allows permiten",
                        "$denies niegan",
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextFaint,
                )
            }
            TextButton(onClick = { viewModel.openCreate(actionKey) }) {
                Text("Agregar", color = AppColors.EmeraldPrimary)
            }
        }
        Spacer(Modifier.height(6.dp))
        rules.forEachIndexed { index, policy ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                Text(
                    "${index + 1}.",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                    modifier = Modifier.width(22.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(
                            label = if (policy.effect == "ALLOW") "Permite" else "Niega",
                            tone = if (policy.effect == "ALLOW") BentoTone.Mint else BentoTone.Rose,
                        )
                        Spacer(Modifier.width(6.dp))
                        if (!policy.active) StatusChip(label = "Inactiva", tone = BentoTone.Sun)
                        if (policy.key != null) {
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Filled.Lock, contentDescription = "Regla base del sistema", tint = AppColors.TextFaint, modifier = Modifier.size(12.dp))
                        }
                    }
                    Text(
                        policy.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val applies = if (policy.roles.isEmpty()) {
                        "Todas las personas"
                    } else {
                        policy.roles.joinToString(", ") { state.roleName(it) }
                    }
                    val conditions = if (policy.conditions.isEmpty()) {
                        "sin condiciones"
                    } else {
                        policy.conditions.joinToString("; ") {
                            "${state.fieldOf(policy.action, it.field)?.label ?: it.field} " +
                                "${policyOperatorLabels.firstOrNull { op -> op.first == it.operator }?.second ?: it.operator} " +
                                (it.value ?: "")
                        }.trim()
                    }
                    Text(
                        "Aplica a: $applies · $conditions · prioridad ${policy.priority}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = { viewModel.move(policy, up = true) }, enabled = index > 0) { Text("↑") }
                TextButton(
                    onClick = { viewModel.move(policy, up = false) },
                    enabled = index < rules.size - 1,
                ) { Text("↓") }
                IconButton(onClick = { viewModel.toggleActive(policy) }) {
                    Icon(
                        imageVector = if (policy.active) Icons.Filled.VisibilityOff else Icons.Filled.RestartAlt,
                        contentDescription = if (policy.active) "Desactivar" else "Activar",
                        tint = if (policy.active) AppColors.Warning else AppColors.Success,
                    )
                }
                IconButton(onClick = { viewModel.openEdit(policy) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = AppColors.EmeraldPrimary)
                }
                if (policy.key == null) {
                    IconButton(onClick = { viewModel.requestDelete(policy) }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar", tint = AppColors.Danger)
                    }
                }
            }
        }
        if (rules.isEmpty()) {
            Text(
                "Sin reglas: el permiso RBAC decide solo.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
            )
        }
    }
}

@Composable
private fun PolicyFormModal(state: PoliciesTabUiState, form: PolicyFormState, viewModel: PoliciesTabViewModel) {
    val action = state.actionOf(form.action)
    AppModal(
        title = if (form.isEdit) "Editar regla" else "Nueva regla",
        icon = Icons.Filled.VerifiedUser,
        onDismiss = viewModel::dismissForm,
        confirmLabel = if (form.isEdit) "Guardar" else "Crear",
        onConfirm = viewModel::submitForm,
        confirmEnabled = !state.saving,
        saving = state.saving,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            AppTextField(
                value = form.name,
                onValueChange = { value -> viewModel.updateForm { it.copy(name = value) } },
                label = { Text("Nombre de la regla") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            SimpleDropdownField(
                label = "Acción",
                value = form.action,
                options = state.actions.map { it.key to "${it.label} (${it.module})" },
                onSelect = viewModel::changeAction,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SimpleDropdownField(
                    label = "Efecto",
                    value = form.effect,
                    options = listOf("DENY" to "Negar", "ALLOW" to "Permitir"),
                    onSelect = { value -> viewModel.updateForm { it.copy(effect = value) } },
                    modifier = Modifier.weight(1f),
                )
                AppTextField(
                    value = form.priority,
                    onValueChange = { value ->
                        viewModel.updateForm { it.copy(priority = value.filter(Char::isDigit)) }
                    },
                    label = { Text("Prioridad") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Regla activa",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = form.active, onCheckedChange = { value -> viewModel.updateForm { it.copy(active = value) } })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Aplica a todas las personas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = form.allRoles,
                    onCheckedChange = { value -> viewModel.updateForm { it.copy(allRoles = value) } },
                )
            }
            if (!form.allRoles) {
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.roles.filter { it.active }.forEach { role ->
                        AppFilterChip(
                            label = role.name,
                            selected = role.key in form.roles,
                            onClick = { viewModel.toggleRole(role.key) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionLabel("Condiciones (campo operador valor)")
            Spacer(Modifier.height(6.dp))
            if (action == null || action.fields.isEmpty()) {
                Text(
                    "Esta acción no declara campos de registro: la regla decide solo por rol.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                )
            } else {
                form.conditions.forEachIndexed { index, condition ->
                    val field = action.fields.firstOrNull { it.key == condition.field }
                    val fieldType = field?.type ?: "string"
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SimpleDropdownField(
                                label = "Campo",
                                value = condition.field,
                                options = action.fields.map { it.key to it.label },
                                onSelect = { key ->
                                    val newField = action.fields.firstOrNull { it.key == key }
                                    viewModel.updateCondition(index) {
                                        it.copy(
                                            field = key,
                                            operator = newField?.let { f -> operatorsForType(f.type).firstOrNull()?.first } ?: "eq",
                                            value = "",
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { viewModel.removeCondition(index) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Quitar condición", tint = AppColors.Danger)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        SimpleDropdownField(
                            label = "Operador",
                            value = condition.operator,
                            options = operatorsForType(fieldType),
                            onSelect = { value -> viewModel.updateCondition(index) { it.copy(operator = value) } },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (condition.operator !in operatorsWithoutValue) {
                            Spacer(Modifier.height(6.dp))
                            when (fieldType) {
                                "enum" -> SimpleDropdownField(
                                    label = "Valor",
                                    value = condition.value,
                                    options = listOf("" to "Elige un valor") + field.orEmptyOptions(),
                                    onSelect = { value -> viewModel.updateCondition(index) { it.copy(value = value) } },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                "user" -> SimpleDropdownField(
                                    label = "Valor",
                                    value = condition.value,
                                    options = listOf("" to "Elige una referencia") + policyUserReferences,
                                    onSelect = { value -> viewModel.updateCondition(index) { it.copy(value = value) } },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                else -> AppTextField(
                                    value = condition.value,
                                    onValueChange = { value -> viewModel.updateCondition(index) { it.copy(value = value) } },
                                    label = { Text("Valor") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
                TextButton(onClick = viewModel::addCondition, enabled = action.fields.isNotEmpty()) {
                    Text("+ Agregar condición", color = AppColors.EmeraldPrimary)
                }
            }
            Spacer(Modifier.height(10.dp))
            BentoInsight(
                text = policyPreview(form, state),
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
            state.actionError?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
            }
        }
    }
}

/** Lee la regla en voz alta mientras se edita (mismo lenguaje que la web). */
private fun policyPreview(form: PolicyFormState, state: PoliciesTabUiState): String {
    val action = state.actionOf(form.action)
    val rolesLabel = if (form.allRoles) {
        "todas las personas"
    } else {
        form.roles.joinToString(", ") { state.roleName(it) }.ifBlank { "los roles elegidos" }
    }
    val conditions = form.conditions.joinToString(" y ") { condition ->
        val field = action?.fields?.firstOrNull { it.key == condition.field }?.label ?: condition.field
        val operator = policyOperatorLabels.firstOrNull { it.first == condition.operator }?.second ?: condition.operator
        if (condition.operator in operatorsWithoutValue) {
            "$field $operator"
        } else {
            "$field $operator ${condition.value.ifBlank { "…" }}"
        }
    }
    val verb = if (form.effect == "ALLOW") "Puede" else "No puede"
    val actionLabel = action?.label ?: form.action
    return if (conditions.isBlank()) {
        "$verb $rolesLabel · $actionLabel"
    } else {
        "$verb si $conditions · $rolesLabel · $actionLabel"
    }
}

/** Atajo local: opciones de un campo enum en el editor. */
private fun PolicyFieldDefDto?.orEmptyOptions(): List<Pair<String, String>> =
    this?.options?.map { it to it }.orEmpty()
