package com.axzydev.puertonuevoapp.feature.inventory

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.deviceStatusLabel
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import com.axzydev.puertonuevoapp.core.ui.AppTextField

@Composable
fun LocationDetailScreen(locationId: String) {
    val viewModel: LocationDetailViewModel = viewModel(key = "location-$locationId") {
        LocationDetailViewModel(locationId, AppContainer.locationsApi)
    }
    val state by viewModel.uiState.collectAsState()
    val authState by AppContainer.authRepository.state.collectAsState()
    val isAdmin = (authState as? AuthState.LoggedIn)?.user?.canManageCatalogs == true

    when {
        state.loading -> LoadingState(Modifier.fillMaxSize())
        state.error != null || state.location == null -> ErrorState(state.error ?: "Ubicación no encontrada", Modifier.fillMaxSize(), onRetry = viewModel::load)
        else -> {
            val loc = state.location!!
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp)) {
                AppSurfaceCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(46.dp).background(AppColors.EmeraldPrimary, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = AppColors.Surface) }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(loc.name, style = MaterialTheme.typography.titleLarge, color = AppColors.TextPrimary)
                            loc.description?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                AppSurfaceCard(Modifier.fillMaxWidth()) {
                    SectionLabel("Sublugares")
                    if (loc.subLocations.isEmpty()) {
                        Text("Sin sublugares registrados.", style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(loc.subLocations, key = { it.id }) { s ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.background(AppColors.SurfaceVariant, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text(s.name.uppercase(), style = MaterialTheme.typography.labelSmall, color = AppColors.TextPrimary)
                                    if (isAdmin) {
                                        Spacer(Modifier.size(6.dp))
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Eliminar sublugar",
                                            tint = AppColors.TextFaint,
                                            modifier = Modifier.size(14.dp).clip(CircleShape).clickable { viewModel.requestDeleteSubLocation(s) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (isAdmin) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppTextField(
                                value = state.newSubLocation,
                                onValueChange = viewModel::onNewSubLocationChange,
                                placeholder = { Text("Nuevo sublugar…") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.size(8.dp))
                            Button(
                                onClick = viewModel::addSubLocation,
                                enabled = state.newSubLocation.isNotBlank(),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
                            ) { Icon(Icons.Filled.Add, contentDescription = "Agregar", tint = AppColors.Surface) }
                        }
                    }
                }

                if (loc.devices.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    AppSurfaceCard(Modifier.fillMaxWidth()) {
                        SectionLabel("Dispositivos (${loc.devices.size})")
                        loc.devices.forEach { d ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(d.assetTag, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                                    Text("${d.brand} ${d.model}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                                }
                                StatusChip(deviceStatusLabel(d.status), AppColors.deviceStatusColor(d.status))
                            }
                        }
                    }
                }

                if (loc.custodyLetters.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    AppSurfaceCard(Modifier.fillMaxWidth()) {
                        SectionLabel("Cartas responsivas (${loc.custodyLetters.size})")
                        loc.custodyLetters.forEach { c ->
                            Column(Modifier.padding(vertical = 4.dp)) {
                                Text("${c.consecutive} · ${c.custodian?.name ?: "—"}", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                                Text(
                                    "${c.items.size} artículo(s) · ${formatShortDate(c.date)}${if (c.returnDate != null) " · devuelta" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextFaint,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            state.subLocationToDelete?.let { s ->
                AppModal(
                    title = "Eliminar sublugar",
                    icon = Icons.Filled.DeleteOutline,
                    tone = AppModalTone.Danger,
                    onDismiss = viewModel::dismissDeleteSubLocation,
                    confirmLabel = "Eliminar",
                    saving = state.saving,
                    onConfirm = viewModel::confirmDeleteSubLocation,
                ) {
                    Text("¿Eliminar el sublugar \"${s.name}\"?", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextMuted)
                }
            }
        }
    }
}
