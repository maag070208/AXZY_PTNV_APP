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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.network.permissions.PermissionCatalogDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionScopes
import com.axzydev.puertonuevoapp.core.session.permissionScopeLabel
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.roleLabel

/** Roles y permisos (`roles.manage`): matriz rol → permiso → alcance y catálogo. */
@Composable
fun RolesPermissionsScreen(
    viewModel: RolesPermissionsViewModel = viewModel {
        RolesPermissionsViewModel(AppContainer.permissionsApi, AppContainer.authRepository)
    },
) {
    val authState by AppContainer.authRepository.state.collectAsState()
    val allowed = (authState as? AuthState.LoggedIn)?.user?.canManageRoles == true
    if (!allowed) {
        ErrorState(message = "No autorizado — requiere el permiso Administrar roles y permisos", modifier = Modifier.fillMaxSize())
        return
    }

    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = state.tab, containerColor = AppColors.Surface, contentColor = AppColors.EmeraldPrimary) {
            Tab(selected = state.tab == 0, onClick = { viewModel.onTabChange(0) }, text = { Text("Matriz") })
            Tab(selected = state.tab == 1, onClick = { viewModel.onTabChange(1) }, text = { Text("Catálogo") })
        }
        when {
            state.loading && state.data == null -> LoadingState(modifier = Modifier.fillMaxSize())
            state.error != null && state.data == null -> ErrorState(state.error ?: "Error", Modifier.fillMaxSize(), onRetry = viewModel::load)
            state.tab == 0 -> MatrixTab(state, viewModel)
            else -> CatalogTab(state, viewModel)
        }
    }

    state.form?.let { form -> PermissionFormModal(form, state.savingCatalog, viewModel) }
}

@Composable
private fun MatrixTab(state: RolesPermissionsUiState, viewModel: RolesPermissionsViewModel) {
    val role = state.selectedRole
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Define el alcance de cada permiso por rol. Los permisos inactivos no se pueden conceder.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val count = state.changes.size
                StatusChip(
                    if (count == 1) "1 cambio pendiente" else "$count cambios pendientes",
                    if (count > 0) AppColors.Warning else AppColors.TextFaint,
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = viewModel::discard, enabled = state.dirty && !state.savingMatrix) {
                    Text("Descartar")
                }
                Button(
                    onClick = viewModel::saveMatrix,
                    enabled = state.dirty && !state.savingMatrix,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
                ) {
                    if (state.savingMatrix) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = AppColors.Surface, strokeWidth = 2.dp)
                    } else {
                        Text("Guardar")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.roles.forEach { r ->
                    val pending = state.changesByRole[r] ?: 0
                    AppFilterChip(
                        label = roleLabel(r) + if (pending > 0) " ($pending)" else "",
                        selected = r == role,
                        onClick = { viewModel.selectRole(r) },
                    )
                }
            }
            if (state.hasInactive) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Hay permisos inactivos: sus alcances no se pueden modificar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Warning,
                )
            }
        }

        if (role == null || state.catalog.isEmpty()) {
            EmptyState("No hay permisos en el catálogo", Modifier.weight(1f))
        } else LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.modules, key = { it.first }) { (module, permissions) ->
                AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                    SectionLabel(module)
                    permissions.forEachIndexed { index, permission ->
                        if (index > 0) HorizontalDivider(color = AppColors.Outline.copy(alpha = 0.35f))
                        MatrixRow(
                            permission = permission,
                            selectedScope = state.scopeFor(role, permission.key),
                            enabled = permission.active && !state.savingMatrix,
                            onSelect = { scope -> viewModel.setScope(role, permission.key, scope) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixRow(
    permission: PermissionCatalogDto,
    selectedScope: String,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        PermissionTitle(permission)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            permission.assignableScopes().forEach { scope ->
                AppFilterChip(
                    label = permissionScopeLabel(scope),
                    selected = scope == selectedScope,
                    enabled = enabled,
                    selectedColor = if (scope == PermissionScopes.NONE) AppColors.TextMuted else AppColors.EmeraldPrimary,
                    onClick = { onSelect(scope) },
                )
            }
        }
    }
}

/** Nombre, clave y marcas (sensible/inactivo) de un permiso. */
@Composable
private fun PermissionTitle(permission: PermissionCatalogDto) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            permission.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (permission.active) AppColors.TextPrimary else AppColors.TextFaint,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (permission.sensitive) StatusChip("Sensible", AppColors.Warning)
        if (!permission.active) StatusChip("Inactivo", AppColors.Danger)
    }
    Text(permission.key, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = AppColors.TextFaint)
}

@Composable
private fun CatalogTab(state: RolesPermissionsUiState, viewModel: RolesPermissionsViewModel) {
    val list = state.filteredCatalog
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${list.size} permiso(s)", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                IconButton(onClick = viewModel::openCreate) {
                    Icon(Icons.Filled.Add, contentDescription = "Nuevo permiso", tint = AppColors.EmeraldPrimary)
                }
            }
            Text(
                "Los permisos no se eliminan, solo se desactivan.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
            )
            Spacer(Modifier.height(10.dp))
            AppSearchField(state.catalogQuery, viewModel::onCatalogQueryChange, "Buscar por clave, nombre o módulo")
        }

        if (list.isEmpty()) {
            EmptyState("No hay permisos con esta búsqueda", Modifier.weight(1f))
        } else LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(list, key = { it.key }) { permission ->
                CatalogCard(
                    permission = permission,
                    toggling = state.togglingKey == permission.key,
                    onEdit = { viewModel.openEdit(permission) },
                    onToggle = { viewModel.toggleActive(permission) },
                )
            }
        }
    }
}

@Composable
private fun CatalogCard(
    permission: PermissionCatalogDto,
    toggling: Boolean,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        PermissionTitle(permission)
        Spacer(Modifier.height(4.dp))
        Text(
            "${permission.module} · Orden ${permission.sortOrder}",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        permission.description?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            permission.scopes.forEach { StatusChip(permissionScopeLabel(it), AppColors.Info) }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onEdit) { Text("Editar", color = AppColors.EmeraldPrimary) }
            TextButton(onClick = onToggle, enabled = !toggling) {
                Text(
                    if (permission.active) "Desactivar" else "Activar",
                    color = if (permission.active) AppColors.Danger else AppColors.Success,
                )
            }
        }
    }
}

@Composable
private fun PermissionFormModal(form: PermissionForm, saving: Boolean, viewModel: RolesPermissionsViewModel) {
    AppModal(
        title = if (form.isEdit) "Editar permiso" else "Nuevo permiso",
        icon = Icons.Filled.AdminPanelSettings,
        onDismiss = viewModel::dismissForm,
        confirmLabel = "Guardar",
        onConfirm = viewModel::submitForm,
        confirmEnabled = form.error == null,
        saving = saving,
    ) {
        Column(
            modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppTextField(
                value = form.key,
                onValueChange = { v -> viewModel.updateForm { it.copy(key = v.lowercase()) } },
                label = { Text("Clave (modulo.accion)") },
                enabled = !form.isEdit,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            AppTextField(
                value = form.module,
                onValueChange = { v -> viewModel.updateForm { it.copy(module = v) } },
                label = { Text("Módulo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            AppTextField(
                value = form.name,
                onValueChange = { v -> viewModel.updateForm { it.copy(name = v) } },
                label = { Text("Nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            AppTextField(
                value = form.description,
                onValueChange = { v -> viewModel.updateForm { it.copy(description = v) } },
                label = { Text("Descripción") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Alcances permitidos", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PermissionScopes.ORDER.forEach { scope ->
                    AppFilterChip(
                        label = permissionScopeLabel(scope),
                        selected = scope in form.scopes,
                        onClick = { viewModel.toggleFormScope(scope) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = form.sensitive, onCheckedChange = { v -> viewModel.updateForm { it.copy(sensitive = v) } })
                Spacer(Modifier.width(4.dp))
                Text("Permiso sensible (solo ADMIN)", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
            }
            AppTextField(
                value = form.sortOrder,
                onValueChange = { v -> viewModel.updateForm { it.copy(sortOrder = v.filter { c -> c.isDigit() }) } },
                label = { Text("Orden") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            form.error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
            }
        }
    }
}
