package com.axzydev.puertonuevoapp.feature.custodyletters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.inventory.LoanItemDto
import com.axzydev.puertonuevoapp.core.network.inventory.LoanReturnDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppFilterChip
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatDateTime
import com.axzydev.puertonuevoapp.core.util.formatShortDate

/**
 * Vista de la carta responsiva en pantalla (el PDF se genera client-side en la
 * web; aquí se muestra la carta) con **devolución parcial por pieza y
 * condición**: se eligen las piezas que regresan, su condición
 * (BUENO/ACEPTABLE/MALO/ROTO; roto da de baja la pieza en el API) y una nota
 * cuando hay daño. La devolución se registra con `POST /inventory/returns`.
 *
 * «Deshacer devolución» **no** se ofrece: el API no tiene endpoint para ello
 * (la app respondía 501). Se documenta aquí en lugar de inventarlo.
 */
@Composable
fun CustodyLetterDetailScreen(custodyLetterId: String) {
    val viewModel: CustodyLetterDetailViewModel = viewModel(key = "custody-letter-$custodyLetterId") {
        CustodyLetterDetailViewModel(custodyLetterId, AppContainer.custodyLettersApi, AppContainer.inventoryApi)
    }
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    // Editar y registrar/cancelar devoluciones: `loans.edit`; eliminar: `loans.delete`.
    val canEdit = user?.canEditCustodyLetters == true
    val canDelete = user?.canDeleteCustodyLetters == true

    LaunchedEffect(state.deleted) { if (state.deleted) navigator.pop() }

    if (state.loading) {
        LoadingState(modifier = Modifier.fillMaxSize())
        return
    }
    val c = state.custodyLetter
    if (c == null) {
        ErrorState(state.error ?: "Carta no encontrada", Modifier.fillMaxSize(), onRetry = viewModel::load)
        return
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (canEdit || canDelete) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canEdit) {
                    PillButton(
                        text = "Editar",
                        onClick = { navigator.push(Screen.CustodyLetterForm(c.id)) },
                        variant = PillButtonVariant.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (canDelete) {
                    PillButton(
                        text = "Eliminar",
                        onClick = viewModel::requestDelete,
                        variant = PillButtonVariant.Ghost,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        state.successMessage?.let { message ->
            Spacer(Modifier.height(10.dp))
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = AppColors.Success.copy(alpha = 0.10f),
                borderColor = AppColors.Success.copy(alpha = 0.35f),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = AppColors.Success,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Devoluciones registradas (pieza por pieza) y acción de devolver.
        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Devoluciones")
            val returns = state.returns
            if (returns.isEmpty()) {
                if (c.returnDate != null) {
                    // Carta antigua (modelo previo): solo se conoce la fecha de devolución.
                    Text(
                        "Devuelta el ${formatShortDate(c.returnDate)}" +
                            (c.returnedBy?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                    )
                } else {
                    Text(
                        "Sin devoluciones registradas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                    )
                }
            } else {
                returns.forEach { returned ->
                    ReturnHistoryRow(returned)
                    Spacer(Modifier.height(6.dp))
                }
            }

            if (state.hasPending) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Pendiente por devolver: ${state.pendingItems.sumOf { it.pending }} pieza(s).",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                )
                if (canEdit) {
                    Spacer(Modifier.height(8.dp))
                    PillButton(
                        text = "Registrar devolución",
                        onClick = viewModel::openReturn,
                        variant = PillButtonVariant.Primary,
                        leadingIcon = Icons.Filled.Undo,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Renglones pendientes: qué piezas siguen sin regresar.
        if (state.hasPending) {
            AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                SectionLabel("Piezas pendientes")
                state.pendingItems.forEach { item ->
                    PendingItemRow(item)
                    Spacer(Modifier.height(6.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    c.consecutive,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = AppColors.TextPrimary,
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text("Fecha: ${formatShortDate(c.date)}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                    Text("No. empleado: ${c.employeeNumber.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Recibí autorización de uso y/o acceso al siguiente Recurso de TIC (Tecnología de la Información y la Comunicación) propiedad de ${c.company ?: "Puerto Nuevo Hotel y Villas"}, siendo éste para uso exclusivo de las actividades laborales de la empresa, por lo que tomo responsabilidad sobre el mismo y me comprometo a no divulgarlo, no facilitar su acceso, no utilizarlo en perjuicio de la empresa, devolverlo cuando sea solicitado y prevenir su robo o daño.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )

            Spacer(Modifier.height(14.dp))
            SectionLabel("Recurso TIC")
            val items = state.loan?.items.orEmpty().ifEmpty { null }
            if (items != null) {
                items.forEach { item ->
                    EquipmentBlock(item)
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                // Respaldo: el modelo de la app (una pieza por carta).
                val item = c.items.firstOrNull()
                InfoRow("Descripción", item?.description ?: "—")
                InfoRow("Marca", item?.brand ?: "—")
                InfoRow("Modelo", item?.model ?: "—")
                InfoRow("Control de activos", item?.assetTag ?: "—")
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(c.custodian?.name ?: "—", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                    Text("Responsable", style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(c.supervisor?.name ?: "—", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                    Text("Encargado del área", style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (state.showDeleteConfirm) {
        AppModal(
            title = "Eliminar carta",
            icon = Icons.Filled.DeleteOutline,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissDelete,
            confirmLabel = "Eliminar",
            saving = state.actionSaving,
            onConfirm = viewModel::confirmDelete,
        ) {
            Text(
                "¿Eliminar la carta ${c.consecutive} del historial? Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
        }
    }

    if (state.showReturnModal) {
        AppModal(
            title = "Registrar devolución",
            icon = Icons.Filled.Undo,
            onDismiss = viewModel::dismissReturn,
            confirmLabel = if (state.pickedCount == 0) "Registrar devolución" else "Registrar devolución (${state.pickedCount})",
            confirmEnabled = state.pickedCount > 0 && !state.actionSaving,
            saving = state.actionSaving,
            onConfirm = viewModel::confirmReturn,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "Marca las piezas que regresan y la condición de cada una. Roto da de baja la pieza automáticamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                )
                Spacer(Modifier.height(10.dp))

                state.returnPicks.withIndex().groupBy { it.value.loanItemId }.forEach { (_, entries) ->
                    val first = entries.first().value
                    Text(
                        first.deviceName.ifBlank { "Renglón del préstamo" } +
                            " · pendientes ${first.pending}",
                        style = MaterialTheme.typography.titleSmall,
                        color = AppColors.TextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    entries.forEach { indexed ->
                        ReturnPickEditor(
                            index = indexed.index,
                            row = indexed.value,
                            onToggle = viewModel::togglePick,
                            onCondition = viewModel::setCondition,
                            onNotes = viewModel::setNotes,
                            onQuantity = viewModel::setQuantity,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                AppTextField(
                    value = state.returnNotes,
                    onValueChange = viewModel::setReturnNotes,
                    label = { Text("Nota general de la devolución (opcional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )

                state.validationError?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
                }
            }
        }
    }

    state.actionError?.let { message ->
        AppModal(
            title = "No se pudo completar la acción",
            icon = Icons.Filled.ErrorOutline,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissActionError,
        ) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
        }
    }
}

// ---------------------------------------------------------------------------
// Bloques de la carta
// ---------------------------------------------------------------------------

@Composable
private fun ReturnHistoryRow(returned: LoanReturnDto) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = AppColors.SurfaceVariant,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                returned.number.ifBlank { "Devolución" },
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(formatDateTime(returned.date), style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
        }
        returned.items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    item.device?.name ?: "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextMuted,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(label = "×${item.quantity}", tone = BentoTone.Lavender)
                Spacer(Modifier.width(6.dp))
                StatusChip(
                    label = returnConditionChoices.firstOrNull { it.first == item.condition }?.second ?: item.condition,
                    tone = conditionTone(item.condition),
                )
            }
            item.units.mapNotNull { it.deviceUnit }.forEach { unit ->
                Text("  • ${unit.assetTag}", style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
            }
        }
        returned.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Spacer(Modifier.height(2.dp))
            Text(notes, style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
        }
    }
}

@Composable
private fun PendingItemRow(item: LoanItemDto) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = AppColors.SurfaceVariant,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.device?.name ?: "—",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            StatusChip(label = "Pendiente ${item.pending} de ${item.quantity}", tone = BentoTone.Sun)
        }
        item.units.filter { !it.returned }.mapNotNull { it.deviceUnit }.forEach { unit ->
            Text(
                "• ${unit.assetTag}" + (unit.serialNumber?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextMuted,
            )
        }
    }
}

@Composable
private fun EquipmentBlock(item: LoanItemDto) {
    val device = item.device
    InfoRow("Descripción", device?.name ?: "—")
    InfoRow("Marca", device?.brand ?: "—")
    InfoRow("Modelo", device?.model ?: "—")
    device?.code?.takeIf { it.isNotBlank() }?.let { code -> InfoRow("Código", code) }
    item.units.forEach { link ->
        val unit = link.deviceUnit ?: return@forEach
        InfoRow(
            if (link.returned) "Pieza (devuelta)" else "Pieza",
            unit.assetTag + (unit.serialNumber?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint, modifier = Modifier.weight(0.45f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = AppColors.TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.55f))
    }
}

// ---------------------------------------------------------------------------
// Editor de una pieza a devolver
// ---------------------------------------------------------------------------

@Composable
private fun ReturnPickEditor(
    index: Int,
    row: ReturnPickRow,
    onToggle: (Int) -> Unit,
    onCondition: (Int, String) -> Unit,
    onNotes: (Int, String) -> Unit,
    onQuantity: (Int, String) -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = AppColors.Surface,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (row.isQuantityRow) {
                Text(
                    "Cantidad a devolver",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                AppTextField(
                    value = row.quantityText,
                    onValueChange = { value -> onQuantity(index, value) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(96.dp),
                )
            } else {
                AppFilterChip(
                    label = if (row.selected) "Devuelve ${row.assetTag}" else "No devolver ${row.assetTag}",
                    selected = row.selected,
                    onClick = { onToggle(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        row.serialNumber?.takeIf { it.isNotBlank() }?.let { serial ->
            Spacer(Modifier.height(2.dp))
            Text("Serie: $serial", style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
        }
        if (row.included) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                returnConditionChoices.forEach { (value, label) ->
                    AppFilterChip(
                        label = label,
                        selected = row.condition == value,
                        onClick = { onCondition(index, value) },
                        selectedColor = conditionColor(value),
                    )
                }
            }
            if (row.needsNotes) {
                Spacer(Modifier.height(6.dp))
                AppTextField(
                    value = row.notes,
                    onValueChange = { value -> onNotes(index, value) },
                    label = { Text("¿Qué pasó? (obligatorio)") },
                    placeholder = { Text("Daño, falla, motivo…") },
                    minLines = 2,
                    isError = row.notes.trim().length < 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Spacer(Modifier.height(6.dp))
                AppTextField(
                    value = row.notes,
                    onValueChange = { value -> onNotes(index, value) },
                    label = { Text("Observación (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun conditionTone(condition: String): BentoTone = when (condition) {
    "GOOD" -> BentoTone.Mint
    "FAIR" -> BentoTone.Sea
    "POOR" -> BentoTone.Sun
    "BROKEN" -> BentoTone.Rose
    else -> BentoTone.Plain
}

private fun conditionColor(condition: String): androidx.compose.ui.graphics.Color = when (condition) {
    "GOOD" -> AppColors.Success
    "FAIR" -> AppColors.Info
    "POOR" -> AppColors.Warning
    "BROKEN" -> AppColors.Danger
    else -> AppColors.EmeraldPrimary
}
