package com.axzydev.puertonuevoapp.feature.hr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.network.hr.EmployeeDiscountDto
import com.axzydev.puertonuevoapp.core.network.hr.EmployeeProfileDto
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.util.discountTypeLabel
import com.axzydev.puertonuevoapp.core.util.roleLabel

/** Secciones de solo lectura del expediente (personales, contacto, fiscales, domicilio, médicos). */
@Composable
fun EmployeeProfileViewSections(
    profile: EmployeeProfileDto,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ProfileSection(
            title = "Datos personales",
            icon = Icons.Filled.Person,
            color = AppColors.Purple,
            rows = listOfNotNull(
                "Nombre" to profile.name,
                profile.middleName?.takeIf { it.isNotBlank() }?.let { "Segundo nombre" to it },
                profile.paternalSurname?.takeIf { it.isNotBlank() }?.let { "Apellido paterno" to it },
                profile.maternalSurname?.takeIf { it.isNotBlank() }?.let { "Apellido materno" to it },
                profile.birthDate?.takeIf { it.isNotBlank() }?.let { "Fecha de nacimiento" to it },
                profile.gender?.let { "Género" to it.name },
            ),
        )

        ProfileSection(
            title = "Contacto",
            icon = Icons.Filled.Email,
            color = AppColors.Info,
            rows = listOfNotNull(
                profile.email?.takeIf { it.isNotBlank() }?.let { "Correo" to it },
                profile.personalPhone?.takeIf { it.isNotBlank() }?.let { "Celular personal" to it },
                profile.workPhone?.takeIf { it.isNotBlank() }?.let { "Celular empresa" to it },
                profile.emergencyContactName?.takeIf { it.isNotBlank() }?.let { "Contacto de emergencia" to it },
                profile.emergencyContactPhone?.takeIf { it.isNotBlank() }?.let { "Teléfono de emergencia" to it },
                profile.emergencyContactRelationship?.takeIf { it.isNotBlank() }?.let { "Parentesco" to it },
            ),
        )

        ProfileSection(
            title = "Puesto y adscripción",
            icon = Icons.Filled.Work,
            color = AppColors.EmeraldPrimary,
            rows = listOfNotNull(
                "Rol" to roleLabel(profile.role),
                profile.jobTitle?.takeIf { it.isNotBlank() }?.let { "Puesto" to it },
                profile.department?.let { "Departamento" to it.name },
                profile.subarea?.let { "Subárea" to it.name },
                profile.employeeNumber?.takeIf { it.isNotBlank() }?.let { "Núm. empleado" to it },
                profile.company?.takeIf { it.isNotBlank() }?.let { "Empresa" to it },
                profile.hireDate?.takeIf { it.isNotBlank() }?.let { "Fecha de ingreso" to it },
            ),
        )

        ProfileSection(
            title = "Datos fiscales",
            icon = Icons.Filled.Badge,
            color = AppColors.Slate,
            rows = listOfNotNull(
                profile.rfc?.takeIf { it.isNotBlank() }?.let { "RFC" to it },
                profile.curp?.takeIf { it.isNotBlank() }?.let { "CURP" to it },
                profile.nss?.takeIf { it.isNotBlank() }?.let { "NSS" to it },
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
            title = "Datos médicos",
            icon = Icons.Filled.FavoriteBorder,
            color = AppColors.Danger,
            rows = listOfNotNull(
                profile.bloodType?.let { "Tipo de sangre" to it.name },
                profile.medicalConditions?.takeIf { it.isNotBlank() }?.let { "Padecimiento" to it },
                profile.allergies?.takeIf { it.isNotBlank() }?.let { "Alergias" to it },
            ),
        )

        EmployeeDiscountsView(profile.discounts)
    }
}

/** Descuentos de nómina en solo lectura (chips con tipo y nota). */
@Composable
fun EmployeeDiscountsView(discounts: List<EmployeeDiscountDto>) {
    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon = Icons.Filled.Percent, color = AppColors.Info)
        Spacer(Modifier.width(8.dp))
        Text("DESCUENTOS", style = MaterialTheme.typography.titleSmall, color = AppColors.TextMuted)
    }
    Spacer(Modifier.height(8.dp))
    AppSurfaceCard(Modifier.fillMaxWidth()) {
        if (discounts.isEmpty()) {
            Text(
                "Sin descuentos de nómina registrados.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
                modifier = Modifier.padding(vertical = 2.dp),
            )
        } else {
            discounts.forEach { discount ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusChip(discountTypeLabel(discount.type), AppColors.Info)
                    discount.note?.takeIf { it.isNotBlank() }?.let { note ->
                        Spacer(Modifier.width(8.dp))
                        Text(
                            note,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextMuted,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Formulario de edición del expediente por secciones. Los campos vacíos se
 * guardan como "sin dato": el ViewModel decide entre `""` y `clearFields` según
 * lo que exija el API (fechas y selects necesitan `null` explícito).
 */
@Composable
fun EmployeeProfileEditForm(
    state: EmployeeProfileUiState,
    onFieldChange: (ProfileEditField, String) -> Unit,
    onToggleDiscount: (String) -> Unit,
    onDiscountNoteChange: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val edit = state.edit
    fun value(field: ProfileEditField) = edit.value(field)

    Column(modifier = modifier.fillMaxWidth()) {
        EditSectionCard("Datos personales") {
            EditTextField(
                label = "Segundo nombre",
                value = value(ProfileEditField.MiddleName),
                onChange = { onFieldChange(ProfileEditField.MiddleName, it) },
            )
            EditTextField(
                label = "Apellido paterno",
                value = value(ProfileEditField.PaternalSurname),
                onChange = { onFieldChange(ProfileEditField.PaternalSurname, it) },
            )
            EditTextField(
                label = "Apellido materno",
                value = value(ProfileEditField.MaternalSurname),
                onChange = { onFieldChange(ProfileEditField.MaternalSurname, it) },
            )
            EditDropdown(
                label = "Género",
                value = value(ProfileEditField.Gender),
                options = state.genderOptions,
                onSelect = { onFieldChange(ProfileEditField.Gender, it) },
            )
            EditTextField(
                label = "Fecha de nacimiento",
                value = value(ProfileEditField.BirthDate),
                onChange = { onFieldChange(ProfileEditField.BirthDate, it) },
                placeholder = "AAAA-MM-DD",
            )
            EditTextField(
                label = "Fecha de ingreso",
                value = value(ProfileEditField.HireDate),
                onChange = { onFieldChange(ProfileEditField.HireDate, it) },
                placeholder = "AAAA-MM-DD",
            )
        }

        EditSectionCard("Datos fiscales") {
            EditTextField(
                label = "RFC",
                value = value(ProfileEditField.Rfc),
                onChange = { onFieldChange(ProfileEditField.Rfc, it) },
            )
            EditTextField(
                label = "CURP",
                value = value(ProfileEditField.Curp),
                onChange = { onFieldChange(ProfileEditField.Curp, it) },
            )
            EditTextField(
                label = "NSS",
                value = value(ProfileEditField.Nss),
                onChange = { onFieldChange(ProfileEditField.Nss, it) },
                keyboardType = KeyboardType.Number,
            )
        }

        EditSectionCard("Domicilio") {
            EditTextField(
                label = "Calle y número",
                value = value(ProfileEditField.StreetAddress),
                onChange = { onFieldChange(ProfileEditField.StreetAddress, it) },
            )
            EditTextField(
                label = "Colonia",
                value = value(ProfileEditField.Neighborhood),
                onChange = { onFieldChange(ProfileEditField.Neighborhood, it) },
            )
            EditTextField(
                label = "Código postal",
                value = value(ProfileEditField.PostalCode),
                onChange = { onFieldChange(ProfileEditField.PostalCode, it) },
                keyboardType = KeyboardType.Number,
            )
            EditTextField(
                label = "Ciudad",
                value = value(ProfileEditField.City),
                onChange = { onFieldChange(ProfileEditField.City, it) },
            )
            EditTextField(
                label = "Estado",
                value = value(ProfileEditField.AddressState),
                onChange = { onFieldChange(ProfileEditField.AddressState, it) },
            )
            EditTextField(
                label = "País",
                value = value(ProfileEditField.Country),
                onChange = { onFieldChange(ProfileEditField.Country, it) },
            )
        }

        EditSectionCard("Contacto") {
            EditTextField(
                label = "Correo",
                value = value(ProfileEditField.Email),
                onChange = { onFieldChange(ProfileEditField.Email, it) },
                keyboardType = KeyboardType.Email,
            )
            EditTextField(
                label = "Celular personal",
                value = value(ProfileEditField.PersonalPhone),
                onChange = { onFieldChange(ProfileEditField.PersonalPhone, it) },
                keyboardType = KeyboardType.Phone,
            )
            EditTextField(
                label = "Celular empresa",
                value = value(ProfileEditField.WorkPhone),
                onChange = { onFieldChange(ProfileEditField.WorkPhone, it) },
                keyboardType = KeyboardType.Phone,
            )
            Text(
                "Contacto de emergencia",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextFaint,
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
            )
            EditTextField(
                label = "Nombre",
                value = value(ProfileEditField.EmergencyContactName),
                onChange = { onFieldChange(ProfileEditField.EmergencyContactName, it) },
            )
            EditTextField(
                label = "Teléfono",
                value = value(ProfileEditField.EmergencyContactPhone),
                onChange = { onFieldChange(ProfileEditField.EmergencyContactPhone, it) },
                keyboardType = KeyboardType.Phone,
            )
            EditTextField(
                label = "Parentesco",
                value = value(ProfileEditField.EmergencyContactRelationship),
                onChange = { onFieldChange(ProfileEditField.EmergencyContactRelationship, it) },
            )
        }

        EditSectionCard("Datos médicos") {
            EditDropdown(
                label = "Tipo de sangre",
                value = value(ProfileEditField.BloodType),
                options = state.bloodTypeOptions,
                onSelect = { onFieldChange(ProfileEditField.BloodType, it) },
            )
            EditTextField(
                label = "Padecimiento",
                value = value(ProfileEditField.MedicalConditions),
                onChange = { onFieldChange(ProfileEditField.MedicalConditions, it) },
                singleLine = false,
                lines = 2,
            )
            EditTextField(
                label = "Alergias",
                value = value(ProfileEditField.Allergies),
                onChange = { onFieldChange(ProfileEditField.Allergies, it) },
                singleLine = false,
                lines = 2,
            )
        }

        EditSectionCard("Descuentos de nómina") {
            state.discountRows.forEach { row ->
                DiscountRowEditor(
                    row = row,
                    onToggle = { onToggleDiscount(row.type) },
                    onNoteChange = { onDiscountNoteChange(row.type, it) },
                )
            }
            Text(
                "Marca los descuentos que aplican y anota el detalle (número de crédito, importe, etc.).",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
            )
        }
    }
}

// ── Piezas internas del formulario ──────────────────────────────────────────

@Composable
private fun EditSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Spacer(Modifier.height(14.dp))
    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(title)
        content()
    }
}

@Composable
private fun EditTextField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    lines: Int = 1,
) {
    AppTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = singleLine,
        minLines = lines,
        maxLines = if (singleLine) 1 else lines,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun EditDropdown(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    SimpleDropdownField(
        label = label,
        value = value,
        options = options,
        onSelect = onSelect,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(10.dp))
}

/** Fila de descuento: chip sí/no + nota cuando está incluido. */
@Composable
private fun DiscountRowEditor(
    row: DiscountRowState,
    onToggle: () -> Unit,
    onNoteChange: (String) -> Unit,
) {
    val selected = row.enabled
    val container = if (selected) AppColors.Success.copy(alpha = 0.14f) else AppColors.SurfaceVariant
    val content = if (selected) AppColors.Success else AppColors.TextMuted
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShape.row)
                .background(container, AppShape.row)
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                discountTypeLabel(row.type),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (selected) "Incluido" else "No incluido",
                style = MaterialTheme.typography.labelSmall,
                color = content,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (selected) "Quitar" else "Agregar",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (selected) {
            Spacer(Modifier.height(8.dp))
            AppTextField(
                value = row.note,
                onValueChange = onNoteChange,
                label = { Text("Detalle del descuento") },
                placeholder = { Text("Número de crédito, importe…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}

// ── Secciones de solo lectura ───────────────────────────────────────────────

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
        IconBadge(icon = icon, color = color)
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
                HorizontalDivider(
                    color = AppColors.Outline.copy(alpha = 0.35f),
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .background(color.copy(alpha = 0.12f), AppShape.pill),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
    }
}

/** Etiqueta de estado de un tipo de documento (pendiente / subido / obligatorio pendiente). */
@Composable
fun DocumentTypeStatusChip(required: Boolean, covered: Boolean) {
    when {
        covered -> StatusChip("Subido", BentoTone.Mint)
        required -> StatusChip("Obligatorio pendiente", BentoTone.Rose)
        else -> StatusChip("Pendiente", BentoTone.Sun)
    }
}

/** Nota centrada simple (por ejemplo, catálogo vacío). */
@Composable
fun HrHintText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.TextFaint,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
}
