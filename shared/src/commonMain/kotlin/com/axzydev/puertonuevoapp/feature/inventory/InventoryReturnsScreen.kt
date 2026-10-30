package com.axzydev.puertonuevoapp.feature.inventory

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.inventory.LoanReturnDto
import com.axzydev.puertonuevoapp.core.network.inventory.LoanReturnItemDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.ui.table.AppTableScreen
import com.axzydev.puertonuevoapp.core.util.formatDateTime

/** Etiqueta legible de la condición de una pieza devuelta. */
val returnConditionLabels: Map<String, String> = mapOf(
    "GOOD" to "Bueno",
    "FAIR" to "Aceptable",
    "POOR" to "Malo",
    "BROKEN" to "Roto",
)

/** Color del chip por condición (roto es el único que da de baja la pieza). */
fun returnConditionTone(condition: String): BentoTone = when (condition) {
    "GOOD" -> BentoTone.Mint
    "FAIR" -> BentoTone.Sea
    "POOR" -> BentoTone.Sun
    "BROKEN" -> BentoTone.Rose
    else -> BentoTone.Plain
}

/**
 * Bitácora de devoluciones del inventario (`GET /inventory/returns`): folio,
 * carta y quién devolvió, fecha, renglones con su condición y las piezas que
 * volvieron. La acción de detalle abre el resumen; «Ver carta» navega al
 * detalle del préstamo. Requiere `loans.view`.
 */
@Composable
fun InventoryReturnsScreen(
    viewModel: InventoryReturnsViewModel = viewModel { InventoryReturnsViewModel(AppContainer.inventoryApi) },
) {
    val state by viewModel.uiState.collectAsState()
    val detail by viewModel.detail.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canView = user?.can(PermissionKeys.LOANS_VIEW) == true

    if (!canView) {
        Column(modifier = Modifier.fillMaxSize().background(AppBento.canvas).padding(16.dp)) {
            BentoState(message = "Necesitas el permiso de préstamos (loans.view) para ver las devoluciones.")
        }
        return
    }

    AppTableScreen(
        state = state,
        heading = "Devoluciones",
        filterSpecs = emptyList(),
        onSearchChange = {},
        onFiltersChange = {},
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onRetry = viewModel::retry,
        searchPlaceholder = "",
        emptyMessage = "No hay devoluciones registradas",
        itemKey = { it.id },
        modifier = Modifier.background(AppBento.canvas),
        searchKey = null,
    ) { returned ->
        ReturnCard(
            returned = returned,
            onClick = { viewModel.openDetail(returned) },
            onOpenLetter = { navigator.push(Screen.CustodyLetterDetail(returned.loanId)) },
        )
    }

    detail.selected?.let { returned ->
        AppModal(
            title = "Devolución ${returned.number.ifBlank { returned.id.take(8) }}",
            icon = Icons.Filled.Undo,
            onDismiss = viewModel::closeDetail,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "Carta ${returned.loan?.number ?: "—"} · ${formatDateTime(returned.date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                )
                returned.loan?.custodian?.name?.let { who ->
                    Text("Devuelve: $who", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                }
                returned.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    Spacer(Modifier.height(6.dp))
                    Text(notes, style = MaterialTheme.typography.bodySmall, color = AppColors.TextPrimary)
                }
                Spacer(Modifier.height(12.dp))
                returned.items.forEach { item ->
                    ReturnItemDetail(item)
                    Spacer(Modifier.height(8.dp))
                }
                if (returned.items.any { it.condition == "BROKEN" }) {
                    Text(
                        "Las piezas rotas se dieron de baja automáticamente en el inventario.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Warning,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(4.dp))
                AppFilterChip(
                    label = "Ver carta responsiva",
                    selected = false,
                    onClick = {
                        viewModel.closeDetail()
                        navigator.push(Screen.CustodyLetterDetail(returned.loanId))
                    },
                )
            }
        }
    }
}

@Composable
private fun ReturnCard(
    returned: LoanReturnDto,
    onClick: () -> Unit,
    onOpenLetter: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                returned.number.ifBlank { "Devolución" },
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                formatDateTime(returned.date),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Carta ${returned.loan?.number ?: "—"}",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        val assigned = returned.loan?.custodian?.name ?: returned.loan?.department?.name
        if (!assigned.isNullOrBlank()) {
            Text(assigned, style = MaterialTheme.typography.bodySmall, color = AppColors.TextPrimary)
        }
        Spacer(Modifier.height(8.dp))
        returned.items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    item.device?.name ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                StatusChip(label = "×${item.quantity}", tone = BentoTone.Lavender)
                Spacer(Modifier.width(6.dp))
                StatusChip(
                    label = returnConditionLabels[item.condition] ?: item.condition,
                    tone = returnConditionTone(item.condition),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        AppFilterChip(
            label = "Ver carta",
            selected = false,
            onClick = onOpenLetter,
        )
    }
}

@Composable
private fun ReturnItemDetail(item: LoanReturnItemDto) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = AppColors.SurfaceVariant,
        contentPadding = PaddingValues(10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.device?.name ?: "—",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            StatusChip(label = "×${item.quantity}", tone = BentoTone.Lavender)
            Spacer(Modifier.width(6.dp))
            StatusChip(
                label = returnConditionLabels[item.condition] ?: item.condition,
                tone = returnConditionTone(item.condition),
            )
        }
        item.units.mapNotNull { it.deviceUnit }.forEach { unit ->
            Text(
                "• ${unit.assetTag}" + (unit.serialNumber?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextMuted,
            )
        }
        item.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Spacer(Modifier.height(2.dp))
            Text(notes, style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
        }
    }
}
