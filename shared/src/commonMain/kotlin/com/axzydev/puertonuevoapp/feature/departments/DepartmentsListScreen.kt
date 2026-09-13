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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.DepartmentDto
import com.axzydev.puertonuevoapp.core.network.DepartmentUpdateDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import kotlinx.coroutines.launch

@Composable
fun DepartmentsListScreen() {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val authState by AppContainer.authRepository.state.collectAsState()
    val isAdmin = (authState as? AuthState.LoggedIn)?.user?.role == "ADMIN"

    var departments by remember { mutableStateOf<List<DepartmentDto>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var actionSaving by remember { mutableStateOf(false) }

    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var editTarget by remember { mutableStateOf<DepartmentDto?>(null) }
    var editName by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<DepartmentDto?>(null) }

    suspend fun load() {
        loading = true
        error = null
        try {
            departments = AppContainer.departmentsApi.list(includeInactive = true).sortedBy { it.name }
        } catch (e: Exception) {
            error = e.message ?: "No se pudieron cargar los departamentos"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    val filtered = departments.filter { d -> query.isBlank() || d.name.contains(query, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Estructura organizacional · ${filtered.size} resultados", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                if (isAdmin) {
                    IconButton(onClick = { newName = ""; showCreate = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Nuevo departamento", tint = AppColors.EmeraldPrimary)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            AppSearchField(query, { query = it }, "Buscar departamento")
        }

        when {
            loading -> LoadingState(modifier = Modifier.weight(1f))
            error != null -> ErrorState(error ?: "Error", Modifier.weight(1f), onRetry = { scope.launch { load() } })
            filtered.isEmpty() -> EmptyState("No hay departamentos con estos filtros", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filtered, key = { it.id }) { d ->
                    DepartmentCard(
                        department = d,
                        isAdmin = isAdmin,
                        onClick = { navigator.push(Screen.DepartmentDetail(d.id)) },
                        onEdit = { editName = d.name; editTarget = d },
                        onDelete = { deleteTarget = d },
                    )
                }
            }
        }
    }

    if (showCreate) {
        AppModal(
            title = "Nuevo departamento",
            icon = Icons.Filled.Business,
            onDismiss = { showCreate = false },
            confirmLabel = if (actionSaving) "Creando…" else "Crear",
            confirmEnabled = newName.isNotBlank(),
            saving = actionSaving,
            onConfirm = {
                scope.launch {
                    actionSaving = true
                    try {
                        AppContainer.departmentsApi.create(newName.trim())
                        showCreate = false
                        load()
                    } catch (e: Exception) {
                        showCreate = false
                        actionError = e.message ?: "No se pudo crear el departamento"
                    } finally {
                        actionSaving = false
                    }
                }
            },
        ) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Nombre del departamento") },
                placeholder = { Text("Ej. RECEPCIÓN") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    editTarget?.let { d ->
        AppModal(
            title = "Editar departamento",
            icon = Icons.Filled.Edit,
            onDismiss = { editTarget = null },
            confirmLabel = if (actionSaving) "Guardando…" else "Guardar",
            confirmEnabled = editName.isNotBlank(),
            saving = actionSaving,
            onConfirm = {
                scope.launch {
                    actionSaving = true
                    try {
                        AppContainer.departmentsApi.update(d.id, DepartmentUpdateDto(name = editName.trim()))
                        editTarget = null
                        load()
                    } catch (e: Exception) {
                        editTarget = null
                        actionError = e.message ?: "No se pudo actualizar el departamento"
                    } finally {
                        actionSaving = false
                    }
                }
            },
        ) {
            OutlinedTextField(
                value = editName,
                onValueChange = { editName = it },
                label = { Text("Nombre del departamento") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    deleteTarget?.let { d ->
        AppModal(
            title = if (d.active) "Eliminar departamento" else "Eliminar definitivamente",
            icon = Icons.Filled.DeleteOutline,
            tone = AppModalTone.Danger,
            onDismiss = { deleteTarget = null },
            confirmLabel = if (d.active) "Eliminar" else "Eliminar definitivamente",
            saving = actionSaving,
            onConfirm = {
                scope.launch {
                    actionSaving = true
                    try {
                        AppContainer.departmentsApi.remove(d.id)
                        deleteTarget = null
                        load()
                    } catch (e: Exception) {
                        deleteTarget = null
                        actionError = e.message ?: "No se pudo eliminar el departamento"
                    } finally {
                        actionSaving = false
                    }
                }
            },
        ) {
            Text(
                if (d.active) "¿Eliminar ${d.name}? Se desactivará; si tiene usuarios asociados no se podrá eliminar."
                else "¿Eliminar definitivamente ${d.name}? Se borrarán sus áreas y se desligará de usuarios y tickets. Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
        }
    }

    actionError?.let { message ->
        AppModal(
            title = "No se pudo completar la acción",
            icon = Icons.Filled.Business,
            tone = AppModalTone.Danger,
            onDismiss = { actionError = null },
        ) { Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted) }
    }
}

@Composable
private fun DepartmentCard(
    department: DepartmentDto,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(8.dp))
            .border(1.dp, AppColors.Outline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
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
            if (isAdmin) {
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = AppColors.TextFaint, modifier = Modifier.size(18.dp)) }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar", tint = AppColors.Danger, modifier = Modifier.size(18.dp)) }
            } else {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Abrir departamento", tint = AppColors.TextFaint, modifier = Modifier.size(22.dp))
            }
        }
    }
}
