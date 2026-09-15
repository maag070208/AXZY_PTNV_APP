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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import com.axzydev.puertonuevoapp.core.util.ticketStatusLabel

@Composable
fun DepartmentDetailScreen(departmentId: String) {
    val viewModel: DepartmentDetailViewModel = viewModel(key = "department-$departmentId") {
        DepartmentDetailViewModel(departmentId, AppContainer.departmentsApi)
    }
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val isAdmin = (authState as? AuthState.LoggedIn)?.user?.canManageCatalogs == true

    LaunchedEffect(state.deleted) { if (state.deleted) navigator.pop() }

    when {
        state.loading -> LoadingState(modifier = Modifier.fillMaxSize())
        state.error != null && state.department == null -> ErrorState(state.error ?: "Departamento no encontrado", Modifier.fillMaxSize(), onRetry = viewModel::load)
        state.department == null -> LoadingState(modifier = Modifier.fillMaxSize())
        else -> {
            val d = state.department!!
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
                                            modifier = Modifier.size(14.dp).clickable { viewModel.requestDeleteSubarea(s) },
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
                                value = state.newSubarea,
                                onValueChange = viewModel::onNewSubareaChange,
                                placeholder = { Text("Nueva subárea…") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.size(8.dp))
                            Button(
                                onClick = viewModel::addSubarea,
                                enabled = state.newSubarea.isNotBlank(),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
                            ) { Icon(Icons.Filled.Add, contentDescription = "Agregar", tint = AppColors.Surface) }
                        }
                    }
                }

                if (d.locations.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        SectionLabel("Ubicaciones (${d.locations.size})")
                        d.locations.forEach { loc ->
                            Text(loc.lugar, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary, modifier = Modifier.padding(vertical = 3.dp))
                        }
                    }
                }

                if (d.tickets.isNotEmpty() || d.ticketsTotal > 0) {
                    Spacer(Modifier.height(14.dp))
                    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        SectionLabel("Tickets (${d.ticketsTotal})")
                        d.tickets.forEach { t ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(t.titulo, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
                                StatusChip(ticketStatusLabel(t.status), AppColors.ticketStatusColor(t.status))
                            }
                        }
                    }
                }

                if (d.cartas.isNotEmpty() || d.cartasTotal > 0) {
                    Spacer(Modifier.height(14.dp))
                    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        SectionLabel("Cartas responsivas (${d.cartasTotal})")
                        d.cartas.forEach { c ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    "${c.consecutive} · ${c.responsable?.name ?: "—"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppColors.TextPrimary,
                                )
                                Text(
                                    "${c.itemsCount} artículo(s) · ${formatShortDate(c.fecha)}${if (c.returnDate != null) " · devuelta" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextFaint,
                                )
                            }
                        }
                    }
                }

                if (isAdmin) {
                    Spacer(Modifier.height(20.dp))
                    OutlinedButton(
                        onClick = viewModel::requestDeleteDepartment,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Danger),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(if (d.active) "Eliminar departamento" else "Eliminar definitivamente")
                    }
                }
            }

            state.subareaToDelete?.let { s ->
                AppModal(
                    title = if (s.active) "Eliminar subárea" else "Eliminar definitivamente",
                    icon = Icons.Filled.DeleteOutline,
                    tone = AppModalTone.Danger,
                    onDismiss = viewModel::dismissDeleteSubarea,
                    confirmLabel = if (s.active) "Eliminar" else "Eliminar definitivamente",
                    saving = state.saving,
                    onConfirm = viewModel::confirmDeleteSubarea,
                ) {
                    Text(
                        if (s.active) "¿Eliminar la subárea \"${s.name}\"? Se desactivará."
                        else "¿Eliminar definitivamente la subárea \"${s.name}\"? Esta acción no se puede deshacer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextMuted,
                    )
                }
            }

            if (state.showDeleteDept) {
                AppModal(
                    title = if (d.active) "Eliminar departamento" else "Eliminar definitivamente",
                    icon = Icons.Filled.DeleteOutline,
                    tone = AppModalTone.Danger,
                    onDismiss = viewModel::dismissDeleteDepartment,
                    confirmLabel = if (d.active) "Eliminar" else "Eliminar definitivamente",
                    saving = state.saving,
                    onConfirm = viewModel::confirmDeleteDepartment,
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
