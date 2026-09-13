package com.axzydev.puertonuevoapp.feature.departments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.axzydev.puertonuevoapp.core.network.DepartmentDto
import com.axzydev.puertonuevoapp.core.network.SubareaDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import kotlinx.coroutines.launch

@Composable
fun DepartmentDetailScreen(departmentId: String) {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val authState by AppContainer.authRepository.state.collectAsState()
    val isAdmin = (authState as? AuthState.LoggedIn)?.user?.role == "ADMIN"

    var dept by remember { mutableStateOf<DepartmentDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var newSubarea by remember { mutableStateOf("") }
    var subareaToDelete by remember { mutableStateOf<SubareaDto?>(null) }
    var showDeleteDept by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    suspend fun load() {
        loading = true
        error = null
        try {
            dept = AppContainer.departmentsApi.get(departmentId)
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el departamento"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(departmentId) { load() }

    when {
        loading -> LoadingState(modifier = Modifier.fillMaxSize())
        error != null || dept == null -> ErrorState(error ?: "Departamento no encontrado", Modifier.fillMaxSize(), onRetry = { scope.launch { load() } })
        else -> {
            val d = dept!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(16.dp),
            ) {
                AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(46.dp).background(AppColors.EmeraldPrimary, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.Business, contentDescription = null, tint = AppColors.Surface) }
                        Spacer(Modifier.size(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(d.name, style = MaterialTheme.typography.titleLarge, color = AppColors.TextPrimary)
                            Text(
                                if (d.active) "Activo" else "Inactivo",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (d.active) AppColors.Success else AppColors.Danger,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Groups, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("${d.count?.users ?: 0} usuario(s)", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
                    }
                }

                Spacer(Modifier.height(14.dp))

                AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    SectionLabel("Áreas")
                    if (d.subareas.isEmpty()) {
                        Text("Este departamento aún no tiene subáreas.", style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(d.subareas, key = { it.id }) { s ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(AppColors.SurfaceVariant, RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text(s.name.uppercase(), style = MaterialTheme.typography.labelSmall, color = AppColors.TextPrimary)
                                    if (isAdmin) {
                                        Spacer(Modifier.size(6.dp))
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Eliminar subárea",
                                            tint = AppColors.TextFaint,
                                            modifier = Modifier.size(14.dp).clickable { subareaToDelete = s },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (isAdmin) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newSubarea,
                                onValueChange = { newSubarea = it },
                                placeholder = { Text("Nueva subárea…") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.size(8.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            AppContainer.departmentsApi.addSubarea(d.id, newSubarea.trim())
                                            newSubarea = ""
                                            load()
                                        } catch (e: Exception) {
                                            error = e.message
                                        }
                                    }
                                },
                                enabled = newSubarea.isNotBlank(),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
                            ) { Icon(Icons.Filled.Add, contentDescription = "Agregar", tint = AppColors.Surface) }
                        }
                    }
                }

                if (isAdmin) {
                    Spacer(Modifier.height(20.dp))
                    OutlinedButton(
                        onClick = { showDeleteDept = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Danger),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(if (d.active) "Eliminar departamento" else "Eliminar definitivamente")
                    }
                }
            }

            subareaToDelete?.let { s ->
                AppModal(
                    title = if (s.active) "Eliminar subárea" else "Eliminar definitivamente",
                    icon = Icons.Filled.DeleteOutline,
                    tone = AppModalTone.Danger,
                    onDismiss = { subareaToDelete = null },
                    confirmLabel = if (s.active) "Eliminar" else "Eliminar definitivamente",
                    saving = saving,
                    onConfirm = {
                        scope.launch {
                            saving = true
                            try {
                                AppContainer.departmentsApi.removeSubarea(s.id)
                                subareaToDelete = null
                                load()
                            } catch (e: Exception) {
                                subareaToDelete = null
                                error = e.message
                            } finally {
                                saving = false
                            }
                        }
                    },
                ) {
                    Text(
                        if (s.active) "¿Eliminar la subárea \"${s.name}\"? Se desactivará."
                        else "¿Eliminar definitivamente la subárea \"${s.name}\"? Esta acción no se puede deshacer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextMuted,
                    )
                }
            }

            if (showDeleteDept) {
                AppModal(
                    title = if (d.active) "Eliminar departamento" else "Eliminar definitivamente",
                    icon = Icons.Filled.DeleteOutline,
                    tone = AppModalTone.Danger,
                    onDismiss = { showDeleteDept = false },
                    confirmLabel = if (d.active) "Eliminar" else "Eliminar definitivamente",
                    saving = saving,
                    onConfirm = {
                        scope.launch {
                            saving = true
                            try {
                                AppContainer.departmentsApi.remove(d.id)
                                showDeleteDept = false
                                navigator.pop()
                            } catch (e: Exception) {
                                showDeleteDept = false
                                error = e.message
                            } finally {
                                saving = false
                            }
                        }
                    },
                ) {
                    Text(
                        if (d.active) "¿Eliminar ${d.name}? Se desactivará; si tiene usuarios asociados no se podrá eliminar."
                        else "¿Eliminar definitivamente ${d.name}? Se borrarán sus áreas y se desligará de usuarios y tickets. Esta acción no se puede deshacer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextMuted,
                    )
                }
            }
        }
    }
}
