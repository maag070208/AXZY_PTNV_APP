package com.axzydev.puertonuevoapp.feature.hr

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.hr.EmployeeDocumentDto
import com.axzydev.puertonuevoapp.core.network.hr.EmployeeProfileDto
import com.axzydev.puertonuevoapp.core.platform.FileMimeTypes
import com.axzydev.puertonuevoapp.core.platform.rememberFilePicker
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.AttachmentFileRow
import com.axzydev.puertonuevoapp.core.ui.AttachmentPickerField
import com.axzydev.puertonuevoapp.core.ui.BentoErrorState
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.RemoteImage
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.formatDateTime
import com.axzydev.puertonuevoapp.core.util.formatFileSize
import com.axzydev.puertonuevoapp.core.util.roleLabel

/**
 * Expediente de una persona (RH): foto, documentos del expediente con sus
 * obligatorios faltantes, datos en solo lectura, edición del perfil por
 * secciones, descuentos de nómina y avisos.
 *
 * Requiere `hr.records` (el API lo vuelve a validar); las acciones se ocultan
 * sin permiso. Referencia web: `web/src/pages/employees/EmployeeDetailPage.tsx`
 * y `features/hr/employee-detail/ui/{EmployeeDocumentsCard,EmployeeProfileEditDialog}.tsx`.
 */
@Composable
fun EmployeeProfileScreen(personId: String) {
    val viewModel: EmployeeProfileViewModel = viewModel(key = "employee-profile-$personId") {
        EmployeeProfileViewModel(personId, AppContainer.hrApi)
    }
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val canView = user?.can(PermissionKeys.HR_RECORDS) == true
    val canEditAccount = user?.canEditUsers == true

    // Foto del expediente: se sube con el selector del sistema y se refresca por
    // versión (`/hr/:id/photo/raw?v=N`), porque la URL del API apunta a S3.
    val pickPhoto = rememberFilePicker(
        mimeTypes = listOf(FileMimeTypes.IMAGE),
        onPicked = viewModel::uploadPhoto,
    )

    // Al volver de "Editar cuenta" (otra pantalla) el expediente pudo cambiar.
    LaunchedEffect(Unit) { viewModel.onReturnToScreen() }
    DisposableEffect(Unit) { onDispose { viewModel.onLeaveScreen() } }

    when {
        user != null && !canView -> Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            BentoErrorState(
                message = "No tienes permiso para consultar expedientes (hr.records). Pídeselo a quien administra los permisos.",
                onRetry = null,
            )
        }

        state.loading -> Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            BentoState(message = "Cargando expediente…", loading = true)
        }

        state.error != null && state.profile == null -> Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            BentoErrorState(
                message = state.error ?: "No se pudo cargar el expediente",
                onRetry = viewModel::load,
            )
        }

        else -> {
            val profile = state.profile ?: return
            EmployeeProfileDialogs(state = state, profile = profile, viewModel = viewModel)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    EmployeeProfileHeader(
                        profile = profile,
                        state = state,
                        canEdit = canView,
                        canEditAccount = canEditAccount,
                        onToggleEdit = {
                            if (state.editing) viewModel.cancelEdit() else viewModel.startEdit()
                        },
                        onPickPhoto = pickPhoto,
                        onEditAccount = { navigator.push(Screen.UserForm(profile.id)) },
                    )
                }

                if (state.editing) {
                    item {
                        EmployeeProfileEditForm(
                            state = state,
                            onFieldChange = viewModel::onEditFieldChange,
                            onToggleDiscount = viewModel::onToggleDiscount,
                            onDiscountNoteChange = viewModel::onDiscountNoteChange,
                        )
                    }
                    state.editError?.let { message ->
                        item { BentoErrorState(message = message, onRetry = null) }
                    }
                    item {
                        EmployeeProfileEditActions(
                            state = state,
                            onCancel = viewModel::cancelEdit,
                            onSave = viewModel::saveEdit,
                        )
                    }
                } else {
                    item {
                        EmployeeDocumentsCard(
                            state = state,
                            profile = profile,
                            canEdit = canView,
                            viewModel = viewModel,
                        )
                    }
                    item { EmployeeProfileViewSections(profile = profile) }
                }

                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

/** Cabecera del expediente: foto, identidad, adscripción y acciones por permiso. */
@Composable
private fun EmployeeProfileHeader(
    profile: EmployeeProfileDto,
    state: EmployeeProfileUiState,
    canEdit: Boolean,
    canEditAccount: Boolean,
    onToggleEdit: () -> Unit,
    onPickPhoto: () -> Unit,
    onEditAccount: () -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                RemoteImage(
                    path = state.photoPath,
                    loadBytes = { AppContainer.apiClient.getBytes(it) },
                    modifier = Modifier.size(64.dp).clip(CircleShape),
                    contentDescription = profile.name,
                )
                if (state.photoUploading) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(AppColors.TextPrimary.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("@${profile.username}", style = MaterialTheme.typography.labelMedium, color = AppColors.TextFaint)
                Text(
                    listOfNotNull(
                        profile.jobTitle?.takeIf { it.isNotBlank() },
                        roleLabel(profile.role),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusChip(
                label = if (profile.active) "Activo" else "Inactivo",
                color = if (profile.active) AppColors.Success else AppColors.Danger,
            )
        }

        val tags = listOfNotNull(
            profile.department?.let { it.name to BentoTone.Sea },
            profile.subarea?.let { it.name to BentoTone.Lavender },
            profile.employeeNumber?.takeIf { it.isNotBlank() }?.let { "N. $it" to BentoTone.Plain },
        )
        if (tags.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { (label, tone) -> StatusChip(label, tone) }
            }
        }

        if (canEdit) {
            Spacer(Modifier.height(14.dp))
            if (state.editing) {
                Text(
                    "Editando el expediente: los campos vacíos se guardan como “sin dato”.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                )
            } else {
                PillButton(
                    text = "Editar expediente",
                    onClick = onToggleEdit,
                    leadingIcon = Icons.Filled.Edit,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton(
                        text = "Cambiar foto",
                        onClick = onPickPhoto,
                        variant = PillButtonVariant.Secondary,
                        enabled = !state.photoUploading,
                        loading = state.photoUploading,
                        modifier = Modifier.weight(1f),
                    )
                    if (canEditAccount) {
                        PillButton(
                            text = "Editar cuenta",
                            onClick = onEditAccount,
                            variant = PillButtonVariant.Secondary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Documentos del expediente: resumen, obligatorios faltantes, alta de un
 * documento por tipo y archivos por tipo con compartir/eliminar. Los avisos a
 * la persona viven al final (notificación/correo).
 */
@Composable
private fun EmployeeDocumentsCard(
    state: EmployeeProfileUiState,
    profile: EmployeeProfileDto,
    canEdit: Boolean,
    viewModel: EmployeeProfileViewModel,
) {
    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("Documentos")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(
                label = "${state.coveredTypeCount} de ${state.documentTypes.size} tipos",
                tone = BentoTone.Sea,
            )
            if (state.requiredMissing.isNotEmpty()) {
                StatusChip(
                    label = "${state.requiredMissing.size} obligatorios pendientes",
                    tone = BentoTone.Rose,
                )
            }
        }

        if (state.documentTypes.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            HrHintText("El catálogo de tipos de documento está vacío.")
        }

        if (state.requiredMissing.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            BentoState(
                message = "Faltan documentos obligatorios: " +
                    state.requiredMissing.joinToString(", ") { it.name } + ".",
                tone = BentoTone.Sun,
                icon = Icons.Filled.Info,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (canEdit) {
            Spacer(Modifier.height(14.dp))
            Text("Subir documento", style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            SimpleDropdownField(
                label = "Tipo de documento",
                value = state.documentTypeId,
                options = state.documentTypeOptions,
                onSelect = viewModel::onDocumentTypeChange,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            AttachmentPickerField(
                label = "Archivo",
                file = state.documentFile,
                onFilePicked = viewModel::onDocumentPicked,
                onClear = viewModel::clearDocumentFile,
                hint = "PDF o imagen (JPG/PNG/WebP) · máximo 15 MB",
                mimeTypes = listOf(FileMimeTypes.PDF, FileMimeTypes.IMAGE),
                enabled = !state.uploadingDocument,
            )
            Spacer(Modifier.height(10.dp))
            PillButton(
                text = "Subir documento",
                onClick = viewModel::uploadDocument,
                enabled = state.documentTypeId.isNotBlank() && state.documentFile != null,
                loading = state.uploadingDocument,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Archivos agrupados por tipo del catálogo.
        state.documentTypes.forEach { type ->
            val files = state.documents.filter { it.documentTypeId == type.id }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    type.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                DocumentTypeStatusChip(required = type.required, covered = files.isNotEmpty())
            }
            files.forEach { document ->
                DocumentFileRow(
                    document = document,
                    state = state,
                    canEdit = canEdit,
                    viewModel = viewModel,
                )
            }
        }

        // Documentos cuyo tipo ya no está en el catálogo (baja lógica del tipo).
        val orphanDocuments = state.documents.filter { document ->
            state.documentTypes.none { it.id == document.documentTypeId }
        }
        if (orphanDocuments.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("Otros documentos", style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
            orphanDocuments.forEach { document ->
                DocumentFileRow(
                    document = document,
                    state = state,
                    canEdit = canEdit,
                    viewModel = viewModel,
                )
            }
        }

        if (state.documentTypes.isEmpty() && state.documents.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            HrHintText("Sin documentos registrados.")
        }

        if (canEdit) {
            Spacer(Modifier.height(16.dp))
            Text("Avisos", style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(
                    text = "Avisar faltantes",
                    onClick = viewModel::requestNotifyMissing,
                    variant = PillButtonVariant.Secondary,
                    enabled = !state.notifyingMissing,
                    loading = state.notifyingMissing,
                    modifier = Modifier.weight(1f),
                )
                PillButton(
                    text = "Enviar alta",
                    onClick = viewModel::requestNotifyRegistration,
                    variant = PillButtonVariant.Secondary,
                    enabled = !state.notifyingRegistration,
                    loading = state.notifyingRegistration,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Avisar faltantes notifica en la app y por correo lo que le falta. Enviar alta manda el correo de alta de personal con los documentos cargados.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
            )
            if (profile.email.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "La persona no tiene correo registrado: los avisos por correo no llegarán.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Danger,
                )
            }
        }
    }
}

/** Un archivo del expediente: compartir (descarga + hoja del sistema) y eliminar. */
@Composable
private fun DocumentFileRow(
    document: EmployeeDocumentDto,
    state: EmployeeProfileUiState,
    canEdit: Boolean,
    viewModel: EmployeeProfileViewModel,
) {
    Spacer(Modifier.height(8.dp))
    val downloading = state.downloadingDocumentId == document.id
    AttachmentFileRow(
        name = document.originalName,
        mime = document.mimeType,
        subtitle = listOfNotNull(
            formatDateTime(document.createdAt).takeIf { it != "—" },
            formatFileSize(document.sizeBytes),
            if (downloading) "Descargando…" else null,
        ).joinToString(" · "),
        onShare = { viewModel.downloadAndShare(document) },
        onClear = if (canEdit) {
            { viewModel.requestDeleteDocument(document) }
        } else {
            null
        },
    )
}

/** Botones de guardado del formulario de edición. */
@Composable
private fun EmployeeProfileEditActions(
    state: EmployeeProfileUiState,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PillButton(
            text = "Cancelar",
            onClick = onCancel,
            variant = PillButtonVariant.Ghost,
            enabled = !state.saving,
            modifier = Modifier.weight(1f),
        )
        PillButton(
            text = "Guardar cambios",
            onClick = onSave,
            enabled = !state.saving,
            loading = state.saving,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Diálogos de confirmación (borrar documento y avisos). */
@Composable
private fun EmployeeProfileDialogs(
    state: EmployeeProfileUiState,
    profile: EmployeeProfileDto,
    viewModel: EmployeeProfileViewModel,
) {
    state.deletingDocument?.let { document ->
        AppModal(
            title = "Eliminar documento",
            onDismiss = viewModel::cancelDeleteDocument,
            icon = Icons.Filled.Delete,
            tone = AppModalTone.Danger,
            confirmLabel = "Eliminar",
            onConfirm = viewModel::confirmDeleteDocument,
            confirmEnabled = !state.deletingDocumentBusy,
            saving = state.deletingDocumentBusy,
        ) {
            Text(
                "Se eliminará «${document.originalName}» del expediente de ${profile.name}. Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary,
            )
        }
    }

    if (state.confirmNotifyMissing) {
        AppModal(
            title = "Avisar faltantes",
            onDismiss = viewModel::cancelNotifyMissing,
            icon = Icons.Filled.Notifications,
            confirmLabel = "Avisar",
            onConfirm = viewModel::notifyMissing,
            confirmEnabled = !state.notifyingMissing,
            saving = state.notifyingMissing,
        ) {
            Text(
                "Se avisará a ${profile.name} por notificación y correo sobre los documentos y datos que le faltan del expediente.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary,
            )
        }
    }

    if (state.confirmNotifyRegistration) {
        AppModal(
            title = "Enviar alta por correo",
            onDismiss = viewModel::cancelNotifyRegistration,
            icon = Icons.Filled.Email,
            confirmLabel = "Enviar",
            onConfirm = viewModel::confirmNotifyRegistration,
            confirmEnabled = !state.notifyingRegistration,
            saving = state.notifyingRegistration,
        ) {
            Text(
                "Se enviará el correo de alta de personal a ${profile.email ?: "la persona"} con los documentos del expediente adjuntos.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary,
            )
        }
    }
}
