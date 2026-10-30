package com.axzydev.puertonuevoapp.feature.hr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.BentoAvatar
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatDateTime
import com.axzydev.puertonuevoapp.core.util.formatShortDate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import com.axzydev.puertonuevoapp.core.theme.BentoTone

/**
 * Detalle de un acta administrativa: vista en pantalla del acta (persona,
 * motivo, fecha del incidente, hechos, sanción y quién la levantó) y
 * eliminación con confirmación.
 *
 * El **PDF del acta se genera en la web** (render client-side) y el API no
 * tiene endpoint de PDF, así que aquí no se ofrece descarga: solo la vista en
 * pantalla. Requiere `hr.disciplinary_reports`.
 */
@Composable
fun DisciplinaryReportDetailScreen(reportId: String) {
    val viewModel: DisciplinaryReportDetailViewModel = viewModel(key = "disciplinary-report-$reportId") {
        DisciplinaryReportDetailViewModel(reportId, AppContainer.hrApi)
    }
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canView = user?.can(PermissionKeys.HR_DISCIPLINARY_REPORTS) == true

    LaunchedEffect(state.deleted) { if (state.deleted) navigator.pop() }

    if (!canView) {
        Column(modifier = Modifier.fillMaxSize().background(AppBento.canvas).padding(16.dp)) {
            BentoState(message = "Necesitas el permiso de actas administrativas (hr.disciplinary_reports).")
        }
        return
    }

    if (state.loading) {
        LoadingState(modifier = Modifier.fillMaxSize())
        return
    }
    val report = state.report
    if (report == null) {
        ErrorState(state.error ?: "Acta no encontrada", Modifier.fillMaxSize(), onRetry = viewModel::load)
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().background(AppBento.canvas)
            .verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BentoAvatar(name = report.user.name, color = AppColors.Danger, size = 48.dp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        report.user.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(
                            report.user.employeeNumber?.let { "#$it" },
                            report.user.jobTitle,
                            report.user.department?.name,
                            report.user.subarea?.name,
                        ).joinToString(" · ").ifBlank { "Sin datos de puesto" },
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StatusChip(disciplinaryReasonLabel(report.reason), toneOf(reasonColor(report.reason)))
            }
        }

        Spacer(Modifier.height(12.dp))

        AppCard(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Hechos del incidente")
            DetailLine("Fecha del incidente", formatShortDate(report.incidentDate))
            Spacer(Modifier.height(8.dp))
            Text(
                "Descripción del hecho",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                report.description,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Sanción / medida aplicada",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                report.sanction?.takeIf { it.isNotBlank() } ?: "Sin sanción registrada",
                style = MaterialTheme.typography.bodyMedium,
                color = if (report.sanction.isNullOrBlank()) AppColors.TextMuted else AppColors.TextPrimary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Levantada por ${report.createdBy.name} · ${formatDateTime(report.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "El PDF del acta se genera en la web; el API no expone descarga, así que la app muestra la vista en pantalla.",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextFaint,
        )

        Spacer(Modifier.height(12.dp))
        PillButton(
            text = "Eliminar acta",
            onClick = viewModel::requestDelete,
            variant = PillButtonVariant.Ghost,
            leadingIcon = Icons.Filled.DeleteOutline,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
    }

    if (state.showDeleteConfirm) {
        AppModal(
            title = "Eliminar acta administrativa",
            icon = Icons.Filled.DeleteOutline,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissDelete,
            confirmLabel = "Eliminar",
            saving = state.deleting,
            onConfirm = viewModel::confirmDelete,
        ) {
            Text(
                "¿Eliminar el acta administrativa de ${report.user.name}? Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
        }
    }

    state.deleteError?.let { message ->
        AppModal(
            title = "No se pudo eliminar",
            icon = Icons.Filled.ErrorOutline,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissDeleteError,
        ) {
            BentoErrorState(message = message, onRetry = null)
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextFaint,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.6f),
        )
    }
}

/** Tono bento equivalente a un color semántico (chips con contraste AA). */
private fun toneOf(color: Color): BentoTone = when (color) {
    AppColors.Success -> BentoTone.Mint
    AppColors.Warning -> BentoTone.Sun
    AppColors.Danger -> BentoTone.Rose
    AppColors.Info -> BentoTone.Lavender
    AppColors.EmeraldPrimary -> BentoTone.Sea
    else -> BentoTone.Plain
}
