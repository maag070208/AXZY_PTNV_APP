package com.axzydev.puertonuevoapp.feature.departments

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.BentoIconButton
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import com.axzydev.puertonuevoapp.core.util.ticketStatusLabel
import com.axzydev.puertonuevoapp.core.ui.AppTextField

@Composable
fun DepartmentDetailScreen(departmentId: String) {
    val viewModel: DepartmentDetailViewModel = viewModel(key = "department-$departmentId") {
        DepartmentDetailViewModel(departmentId, AppContainer.departmentsApi)
    }
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val canManage = (authState as? AuthState.LoggedIn)?.user?.canManageDepartments == true

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
                            modifier = Modifier
                                .size(52.dp)
                                .background(AppColors.EmeraldContainer, AppShape.icon),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Business,
                                contentDescription = null,
                                tint = AppColors.EmeraldPrimary,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                d.name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = AppColors.TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusChip(
                                    label = if (d.active) "Activo" else "Inactivo",
                                    tone = if (d.active) BentoTone.Mint else BentoTone.Rose,
                                )
                                StatusChip(
                                    label = "${d.count?.users ?: 0} persona(s)",
                                    tone = if ((d.count?.users ?: 0) == 0) BentoTone.Plain else BentoTone.Sea,
                                )
                                StatusChip(
                                    label = "${d.subareas.size} subárea(s)",
                                    tone = if (d.subareas.isEmpty()) BentoTone.Plain else BentoTone.Lavender,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    SectionLabel("Áreas")
                    if (d.subareas.isEmpty()) {
                        Text("Este departamento aún no tiene subáreas.", style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(d.subareas, key = { it.id }) { subarea ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusChip(label = subarea.name.uppercase(), tone = BentoTone.Sea)
                                    if (canManage) {
                                        Spacer(Modifier.width(2.dp))
                                        BentoIconButton(
                                            icon = Icons.Filled.Close,
                                            contentDescription = "Eliminar subárea ${subarea.name}",
                                            onClick = { viewModel.requestDeleteSubarea(subarea) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (canManage) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppTextField(
                                value = state.newSubarea,
                                onValueChange = viewModel::onNewSubareaChange,
                                placeholder = { Text("Nueva subárea…") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            BentoIconButton(
                                icon = Icons.Filled.Add,
                                contentDescription = "Agregar subárea",
                                onClick = { if (state.newSubarea.isNotBlank()) viewModel.addSubarea() },
                            )
                        }
                    }
                }

                if (d.locations.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        SectionLabel("Ubicaciones (${d.locations.size})")
                        d.locations.forEach { loc ->
                            Text(loc.name, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary, modifier = Modifier.padding(vertical = 3.dp))
                        }
                    }
                }

                if (d.tickets.isNotEmpty() || d.ticketsTotal > 0) {
                    Spacer(Modifier.height(14.dp))
                    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        SectionLabel("Tickets (${d.ticketsTotal})")
                        d.tickets.forEach { t ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(t.title, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
                                StatusChip(ticketStatusLabel(t.status), AppColors.ticketStatusColor(t.status))
                            }
                        }
                    }
                }

                if (d.custodyLetters.isNotEmpty() || d.custodyLettersTotal > 0) {
                    Spacer(Modifier.height(14.dp))
                    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        SectionLabel("Cartas responsivas (${d.custodyLettersTotal})")
                        d.custodyLetters.forEach { c ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    "${c.consecutive} · ${c.custodian?.name ?: "—"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppColors.TextPrimary,
                                )
                                Text(
                                    "${c.itemsCount} artículo(s) · ${formatShortDate(c.date)}${if (c.returnDate != null) " · devuelta" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextFaint,
                                )
                            }
                        }
                    }
                }

                if (canManage) {
                    Spacer(Modifier.height(20.dp))
                    PillButton(
                        text = if (d.active) "Eliminar departamento" else "Eliminar definitivamente",
                        onClick = viewModel::requestDeleteDepartment,
                        variant = PillButtonVariant.Ghost,
                        leadingIcon = Icons.Filled.DeleteOutline,
                        modifier = Modifier.fillMaxWidth(),
                    )
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
