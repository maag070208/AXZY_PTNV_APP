package com.axzydev.puertonuevoapp.feature.departments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.AppTextField

@Composable
fun DepartmentsListScreen(viewModel: DepartmentsListViewModel = viewModel { DepartmentsListViewModel(AppContainer.departmentsApi) }) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val canManage = (authState as? AuthState.LoggedIn)?.user?.canManageDepartments == true

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Estructura organizacional · ${state.filtered.size} resultados", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                if (canManage) {
                    IconButton(onClick = viewModel::openCreate) {
                        Icon(Icons.Filled.Add, contentDescription = "Nuevo departamento", tint = AppColors.EmeraldPrimary)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            AppSearchField(state.query, viewModel::onQueryChange, "Buscar departamento")
        }

        when {
            state.loading -> LoadingState(modifier = Modifier.weight(1f))
            state.error != null -> ErrorState(state.error ?: "Error", Modifier.weight(1f), onRetry = viewModel::load)
            state.filtered.isEmpty() -> EmptyState("No hay departamentos con estos filtros", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.filtered, key = { it.id }) { d ->
                    DepartmentCard(
                        department = d,
                        canManage = canManage,
                        onClick = { navigator.push(Screen.DepartmentDetail(d.id)) },
                        onEdit = { viewModel.openEdit(d) },
                        onDelete = { viewModel.requestDelete(d) },
                    )
                }
            }
        }
    }

    if (state.showCreate) {
        AppModal(
            title = "Nuevo departamento",
            icon = Icons.Filled.Business,
            onDismiss = viewModel::dismissCreate,
            confirmLabel = if (state.actionSaving) "Creando…" else "Crear",
            confirmEnabled = state.newName.isNotBlank(),
            saving = state.actionSaving,
            onConfirm = viewModel::submitCreate,
        ) {
            AppTextField(
                value = state.newName,
                onValueChange = viewModel::onNewNameChange,
                label = { Text("Nombre del departamento") },
                placeholder = { Text("Ej. RECEPCIÓN") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    state.editTarget?.let {
        AppModal(
            title = "Editar departamento",
            icon = Icons.Filled.Edit,
            onDismiss = viewModel::dismissEdit,
            confirmLabel = if (state.actionSaving) "Guardando…" else "Guardar",
            confirmEnabled = state.editName.isNotBlank(),
            saving = state.actionSaving,
            onConfirm = viewModel::submitEdit,
        ) {
            AppTextField(
                value = state.editName,
                onValueChange = viewModel::onEditNameChange,
                label = { Text("Nombre del departamento") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    state.deleteTarget?.let { d ->
        AppModal(
            title = if (d.active) "Eliminar departamento" else "Eliminar definitivamente",
            icon = Icons.Filled.DeleteOutline,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissDelete,
            confirmLabel = if (d.active) "Eliminar" else "Eliminar definitivamente",
            saving = state.actionSaving,
            onConfirm = viewModel::confirmDelete,
        ) {
            Text(
                if (d.active) "¿Eliminar ${d.name}? Se desactivará; si tiene usuarios asociados no se podrá eliminar."
                else "¿Eliminar definitivamente ${d.name}? Se borrarán sus áreas y se desligará de usuarios y tickets. Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
        }
    }

    state.actionError?.let { message ->
        AppModal(
            title = "No se pudo completar la acción",
            icon = Icons.Filled.Business,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissActionError,
        ) { Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted) }
    }
}

@Composable
private fun DepartmentCard(
    department: DepartmentDto,
    canManage: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        borderColor = AppColors.Outline,
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).background(AppColors.EmeraldPrimary, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Business, contentDescription = null, tint = AppColors.Surface, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(department.name, style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
                Text(
                    "${department.subareas.size} área(s) · ${department.count?.users ?: 0} usuario(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                )
            }
            if (!department.active) {
                Text(
                    "INACTIVO",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Danger,
                    modifier = Modifier
                        .background(AppColors.Danger.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
                Spacer(Modifier.size(8.dp))
            }
            if (canManage) {
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = AppColors.TextFaint, modifier = Modifier.size(18.dp)) }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar", tint = AppColors.Danger, modifier = Modifier.size(18.dp)) }
            } else {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Abrir departamento", tint = AppColors.TextFaint, modifier = Modifier.size(22.dp))
            }
        }
    }
}
