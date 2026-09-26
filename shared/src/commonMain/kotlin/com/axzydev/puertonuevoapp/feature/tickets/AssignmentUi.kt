package com.axzydev.puertonuevoapp.feature.tickets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.network.tickets.KanbanAssignmentDto
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.assignmentStatusLabel
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import com.axzydev.puertonuevoapp.core.util.isOverdue

/** Estados del kanban de tareas, en el orden en el que se muestran las columnas. */
val assignmentStatusOrder = listOf("PENDING", "IN_PROGRESS", "IN_REVIEW", "COMPLETED")

fun assignmentStatusColor(status: String): Color = when (status) {
    "PENDING" -> AppColors.TextFaint
    "IN_PROGRESS" -> AppColors.Info
    "IN_REVIEW" -> AppColors.Purple
    "COMPLETED" -> AppColors.Success
    else -> AppColors.TextFaint
}

fun KanbanAssignmentDto.overdue(): Boolean = isOverdue(dueDate, status == "COMPLETED")

@Composable
fun AssignmentRow(assignment: KanbanAssignmentDto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        borderColor = null,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(assignment.title, style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
                Text(assignment.ticket.title, style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
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
