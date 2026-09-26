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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.hr.EmployeeDocumentDto
import com.axzydev.puertonuevoapp.core.network.hr.EmployeeProfileDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.discountTypeLabel
import com.axzydev.puertonuevoapp.core.util.formatDateTime
import com.axzydev.puertonuevoapp.core.util.formatFileSize
import com.axzydev.puertonuevoapp.core.util.initials
import com.axzydev.puertonuevoapp.core.util.roleLabel

/**
 * Perfil de personal desde el expediente (`GET /hr/:id`) con el apartado
 * de documentos como en la web (`GET /hr/:id/documents` + catálogo de
 * tipos). Los datos del detalle reemplazan a la antigua pantalla de Usuarios:
 * la web ya no tiene "Usuarios", ahora es PERSONAL.
 */
@Composable
fun EmployeeProfileScreen(personId: String) {
    val viewModel: EmployeeProfileViewModel = viewModel(key = "employee-profile-$personId") {
        EmployeeProfileViewModel(personId, AppContainer.hrApi)
    }
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val canEdit = (authState as? AuthState.LoggedIn)?.user?.canEditUsers == true

    when {
        state.loading -> LoadingState(Modifier.fillMaxSize())
        state.error != null || state.profile == null -> ErrorState(
            state.error ?: "Personal no encontrado",
            Modifier.fillMaxSize(),
            onRetry = viewModel::load,
        )
        else -> EmployeeProfileContent(
            profile = state.profile!!,
            documents = state.documents,
            documentTypes = state.documentTypes,
            canEdit = canEdit,
            onEdit = { navigator.push(Screen.UserForm(personId)) },
        )
    }
}

@Composable
private fun EmployeeProfileContent(
    profile: EmployeeProfileDto,
    documents: List<EmployeeDocumentDto>,
    documentTypes: List<com.axzydev.puertonuevoapp.core.network.hr.DocumentTypeDto>,
    canEdit: Boolean,
    onEdit: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        EmployeeProfileHeader(profile, canEdit, onEdit)

        DocumentsSection(
            documents = documents,
            documentTypes = documentTypes,
            profile = profile,
        )

        ProfileSection(
            title = "Contacto",
            icon = Icons.Filled.Email,
            color = AppColors.Info,
            rows = listOfNotNull(
                profile.email?.takeIf { it.isNotBlank() }?.let { "Correo" to it },
                profile.personalPhone?.takeIf { it.isNotBlank() }?.let { "Celular personal" to it },
                profile.workPhone?.takeIf { it.isNotBlank() }?.let { "Celular empresa" to it },
            ),
        )

        ProfileSection(
            title = "Puesto y adscripción",
            icon = Icons.Filled.Work,
            color = AppColors.EmeraldPrimary,
            rows = listOfNotNull(
                "Rol" to roleLabel(profile.role),
                profile.jobTitle?.let { "Puesto" to it },
                profile.department?.let { "Departamento" to it.name },
                profile.subarea?.let { "Subárea" to it.name },
                profile.employeeNumber?.let { "Núm. empleado" to it },
                profile.company?.let { "Empresa" to it },
                profile.hireDate?.let { "Fecha de ingreso" to it },
            ),
        )

        ProfileSection(
            title = "Datos personales",
            icon = Icons.Filled.Badge,
            color = AppColors.Purple,
            rows = listOfNotNull(
                profile.birthDate?.let { "Fecha de nacimiento" to it },
                profile.gender?.let { "Género" to it.name },
                profile.bloodType?.let { "Tipo de sangre" to it.name },
                profile.rfc?.takeIf { it.isNotBlank() }?.let { "RFC" to it },
                profile.curp?.takeIf { it.isNotBlank() }?.let { "CURP" to it },
                profile.nss?.takeIf { it.isNotBlank() }?.let { "NSS" to it },
                profile.medicalConditions?.takeIf { it.isNotBlank() }?.let { "Padecimiento" to it },
                profile.allergies?.takeIf { it.isNotBlank() }?.let { "Alergias" to it },
            ),
        )

        ProfileSection(
            title = "Domicilio",
            icon = Icons.Filled.LocationOn,
            color = AppColors.Warning,
            rows = listOfNotNull(
                profile.streetAddress?.takeIf { it.isNotBlank() }?.let { "Calle y número" to it },
                profile.neighborhood?.takeIf { it.isNotBlank() }?.let { "Colonia" to it },
                profile.postalCode?.takeIf { it.isNotBlank() }?.let { "Código postal" to it },
                profile.city?.takeIf { it.isNotBlank() }?.let { "Ciudad" to it },
                profile.addressState?.takeIf { it.isNotBlank() }?.let { "Estado" to it },
                profile.country?.takeIf { it.isNotBlank() }?.let { "País" to it },
            ),
        )

        ProfileSection(
            title = "Contacto de emergencia",
            icon = Icons.Filled.FavoriteBorder,
            color = AppColors.Danger,
            rows = listOfNotNull(
                profile.emergencyContactName?.takeIf { it.isNotBlank() }?.let { "Contacto" to it },
                profile.emergencyContactPhone?.takeIf { it.isNotBlank() }?.let { "Teléfono" to it },
                profile.emergencyContactRelationship?.takeIf { it.isNotBlank() }?.let { "Parentesco" to it },
            ),
        )

        if (profile.discounts.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(26.dp).background(AppColors.Info.copy(alpha = 0.12f), AppShape.pill),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Percent,
                        contentDescription = null,
                        tint = AppColors.Info,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("DESCUENTOS", style = MaterialTheme.typography.titleSmall, color = AppColors.TextMuted)
            }
            Spacer(Modifier.height(8.dp))
            AppSurfaceCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    profile.discounts.forEach { discount ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusChip(discountTypeLabel(discount.type), AppColors.Info)
                            Spacer(Modifier.width(8.dp))
                            discount.note
                                ?.takeIf { it.isNotBlank() }
                                ?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EmployeeProfileHeader(profile: EmployeeProfileDto, canEdit: Boolean, onEdit: () -> Unit) {
    val accent = AppColors.roleAccent(profile.role)
    val gradient = Brush.verticalGradient(listOf(accent, lerp(accent, Color.Black, 0.35f)))

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 3.dp,
        borderColor = AppColors.Outline.copy(alpha = 0.5f),
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(gradient, AppShape.pill),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initials(profile.name),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
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
                    ).joinToString(" · ").ifBlank { roleLabel(profile.role) },
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
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            profile.department?.let { MiniTag(it.name, AppColors.Info) }
            profile.subarea?.let { MiniTag(it.name, AppColors.Purple) }
            profile.employeeNumber?.let { MiniTag("N.${it}", AppColors.Slate) }
        }
        if (canEdit) {
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth(),
                shape = AppShape.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.EmeraldPrimary,
                    contentColor = Color.White,
                ),
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Editar personal")
            }
        }
    }
}

/**
 * Apartado de documentos del expediente (equivalente a
 * `EmployeeDocumentsCard` de la web): cuenta, barra de avance, estado por tipo
 * y archivos subidos por tipo.
 */
@Composable
private fun DocumentsSection(
    documents: List<EmployeeDocumentDto>,
    documentTypes: List<com.axzydev.puertonuevoapp.core.network.hr.DocumentTypeDto>,
    profile: EmployeeProfileDto,
) {
    val byType = documents.groupBy { it.documentTypeId }
    val requiredCount = documentTypes.size
    val uploadedTypes = documentTypes.count { type -> (byType[type.id]?.size ?: 0) > 0 }
    val percent = if (requiredCount > 0) (uploadedTypes * 100) / requiredCount else 0

    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(26.dp).background(AppColors.Info.copy(alpha = 0.12f), AppShape.pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.InsertDriveFile,
                contentDescription = null,
                tint = AppColors.Info,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("DOCUMENTOS (${documents.size})", style = MaterialTheme.typography.titleSmall, color = AppColors.TextMuted)
    }
    Spacer(Modifier.height(8.dp))
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = AppColors.Outline.copy(alpha = 0.6f),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniTag("$requiredCount tipos", AppColors.Info)
            Spacer(Modifier.width(8.dp))
            MiniTag("$uploadedTypes completos", AppColors.Success)
            Spacer(Modifier.width(8.dp))
            MiniTag("$percent%", if (percent == 100) AppColors.Success else AppColors.Warning)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(AppColors.Outline.copy(alpha = 0.25f), AppShape.pill)
                .padding(2.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(if (percent == 100) AppColors.Success else AppColors.Info, AppShape.pill),
            )
        }

        if (requiredCount == 0 && documents.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Sin documentos registrados",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextFaint,
            )
        }

        documentTypes.forEach { type ->
            val files = byType[type.id].orEmpty()
            val covered = files.isNotEmpty()
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    type.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusChip(
                    label = if (covered) "Subido" else "Pendiente",
                    color = if (covered) AppColors.Success else AppColors.Warning,
                )
            }
            files.forEach { doc ->
                DocumentRow(doc)
            }
        }

        documents.filter { doc -> documentTypes.none { it.id == doc.documentTypeId } }
            .forEach { doc ->
                Spacer(Modifier.height(10.dp))
                Text(
                    doc.documentType?.name ?: "Documento",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                )
                DocumentRow(doc)
            }
    }
}

@Composable
private fun DocumentRow(doc: EmployeeDocumentDto) {
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    if (doc.mimeType == "application/pdf") AppColors.Danger.copy(alpha = 0.10f) else AppColors.SurfaceVariant,
                    AppShape.icon,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.InsertDriveFile,
                contentDescription = null,
                tint = if (doc.mimeType == "application/pdf") AppColors.Danger else AppColors.TextMuted,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                doc.originalName,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${formatDateTime(doc.createdAt)} · ${formatFileSize(doc.sizeBytes)}",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
            )
        }
    }
}

@Composable
private fun MiniTag(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.10f), AppShape.pill)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun ProfileSection(
    title: String,
    icon: ImageVector,
    color: Color,
    rows: List<Pair<String, String>>,
) {
    if (rows.isEmpty()) return

    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(26.dp).background(color.copy(alpha = 0.12f), AppShape.pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(title.uppercase(), style = MaterialTheme.typography.titleSmall, color = AppColors.TextMuted)
    }
    Spacer(Modifier.height(8.dp))
    AppSurfaceCard(Modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                    modifier = Modifier.width(130.dp),
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
            if (index < rows.lastIndex) {
                HorizontalDivider(color = AppColors.Outline.copy(alpha = 0.35f), modifier = Modifier.padding(start = 14.dp, end = 14.dp))
            }
        }
    }
}