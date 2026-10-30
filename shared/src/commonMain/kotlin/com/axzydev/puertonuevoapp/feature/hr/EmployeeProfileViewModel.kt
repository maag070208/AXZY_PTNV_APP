package com.axzydev.puertonuevoapp.feature.hr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.hr.EmployeeDiscountDto
import com.axzydev.puertonuevoapp.core.network.hr.EmployeeDocumentDto
import com.axzydev.puertonuevoapp.core.network.hr.EmployeeProfileDto
import com.axzydev.puertonuevoapp.core.network.hr.EmployeeProfileField
import com.axzydev.puertonuevoapp.core.network.hr.EmployeeProfileUpdateInput
import com.axzydev.puertonuevoapp.core.network.hr.HrApi
import com.axzydev.puertonuevoapp.core.platform.PickedFile
import com.axzydev.puertonuevoapp.core.platform.shareFile
import com.axzydev.puertonuevoapp.core.platform.toMultipartPart
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Expediente de una persona: ficha, foto, documentos (subir/eliminar/compartir),
 * edición del perfil por secciones, descuentos de nómina y avisos de faltantes.
 *
 * Todo exige `hr.records` en el API; la pantalla solo oculta acciones, la API
 * vuelve a validar. Las acciones puntuales (foto, documento, avisos, guardado)
 * avisan por [AppSnackbar] para no reemplazar la pantalla entera por un error.
 */
class EmployeeProfileViewModel(
    private val personId: String,
    private val hrApi: HrApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmployeeProfileUiState())
    val uiState: StateFlow<EmployeeProfileUiState> = _uiState.asStateFlow()

    /** Se fue a otra pantalla (p. ej. editar la cuenta): al volver, refresca. */
    private var refreshArmed = false

    init {
        fetch(showSpinner = true)
    }

    fun load() = fetch(showSpinner = true)

    fun onLeaveScreen() {
        refreshArmed = true
    }

    fun onReturnToScreen() {
        if (!refreshArmed) return
        refreshArmed = false
        fetch(showSpinner = false)
    }

    private fun fetch(showSpinner: Boolean) {
        if (showSpinner) _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val profileResult = runCatching { hrApi.get(personId) }
            val documentsResult = runCatching { hrApi.documents(personId) }
            val typesResult = runCatching { hrApi.documentTypes() }
            // Catálogos: si fallan, el formulario se puede llenar a mano y el resto sigue.
            val gendersResult = runCatching { hrApi.genders() }
            val bloodTypesResult = runCatching { hrApi.bloodTypes() }
            val firstError = profileResult.exceptionOrNull()
                ?: documentsResult.exceptionOrNull()
                ?: typesResult.exceptionOrNull()

            _uiState.update { state ->
                val types = typesResult.getOrNull() ?: state.documentTypes
                state.copy(
                    loading = false,
                    error = if (state.profile == null) firstError?.let(::networkMessage) else null,
                    profile = profileResult.getOrNull() ?: state.profile,
                    documents = documentsResult.getOrNull() ?: state.documents,
                    documentTypes = types,
                    genders = gendersResult.getOrNull() ?: state.genders,
                    bloodTypes = bloodTypesResult.getOrNull() ?: state.bloodTypes,
                    documentTypeId = state.documentTypeId.ifBlank { types.firstOrNull()?.id.orEmpty() },
                )
            }
            if (firstError != null && profileResult.getOrNull() == null) {
                AppSnackbar.showError(networkMessage(firstError))
            }
        }
    }

    // ── Foto ──────────────────────────────────────────────────────────────────

    /** Sube la foto del expediente (`multipart`, campo `file`). */
    fun uploadPhoto(file: PickedFile) {
        if (_uiState.value.photoUploading) return
        _uiState.update { it.copy(photoUploading = true) }
        viewModelScope.launch {
            runCatching { hrApi.uploadPhoto(personId, file.toMultipartPart()) }
                .fold(
                    onSuccess = {
                        _uiState.update { it.copy(photoUploading = false, photoVersion = it.photoVersion + 1) }
                        AppSnackbar.show("Foto actualizada")
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(photoUploading = false) }
                        AppSnackbar.showError(networkMessage(error))
                    },
                )
        }
    }

    // ── Documentos del expediente ────────────────────────────────────────────

    fun onDocumentTypeChange(typeId: String) = _uiState.update { it.copy(documentTypeId = typeId) }

    fun onDocumentPicked(file: PickedFile) = _uiState.update { it.copy(documentFile = file) }

    fun clearDocumentFile() = _uiState.update { it.copy(documentFile = null) }

    /** Sube el archivo elegido para el tipo seleccionado (`POST /hr/:id/documents`). */
    fun uploadDocument() {
        val state = _uiState.value
        val file = state.documentFile ?: return
        val typeId = state.documentTypeId
        if (typeId.isBlank() || state.uploadingDocument) return
        _uiState.update { it.copy(uploadingDocument = true) }
        viewModelScope.launch {
            runCatching { hrApi.uploadDocument(personId, typeId, file.toMultipartPart()) }
                .fold(
                    onSuccess = { uploaded ->
                        _uiState.update { current ->
                            current.copy(
                                uploadingDocument = false,
                                documentFile = null,
                                documents = listOf(uploaded) + current.documents.filterNot { it.id == uploaded.id },
                            )
                        }
                        AppSnackbar.show("Documento subido")
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(uploadingDocument = false) }
                        AppSnackbar.showError(networkMessage(error))
                    },
                )
        }
    }

    fun requestDeleteDocument(document: EmployeeDocumentDto) =
        _uiState.update { it.copy(deletingDocument = document) }

    fun cancelDeleteDocument() =
        _uiState.update { it.copy(deletingDocument = null, deletingDocumentBusy = false) }

    /** Elimina el documento confirmado en el diálogo. */
    fun confirmDeleteDocument() {
        val state = _uiState.value
        val document = state.deletingDocument ?: return
        if (state.deletingDocumentBusy) return
        _uiState.update { it.copy(deletingDocumentBusy = true) }
        viewModelScope.launch {
            runCatching { hrApi.removeDocument(personId, document.id) }
                .fold(
                    onSuccess = {
                        _uiState.update { current ->
                            current.copy(
                                deletingDocumentBusy = false,
                                deletingDocument = null,
                                documents = current.documents.filterNot { it.id == document.id },
                            )
                        }
                        AppSnackbar.show("Documento eliminado")
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(deletingDocumentBusy = false) }
                        AppSnackbar.showError(networkMessage(error))
                    },
                )
        }
    }

    /** Descarga los bytes del documento y abre la hoja de compartir del sistema. */
    fun downloadAndShare(document: EmployeeDocumentDto) {
        if (_uiState.value.downloadingDocumentId != null) return
        _uiState.update { it.copy(downloadingDocumentId = document.id) }
        viewModelScope.launch {
            runCatching { hrApi.downloadDocument(personId, document.id) }
                .fold(
                    onSuccess = { bytes ->
                        runCatching { shareFile(document.originalName, document.mimeType, bytes) }
                        _uiState.update { it.copy(downloadingDocumentId = null) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(downloadingDocumentId = null) }
                        AppSnackbar.showError(networkMessage(error))
                    },
                )
        }
    }

    // ── Avisos ───────────────────────────────────────────────────────────────

    fun requestNotifyMissing() = _uiState.update { it.copy(confirmNotifyMissing = true) }

    fun cancelNotifyMissing() = _uiState.update { it.copy(confirmNotifyMissing = false) }

    /** Avisa a la persona qué le falta del expediente (notificación + correo). */
    fun notifyMissing() {
        if (_uiState.value.notifyingMissing) return
        _uiState.update { it.copy(confirmNotifyMissing = false, notifyingMissing = true) }
        viewModelScope.launch {
            runCatching { hrApi.notifyMissing(personId) }
                .fold(
                    onSuccess = { result ->
                        _uiState.update { it.copy(notifyingMissing = false) }
                        AppSnackbar.show(
                            if (result.emailed) {
                                "Se avisó a la persona (notificación y correo)"
                            } else {
                                "Se avisó a la persona en la app"
                            },
                        )
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(notifyingMissing = false) }
                        AppSnackbar.showError(networkMessage(error))
                    },
                )
        }
    }

    fun requestNotifyRegistration() = _uiState.update { it.copy(confirmNotifyRegistration = true) }

    fun cancelNotifyRegistration() = _uiState.update { it.copy(confirmNotifyRegistration = false) }

    /** Envía el correo de "Alta de personal" con los documentos adjuntos. */
    fun confirmNotifyRegistration() {
        val state = _uiState.value
        if (state.notifyingRegistration) return
        _uiState.update { it.copy(confirmNotifyRegistration = false, notifyingRegistration = true) }
        viewModelScope.launch {
            runCatching { hrApi.notifyRegistration(personId) }
                .fold(
                    onSuccess = { result ->
                        _uiState.update { it.copy(notifyingRegistration = false) }
                        AppSnackbar.show(
                            if (result.attachments > 0) {
                                "Correo de alta enviado con ${result.attachments} documento(s)"
                            } else {
                                "Correo de alta enviado"
                            },
                        )
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(notifyingRegistration = false) }
                        AppSnackbar.showError(networkMessage(error))
                    },
                )
        }
    }

    // ── Edición del expediente ───────────────────────────────────────────────

    fun startEdit() {
        val profile = _uiState.value.profile ?: return
        _uiState.update {
            it.copy(
                editing = true,
                edit = EmployeeProfileEditState.fromProfile(profile),
                editError = null,
                discountRows = discountRowsFrom(profile.discounts),
            )
        }
    }

    fun cancelEdit() = _uiState.update { it.copy(editing = false, editError = null) }

    fun onEditFieldChange(field: ProfileEditField, value: String) = _uiState.update {
        it.copy(edit = it.edit.with(field, value), editError = null)
    }

    fun onToggleDiscount(type: String) = _uiState.update {
        it.copy(
            discountRows = it.discountRows.map { row ->
                if (row.type == type) row.copy(enabled = !row.enabled) else row
            },
        )
    }

    fun onDiscountNoteChange(type: String, note: String) = _uiState.update {
        it.copy(
            discountRows = it.discountRows.map { row ->
                if (row.type == type) row.copy(note = note) else row
            },
        )
    }

    /** Guarda el expediente (`PATCH profile`) y, si cambiaron, los descuentos. */
    fun saveEdit() {
        val state = _uiState.value
        val profile = state.profile ?: return
        if (state.saving) return
        validateEdit(state.edit)?.let { message ->
            _uiState.update { it.copy(editError = message) }
            return
        }

        val update = buildProfileUpdate(profile, state.edit)
        val discounts = state.selectedDiscounts
        val currentDiscounts = profile.discounts
            .map { EmployeeDiscountDto(type = it.type, note = it.note?.trim()?.ifBlank { null }) }
            .sortedBy { it.type }
        val discountsChanged = discounts.sortedBy { it.type } != currentDiscounts

        _uiState.update { it.copy(saving = true, editError = null) }
        viewModelScope.launch {
            runCatching {
                var updated = hrApi.updateProfile(personId, update)
                if (discountsChanged) updated = hrApi.setDiscounts(personId, discounts)
                updated
            }.fold(
                onSuccess = { updated ->
                    _uiState.update {
                        it.copy(
                            saving = false,
                            editing = false,
                            profile = updated,
                            discountRows = discountRowsFrom(updated.discounts),
                            editError = null,
                        )
                    }
                    AppSnackbar.show("Expediente actualizado")
                },
                onFailure = { error ->
                    _uiState.update { it.copy(saving = false, editError = networkMessage(error)) }
                },
            )
        }
    }

    /** Validaciones de formato que el API también exige (fechas y correo). */
    private fun validateEdit(edit: EmployeeProfileEditState): String? {
        val datePattern = Regex("""^\d{4}-\d{2}-\d{2}$""")
        if (edit.birthDate.isNotBlank() && !datePattern.matches(edit.birthDate.trim())) {
            return "La fecha de nacimiento debe tener el formato AAAA-MM-DD."
        }
        if (edit.hireDate.isNotBlank() && !datePattern.matches(edit.hireDate.trim())) {
            return "La fecha de ingreso debe tener el formato AAAA-MM-DD."
        }
        val email = edit.email.trim()
        if (email.isNotEmpty() && (!email.contains("@") || email.length < 5)) {
            return "El correo no tiene un formato válido."
        }
        return null
    }
}

/**
 * Arma el `PATCH /hr/:id/profile` con SOLO lo que cambió. Los campos de texto
 * vaciados y los selects/fechas que se quitaron viajan en `clearFields` (el
 * `ApiClient` omite los nulos, así que el borrado necesita un `null` explícito).
 */
private fun buildProfileUpdate(
    profile: EmployeeProfileDto,
    edit: EmployeeProfileEditState,
): EmployeeProfileUpdateInput {
    val clear = mutableSetOf<EmployeeProfileField>()

    fun text(field: EmployeeProfileField, value: String, original: String?): String? {
        val trimmed = value.trim()
        val old = original?.trim().orEmpty()
        if (trimmed == old) return null
        if (trimmed.isEmpty()) {
            clear += field
            return null
        }
        return trimmed
    }

    fun choice(field: EmployeeProfileField, value: String, original: String?): String? = when {
        value == original.orEmpty() -> null
        value.isBlank() -> {
            clear += field
            null
        }
        else -> value
    }

    return EmployeeProfileUpdateInput(
        middleName = text(EmployeeProfileField.MIDDLE_NAME, edit.middleName, profile.middleName),
        paternalSurname = text(EmployeeProfileField.PATERNAL_SURNAME, edit.paternalSurname, profile.paternalSurname),
        maternalSurname = text(EmployeeProfileField.MATERNAL_SURNAME, edit.maternalSurname, profile.maternalSurname),
        email = text(EmployeeProfileField.EMAIL, edit.email, profile.email),
        genderId = choice(EmployeeProfileField.GENDER, edit.genderId, profile.gender?.id),
        bloodTypeId = choice(EmployeeProfileField.BLOOD_TYPE, edit.bloodTypeId, profile.bloodType?.id),
        medicalConditions = text(EmployeeProfileField.MEDICAL_CONDITIONS, edit.medicalConditions, profile.medicalConditions),
        allergies = text(EmployeeProfileField.ALLERGIES, edit.allergies, profile.allergies),
        birthDate = text(EmployeeProfileField.BIRTH_DATE, edit.birthDate, profile.birthDate),
        hireDate = text(EmployeeProfileField.HIRE_DATE, edit.hireDate, profile.hireDate),
        rfc = text(EmployeeProfileField.RFC, edit.rfc, profile.rfc),
        curp = text(EmployeeProfileField.CURP, edit.curp, profile.curp),
        nss = text(EmployeeProfileField.NSS, edit.nss, profile.nss),
        streetAddress = text(EmployeeProfileField.STREET_ADDRESS, edit.streetAddress, profile.streetAddress),
        neighborhood = text(EmployeeProfileField.NEIGHBORHOOD, edit.neighborhood, profile.neighborhood),
        postalCode = text(EmployeeProfileField.POSTAL_CODE, edit.postalCode, profile.postalCode),
        city = text(EmployeeProfileField.CITY, edit.city, profile.city),
        addressState = text(EmployeeProfileField.ADDRESS_STATE, edit.addressState, profile.addressState),
        country = text(EmployeeProfileField.COUNTRY, edit.country, profile.country),
        personalPhone = text(EmployeeProfileField.PERSONAL_PHONE, edit.personalPhone, profile.personalPhone),
        workPhone = text(EmployeeProfileField.WORK_PHONE, edit.workPhone, profile.workPhone),
        emergencyContactName = text(
            EmployeeProfileField.EMERGENCY_CONTACT_NAME,
            edit.emergencyContactName,
            profile.emergencyContactName,
        ),
        emergencyContactPhone = text(
            EmployeeProfileField.EMERGENCY_CONTACT_PHONE,
            edit.emergencyContactPhone,
            profile.emergencyContactPhone,
        ),
        emergencyContactRelationship = text(
            EmployeeProfileField.EMERGENCY_CONTACT_RELATIONSHIP,
            edit.emergencyContactRelationship,
            profile.emergencyContactRelationship,
        ),
        clearFields = clear,
    )
}
