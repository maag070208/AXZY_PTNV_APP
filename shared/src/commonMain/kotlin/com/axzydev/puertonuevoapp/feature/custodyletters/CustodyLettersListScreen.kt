package com.axzydev.puertonuevoapp.feature.custodyletters

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
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
import com.axzydev.puertonuevoapp.core.network.custodyletters.CustodyLetterDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.BentoAvatar
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoIconButton
import com.axzydev.puertonuevoapp.core.ui.BentoMiniStat
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatShortDate

/**
 * Cartas responsivas: listado con folio protagonista, persona (avatar), fecha,
 * estado y cuántos equipos cubre. Encabezado con la acción primaria (nueva
 * carta) y filtros de estado; generar por tipo queda como acción secundaria de
 * ícono. Estados con `BentoState`.
 */
@Composable
fun CustodyLettersListScreen(viewModel: CustodyLettersListViewModel = viewModel { CustodyLettersListViewModel(AppContainer.custodyLettersApi) }) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canCreate = user?.canCreateCustodyLetters == true
    val canDelete = user?.canDeleteCustodyLetters == true
    val canGenerate = user?.canGenerateCustodyLetters == true

    val letters = state.custodyLetters
    val activeCount = letters.count { it.returnDate == null }
    val returnedCount = letters.count { it.returnDate != null }
    val equipmentCount = letters.sumOf { it.items.size }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp)) {
            // Encabezado: descripción + acción primaria (y generar por tipo como ícono).
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Altas y devoluciones de equipo con carta firmada",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextMuted,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (canGenerate) {
                    BentoIconButton(
                        icon = Icons.Filled.Bolt,
                        contentDescription = "Generar carta por tipo",
                        onClick = { navigator.push(Screen.GenerateCustodyLetter) },
                    )
                }
                if (canCreate) {
                    Spacer(Modifier.width(10.dp))
                    PillButton(
                        text = "Nueva carta",
                        onClick = { navigator.push(Screen.CustodyLetterForm()) },
                        variant = PillButtonVariant.Secondary,
                        leadingIcon = Icons.Filled.Add,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Franja de indicadores (0 = neutral).
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BentoMiniStat(
                    label = "Cartas",
                    value = letters.size.toString(),
                    tone = BentoTone.Sea,
                    modifier = Modifier.weight(1f),
                )
                BentoMiniStat(
                    label = "Vigentes",
                    value = activeCount.toString(),
                    tone = if (activeCount > 0) BentoTone.Sun else BentoTone.Plain,
                    modifier = Modifier.weight(1f),
                )
                BentoMiniStat(
                    label = "Devueltas",
                    value = returnedCount.toString(),
                    tone = if (returnedCount > 0) BentoTone.Mint else BentoTone.Plain,
                    modifier = Modifier.weight(1f),
                )
                BentoMiniStat(
                    label = "Equipos",
                    value = equipmentCount.toString(),
                    tone = if (equipmentCount > 0) BentoTone.Lavender else BentoTone.Plain,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))
            AppSearchField(state.query, viewModel::onQueryChange, "Buscar por folio, empleado, descripción…")
            Spacer(Modifier.height(10.dp))

            // Filtros de estado en fila con scroll.
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                custodyLetterStatusFilters.forEach { (value, label) ->
                    AppFilterChip(
                        label = label,
                        selected = state.statusFilter == value,
                        onClick = { viewModel.onStatusFilterChange(value) },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        when {
            state.loading -> Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                BentoState(message = "Cargando cartas responsivas…", loading = true)
            }

            state.error != null -> Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                BentoErrorState(message = state.error ?: "No se pudieron cargar las cartas", onRetry = viewModel::load)
            }

            state.filtered.isEmpty() -> Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                BentoState(
                    message = if (letters.isEmpty()) {
                        "No hay cartas responsivas registradas."
                    } else {
                        "No hay cartas con estos filtros."
                    },
                    tone = BentoTone.Sea,
                    icon = Icons.Filled.Assignment,
                    actionLabel = if (canCreate && letters.isEmpty()) "Nueva carta" else "Reintentar",
                    onAction = if (canCreate && letters.isEmpty()) {
                        { navigator.push(Screen.CustodyLetterForm()) }
                    } else {
                        viewModel::load
                    },
                )
            }

            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.filtered, key = { it.id }) { c ->
                    CustodyLetterCard(
                        custodyLetter = c,
                        canDelete = canDelete,
                        onClick = { navigator.push(Screen.CustodyLetterDetail(c.id)) },
                        onDelete = { viewModel.requestDelete(c) },
                    )
                }
            }
        }
    }

    state.deleteTarget?.let { c ->
        AppModal(
            title = "Eliminar carta",
            icon = Icons.Filled.DeleteOutline,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissDelete,
            confirmLabel = "Eliminar",
            saving = state.actionSaving,
            onConfirm = viewModel::confirmDelete,
        ) {
            Text("¿Eliminar la carta ${c.consecutive} del historial? Esta acción no se puede deshacer.", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
        }
    }

    state.actionError?.let { message ->
        AppModal(
            title = "No se pudo completar la acción",
            icon = Icons.Filled.ErrorOutline,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissActionError,
        ) { Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted) }
    }
}

private val custodyLetterStatusFilters: List<Pair<String, String>> = listOf(
    "" to "Todas",
    "ACTIVE" to "Vigentes",
    "RETURNED" to "Devueltas",
)

/** Una carta: folio, persona con avatar, estado, fecha y equipos que cubre. */
@Composable
private fun CustodyLetterCard(
    custodyLetter: CustodyLetterDto,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val person = custodyLetter.custodian?.name
        ?: custodyLetter.employeeNumber.takeIf { it.isNotBlank() }?.let { "No. empleado $it" }
        ?: "Sin responsable"
    val returned = custodyLetter.returnDate != null
    val equipmentCount = custodyLetter.items.size
    val firstItem = custodyLetter.items.firstOrNull()
    val equipmentLine = listOfNotNull(
        firstItem?.description ?: firstItem?.assetTag,
        if (equipmentCount > 1) "y ${equipmentCount - 1} más" else null,
    ).joinToString(" · ").ifBlank { "Sin recurso asignado" }

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        borderColor = AppColors.Outline.copy(alpha = 0.5f),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            BentoAvatar(name = person, color = AppColors.Info, size = 44.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        custodyLetter.consecutive,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusChip(
                        label = if (returned) "Devuelta" else "Vigente",
                        tone = if (returned) BentoTone.Mint else BentoTone.Sun,
                    )
                }
                Text(
                    person,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        custodyLetter.department.takeIf { it.isNotBlank() },
                        if (equipmentCount == 1) "1 equipo" else "$equipmentCount equipos",
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    equipmentLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatShortDate(custodyLetter.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                )
                if (canDelete) {
                    Spacer(Modifier.height(2.dp))
                    BentoIconButton(
                        icon = Icons.Filled.DeleteOutline,
                        contentDescription = "Eliminar la carta ${custodyLetter.consecutive}",
                        onClick = onDelete,
                    )
                }
            }
        }
    }
}
