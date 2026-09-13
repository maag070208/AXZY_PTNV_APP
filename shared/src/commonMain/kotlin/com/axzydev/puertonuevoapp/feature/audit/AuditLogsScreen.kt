package com.axzydev.puertonuevoapp.feature.audit

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.network.AuditLogDto
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatDateTime
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private val actionOptions = listOf(
    "" to "Todas",
    "MOVEMENT_ENTRADA" to "Entrada",
    "MOVEMENT_SALIDA" to "Salida",
    "MOVEMENT_TRASLADO" to "Traslado",
    "MOVEMENT_BAJA" to "Baja",
    "MOVEMENT_PRESTAMO" to "Asignado",
    "MOVEMENT_DEVOLUCION" to "Devolución",
    "CARTA_CREATED" to "Carta creada",
    "DEVICE_UPDATED" to "Dispositivo actualizado",
)

private fun actionLabel(action: String): String = actionOptions.firstOrNull { it.first == action }?.second ?: action

// Lectura defensiva: los campos Json (previousState/newState/metadata) los
// escriben varios módulos distintos (tickets, inventario, cartas,
// dispositivos) sin un shape garantizado, así que se castea en vez de usar
// `.jsonPrimitive` (que lanza si el valor resultara ser objeto o arreglo).
private fun jsonString(obj: kotlinx.serialization.json.JsonObject?, key: String): String? =
    (obj?.get(key) as? JsonPrimitive)?.contentOrNull

private fun actionColor(action: String): Color = when {
    action.contains("ENTRADA") -> AppColors.Success
    action.contains("SALIDA") -> AppColors.Warning
    action.contains("TRASLADO") -> AppColors.Info
    action.contains("BAJA") -> AppColors.Danger
    action.contains("PRESTAMO") -> AppColors.EmeraldPrimary
    action.contains("DEVOLUCION") -> AppColors.Info
    action.contains("CARTA") -> AppColors.EmeraldPrimary
    else -> AppColors.TextFaint
}

/**
 * Auditoría del sistema — ver la nota en AuditApi.kt: el backend no exige
 * ningún rol para consultar esta lista y la web nunca llegó a enrutar su
 * propia página equivalente, así que aquí no se inventa una restricción
 * de rol adicional. Exportar a PDF (client-side en el web) queda fuera.
 */
@Composable
fun AuditLogsScreen() {
    val scope = rememberCoroutineScope()

    var logs by remember { mutableStateOf<List<AuditLogDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var filterAction by remember { mutableStateOf("") }
    var filterStart by remember { mutableStateOf("") }
    var filterEnd by remember { mutableStateOf("") }
    var page by remember { mutableStateOf(1) }
    var total by remember { mutableStateOf(0) }
    val limit = 20

    suspend fun load() {
        loading = true
        error = null
        try {
            val res = AppContainer.auditApi.list(
                action = filterAction.ifBlank { null },
                start = filterStart.ifBlank { null },
                end = filterEnd.ifBlank { null },
                page = page,
                limit = limit,
            )
            logs = res.data
            total = res.total
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar la auditoría"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(page) { load() }

    val totalPages = if (total == 0) 1 else ((total - 1) / limit) + 1

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("$total registro(s)", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
            Spacer(Modifier.height(10.dp))
            SimpleDropdownField(
                label = "Tipo de acción",
                value = filterAction,
                options = actionOptions,
                onSelect = { filterAction = it; page = 1; scope.launch { load() } },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = filterStart,
                    onValueChange = { filterStart = it },
                    label = { Text("Desde (AAAA-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = filterEnd,
                    onValueChange = { filterEnd = it },
                    label = { Text("Hasta (AAAA-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { page = 1; scope.launch { load() } }, modifier = Modifier.fillMaxWidth()) {
                Text("Aplicar filtros")
            }
        }

        when {
            loading -> LoadingState(modifier = Modifier.weight(1f))
            error != null -> ErrorState(error ?: "Error", Modifier.weight(1f), onRetry = { scope.launch { load() } })
            logs.isEmpty() -> EmptyState("No hay registros de auditoría", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(logs, key = { it.id }) { log -> AuditLogCard(log) }
            }
        }

        if (totalPages > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = { if (page > 1) { page -= 1 } }, enabled = page > 1) { Text("Anterior") }
                Spacer(Modifier.width(12.dp))
                Text("Página $page de $totalPages", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = { if (page < totalPages) { page += 1 } }, enabled = page < totalPages) { Text("Siguiente") }
            }
        }
    }
}

@Composable
private fun AuditLogCard(log: AuditLogDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(actionLabel(log.action), actionColor(log.action))
                Spacer(Modifier.width(8.dp))
                Text(log.deviceCode ?: "—", style = MaterialTheme.typography.bodySmall, color = AppColors.TextPrimary)
            }
            Text(formatDateTime(log.createdAt), style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Usuario: ${log.userName ?: "—"}  ·  Entidad: ${log.entityType}",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        val newState = log.newState
        val notas = jsonString(log.metadata, "notas")
        val tipo = jsonString(newState, "tipo")
        val estado = jsonString(newState, "estado")
        val condicion = jsonString(newState, "condicion")
        val consecutive = jsonString(newState, "consecutive")
        if (tipo != null || estado != null || condicion != null || consecutive != null || notas != null) {
            Spacer(Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.SurfaceVariant, RoundedCornerShape(8.dp))
                    .padding(10.dp),
            ) {
                tipo?.let { Text("Tipo: $it", style = MaterialTheme.typography.labelSmall, color = AppColors.TextMuted) }
                estado?.let { Text("Estado: $it", style = MaterialTheme.typography.labelSmall, color = AppColors.TextMuted) }
                condicion?.let { Text("Condición: $it", style = MaterialTheme.typography.labelSmall, color = AppColors.TextMuted) }
                consecutive?.let { Text("Carta: $it", style = MaterialTheme.typography.labelSmall, color = AppColors.TextMuted) }
                notas?.let { Text("Notas: $it", style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint) }
            }
        }
    }
}
