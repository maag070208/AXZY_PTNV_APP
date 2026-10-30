package com.axzydev.puertonuevoapp.feature.inventory

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.BentoIconButton
import com.axzydev.puertonuevoapp.core.ui.BentoMiniStat
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.SectionLabel

/**
 * Resumen de Inventario: la foto de disponibilidad (¿dónde está el equipo?) en
 * una franja de KPIs compactos y dos paneles —ubicaciones y kardex— con una
 * sola idea cada uno. La acción primaria (nuevo movimiento) va en el
 * encabezado, no flotando sobre la lista.
 */
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                // Encabezado: descripción + acción primaria.
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Inventario y disponibilidad",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextMuted,
                        modifier = Modifier.weight(1f),
                    )
                    if (canRegister) {
                        Spacer(Modifier.width(10.dp))
                        PillButton(
                            text = "Nuevo movimiento",
                            onClick = { navigator.push(Screen.NewInventoryMovement()) },
                            variant = PillButtonVariant.Secondary,
                            leadingIcon = Icons.Filled.Add,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Franja de indicadores: tipo de dato por tono, en cero = neutral.
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BentoMiniStat(
                        label = "Total",
                        value = s.stats.totalDevices.toString(),
                        tone = BentoTone.Sea,
                        modifier = Modifier.weight(1f),
                    )
                    BentoMiniStat(
                        label = "En ubicación",
                        value = s.stats.locatedDevices.toString(),
                        tone = if (s.stats.locatedDevices > 0) BentoTone.Mint else BentoTone.Plain,
                        modifier = Modifier.weight(1f),
                    )
                    BentoMiniStat(
                        label = "Sin ubicación",
                        value = s.stats.unlocatedDevices.toString(),
                        tone = if (s.stats.unlocatedDevices > 0) BentoTone.Sun else BentoTone.Plain,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(16.dp))

                AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(AppColors.Info.copy(alpha = 0.12f), AppShape.icon),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Warehouse,
                                contentDescription = null,
                                tint = AppColors.Info,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Ubicaciones",
                                style = MaterialTheme.typography.titleSmall,
                                color = AppColors.TextPrimary,
                            )
                            Text(
                                "${s.locations.size} registradas",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextFaint,
                            )
                        }
                        BentoIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Ver todas las ubicaciones",
                            onClick = { navigator.push(Screen.LocationsList) },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    if (s.locations.isEmpty()) {
                        Text(
                            "Aún no hay ubicaciones registradas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextFaint,
                        )
                    } else {
                        s.locations.take(5).forEach { loc ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(AppColors.Info.copy(alpha = 0.12f), AppShape.pill),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            tint = AppColors.Info,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        loc.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AppColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    "${loc.count?.devices ?: 0} equipos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextMuted,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(AppColors.EmeraldPrimary.copy(alpha = 0.12f), AppShape.icon),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.SwapHoriz,
                                contentDescription = null,
                                tint = AppColors.EmeraldPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Kardex",
                                style = MaterialTheme.typography.titleSmall,
                                color = AppColors.TextPrimary,
                            )
                            Text(
                                "Entradas, salidas, bajas y reversiones",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextFaint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        BentoIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Ver kardex de movimientos",
                            onClick = { navigator.push(Screen.InventoryMovements) },
                        )
                    }
                }

                if (s.stats.totalDevices == 0) {
                    Spacer(Modifier.height(14.dp))
                    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Inventory2,
                                contentDescription = null,
                                tint = AppColors.TextFaint,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Todavía no hay dispositivos en el inventario. Da de alta el primero desde Dispositivos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextMuted,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
