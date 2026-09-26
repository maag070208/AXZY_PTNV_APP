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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.StatCard

@Composable
fun InventoryIndexScreen(viewModel: InventoryIndexViewModel = viewModel { InventoryIndexViewModel(AppContainer.inventoryApi) }) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val canRegister = (authState as? AuthState.LoggedIn)?.user?.canRegisterMovement == true

    when {
        state.loading -> LoadingState(Modifier.fillMaxSize())
        state.error != null || state.summary == null -> ErrorState(state.error ?: "Error", Modifier.fillMaxSize(), onRetry = viewModel::load)
        else -> {
            val s = state.summary!!
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Inventario y disponibilidad", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                    if (canRegister) {
                        IconButton(onClick = { navigator.push(Screen.NewInventoryMovement()) }) {
                            Icon(Icons.Filled.Add, contentDescription = "Nuevo movimiento", tint = AppColors.EmeraldPrimary)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        value = "${s.stats.totalDevices}",
                        label = "Total",
                        color = AppColors.Info,
                        icon = { Icon(Icons.Filled.Inventory2, contentDescription = null, tint = AppColors.Info, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        value = "${s.stats.locatedDevices}",
                        label = "En ubicación",
                        color = AppColors.Success,
                        icon = { Icon(Icons.Filled.Warehouse, contentDescription = null, tint = AppColors.Success, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        value = "${s.stats.unlocatedDevices}",
                        label = "Sin ubicación",
                        color = if (s.stats.unlocatedDevices > 0) AppColors.Warning else AppColors.TextFaint,
                        icon = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = if (s.stats.unlocatedDevices > 0) AppColors.Warning else AppColors.TextFaint, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(16.dp))

                AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("Ubicaciones (${s.locations.size})")
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(AppShape.row).clickable { navigator.push(Screen.LocationsList) }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("Ver todas", style = MaterialTheme.typography.labelSmall, color = AppColors.EmeraldPrimary)
                            Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = AppColors.EmeraldPrimary, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (s.locations.isEmpty()) {
                        Text("Aún no hay ubicaciones registradas.", style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
                    } else {
                        s.locations.take(5).forEach { loc ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(28.dp).background(AppColors.Info.copy(alpha = 0.12f), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = AppColors.Info, modifier = Modifier.size(14.dp)) }
                                    Spacer(Modifier.size(8.dp))
                                    Text(loc.name, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                                }
                                Text("${loc.count?.devices ?: 0}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            SectionLabel("Movimientos")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(AppShape.row).clickable { navigator.push(Screen.InventoryMovements) }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("Ver kardex", style = MaterialTheme.typography.labelSmall, color = AppColors.EmeraldPrimary)
                            Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = AppColors.EmeraldPrimary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
