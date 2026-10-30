package com.axzydev.puertonuevoapp.feature.tickets

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.SectionLabel

/**
 * Piezas de presentación compartidas por las pantallas de Tickets.
 *
 * El estado y la prioridad se pintan SIEMPRE con el chip estándar y tonos bento
 * (no con colores sueltos), para que lista, tablero y detalle se lean como la
 * misma familia visual.
 */

/** Folio corto y legible del ticket (mismos 8 caracteres que usa la web). */
fun ticketFolio(id: String): String = "#" + id.take(8).uppercase()

/** Tono del estado: ámbar = pendiente de atención, océano = en curso, menta = cerrado. */
fun ticketStatusTone(status: String): BentoTone = when (status) {
    "OPEN" -> BentoTone.Sun
    "IN_PROGRESS" -> BentoTone.Sea
    "CLOSED" -> BentoTone.Mint
    else -> BentoTone.Plain
}

/** Tono de la prioridad: sin color para baja y rosa para lo urgente. */
fun ticketPriorityTone(priority: String): BentoTone = when (priority) {
    "LOW" -> BentoTone.Plain
    "MEDIUM" -> BentoTone.Sun
    "HIGH", "URGENT" -> BentoTone.Rose
    else -> BentoTone.Plain
}

/** Tono del estado de una tarea del tablero (mismo criterio que el kanban). */
fun assignmentStatusTone(status: String): BentoTone = when (status) {
    "PENDING" -> BentoTone.Sun
    "IN_PROGRESS" -> BentoTone.Sea
    "IN_REVIEW" -> BentoTone.Lavender
    "COMPLETED" -> BentoTone.Mint
    else -> BentoTone.Plain
}

/** Sección de una pantalla de ticket: una tarjeta con título y una idea. */
@Composable
fun TicketSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppSurfaceCard(modifier = modifier.fillMaxWidth()) {
        SectionLabel(title)
        content()
    }
}
