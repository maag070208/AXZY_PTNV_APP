package com.axzydev.puertonuevoapp.feature.tickets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.network.tickets.KanbanAssignmentDto
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.BentoAvatar
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.assignmentStatusLabel
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import com.axzydev.puertonuevoapp.core.util.isOverdue

/** Estados del kanban de tareas, en el orden en el que se muestran las columnas. */
val assignmentStatusOrder = listOf("PENDING", "IN_PROGRESS", "IN_REVIEW", "COMPLETED")

fun KanbanAssignmentDto.overdue(): Boolean = isOverdue(dueDate, status == "COMPLETED")

/**
 * Tarjeta de una tarea del tablero / listado de tareas: título (2 líneas),
 * ticket al que pertenece, descripción solo si aporta algo distinto, y al pie
 * el responsable con su avatar y la fecha límite (o el chip "Vencida").
 *
 * [showStatus] se apaga en el kanban: la columna ya dice el estado y repetir el
 * chip en cada tarjeta duplicaba la información.
 */
@Composable
fun AssignmentRow(
    assignment: KanbanAssignmentDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showStatus: Boolean = true,
) {
    val description = assignment.description.trim()
    val showDescription = description.isNotBlank() &&
        !assignment.title.trim().contains(description, ignoreCase = true) &&
        !description.contains(assignment.title.trim(), ignoreCase = true)

    AppCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        borderColor = null,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    assignment.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    assignment.ticket.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showStatus) {
                Spacer(Modifier.width(8.dp))
                StatusChip(assignmentStatusLabel(assignment.status), assignmentStatusTone(assignment.status))
            }
        }

        if (showDescription) {
            Spacer(Modifier.height(6.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BentoAvatar(
                name = assignment.user.name,
                color = AppColors.EmeraldPrimary,
                size = 24.dp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                assignment.user.name,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (assignment.overdue()) {
                StatusChip("Vencida", BentoTone.Rose)
                Spacer(Modifier.width(6.dp))
            }
            assignment.dueDate?.let { dueDate ->
                Text(
                    formatShortDate(dueDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                    maxLines = 1,
                )
            }
        }
    }
}
