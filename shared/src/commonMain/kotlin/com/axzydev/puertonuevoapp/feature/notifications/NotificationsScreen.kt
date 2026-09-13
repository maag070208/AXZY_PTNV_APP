package com.axzydev.puertonuevoapp.feature.notifications

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.axzydev.puertonuevoapp.core.network.NotificationDto
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.util.formatDateTime
import kotlinx.coroutines.launch

private fun typeIcon(type: String) = when (type) {
    "COMMENT" -> Icons.Filled.Comment
    else -> Icons.Filled.ConfirmationNumber
}

private fun typeColor(type: String) = when (type) {
    "COMMENT" -> AppColors.Info
    "ASSIGNED" -> AppColors.EmeraldPrimary
    "TICKET_UPDATED" -> AppColors.Warning
    "TICKET_CREATED" -> AppColors.Success
    else -> AppColors.TextFaint
}

@Composable
fun NotificationsScreen() {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()

    var notifications by remember { mutableStateOf<List<NotificationDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var unreadCount by remember { mutableStateOf(0) }

    suspend fun load() {
        loading = true
        error = null
        try {
            notifications = AppContainer.notificationsApi.list()
            unreadCount = notifications.count { !it.read }
        } catch (e: Exception) {
            error = e.message ?: "No se pudieron cargar las notificaciones"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    fun onOpen(n: NotificationDto) {
        if (!n.read) {
            scope.launch {
                try {
                    AppContainer.notificationsApi.markRead(n.id)
                    notifications = notifications.map { if (it.id == n.id) it.copy(read = true) else it }
                } catch (_: Exception) { }
            }
        }
        n.ticketId?.let { navigator.push(Screen.TicketDetail(it)) }
    }

    fun onDelete(n: NotificationDto) {
        scope.launch {
            try {
                AppContainer.notificationsApi.remove(n.id)
                notifications = notifications.filter { it.id != n.id }
            } catch (_: Exception) { }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${notifications.size} notificación(es)", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
            if (notifications.any { !it.read }) {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            AppContainer.notificationsApi.markAllRead()
                            notifications = notifications.map { it.copy(read = true) }
                        } catch (_: Exception) { }
                    }
                }) {
                    Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp), tint = AppColors.EmeraldPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text("Marcar todo leído", color = AppColors.EmeraldPrimary)
                }
            }
        }

        when {
            loading -> LoadingState(modifier = Modifier.weight(1f))
            error != null -> ErrorState(error ?: "Error", Modifier.weight(1f), onRetry = { scope.launch { load() } })
            notifications.isEmpty() -> EmptyState("Sin notificaciones", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(notifications, key = { it.id }) { n ->
                    NotificationCard(n, onClick = { onOpen(n) }, onDelete = { onDelete(n) })
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(n: NotificationDto, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (n.read) AppColors.Surface else AppColors.EmeraldContainer.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(typeColor(n.type), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(typeIcon(n.type), contentDescription = null, tint = AppColors.Surface, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                n.title,
                style = if (n.read) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
                color = if (n.read) AppColors.TextMuted else AppColors.TextPrimary,
            )
            n.detail?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint, maxLines = 2)
            }
            Spacer(Modifier.height(4.dp))
            Text(formatDateTime(n.createdAt), style = MaterialTheme.typography.labelSmall, color = AppColors.TextFaint)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar", tint = AppColors.TextFaint, modifier = Modifier.size(16.dp))
        }
    }
}
