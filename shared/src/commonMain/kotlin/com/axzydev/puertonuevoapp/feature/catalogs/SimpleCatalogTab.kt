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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
 * Pestaña genérica de un catálogo simple: lista con activos e inactivos, alta y
 * edición en `AppModal`, activar/desactivar y baja con confirmación.
 *
 * Solo se muestran las acciones de escritura con `catalogs.manage`; la lectura
 * de los catálogos de RH se oculta sin `hr.records` (lo exige el API).
 */
@Composable
internal fun SimpleCatalogTab(
    tab: CatalogsTab,
    source: SimpleCatalogSource,
    user: SessionUser,
    modifier: Modifier = Modifier,
) {
    val viewModel: SimpleCatalogViewModel = viewModel(key = "catalog-${tab.name}") {
        SimpleCatalogViewModel(source)
    }
    val state by viewModel.uiState.collectAsState()
    val canManage = user.can(PermissionKeys.CATALOGS_MANAGE)
    val denied = source.viewPermission?.let { !user.can(it) } == true

    if (denied) {
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            BentoState(
                message = source.deniedMessage,
                tone = BentoTone.Sun,
                icon = Icons.Filled.Lock,
            )
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${state.items.size} en el catálogo",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                )
                if (canManage) {
                    BentoIconButton(
                        icon = Icons.Filled.Add,
                        contentDescription = "Nuevo ${source.singular}",
                        onClick = viewModel::openCreate,
                    )
                }
            }
            state.actionError?.let { message ->
                Spacer(Modifier.height(6.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
            }
        }

        when {
            state.loading && state.items.isEmpty() -> BentoState(
                message = "Cargando ${source.label.lowercase()}…",
                loading = true,
                modifier = Modifier.padding(16.dp),
            )
            state.error != null && state.items.isEmpty() -> BentoErrorState(
                message = state.error ?: "No se pudo cargar el catálogo",
                onRetry = viewModel::load,
                modifier = Modifier.padding(16.dp),
            )
            state.items.isEmpty() -> BentoState(
                message = source.emptyMessage,
                modifier = Modifier.padding(16.dp),
            )
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.items, key = { it.id }) { item ->
                    SimpleCatalogRow(
                        item = item,
                        canManage = canManage,
                        onEdit = { viewModel.openEdit(item) },
                        onToggleActive = { viewModel.toggleActive(item) },
                        onDelete = { viewModel.requestDelete(item) },
                    )
                }
            }
        }
    }

    if (state.showForm) {
        val editing = state.editing != null
        AppModal(
            title = if (editing) "Editar ${source.singular}" else "Nuevo ${source.singular}",
            icon = if (editing) Icons.Filled.Edit else Icons.Filled.Add,
            onDismiss = viewModel::dismissForm,
            confirmLabel = "Guardar",
            onConfirm = viewModel::save,
            confirmEnabled = state.canSave,
            saving = state.saving,
        ) {
            AppTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            state.formError?.let { message ->
                Spacer(Modifier.height(10.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
            }
        }
    }

    state.deleteTarget?.let { target ->
        AppModal(
            title = "Eliminar ${source.singular}",
            icon = Icons.Filled.DeleteOutline,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissDelete,
            confirmLabel = "Eliminar",
            onConfirm = viewModel::confirmDelete,
            saving = state.deleting,
        ) {
            Text(
                "¿Eliminar «${target.name}»? Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
            state.actionError?.let { message ->
                Spacer(Modifier.height(10.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
            }
        }
    }
}

@Composable
private fun SimpleCatalogRow(
    item: SimpleCatalogEntry,
    canManage: Boolean,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            StatusChip(
                label = if (item.active) "Activo" else "Inactivo",
                tone = if (item.active) BentoTone.Mint else BentoTone.Rose,
            )
            if (canManage) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = AppColors.EmeraldPrimary)
                }
                IconButton(onClick = onToggleActive) {
                    Icon(
                        imageVector = if (item.active) Icons.Filled.VisibilityOff else Icons.Filled.RestartAlt,
                        contentDescription = if (item.active) "Desactivar" else "Activar",
                        tint = if (item.active) AppColors.Warning else AppColors.Success,
                    )
                }
                if (!item.active) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar", tint = AppColors.Danger)
                    }
                }
            }
        }
    }
}
