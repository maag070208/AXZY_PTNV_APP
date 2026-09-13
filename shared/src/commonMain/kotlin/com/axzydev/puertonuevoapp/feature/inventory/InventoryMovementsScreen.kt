package com.axzydev.puertonuevoapp.feature.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.axzydev.puertonuevoapp.core.network.InventoryMovementDto
import com.axzydev.puertonuevoapp.core.network.LocationDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.condicionLabel
import com.axzydev.puertonuevoapp.core.util.formatLocation
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import com.axzydev.puertonuevoapp.core.util.movementTypeLabel
import kotlinx.coroutines.launch

@Composable
fun InventoryMovementsScreen() {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val authState by AppContainer.authRepository.state.collectAsState()
    val role = (authState as? AuthState.LoggedIn)?.user?.role
    val canRegister = role == "ADMIN" || role == "GERENTE" || role == "JEFE_DE_AREA"

    var movements by remember { mutableStateOf<List<InventoryMovementDto>>(emptyList()) }
    var locations by remember { mutableStateOf<List<LocationDto>>(emptyList()) }
    var locationFilter by remember { mutableStateOf("") }
    var startFilter by remember { mutableStateOf("") }
    var endFilter by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        loading = true
        error = null
        try {
            movements = AppContainer.inventoryApi.movements(
                locationId = locationFilter.ifBlank { null },
                start = startFilter.ifBlank { null },
                end = endFilter.ifBlank { null },
            )
        } catch (e: Exception) {
            error = e.message ?: "No se pudieron cargar los movimientos"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        try { locations = AppContainer.locationsApi.list() } catch (_: Exception) { }
        load()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${movements.size} movimiento(s)", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                if (canRegister) {
                    IconButton(onClick = { navigator.push(Screen.NewInventoryMovement()) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Nuevo movimiento", tint = AppColors.EmeraldPrimary)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            SimpleDropdownField(
                label = "Ubicación",
                value = locationFilter,
                options = listOf("" to "Todas") + locations.map { it.id to formatLocation(it.lugar, it.subLugar, it.numero) },
                onSelect = { locationFilter = it; scope.launch { load() } },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startFilter,
                    onValueChange = { startFilter = it },
                    label = { Text("Desde (AAAA-MM-DD)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = endFilter,
                    onValueChange = { endFilter = it },
                    label = { Text("Hasta (AAAA-MM-DD)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Aplicar filtros",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.EmeraldPrimary,
                modifier = Modifier
                    .background(AppColors.SurfaceVariant, RoundedCornerShape(20.dp))
                    .clickable { scope.launch { load() } }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        when {
            loading -> LoadingState(modifier = Modifier.weight(1f))
            error != null -> ErrorState(error ?: "Error", Modifier.weight(1f), onRetry = { scope.launch { load() } })
            movements.isEmpty() -> EmptyState("No hay movimientos registrados", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(movements, key = { it.id }) { m -> MovementCard(m) }
            }
        }
    }
}

@Composable
private fun MovementCard(movement: InventoryMovementDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(AppColors.movementTypeColor(movement.tipo), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = AppColors.Surface, modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row {
                    StatusChip(movementTypeLabel(movement.tipo), AppColors.movementTypeColor(movement.tipo))
                    movement.condicion?.let {
                        Spacer(Modifier.size(6.dp))
                        StatusChip(condicionLabel(it), AppColors.condicionColor(it))
                    }
                }
                Text(formatShortDate(movement.createdAt), style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "${movement.device?.controlActivos ?: "—"} · ${movement.device?.descripcion ?: ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary,
                maxLines = 1,
            )
            if (movement.motivoBaja != null) {
                Text(movement.motivoBaja, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger, maxLines = 1)
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    movement.location?.let { formatLocation(it.lugar, it.subLugar, it.numero) } ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                )
                Text("Por: ${movement.user?.name ?: "—"}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
            }
            movement.notas?.takeIf { it.isNotBlank() }?.let {
                Text("\"$it\"", style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
