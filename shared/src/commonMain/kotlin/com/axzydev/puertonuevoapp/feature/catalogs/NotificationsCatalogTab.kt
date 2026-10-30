package com.axzydev.puertonuevoapp.feature.catalogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.network.catalogs.CatalogsApi
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.session.SessionUser
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip

/**
 * Pestaña de configuración de notificaciones (`sys-config`): destinatarios en
 * copia, interruptor del correo transaccional y primer día de la semana.
 *
 * Leer una clave está abierto a la sesión; guardar exige `system.configure`.
 * Sin ese permiso la pestaña se ve en solo lectura, con los valores vigentes.
 */
@Composable
internal fun NotificationsCatalogTab(
    user: SessionUser,
    modifier: Modifier = Modifier,
) {
    val viewModel: NotificationsCatalogViewModel = viewModel {
        NotificationsCatalogViewModel(CatalogsApi(AppContainer.apiClient))
    }
    val state by viewModel.uiState.collectAsState()
    val canEdit = user.can(PermissionKeys.SYSTEM_CONFIGURE)

    when {
        state.loading -> Box(modifier = modifier.fillMaxSize().padding(16.dp)) {
            BentoState(message = "Cargando la configuración…", loading = true)
        }
        state.error != null -> Box(modifier = modifier.fillMaxSize().padding(16.dp)) {
            BentoErrorState(
                message = state.error ?: "No se pudo cargar la configuración",
                onRetry = viewModel::load,
            )
        }
        else -> Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!canEdit) {
                BentoState(
                    message = "Solo lectura: se requiere el permiso de configuración del sistema para guardar cambios.",
                    tone = BentoTone.Sun,
                    icon = Icons.Filled.Lock,
                )
            }

            state.actionError?.let { message ->
                Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
            }

            RecipientsCard(
                state = state,
                canEdit = canEdit,
                onChange = viewModel::onRecipientsChange,
                onReset = viewModel::resetRecipients,
                onSave = viewModel::saveRecipients,
            )

            SendEmailCard(
                state = state,
                canEdit = canEdit,
                onChange = viewModel::onSendEmailChange,
                onReset = viewModel::resetSendEmail,
                onSave = viewModel::saveSendEmail,
            )

            WeekStartCard(
                state = state,
                canEdit = canEdit,
                onChange = viewModel::onWeekStartChange,
                onReset = viewModel::resetWeekStart,
                onSave = viewModel::saveWeekStart,
            )
        }
    }
}

@Composable
private fun RecipientsCard(
    state: NotificationsCatalogUiState,
    canEdit: Boolean,
    onChange: (String) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("Destinatarios en copia (CC)")
        Spacer(Modifier.height(4.dp))
        Text(
            "Estos correos reciben una copia de las notificaciones del sistema. Sepáralos con coma.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        Spacer(Modifier.height(10.dp))
        if (canEdit) {
            AppTextField(
                value = state.recipientsDraft,
                onValueChange = onChange,
                label = { Text("Correos") },
                placeholder = { Text("correo1@dominio.com, correo2@dominio.com") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            ConfigActions(
                dirty = state.recipientsDirty,
                saving = state.savingRecipients,
                onReset = onReset,
                onSave = onSave,
            )
        } else {
            Text(
                state.recipients.ifBlank { "Sin destinatarios configurados" },
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary,
            )
        }
    }
}

@Composable
private fun SendEmailCard(
    state: NotificationsCatalogUiState,
    canEdit: Boolean,
    onChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Correo transaccional",
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Si lo apagas, el sistema no envía correos de notificación (las notificaciones dentro de la app siguen funcionando).",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                )
            }
            Spacer(Modifier.width(10.dp))
            if (canEdit) {
                Switch(checked = state.sendEmailDraft, onCheckedChange = onChange)
            } else {
                StatusChip(
                    label = if (state.sendEmail) "Activado" else "Desactivado",
                    tone = if (state.sendEmail) BentoTone.Mint else BentoTone.Rose,
                )
            }
        }
        if (canEdit) {
            Spacer(Modifier.height(10.dp))
            ConfigActions(
                dirty = state.sendEmailDirty,
                saving = state.savingSendEmail,
                onReset = onReset,
                onSave = onSave,
            )
        }
    }
}

@Composable
private fun WeekStartCard(
    state: NotificationsCatalogUiState,
    canEdit: Boolean,
    onChange: (String) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("Primer día de la semana")
        Spacer(Modifier.height(4.dp))
        Text(
            "Define dónde empiezan las semanas de los filtros y reportes. El cliente opera de miércoles a miércoles.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextMuted,
        )
        Spacer(Modifier.height(10.dp))
        if (canEdit) {
            SimpleDropdownField(
                label = "Día",
                value = state.weekStartDraft,
                options = weekStartDayOptions,
                onSelect = onChange,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            ConfigActions(
                dirty = state.weekStartDirty,
                saving = state.savingWeekStart,
                onReset = onReset,
                onSave = onSave,
            )
        } else {
            Text(
                weekStartDayOptions.firstOrNull { it.first == state.weekStartDay }?.second ?: state.weekStartDay,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary,
            )
        }
    }
}

/** Botones estándar de una tarjeta de configuración: descartar y guardar. */
@Composable
private fun ConfigActions(
    dirty: Boolean,
    saving: Boolean,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PillButton(
            text = "Descartar",
            onClick = onReset,
            variant = PillButtonVariant.Ghost,
            enabled = dirty && !saving,
            modifier = Modifier.weight(1f),
        )
        PillButton(
            text = "Guardar",
            onClick = onSave,
            enabled = dirty && !saving,
            loading = saving,
            modifier = Modifier.weight(1f),
        )
    }
}
