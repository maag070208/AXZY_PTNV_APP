package com.axzydev.puertonuevoapp.feature.linen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppChrome
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.BentoActionButton
import com.axzydev.puertonuevoapp.core.ui.BentoSegment
import com.axzydev.puertonuevoapp.core.ui.BentoSegmentedBar
import com.axzydev.puertonuevoapp.core.ui.BentoShortcut
import com.axzydev.puertonuevoapp.core.ui.BentoIconSquare

/**
 * Inicio del puesto solo de blancos: saludo, resumen del día (pendientes / en proceso / listas),
 * accesos según permisos, pedidos recientes y [equipment] ("Mi equipo", lo pinta el Inicio).
 */
@Composable
fun LinenHomeScreen(equipment: @Composable () -> Unit, viewModel: LinenViewModel = linenViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val user = currentLinenUser() ?: return
    val navigator = LocalNavigator.current
    LinenPolling(viewModel)

    val pending = state.count(LinenBucket.Pending)
    val inProgress = state.count(LinenBucket.InProgress)
    val done = state.count(LinenBucket.Done)
    val hero = AppBento.tone(BentoTone.Brand)

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize().background(AppBento.canvas),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Hero: pendientes de hoy.
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = hero.container,
                borderColor = null,
                shape = AppShape.bento,
                elevation = 8.dp,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            ) {
                Text(
                    "Blancos · pendientes hoy",
                    style = MaterialTheme.typography.labelLarge,
                    color = hero.content.copy(alpha = 0.85f),
                )
                Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                    Text(
                        pending.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = hero.content,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (pending == 1) "habitación por entregar" else "habitaciones por entregar",
                        style = MaterialTheme.typography.bodyLarge,
                        color = hero.content.copy(alpha = 0.9f),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Spacer(Modifier.padding(top = 12.dp))
                BentoSegmentedBar(
                    listOf(
                        BentoSegment("Pendientes", pending, AppChrome.SegmentPending),
                        BentoSegment("En proceso", inProgress, AppChrome.SegmentInProgress),
                        BentoSegment("Listas", done, AppChrome.SegmentDone),
                    ),
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("$pending pendientes", "$inProgress en proceso", "$done listas").forEach {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = hero.content.copy(alpha = 0.85f))
                    }
                }
                BentoActionButton(
                    label = "Ver habitaciones",
                    onClick = { navigator.switchTab(Screen.LinenOrders) },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    containerColor = hero.content,
                    contentColor = hero.container,
                )
            }

            // Accesos (solo lo que el rol puede usar).
            Text("Accesos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            AppCard(modifier = Modifier.fillMaxWidth(), shape = AppShape.listCard, borderColor = null, elevation = 1.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BentoShortcut("Blancos", Icons.Filled.Bed, BentoTone.Sea, { navigator.switchTab(Screen.LinenOrders) }, Modifier.weight(1f))
                    if (user.canChangeRoomStatus) {
                        BentoShortcut("Habitaciones", Icons.Filled.MeetingRoom, BentoTone.Sea, { navigator.push(Screen.LinenRooms) }, Modifier.weight(1f))
                    }
                    if (user.canUseHousekeeping) {
                        BentoShortcut("Limpieza", Icons.Filled.CleaningServices, BentoTone.Mint, { navigator.push(Screen.Housekeeping) }, Modifier.weight(1f))
                    }
                    BentoShortcut("Equipo", Icons.Filled.Groups, BentoTone.Sun, { navigator.switchTab(Screen.LinenEquipment) }, Modifier.weight(1f))
                    BentoShortcut("Historial", Icons.Filled.History, BentoTone.Mint, { navigator.push(Screen.LinenHistory) }, Modifier.weight(1f))
                }
            }

            // Recientes.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Recientes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Ver todo",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(AppShape.row).clickable { navigator.switchTab(Screen.LinenOrders) }.padding(8.dp),
                )
            }
            val recent = state.today.sortedByDescending { it.updatedAt ?: it.createdAt }.take(3)
            if (recent.isEmpty()) {
                Text("Sin pedidos de blancos hoy.", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextFaint)
            }
            if (recent.isNotEmpty()) {
                AppCard(modifier = Modifier.fillMaxWidth(), shape = AppShape.listCard, borderColor = null, elevation = 1.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    recent.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(color = AppColors.Outline.copy(alpha = 0.4f))
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { navigator.switchTab(Screen.LinenOrders) }.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BentoIconSquare(Icons.Filled.Bed, item.bucket.tone(), size = 42.dp, shape = AppShape.iconRowSmall)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.room?.number ?: item.folio, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                                Text(
                                    when (item.bucket) {
                                        LinenBucket.Pending -> "Se desocupó"
                                        LinenBucket.InProgress -> "En proceso"
                                        LinenBucket.Done -> "Lista"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppBento.tone(item.bucket.tone()).content,
                                )
                            }
                            Text(
                                sinceText(item.updatedAt ?: item.createdAt).replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                color = AppColors.TextMuted,
                            )
                        }
                    }
                }
            }

            // Mi equipo (contenido existente del Inicio).
            Text("Mi equipo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            equipment()
            Spacer(Modifier.padding(bottom = 24.dp))
        }
    }
}
