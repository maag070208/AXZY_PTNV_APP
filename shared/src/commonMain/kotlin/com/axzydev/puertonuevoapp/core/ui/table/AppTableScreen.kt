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
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.RegisterTopBarAction
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField

/**
 * Pantalla-tabla genérica: búsqueda, filtros en modal (icono en el TopAppBar
 * del shell), badges de filtros activos, lista con paginación infinita de
 * [TableUiState.pageSize] en [TableUiState.pageSize] (toast al llegar al
 * final) y pull-to-refresh. Reutilizable por cualquier módulo; el módulo solo
 * implementa la fila ([itemContent]) y sus especificaciones de filtro.
 *
 * La caja de búsqueda escribe en el filtro [searchKey] (debounce en el
 * ViewModel): si es `null` no se muestra el buscador.
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
    itemContent: @Composable (T) -> Unit,
) {
    var showFilters by remember { mutableStateOf(false) }

    if (filterSpecs.isNotEmpty()) {
        RegisterTopBarAction(Icons.Filled.FilterList, "Filtros") { showFilters = true }
    }

    val activeFilters = state.filters.filterKeys { it != searchKey }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 14.dp)) {
            Text(
                text = "$heading · ${state.total} resultados",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
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
                            label = badgeLabel(filterSpecs, key, value),
                            onRemove = { onFiltersChange(state.filters - key) },
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.loading && state.items.isEmpty() -> LoadingState()
                state.error != null && state.items.isEmpty() -> ErrorState(state.error, onRetry = onRetry)
                state.items.isEmpty() -> EmptyState(emptyMessage)
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
    LaunchedEffect(state.filters) { endNotified = false }
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

private fun badgeLabel(specs: List<TableFilterSpec>, key: String, value: String): String {
    val spec = specs.firstOrNull { it.key == key }
        ?: return "$key: $value"
    val displayValue = if (spec.control is FilterControl.Select) {
        (spec.control as FilterControl.Select).options.firstOrNull { it.value == value }?.label ?: value
    } else {
        value
    }
    return "${spec.label}: $displayValue"
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
            specs.forEach { spec -> put(spec.key, values[spec.key].orEmpty()) }
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
                }
            }
            TextButton(onClick = { specs.forEach { spec -> draft[spec.key] = "" } }) {
                Text("Limpiar todo", color = AppColors.TextMuted)
            }
        }
    }
}