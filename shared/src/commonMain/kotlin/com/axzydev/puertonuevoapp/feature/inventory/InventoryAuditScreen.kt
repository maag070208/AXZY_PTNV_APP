package com.axzydev.puertonuevoapp.feature.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.inventory.InventoryAuditCheckDto
import com.axzydev.puertonuevoapp.core.network.inventory.InventoryAuditRowDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoMiniStat
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatDateTime
import com.axzydev.puertonuevoapp.core.util.formatShortDate

/** Nombre legible de cada una de las 8 reglas del auditor. */
private val AuditCheckLabels: Map<String, String> = mapOf(
    "UNIT_IN_MULTIPLE_OPEN_LOANS" to "Unidades en más de un préstamo abierto",
    "LOAN_ITEM_PENDING_MISMATCH" to "Préstamos cuyo pendiente no coincide con sus unidades",
    "OPEN_LOAN_UNIT_NOT_ON_LOAN" to "Unidades en préstamo abierto que no están prestadas",
    "ON_LOAN_UNIT_WITHOUT_LOAN" to "Unidades prestadas sin préstamo abierto",
    "CLOSED_LOAN_WITH_OPEN_UNITS" to "Préstamos cerrados con unidades sin devolver",
    "LOAN_STATUS_MISMATCH" to "Préstamos con estado incorrecto",
    "MOVEMENT_UNITS_MISMATCH" to "Movimientos que dicen tener otra cantidad de piezas",
    "LEDGER_MISMATCH" to "Dispositivos cuyo kardex no coincide con lo disponible",
)

/** Cómo se corrige cada regla (lo que no se resuelve desde esta pantalla). */
private val AuditFixHints: Map<String, String> = mapOf(
    "UNIT_IN_MULTIPLE_OPEN_LOANS" to "Revisa las cartas de esas unidades y devuelve la que sobre.",
    "LOAN_ITEM_PENDING_MISMATCH" to "Revisa el detalle de esas cartas.",
    "OPEN_LOAN_UNIT_NOT_ON_LOAN" to "La unidad cambió de estado sin devolverse: registra su devolución o revisa su historial.",
    "ON_LOAN_UNIT_WITHOUT_LOAN" to "La unidad figura prestada sin carta: revisa su historial.",
    "CLOSED_LOAN_WITH_OPEN_UNITS" to "Revisa esas cartas cerradas.",
    "LOAN_STATUS_MISMATCH" to "El estado de la carta no corresponde a sus devoluciones.",
    "MOVEMENT_UNITS_MISMATCH" to "Ábrelo y decide cómo cuadrarlo.",
    "LEDGER_MISMATCH" to "Cuenta físicamente las piezas y registra el ajuste de entrada o salida que corresponda.",
)

/**
 * Auditor de inventario: revisa que préstamos, unidades y kardex cuadren
 * (`GET /inventory/audit`, 8 reglas) y resuelve los descuadres de movimientos
 * (`POST /inventory/audit/mismatches/resolve`). Requiere `inventory.audit`.
 */
@Composable
fun InventoryAuditScreen(
    viewModel: InventoryAuditViewModel = viewModel { InventoryAuditViewModel(AppContainer.inventoryApi) },
) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canAudit = user?.can(PermissionKeys.INVENTORY_AUDIT) == true

    var confirmQuantityRow by remember { mutableStateOf<InventoryAuditRowDto?>(null) }

    if (!canAudit) {
        Column(modifier = Modifier.fillMaxSize().background(AppBento.canvas).padding(16.dp)) {
            BentoState(message = "Necesitas el permiso de auditoría de inventario (inventory.audit).")
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBento.canvas)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Consistencia del inventario",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.TextPrimary,
                    )
                    Text(
                        state.audit?.let { "Revisado: ${formatDateTime(it.checkedAt)}" }
                            ?: "Comprobando préstamos, unidades y kardex…",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                    )
                }
                Spacer(Modifier.width(8.dp))
                PillButton(
                    text = if (state.refreshing) "Revisando…" else "Revisar ahora",
                    onClick = viewModel::run,
                    variant = PillButtonVariant.Secondary,
                    loading = state.refreshing,
                    leadingIcon = Icons.Filled.Refresh,
                    modifier = Modifier.width(170.dp),
                )
            }
            Spacer(Modifier.height(10.dp))

            state.resolvedMessage?.let { message ->
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = AppColors.Success.copy(alpha = 0.10f),
                    borderColor = AppColors.Success.copy(alpha = 0.35f),
                    contentPadding = PaddingValues(12.dp),
                ) {
                    Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.TextPrimary)
                }
                Spacer(Modifier.height(8.dp))
            }
            state.actionError?.let { message ->
                BentoErrorState(message = message, onRetry = null)
                Spacer(Modifier.height(8.dp))
            }

            state.audit?.let { audit ->
                val failingCases = state.failing.sumOf { it.count }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BentoMiniStat(
                        label = "Reglas revisadas",
                        value = audit.checks.size.toString(),
                        tone = BentoTone.Sea,
                        modifier = Modifier.weight(1f),
                    )
                    BentoMiniStat(
                        label = "Con casos",
                        value = state.failing.size.toString(),
                        tone = if (state.failing.isNotEmpty()) BentoTone.Sun else BentoTone.Plain,
                        modifier = Modifier.weight(1f),
                    )
                    BentoMiniStat(
                        label = "Casos por corregir",
                        value = failingCases.toString(),
                        tone = if (failingCases > 0) BentoTone.Sun else BentoTone.Plain,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        when {
            state.loading && state.audit == null -> BentoState(
                message = "Revisando préstamos, unidades y kardex…",
                loading = true,
                modifier = Modifier.padding(16.dp),
            )

            state.error != null && state.audit == null -> BentoErrorState(
                message = state.error ?: "No se pudo correr el auditor.",
                onRetry = viewModel::run,
                modifier = Modifier.padding(16.dp),
            )

            state.audit == null -> Unit

            state.audit?.ok == true -> Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            ) {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = AppBento.tone(BentoTone.Mint).container,
                    borderColor = null,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AppColors.Success)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Todo cuadra: préstamos, unidades y kardex son consistentes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextPrimary,
                        )
                    }
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "summary") {
                    Text(
                        "${state.failing.size} regla(s) con casos por corregir",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                    )
                }
                items(state.failing, key = { it.key }) { check ->
                    AuditCheckCard(
                        check = check,
                        busy = state.actionSaving,
                        onResolve = { row, mode ->
                            if (mode == "quantity") confirmQuantityRow = row else viewModel.resolve(row, mode)
                        },
                        onOpenDevice = { deviceId -> navigator.push(Screen.DeviceDetail(deviceId)) },
                    )
                }
            }
        }
    }

    confirmQuantityRow?.let { row ->
        val linked = row.linkedAfter ?: row.linked ?: 0
        AppModal(
            title = "Dejar la cantidad en lo que hay",
            icon = Icons.Filled.Warning,
            tone = AppModalTone.Warning,
            onDismiss = { confirmQuantityRow = null },
            confirmLabel = "Sí, corregir la cantidad",
            saving = state.actionSaving,
            onConfirm = {
                viewModel.resolve(row, "quantity")
                confirmQuantityRow = null
            },
        ) {
            Text(
                "El movimiento «${row.device ?: "—"}» quedará con $linked piezas registradas en vez de " +
                    "${row.quantity ?: 0}, y el disponible del dispositivo pasará de ${row.ledger ?: 0} a " +
                    "${row.ledgerIfQuantity ?: row.ledger ?: 0}. Esto mueve las existencias y no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
        }
    }
}

@Composable
private fun AuditCheckCard(
    check: InventoryAuditCheckDto,
    busy: Boolean,
    onResolve: (InventoryAuditRowDto, String) -> Unit,
    onOpenDevice: (String) -> Unit,
) {
    val label = AuditCheckLabels[check.key] ?: check.key
    val fix = AuditFixHints[check.key].orEmpty()
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = null,
        containerColor = AppBento.tone(BentoTone.Sun).container,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = AppColors.Warning)
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(6.dp))
            StatusChip(
                label = if (check.count == 1) "1 caso" else "${check.count} casos",
                tone = BentoTone.Sun,
            )
        }

        if (check.rows.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            check.rows.forEach { row ->
                AuditRowCard(row = row, busy = busy, onResolve = onResolve, onOpenDevice = onOpenDevice)
                Spacer(Modifier.height(8.dp))
            }
        } else if (check.samples.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                check.samples.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
        }

        if (fix.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(fix, style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
        }
    }
}

@Composable
private fun AuditRowCard(
    row: InventoryAuditRowDto,
    busy: Boolean,
    onResolve: (InventoryAuditRowDto, String) -> Unit,
    onOpenDevice: (String) -> Unit,
) {
    val says = row.quantity ?: 0
    val linked = row.linked ?: 0
    val after = row.linkedAfter ?: linked
    val free = (after - linked).coerceAtLeast(0)
    val movementItemId = row.movementItemId

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = AppColors.Surface,
        contentPadding = PaddingValues(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            row.movementType?.let { type ->
                StatusChip(label = type, tone = BentoTone.Sea)
                Spacer(Modifier.width(6.dp))
            }
            row.date?.let { day ->
                Text(formatAuditDay(day), style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                row.device ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusChip(label = "Dice $says", tone = BentoTone.Plain)
            StatusChip(label = "Registradas $linked", tone = BentoTone.Sea)
            if (says > linked) {
                StatusChip(label = "Faltan ${says - linked}", tone = BentoTone.Sun)
            } else if (linked > says) {
                StatusChip(label = "Sobran ${linked - says}", tone = BentoTone.Sun)
            }
        }
        if (row.ledger != null || row.available != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Kardex: ${row.ledger ?: 0} · Unidades disponibles: ${row.available ?: 0}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
        }
        if (free > 0) {
            Spacer(Modifier.height(2.dp))
            Text(
                "Hay $free pieza(s) libres en el dispositivo que se le pueden ligar.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
            )
        }
        if (movementItemId != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (free > 0) {
                    PillButton(
                        text = "Ligar $free",
                        onClick = { onResolve(row, "link") },
                        enabled = !busy,
                        variant = PillButtonVariant.Primary,
                    )
                }
                if (after != says) {
                    PillButton(
                        text = "Cuadrar a $after",
                        onClick = { onResolve(row, "quantity") },
                        enabled = !busy,
                        variant = PillButtonVariant.Secondary,
                    )
                }
                PillButton(
                    text = "Dar por revisado",
                    onClick = { onResolve(row, "review") },
                    enabled = !busy,
                    variant = PillButtonVariant.Ghost,
                )
            }
        } else if (row.deviceId != null) {
            Spacer(Modifier.height(10.dp))
            PillButton(
                text = "Ver dispositivo",
                onClick = { onOpenDevice(row.deviceId) },
                variant = PillButtonVariant.Secondary,
            )
        }
    }
}

/** `YYYY-MM-DD` → `DD/MM/AAAA` sin pasar por `Date` (la zona correría el día). */
private fun formatAuditDay(day: String): String = formatShortDate(day)
