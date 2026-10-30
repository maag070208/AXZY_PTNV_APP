package com.axzydev.puertonuevoapp.core.ui.table

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.network.http.TableSort
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.util.formatShortDate

/**
 * Pantalla-tabla del estándar de listados (`core/ui/table`). Sirve igual para los
 * endpoints con `POST /<recurso>/query` ([PaginatedTableViewModel]) que para los que
 * solo devuelven la lista completa ([ClientTableViewModel]); la pantalla solo aporta
 * la fila ([itemContent]) y sus especificaciones de filtro/orden.
 *
 * Incluye: conteo de resultados, búsqueda con debounce (filtro [searchKey]), chips de
 * "Filtros"/orden que abren sus modales (con [FilterControl.DateRange] para fechas),
 * badges de filtros activos, lista con paginación infinita (toast al llegar al final)
 * y pull-to-refresh. Estados de carga/vacío/error con [BentoState].
 *
 * El orden por columna se declara en [sortSpecs]; el ViewModel recibe el
 * [TableSort] elegido por [onSortChange] (o `null` para el orden por defecto).
 */
@Composable
fun <T> AppTableScreen(
    state: TableUiState<T>,
    heading: String,
    filterSpecs: List<TableFilterSpec>,
    onSearchChange: (String) -> Unit,
    onFiltersChange: (Map<String, String>) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    searchPlaceholder: String,
    emptyMessage: String,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    searchKey: String? = null,
    sortSpecs: List<TableSortSpec> = emptyList(),
    onSortChange: (TableSort?) -> Unit = {},
    itemContent: @Composable (T) -> Unit,
) {
    var showFilters by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }

    val activeFilters = state.filters.filterKeys { it != searchKey }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$heading · ${state.total} resultados",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (state.refreshing) {
                        Text(
                            "Actualizando…",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextFaint,
                        )
                    }
                }
                if (filterSpecs.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    AppFilterChip(
                        label = if (activeFilters.isEmpty()) "Filtros" else "Filtros (${activeFilters.size})",
                        selected = activeFilters.isNotEmpty(),
                        onClick = { showFilters = true },
                    )
                }
                if (sortSpecs.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    AppFilterChip(
                        label = sortChipLabel(state.sort, sortSpecs),
                        selected = state.sort != null,
                        onClick = { showSort = true },
                    )
                }
            }
            if (searchKey != null) {
                Spacer(Modifier.height(10.dp))
                AppSearchField(
                    value = state.filters[searchKey].orEmpty(),
                    onValueChange = onSearchChange,
                    placeholder = searchPlaceholder,
                )
            }
            if (activeFilters.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    activeFilters.forEach { (key, value) ->
                        ActiveFilterBadge(
                            label = filterLabel(filterSpecs, key, value),
                            onRemove = { onFiltersChange(state.filters - key) },
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                state.loading && state.items.isEmpty() -> BentoState(
                    message = "Cargando…",
                    loading = true,
                    modifier = Modifier.padding(24.dp),
                )
                state.error != null && state.items.isEmpty() -> BentoErrorState(
                    message = state.error,
                    onRetry = onRetry,
                    modifier = Modifier.padding(24.dp),
                )
                state.items.isEmpty() -> BentoState(
                    message = emptyMessage,
                    modifier = Modifier.padding(24.dp),
                )
                else -> AppTableList(
                    state = state,
                    onRefresh = onRefresh,
                    onLoadMore = onLoadMore,
                    itemKey = itemKey,
                    itemContent = itemContent,
                )
            }
        }
    }

    if (showFilters) {
        AppFilterModal(
            specs = filterSpecs,
            values = state.filters,
            onApply = { applied ->
                onFiltersChange(applied)
                showFilters = false
            },
            onDismiss = { showFilters = false },
        )
    }

    if (showSort) {
        AppSortModal(
            specs = sortSpecs,
            current = state.sort,
            onSelect = { selected ->
                onSortChange(selected)
                showSort = false
            },
            onDismiss = { showSort = false },
        )
    }
}

@Composable
private fun <T> AppTableList(
    state: TableUiState<T>,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    itemKey: (T) -> Any,
    itemContent: @Composable (T) -> Unit,
) {
    val listState = rememberLazyListState()

    val atSoftEnd by remember(state.items.size) {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 4
        }
    }

    LaunchedEffect(atSoftEnd, state.items.size, state.hasMore, state.loading, state.loadingMore, state.refreshing, state.error) {
        if (atSoftEnd && state.hasMore && !state.loading && !state.loadingMore && !state.refreshing && state.error == null) {
            onLoadMore()
        }
    }

    var endNotified by remember { mutableStateOf(false) }
    LaunchedEffect(state.filters, state.sort) { endNotified = false }
    LaunchedEffect(state.hasMore) { if (state.hasMore) endNotified = false }
    LaunchedEffect(atSoftEnd, state.items.size, state.hasMore) {
        if (atSoftEnd && !state.hasMore && state.items.isNotEmpty() && !endNotified) {
            val info = listState.layoutInfo
            if (info.totalItemsCount > info.visibleItemsInfo.size) {
                endNotified = true
                AppSnackbar.show("No hay más resultados")
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.items.size, key = { itemKey(state.items[it]) }) { index ->
                itemContent(state.items[index])
            }
            item(key = "footer") {
                if (state.loadingMore) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AppColors.EmeraldPrimary, strokeWidth = 2.dp)
                    }
                } else {
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}

/** Etiqueta del chip de orden: "Ordenar" o "Fecha ↓" cuando hay un orden activo. */
private fun sortChipLabel(sort: TableSort?, specs: List<TableSortSpec>): String {
    if (sort == null) return "Ordenar"
    val spec = specs.firstOrNull { it.key == sort.key } ?: return "Ordenar"
    val arrow = if (sort.direction == "desc") "↓" else "↑"
    return "${spec.label} $arrow"
}

@Composable
private fun AppSortModal(
    specs: List<TableSortSpec>,
    current: TableSort?,
    onSelect: (TableSort?) -> Unit,
    onDismiss: () -> Unit,
) {
    AppModal(
        title = "Ordenar",
        icon = Icons.AutoMirrored.Filled.Sort,
        onDismiss = onDismiss,
        confirmLabel = "Listo",
        onConfirm = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppFilterChip(
                label = "Sin orden",
                selected = current == null,
                onClick = { onSelect(null) },
            )
            specs.forEach { spec ->
                val active = current?.key == spec.key
                val arrow = when {
                    !active -> ""
                    current.direction == "desc" -> " ↓"
                    else -> " ↑"
                }
                AppFilterChip(
                    label = spec.label + arrow,
                    selected = active,
                    // Tocar el mismo campo alterna asc/desc; otro campo arranca asc.
                    onClick = {
                        val next = if (active) {
                            TableSort(spec.key, if (current.direction == "asc") "desc" else "asc")
                        } else {
                            TableSort(spec.key, "asc")
                        }
                        onSelect(next)
                    },
                )
            }
        }
    }
}

@Composable
private fun ActiveFilterBadge(label: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(AppShape.pill)
            .background(AppColors.EmeraldPrimary.copy(alpha = 0.12f), AppShape.pill)
            .clickable(onClick = onRemove)
            .padding(start = 12.dp, top = 6.dp, end = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.EmeraldPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Filled.Close, contentDescription = "Quitar filtro", tint = AppColors.EmeraldPrimary, modifier = Modifier.size(14.dp))
    }
}

/** Etiqueta de un filtro activo; los rangos de fecha se muestran como "desde/hasta". */
private fun filterLabel(specs: List<TableFilterSpec>, key: String, value: String): String {
    val spec = specs.firstOrNull { it.key == key }
    if (spec != null) {
        val displayValue = if (spec.control is FilterControl.Select) {
            spec.control.options.firstOrNull { it.value == value }?.label ?: value
        } else {
            value
        }
        return "${spec.label}: $displayValue"
    }
    val range = specs.firstOrNull {
        it.control is FilterControl.DateRange && (it.fromKey == key || it.toKey == key)
    } ?: return "$key: $value"
    val qualifier = if (range.fromKey == key) "desde" else "hasta"
    return "${range.label} $qualifier: ${formatShortDate(value)}"
}

@Composable
private fun AppFilterModal(
    specs: List<TableFilterSpec>,
    values: Map<String, String>,
    onApply: (Map<String, String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val draft = remember(specs, values) {
        mutableStateMapOf<String, String>().apply {
            specs.forEach { spec ->
                when (spec.control) {
                    FilterControl.Text, is FilterControl.Select -> put(spec.key, values[spec.key].orEmpty())
                    FilterControl.DateRange -> {
                        put(spec.fromKey, values[spec.fromKey].orEmpty())
                        put(spec.toKey, values[spec.toKey].orEmpty())
                    }
                }
            }
        }
    }

    AppModal(
        title = "Filtros",
        icon = Icons.Filled.FilterList,
        onDismiss = onDismiss,
        confirmLabel = "Aplicar",
        onConfirm = { onApply(draft.filterValues { it.isNotBlank() }) },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            specs.forEach { spec ->
                when (val control = spec.control) {
                    FilterControl.Text -> AppTextField(
                        value = draft[spec.key].orEmpty(),
                        onValueChange = { draft[spec.key] = it },
                        label = { Text(spec.label) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    is FilterControl.Select -> SimpleDropdownField(
                        label = spec.label,
                        value = draft[spec.key].orEmpty(),
                        options = listOf("" to "Todos") + control.options.map { it.value to it.label },
                        onSelect = { draft[spec.key] = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FilterControl.DateRange -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(spec.label, style = MaterialTheme.typography.labelSmall, color = AppColors.TextMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppTextField(
                                value = draft[spec.fromKey].orEmpty(),
                                onValueChange = { draft[spec.fromKey] = it },
                                label = { Text("Desde") },
                                placeholder = { Text("AAAA-MM-DD") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            AppTextField(
                                value = draft[spec.toKey].orEmpty(),
                                onValueChange = { draft[spec.toKey] = it },
                                label = { Text("Hasta") },
                                placeholder = { Text("AAAA-MM-DD") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            TextButton(onClick = { draft.keys.toList().forEach { draft[it] = "" } }) {
                Text("Limpiar todo", color = AppColors.TextMuted)
            }
        }
    }
}
