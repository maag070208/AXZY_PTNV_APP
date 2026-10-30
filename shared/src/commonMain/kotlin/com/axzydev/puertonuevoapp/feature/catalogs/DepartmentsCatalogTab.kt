package com.axzydev.puertonuevoapp.feature.catalogs

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentDto
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.session.SessionUser
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoIconButton
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.StatusChip

/**
 * Pestaña Departamentos/Subáreas: el CRUD que la web reparte en dos pestañas
 * (Departamentos y Subáreas) aquí se ve junto, porque una subárea siempre
 * pertenece a un departamento. La lectura es de sesión; escribir exige
 * `departments.manage`.
 */
@Composable
internal fun DepartmentsCatalogTab(
    user: SessionUser,
    modifier: Modifier = Modifier,
) {
    val viewModel: DepartmentsCatalogViewModel = viewModel {
        DepartmentsCatalogViewModel(AppContainer.departmentsApi)
    }
    val state by viewModel.uiState.collectAsState()
    val canManage = user.can(PermissionKeys.DEPARTMENTS_MANAGE)

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${state.departments.size} departamento(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                )
                if (canManage) {
                    BentoIconButton(
                        icon = Icons.Filled.Add,
                        contentDescription = "Nuevo departamento",
                        onClick = viewModel::openCreate,
                    )
                }
            }
            if (!canManage) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Solo lectura: se requiere el permiso de administrar departamentos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextFaint,
                )
            }
            state.actionError?.let { message ->
                Spacer(Modifier.height(6.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
            }
        }

        when {
            state.loading && state.departments.isEmpty() -> BentoState(
                message = "Cargando departamentos…",
                loading = true,
                modifier = Modifier.padding(16.dp),
            )
            state.error != null && state.departments.isEmpty() -> BentoErrorState(
                message = state.error ?: "No se pudieron cargar los departamentos",
                onRetry = viewModel::load,
                modifier = Modifier.padding(16.dp),
            )
            state.departments.isEmpty() -> BentoState(
                message = "Aún no hay departamentos.",
                modifier = Modifier.padding(16.dp),
            )
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.departments, key = { it.id }) { department ->
                    DepartmentCatalogRow(
                        department = department,
                        canManage = canManage,
                        onSubareas = { viewModel.openSubareas(department) },
                        onEdit = { viewModel.openEdit(department) },
                        onDelete = { viewModel.requestDelete(department) },
                    )
                }
            }
        }
    }

    if (state.showForm) {
        val editing = state.editing != null
        AppModal(
            title = if (editing) "Editar departamento" else "Nuevo departamento",
            icon = if (editing) Icons.Filled.Edit else Icons.Filled.Add,
            onDismiss = viewModel::dismissForm,
            confirmLabel = "Guardar",
            onConfirm = viewModel::saveDepartment,
            confirmEnabled = state.canSaveForm,
            saving = state.saving,
        ) {
            AppTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nombre del departamento") },
                placeholder = { Text("Ej. RECEPCIÓN") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (editing) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Activo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = state.formActive, onCheckedChange = viewModel::onFormActiveChange)
                }
            }
            state.formError?.let { message ->
                Spacer(Modifier.height(10.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
            }
        }
    }

    state.deleteTarget?.let { target ->
        AppModal(
            title = if (target.active) "Desactivar departamento" else "Eliminar departamento",
            icon = Icons.Filled.DeleteOutline,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissDelete,
            confirmLabel = if (target.active) "Desactivar" else "Eliminar",
            onConfirm = viewModel::confirmDelete,
            saving = state.deleting,
        ) {
            Text(
                if (target.active) {
                    "«${target.name}» se desactivará (no se podrá asignar a personas nuevas). Una segunda baja lo elimina del todo."
                } else {
                    "«${target.name}» ya está inactivo: esta baja lo elimina definitivamente."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
            state.actionError?.let { message ->
                Spacer(Modifier.height(10.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
            }
        }
    }

    state.subareasOf?.let { department ->
        AppModal(
            title = "Subáreas de ${department.name}",
            icon = Icons.Filled.AccountTree,
            onDismiss = viewModel::closeSubareas,
        ) {
            Text(
                "Agrega o quita las áreas de este departamento. Los cambios se ven de inmediato.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppTextField(
                    value = state.subareaName,
                    onValueChange = viewModel::onSubareaNameChange,
                    label = { Text("Nueva subárea") },
                    placeholder = { Text("Ej. MOSTRADOR") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    enabled = canManage && !state.subareaSaving,
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = viewModel::addSubarea,
                    enabled = canManage && state.canAddSubarea,
                ) {
                    if (state.subareaSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = AppColors.EmeraldPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Filled.Add, contentDescription = "Agregar subárea", tint = AppColors.EmeraldPrimary)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (department.subareas.isEmpty()) {
                Text(
                    "Este departamento aún no tiene subáreas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextFaint,
                )
            } else {
                department.subareas.forEach { subarea ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            subarea.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (!subarea.active) {
                            StatusChip(label = "Inactiva", tone = BentoTone.Rose)
                            Spacer(Modifier.width(4.dp))
                        }
                        if (canManage) {
                            IconButton(
                                onClick = { viewModel.removeSubarea(subarea) },
                                enabled = !state.subareaSaving,
                            ) {
                                Icon(
                                    Icons.Filled.DeleteOutline,
                                    contentDescription = "Quitar ${subarea.name}",
                                    tint = AppColors.Danger,
                                )
                            }
                        }
                    }
                }
            }
            state.actionError?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = viewModel::closeSubareas) {
                    Text("Cerrar", color = AppColors.TextMuted)
                }
            }
        }
    }
}

@Composable
private fun DepartmentCatalogRow(
    department: DepartmentDto,
    canManage: Boolean,
    onSubareas: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    department.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${department.subareas.size} área(s) · ${department.count?.users ?: 0} usuario(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusChip(
                label = if (department.active) "Activo" else "Inactivo",
                tone = if (department.active) BentoTone.Mint else BentoTone.Rose,
            )
            IconButton(onClick = onSubareas) {
                Icon(Icons.Filled.AccountTree, contentDescription = "Subáreas", tint = AppColors.Info)
            }
            if (canManage) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar departamento", tint = AppColors.EmeraldPrimary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Dar de baja departamento", tint = AppColors.Danger)
                }
            }
        }
    }
}
