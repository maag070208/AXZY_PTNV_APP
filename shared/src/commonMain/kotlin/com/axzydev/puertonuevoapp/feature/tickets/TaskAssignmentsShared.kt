package com.axzydev.puertonuevoapp.feature.tickets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.network.KanbanAssignmentDto
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import com.axzydev.puertonuevoapp.core.util.isOverdue

internal fun assignmentStatusLabel(status: String): String = when (status) {
    "PENDIENTE" -> "Pendiente"
    "EN_PROGRESO" -> "En progreso"
    "EN_REVISION" -> "En revisión"
    "COMPLETADA" -> "Completada"
    else -> status
}

internal fun assignmentStatusColor(status: String): Color = when (status) {
    "PENDIENTE" -> AppColors.TextFaint
    "EN_PROGRESO" -> AppColors.Info
    "EN_REVISION" -> AppColors.Purple
    "COMPLETADA" -> AppColors.Success
    else -> AppColors.TextFaint
}

internal val assignmentStatusOrder = listOf("PENDIENTE", "EN_PROGRESO", "EN_REVISION", "COMPLETADA")

internal fun KanbanAssignmentDto.overdue(): Boolean = isOverdue(dueDate, status == "COMPLETADA")

@Composable
internal fun AssignmentRow(assignment: KanbanAssignmentDto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(assignment.title, style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
                Text(assignment.ticket.titulo, style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
            }
            StatusChip(assignmentStatusLabel(assignment.status), assignmentStatusColor(assignment.status))
        }
        if (assignment.description.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(assignment.description, style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted, maxLines = 2)
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(assignment.user.name, style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (assignment.overdue()) {
                    StatusChip("Vencida", AppColors.Danger)
                    Spacer(Modifier.width(6.dp))
                }
                assignment.dueDate?.let {
                    Text(formatShortDate(it), style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint)
                }
            }
        }
    }
}
