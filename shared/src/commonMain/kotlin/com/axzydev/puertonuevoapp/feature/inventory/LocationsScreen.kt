package com.axzydev.puertonuevoapp.feature.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Visibility
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
import com.axzydev.puertonuevoapp.core.network.DeviceDto
import com.axzydev.puertonuevoapp.core.network.LocationCreateDto
import com.axzydev.puertonuevoapp.core.network.LocationDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.deviceEstadoLabel
import com.axzydev.puertonuevoapp.core.util.formatLocation
import kotlinx.coroutines.launch

private data class LocationForm(
    val lugar: String = "",
    val subLugar: String = "",
    val numero: String = "",
    val descripcion: String = "",
)

@Composable
fun LocationsScreen() {
    val scope = rememberCoroutineScope()
    val authState by AppContainer.authRepository.state.collectAsState()
    val isAdmin = (authState as? AuthState.LoggedIn)?.user?.role == "ADMIN"

    var locations by remember { mutableStateOf<List<LocationDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var actionSaving by remember { mutableStateOf(false) }

    var formTarget by remember { mutableStateOf<LocationDto?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var form by remember { mutableStateOf(LocationForm()) }
    var deleteTarget by remember { mutableStateOf<LocationDto?>(null) }
    var devicesTarget by remember { mutableStateOf<LocationDto?>(null) }

    suspend fun load() {
        loading = true
        error = null
        try {
            locations = AppContainer.locationsApi.list()
        } catch (e: Exception) {
            error = e.message ?: "No se pudieron cargar las ubicaciones"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${locations.size} ubicación(es)", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
            if (isAdmin) {
                IconButton(onClick = { form = LocationForm(); showCreate = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Nueva ubicación", tint = AppColors.EmeraldPrimary)
                }
            }
        }

        when {
            loading -> LoadingState(modifier = Modifier.weight(1f))
            error != null -> ErrorState(error ?: "Error", Modifier.weight(1f), onRetry = { scope.launch { load() } })
            locations.isEmpty() -> EmptyState("Aún no hay ubicaciones registradas", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(locations, key = { it.id }) { loc ->
                    LocationCard(
                        location = loc,
                        isAdmin = isAdmin,
                        onView = { devicesTarget = loc },
                        onEdit = {
                            form = LocationForm(loc.lugar ?: "", loc.subLugar ?: "", loc.numero ?: "", loc.descripcion ?: "")
                            formTarget = loc
                        },
                        onDelete = { deleteTarget = loc },
                    )
                }
            }
        }
    }

    if (showCreate || formTarget != null) {
        val editing = formTarget
        AppModal(
            title = if (editing == null) "Nueva ubicación" else "Editar ubicación",
            icon = Icons.Filled.LocationOn,
            onDismiss = { showCreate = false; formTarget = null },
            confirmLabel = if (actionSaving) "Guardando…" else "Guardar",
            confirmEnabled = form.lugar.isNotBlank() || form.subLugar.isNotBlank() || form.numero.isNotBlank(),
            saving = actionSaving,
            onConfirm = {
                scope.launch {
                    actionSaving = true
                    try {
                        val input = LocationCreateDto(
                            lugar = form.lugar.trim().ifBlank { null },
                            subLugar = form.subLugar.trim().ifBlank { null },
                            numero = form.numero.trim().ifBlank { null },
                            descripcion = form.descripcion.trim().ifBlank { null },
                        )
                        if (editing != null) AppContainer.locationsApi.update(editing.id, input) else AppContainer.locationsApi.create(input)
                        showCreate = false
                        formTarget = null
                        load()
                    } catch (e: Exception) {
                        showCreate = false
                        formTarget = null
                        actionError = e.message ?: "No se pudo guardar la ubicación"
                    } finally {
                        actionSaving = false
                    }
                }
            },
        ) {
            OutlinedTextField(value = form.lugar, onValueChange = { form = form.copy(lugar = it.uppercase()) }, label = { Text("Lugar") }, placeholder = { Text("Ej. OFICINA, BODEGA") }, singleLine = true, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = form.subLugar, onValueChange = { form = form.copy(subLugar = it.uppercase()) }, label = { Text("Sub-lugar") }, placeholder = { Text("Ej. SISTEMAS") }, singleLine = true, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = form.numero, onValueChange = { form = form.copy(numero = it.uppercase()) }, label = { Text("Número") }, placeholder = { Text("Ej. 1, 2, 3") }, singleLine = true, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = form.descripcion, onValueChange = { form = form.copy(descripcion = it) }, label = { Text("Descripción") }, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
        }
    }

    deleteTarget?.let { loc ->
        AppModal(
            title = "Eliminar ubicación",
            icon = Icons.Filled.DeleteOutline,
            tone = AppModalTone.Danger,
            onDismiss = { deleteTarget = null },
            confirmLabel = "Eliminar",
            saving = actionSaving,
            onConfirm = {
                scope.launch {
                    actionSaving = true
                    try {
                        AppContainer.locationsApi.remove(loc.id)
                        deleteTarget = null
                        load()
                    } catch (e: Exception) {
                        deleteTarget = null
                        actionError = e.message ?: "No se pudo eliminar la ubicación"
                    } finally {
                        actionSaving = false
                    }
                }
            },
        ) {
            Text(
                "¿Eliminar ${formatLocation(loc.lugar, loc.subLugar, loc.numero)}? Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
        }
    }

    devicesTarget?.let { loc ->
        LocationDevicesModal(location = loc, onDismiss = { devicesTarget = null })
    }

    actionError?.let { message ->
        AppModal(
            title = "No se pudo completar la acción",
            icon = Icons.Filled.LocationOn,
            tone = AppModalTone.Danger,
            onDismiss = { actionError = null },
        ) { Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted) }
    }
}

@Composable
private fun LocationCard(
    location: LocationDto,
    isAdmin: Boolean,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(8.dp))
            .border(1.dp, AppColors.Outline, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(AppColors.Info.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = AppColors.Info, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(formatLocation(location.lugar, location.subLugar, location.numero), style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
            Text(
                location.descripcion?.ifBlank { null } ?: "${location.count?.devices ?: 0} dispositivo(s)",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
                maxLines = 1,
            )
        }
        IconButton(onClick = onView) { Icon(Icons.Filled.Visibility, contentDescription = "Ver dispositivos", tint = AppColors.TextFaint, modifier = Modifier.size(18.dp)) }
        if (isAdmin) {
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = AppColors.TextFaint, modifier = Modifier.size(18.dp)) }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar", tint = AppColors.Danger, modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
private fun LocationDevicesModal(location: LocationDto, onDismiss: () -> Unit) {
    var devices by remember { mutableStateOf<List<DeviceDto>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(location.id) {
        try {
            devices = AppContainer.locationsApi.get(location.id).devices
        } catch (e: Exception) {
            error = e.message ?: "No se pudieron cargar los dispositivos"
        }
    }

    AppModal(
        title = "Dispositivos en ${formatLocation(location.lugar, location.subLugar, location.numero)}",
        icon = Icons.Filled.LocationOn,
        onDismiss = onDismiss,
    ) {
        when {
            error != null -> Text(error ?: "", style = MaterialTheme.typography.bodyMedium, color = AppColors.Danger)
            devices == null -> LoadingState(modifier = Modifier.height(120.dp))
            devices!!.isEmpty() -> Text("No hay dispositivos en esta ubicación.", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextFaint)
            else -> Column {
                devices!!.forEach { d ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(d.controlActivos, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                            Text("${d.marca} ${d.modelo}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                        }
                        StatusChip(deviceEstadoLabel(d.estado), AppColors.deviceEstadoColor(d.estado))
                    }
                }
            }
        }
    }
}
