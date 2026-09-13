package com.axzydev.puertonuevoapp.feature.salidas

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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.axzydev.puertonuevoapp.core.network.SalidaDto
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
 * Bitácora de salida de material (F-SIS-0005). Cualquier usuario autenticado
 * puede crear/editar/eliminar renglones — así lo permite el backend (sin
 * restricción de rol en salida.routes.ts) — así que se replica igual aquí.
 * La captura por lote (varios renglones a la vez) y el reporte en PDF son
 * flujos de escritorio que se dejan fuera del alcance móvil.
 */
@Composable
fun SalidasListScreen() {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()

    var salidas by remember { mutableStateOf<List<SalidaDto>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var actionSaving by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<SalidaDto?>(null) }

    suspend fun load() {
        loading = true
        error = null
        try {
            salidas = AppContainer.salidasApi.list().data
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar la bitácora de salidas"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    val filtered by remember(salidas, query) {
        derivedStateOf {
            salidas.filter { s ->
                query.isBlank() ||
                    s.descripcion.contains(query, ignoreCase = true) ||
                    s.departamento.contains(query, ignoreCase = true) ||
                    s.usuario.contains(query, ignoreCase = true) ||
                    s.proyecto.orEmpty().contains(query, ignoreCase = true) ||
                    s.marca.orEmpty().contains(query, ignoreCase = true) ||
                    s.modelo.orEmpty().contains(query, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${filtered.size} registro(s)", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                IconButton(onClick = { navigator.push(Screen.SalidaForm()) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Nueva salida", tint = AppColors.EmeraldPrimary)
                }
            }
            Spacer(Modifier.height(10.dp))
            AppSearchField(query, { query = it }, "Buscar por descripción, depto, usuario…")
        }

        when {
            loading -> LoadingState(modifier = Modifier.weight(1f))
            error != null -> ErrorState(error ?: "Error", Modifier.weight(1f), onRetry = { scope.launch { load() } })
            filtered.isEmpty() -> EmptyState("No hay salidas registradas", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filtered, key = { it.id }) { s ->
                    SalidaCard(
                        salida = s,
                        onClick = { navigator.push(Screen.SalidaForm(s.id)) },
                        onDelete = { deleteTarget = s },
                    )
                }
            }
        }
    }

    deleteTarget?.let { s ->
        AppModal(
            title = "Eliminar registro",
            icon = Icons.Filled.DeleteOutline,
            tone = AppModalTone.Danger,
            onDismiss = { deleteTarget = null },
            confirmLabel = "Eliminar",
            saving = actionSaving,
            onConfirm = {
                scope.launch {
                    actionSaving = true
                    try {
                        AppContainer.salidasApi.remove(s.id)
                        deleteTarget = null
                        load()
                    } catch (e: Exception) {
                        deleteTarget = null
                        actionError = e.message ?: "No se pudo eliminar el registro"
                    } finally {
                        actionSaving = false
                    }
                }
            },
        ) {
            Text("¿Eliminar el registro de \"${s.descripcion}\"? Esta acción no se puede deshacer.", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
        }
    }

    actionError?.let { message ->
        AppModal(
            title = "No se pudo completar la acción",
            icon = Icons.Filled.Assignment,
            tone = AppModalTone.Danger,
            onDismiss = { actionError = null },
        ) { Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted) }
    }
}

@Composable
private fun SalidaCard(salida: SalidaDto, onClick: () -> Unit, onDelete: () -> Unit) {
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
                Text(salida.descripcion, style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary, maxLines = 1)
                Text(formatShortDate(salida.fecha), style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
            }
            Text(
                listOfNotNull(salida.marca, salida.modelo).joinToString(" ").ifBlank { null } ?: "Sin marca/modelo",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
                maxLines = 1,
            )
            Text(
                "${salida.departamento} · ${salida.usuario} · Cant. ${salida.cantidad}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
                maxLines = 1,
            )
        }
        IconButton(onClick = onDelete) { Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar", tint = AppColors.Danger, modifier = Modifier.size(18.dp)) }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AppColors.TextFaint, modifier = Modifier.size(20.dp))
    }
}
