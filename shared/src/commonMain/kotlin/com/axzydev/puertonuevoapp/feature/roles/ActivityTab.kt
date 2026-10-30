package com.axzydev.puertonuevoapp.feature.roles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import com.axzydev.puertonuevoapp.core.network.permissions.ActivityRowDto
import com.axzydev.puertonuevoapp.core.network.permissions.PermissionsApi
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.ui.table.AppTableScreen
import com.axzydev.puertonuevoapp.core.ui.table.FilterControl
import com.axzydev.puertonuevoapp.core.ui.table.FilterOption
import com.axzydev.puertonuevoapp.core.ui.table.PaginatedTableViewModel
import com.axzydev.puertonuevoapp.core.ui.table.TableFilterSpec
import com.axzydev.puertonuevoapp.core.util.formatDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Acciones de `audit_logs` que pertenecen al control de acceso (las que filtra la pestaña). */
private val accessActivityActions: List<Pair<String, String>> = listOf(
    "ROLE_CREATED",
    "ROLE_UPDATED",
    "ROLE_DELETED",
    "ROLE_PERMISSIONS_UPDATED",
    "PERMISSION_CREATED",
    "PERMISSION_UPDATED",
    "POLICY_CREATED",
    "POLICY_UPDATED",
    "POLICY_DELETED",
    "PERMISSION_EXCEPTION_SET",
    "PERMISSION_EXCEPTION_REMOVED",
    "USER_ROLE_ADDED",
    "USER_ROLE_REMOVED",
    "ACCESS_DENIED",
).map { it to activityActionLabel(it) }

/**
 * Bitácora del control de acceso (`POST /permissions/activity/query`):
 * cambios de roles, matriz, catálogo, reglas, excepciones, roles adicionales y
 * accesos denegados. Tabla server-side con filtros de acción y entidad; el
 * detalle muestra los estados anterior/nuevo y los metadatos.
 */
class ActivityTabViewModel(
    private val permissionsApi: PermissionsApi,
) : PaginatedTableViewModel<ActivityRowDto>() {

    override val pageSize: Int = 20

    /** La caja de búsqueda filtra por persona (nombre o usuario). */
    override val searchKey: String? = "actor"

    private val _selected = MutableStateFlow<ActivityRowDto?>(null)
    val selected: StateFlow<ActivityRowDto?> = _selected.asStateFlow()

    override suspend fun fetch(page: Int, limit: Int, filters: Map<String, String>): TableResponse<ActivityRowDto> =
        permissionsApi.activity(TableRequest(page = page, limit = limit, filters = filters))

    fun openDetail(row: ActivityRowDto) {
        _selected.value = row
    }

    fun closeDetail() {
        _selected.value = null
    }
}

@Composable
internal fun ActivityTab(modifier: Modifier = Modifier) {
    val viewModel: ActivityTabViewModel = viewModel { ActivityTabViewModel(AppContainer.permissionsApi) }
    val state by viewModel.uiState.collectAsState()
    val selected by viewModel.selected.collectAsState()

    AppTableScreen(
        state = state,
        heading = "Actividad del control de acceso",
        filterSpecs = listOf(
            TableFilterSpec(
                key = "action",
                label = "Acción",
                control = FilterControl.Select(accessActivityActions.map { FilterOption(it.first, it.second) }),
            ),
            TableFilterSpec("entityId", "Entidad", FilterControl.Text),
        ),
        onSearchChange = viewModel::onSearchChange,
        onFiltersChange = viewModel::onFiltersChange,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onRetry = viewModel::retry,
        searchPlaceholder = "Buscar por persona (nombre o usuario)",
        emptyMessage = "Sin actividad registrada",
        searchKey = "actor",
        itemKey = { it.id },
        modifier = modifier,
    ) { row ->
        ActivityCard(row = row, onClick = { viewModel.openDetail(row) })
    }

    selected?.let { row ->
        AppModal(
            title = "Detalle de actividad",
            icon = Icons.Filled.History,
            onDismiss = viewModel::closeDetail,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(
                    label = activityActionLabel(row.action),
                    tone = if (row.action == "ACCESS_DENIED") BentoTone.Rose else BentoTone.Sea,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    formatDateTime(row.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextFaint,
                )
            }
            Spacer(Modifier.height(10.dp))
            DetailLine("Persona", row.actor.name ?: row.actor.username ?: "Sistema")
            DetailLine("Entidad", "${row.entityType} · ${row.entityId}")
            JsonSection("Estado anterior", row.previousState?.toString())
            JsonSection("Estado nuevo", row.newState?.toString())
            JsonSection("Metadatos", row.metadata?.toString())
        }
    }
}

@Composable
private fun ActivityCard(row: ActivityRowDto, onClick: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(
                        label = activityActionLabel(row.action),
                        tone = if (row.action == "ACCESS_DENIED") BentoTone.Rose else BentoTone.Sea,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        row.actor.name ?: row.actor.username ?: "Sistema",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${row.entityType} · ${row.entityId}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = AppColors.TextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                formatDateTime(row.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
            )
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint, modifier = Modifier.width(90.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun JsonSection(title: String, raw: String?) {
    if (raw.isNullOrBlank()) return
    Spacer(Modifier.height(10.dp))
    SectionLabel(title)
    Spacer(Modifier.height(4.dp))
    Text(
        raw,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = AppColors.TextMuted,
        maxLines = 14,
        overflow = TextOverflow.Ellipsis,
    )
}
