package com.axzydev.puertonuevoapp.feature.materialoutputs

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.DeleteOutline
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
import com.axzydev.puertonuevoapp.core.network.materialoutputs.MaterialOutputDto
import com.axzydev.puertonuevoapp.core.network.materialoutputs.materialOutputReasonOptions
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
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
import com.axzydev.puertonuevoapp.core.util.currentTimeMillis
import com.axzydev.puertonuevoapp.core.util.formatIsoUtc
import com.axzydev.puertonuevoapp.core.util.formatShortDate

/**
 * Bitácora de salida de material (F-SIS-0005). Cualquier usuario autenticado
 * puede crear/editar/eliminar renglones (sin restricción de rol en el backend).
 * Encabezado con acción primaria, KPIs compactos y filas con persona (avatar),
 * artículos y motivo. La captura por lote y el reporte en PDF quedan fuera de
 * alcance móvil.
 */
@Composable
fun MaterialOutputsListScreen(viewModel: MaterialOutputsListViewModel = viewModel { MaterialOutputsListViewModel(AppContainer.materialOutputsApi) }) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current

    val outputs = state.materialOutputs
    val currentMonth = formatIsoUtc(currentTimeMillis()).take(7)
    val thisMonth = outputs.count { it.date.take(7) == currentMonth }
    val people = outputs.map { it.userName.trim() }.filter { it.isNotEmpty() }.distinct().size
    val articles = outputs.sumOf { it.quantity.coerceAtLeast(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Material que sale del almacén, con su destino y motivo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextMuted,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(10.dp))
                PillButton(
                    text = "Nueva salida",
                    onClick = { navigator.push(Screen.MaterialOutputForm()) },
                    variant = PillButtonVariant.Secondary,
                    leadingIcon = Icons.Filled.Add,
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BentoMiniStat(
                    label = "Salidas",
                    value = outputs.size.toString(),
                    tone = BentoTone.Sea,
                    modifier = Modifier.weight(1f),
                )
                BentoMiniStat(
                    label = "Este mes",
                    value = thisMonth.toString(),
                    tone = if (thisMonth > 0) BentoTone.Mint else BentoTone.Plain,
                    modifier = Modifier.weight(1f),
                )
                BentoMiniStat(
                    label = "Personas",
                    value = people.toString(),
                    tone = if (people > 0) BentoTone.Lavender else BentoTone.Plain,
                    modifier = Modifier.weight(1f),
                )
                BentoMiniStat(
                    label = "Artículos",
                    value = articles.toString(),
                    tone = if (articles > 0) BentoTone.Sun else BentoTone.Plain,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))
            AppSearchField(state.query, viewModel::onQueryChange, "Buscar por descripción, depto, usuario…")
            Spacer(Modifier.height(10.dp))
        }

        when {
            state.loading -> Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                BentoState(message = "Cargando salidas de material…", loading = true)
            }

            state.error != null -> Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                BentoErrorState(message = state.error ?: "No se pudieron cargar las salidas", onRetry = viewModel::load)
            }

            state.filtered.isEmpty() -> Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                BentoState(
                    message = if (outputs.isEmpty()) "No hay salidas registradas." else "No hay salidas con esta búsqueda.",
                    tone = BentoTone.Sea,
                    icon = Icons.Filled.Assignment,
                    actionLabel = if (outputs.isEmpty()) "Registrar salida" else "Reintentar",
                    onAction = if (outputs.isEmpty()) {
                        { navigator.push(Screen.MaterialOutputForm()) }
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
                items(state.filtered, key = { it.id }) { s ->
                    MaterialOutputCard(
                        materialOutput = s,
                        onClick = { navigator.push(Screen.MaterialOutputForm(s.id)) },
                        onDelete = { viewModel.requestDelete(s) },
                    )
                }
            }
        }
    }

    state.deleteTarget?.let { s ->
        AppModal(
            title = "Eliminar registro",
            icon = Icons.Filled.DeleteOutline,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissDelete,
            confirmLabel = "Eliminar",
            saving = state.actionSaving,
            onConfirm = viewModel::confirmDelete,
        ) {
            Text("¿Eliminar el registro de \"${s.description}\"? Esta acción no se puede deshacer.", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
        }
    }

    state.actionError?.let { message ->
        AppModal(
            title = "No se pudo completar la acción",
            icon = Icons.Filled.Assignment,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissActionError,
        ) { Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted) }
    }
}

/** Una salida: descripción, persona con avatar, cantidad, motivo y destino. */
@Composable
private fun MaterialOutputCard(materialOutput: MaterialOutputDto, onClick: () -> Unit, onDelete: () -> Unit) {
    val brandModel = listOfNotNull(materialOutput.brand, materialOutput.model)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")
    val reasonLabel = materialOutputReasonOptions.firstOrNull { it.first == materialOutput.reason }?.second
        ?.takeIf { it != materialOutputReasonOptions.firstOrNull()?.second }
    val reasonTone = when (materialOutput.reason) {
        "DAMAGED" -> BentoTone.Rose
        "OBSOLETE" -> BentoTone.Sun
        "LOST" -> BentoTone.Rose
        "OTHER" -> BentoTone.Plain
        else -> BentoTone.Plain
    }

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        borderColor = AppColors.Outline.copy(alpha = 0.5f),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            BentoAvatar(name = materialOutput.userName, color = AppColors.Purple, size = 44.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    materialOutput.description,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    materialOutput.userName,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        materialOutput.departmentName.takeIf { it.isNotBlank() },
                        brandModel.takeIf { it.isNotBlank() },
                        materialOutput.project?.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusChip(label = "×${materialOutput.quantity}", tone = BentoTone.Lavender)
                    if (reasonLabel != null) {
                        StatusChip(label = reasonLabel, tone = reasonTone)
                    }
                    materialOutput.device?.assetTag?.takeIf { it.isNotBlank() }?.let { assetTag ->
                        StatusChip(label = assetTag, tone = BentoTone.Sea)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        formatShortDate(materialOutput.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextFaint,
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            BentoIconButton(
                icon = Icons.Filled.DeleteOutline,
                contentDescription = "Eliminar la salida de ${materialOutput.description}",
                onClick = onDelete,
            )
        }
    }
}
