package com.axzydev.puertonuevoapp.feature.departments

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoIconButton
import com.axzydev.puertonuevoapp.core.ui.BentoMiniStat
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.RegisterTopBarAction
import com.axzydev.puertonuevoapp.core.ui.StatusChip

/**
 * Departamentos (estructura organizacional): franja de 3 indicadores, búsqueda
 * y filtro por estado en una fila, y tarjetas con conteo de personas, subáreas
 * y acciones de edición/eliminación (o acceso al detalle sin permiso).
 */
@Composable
fun DepartmentsListScreen(
    viewModel: DepartmentsListViewModel = viewModel { DepartmentsListViewModel(AppContainer.departmentsApi) },
) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val canManage = (authState as? AuthState.LoggedIn)?.user?.canManageDepartments == true

    if (canManage) {
        RegisterTopBarAction(Icons.Filled.Add, "Nuevo departamento", viewModel::openCreate)
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBento.canvas)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BentoMiniStat(
                label = "Departamentos",
                value = state.total.toString(),
                tone = BentoTone.Sea,
                modifier = Modifier.weight(1f),
            )
            BentoMiniStat(
                label = "Personas",
                value = state.peopleCount.toString(),
                tone = BentoTone.Lavender,
                modifier = Modifier.weight(1f),
            )
            BentoMiniStat(
                label = "Inactivos",
                value = state.inactiveCount.toString(),
                tone = if (state.inactiveCount == 0) BentoTone.Plain else BentoTone.Rose,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(4.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 10.dp)) {
            AppSearchField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Buscar departamento",
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DepartmentStatusFilter.entries.forEach { filter ->
                    AppFilterChip(
                        label = filter.label,
                        selected = state.status == filter,
                        onClick = { viewModel.setStatus(filter) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${state.filtered.size} de ${state.total} departamentos",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
            )
            Spacer(Modifier.height(8.dp))
        }

        when {
            state.loading -> LoadingState(modifier = Modifier.weight(1f))
            state.error != null -> ErrorState(state.error ?: "Error", Modifier.weight(1f), onRetry = viewModel::load)
            state.filtered.isEmpty() -> EmptyState("No hay departamentos con estos filtros", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 2.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.filtered, key = { it.id }) { department ->
                    DepartmentCard(
                        department = department,
                        canManage = canManage,
                        onClick = { navigator.push(Screen.DepartmentDetail(department.id)) },
                        onEdit = { viewModel.openEdit(department) },
                        onDelete = { viewModel.requestDelete(department) },
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

    state.deleteTarget?.let { department ->
        AppModal(
            title = if (department.active) "Eliminar departamento" else "Eliminar definitivamente",
            icon = Icons.Filled.DeleteOutline,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissDelete,
            confirmLabel = if (department.active) "Eliminar" else "Eliminar definitivamente",
            saving = state.actionSaving,
            onConfirm = viewModel::confirmDelete,
        ) {
            Text(
                if (department.active) {
                    "¿Eliminar ${department.name}? Se desactivará; si tiene usuarios asociados no se podrá eliminar."
                } else {
                    "¿Eliminar definitivamente ${department.name}? Se borrarán sus áreas y se desligará de usuarios y tickets. Esta acción no se puede deshacer."
                },
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

/**
 * Tarjeta de departamento: ícono, nombre, chip de estado, conteo de personas y
 * subáreas en chips cortos, y acciones (editar/eliminar) o acceso al detalle.
 */
@Composable
private fun DepartmentCard(
    department: DepartmentDto,
    canManage: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val users = department.count?.users ?: 0
    val subareas = department.subareas.size
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(AppColors.EmeraldContainer, AppShape.icon),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Business,
                    contentDescription = null,
                    tint = AppColors.EmeraldPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        department.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (!department.active) {
                        Spacer(Modifier.width(6.dp))
                        StatusChip("Inactivo", BentoTone.Rose)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip(
                        label = if (users == 1) "1 persona" else "$users personas",
                        tone = if (users == 0) BentoTone.Plain else BentoTone.Sea,
                    )
                    StatusChip(
                        label = if (subareas == 1) "1 subárea" else "$subareas subáreas",
                        tone = if (subareas == 0) BentoTone.Plain else BentoTone.Lavender,
                    )
                }
                if (department.subareas.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        department.subareas.joinToString(" · ") { it.name },
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextFaint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (canManage) {
                Spacer(Modifier.width(4.dp))
                BentoIconButton(
                    icon = Icons.Filled.Edit,
                    contentDescription = "Editar ${department.name}",
                    onClick = onEdit,
                )
                BentoIconButton(
                    icon = Icons.Filled.DeleteOutline,
                    contentDescription = "Eliminar ${department.name}",
                    onClick = onDelete,
                )
            } else {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Abrir ${department.name}",
                    tint = AppColors.TextFaint,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
