package com.axzydev.puertonuevoapp.feature.cartas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import com.axzydev.puertonuevoapp.core.network.CartaDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import kotlinx.coroutines.launch

/**
 * Cartas responsivas: la visibilidad ya viene filtrada por rol desde el
 * backend (EMPLEADO ve solo las suyas, JEFE_DE_AREA solo las de su
 * departamento — carta.service.ts `listCartas`), así que aquí no se
 * vuelve a filtrar por dueño. Los gates de UI sí replican exactamente los
 * del backend: crear está vedado a JEFE_DE_AREA (403 explícito en
 * carta.controller.ts) y eliminar está vedado a EMPLEADO
 * (`authorize(["ADMIN","GERENTE","JEFE_DE_AREA"])` en carta.routes.ts).
 * La generación de PDF (client-side con @react-pdf/renderer, sin endpoint
 * en el backend) queda fuera de esta pasada — ver CartaDetailScreen.
 */
@Composable
fun CartasListScreen() {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val authState by AppContainer.authRepository.state.collectAsState()
    val role = (authState as? AuthState.LoggedIn)?.user?.role
    val canCreate = role != "JEFE_DE_AREA"
    val canDelete = role != "EMPLEADO"

    var cartas by remember { mutableStateOf<List<CartaDto>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var actionSaving by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<CartaDto?>(null) }

    suspend fun load() {
        loading = true
        error = null
        try {
            cartas = AppContainer.cartasApi.list().data
        } catch (e: Exception) {
            error = e.message ?: "No se pudieron cargar las cartas responsivas"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    val filtered by remember(cartas, query) {
        derivedStateOf {
            cartas.filter { c ->
                query.isBlank() ||
                    c.consecutivo.contains(query, ignoreCase = true) ||
                    c.numeroEmpleado.contains(query, ignoreCase = true) ||
                    c.departamento.contains(query, ignoreCase = true) ||
                    (c.items.firstOrNull()?.descripcion ?: "").contains(query, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${filtered.size} carta(s)", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                if (canCreate) {
                    IconButton(onClick = { navigator.push(Screen.CartaForm()) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Nueva carta", tint = AppColors.EmeraldPrimary)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            AppSearchField(query, { query = it }, "Buscar por folio, empleado, descripción…")
        }

        when {
            loading -> LoadingState(modifier = Modifier.weight(1f))
            error != null -> ErrorState(error ?: "Error", Modifier.weight(1f), onRetry = { scope.launch { load() } })
            filtered.isEmpty() -> EmptyState("No hay cartas responsivas registradas", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filtered, key = { it.id }) { c ->
                    CartaCard(
                        carta = c,
                        canDelete = canDelete,
                        onClick = { navigator.push(Screen.CartaDetail(c.id)) },
                        onDelete = { deleteTarget = c },
                    )
                }
            }
        }
    }

    deleteTarget?.let { c ->
        AppModal(
            title = "Eliminar carta",
            icon = Icons.Filled.DeleteOutline,
            tone = AppModalTone.Danger,
            onDismiss = { deleteTarget = null },
            confirmLabel = "Eliminar",
            saving = actionSaving,
            onConfirm = {
                scope.launch {
                    actionSaving = true
                    try {
                        AppContainer.cartasApi.remove(c.id)
                        deleteTarget = null
                        load()
                    } catch (e: Exception) {
                        deleteTarget = null
                        actionError = e.message ?: "No se pudo eliminar la carta"
                    } finally {
                        actionSaving = false
                    }
                }
            },
        ) {
            Text(
                "¿Eliminar la carta ${c.consecutivo} del historial? Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
        }
    }

    actionError?.let { message ->
        AppModal(
            title = "No se pudo completar la acción",
            icon = Icons.Filled.ErrorOutline,
            tone = AppModalTone.Danger,
            onDismiss = { actionError = null },
        ) { Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted) }
    }
}

@Composable
private fun CartaCard(carta: CartaDto, canDelete: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    val item = carta.items.firstOrNull()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(8.dp))
            .border(1.dp, AppColors.Outline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(carta.consecutivo, style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary, maxLines = 1)
                Text(formatShortDate(carta.fecha), style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
            }
            Text(
                "No. empleado ${carta.numeroEmpleado.ifBlank { "—" }}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
                maxLines = 1,
            )
            Text(
                item?.descripcion ?: "Sin recurso asignado",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
                maxLines = 1,
            )
            Text(
                carta.departamento,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
                maxLines = 1,
            )
            if (carta.returnDate != null) {
                Spacer(Modifier.height(4.dp))
                Text("Devuelta", style = MaterialTheme.typography.labelSmall, color = AppColors.Success)
            }
        }
        if (canDelete) {
            IconButton(onClick = onDelete) { Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar", tint = AppColors.Danger, modifier = Modifier.size(18.dp)) }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AppColors.TextFaint, modifier = Modifier.size(20.dp))
    }
}
