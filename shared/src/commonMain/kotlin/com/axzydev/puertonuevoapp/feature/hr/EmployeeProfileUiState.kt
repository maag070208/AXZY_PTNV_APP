package com.axzydev.puertonuevoapp.feature.hr

import com.axzydev.puertonuevoapp.core.network.hr.DocumentTypeDto
import com.axzydev.puertonuevoapp.core.network.hr.EmployeeDiscountDto
import com.axzydev.puertonuevoapp.core.network.hr.EmployeeDocumentDto
import com.axzydev.puertonuevoapp.core.network.hr.EmployeeProfileDto
import com.axzydev.puertonuevoapp.core.network.hr.HrCatalogRefDto
import com.axzydev.puertonuevoapp.core.platform.PickedFile

/** Tipos de descuento de nómina que administra el expediente (los del API). */
val hrDiscountTypes: List<String> = listOf("INFONAVIT", "IMSS", "CHILD_SUPPORT")

/**
 * Campos editables del expediente. Se usan como clave única para que la
 * pantalla no tenga una docena de callbacks `onXChange` y para mapear cada uno
 * a su nombre en `PATCH /hr/:id/profile`.
 */
enum class ProfileEditField {
    MiddleName,
    PaternalSurname,
    MaternalSurname,
    Gender,
    BirthDate,
    HireDate,
    Rfc,
    Curp,
    Nss,
    StreetAddress,
    Neighborhood,
    PostalCode,
    City,
    AddressState,
    Country,
    PersonalPhone,
    WorkPhone,
    Email,
    EmergencyContactName,
    EmergencyContactPhone,
    EmergencyContactRelationship,
    BloodType,
    MedicalConditions,
    Allergies,
}

/** Formulario de edición del expediente (se llena desde el perfil y se compara al guardar). */
data class EmployeeProfileEditState(
    val middleName: String = "",
    val paternalSurname: String = "",
    val maternalSurname: String = "",
    val genderId: String = "",
    val birthDate: String = "",
    val hireDate: String = "",
    val rfc: String = "",
    val curp: String = "",
    val nss: String = "",
    val streetAddress: String = "",
    val neighborhood: String = "",
    val postalCode: String = "",
    val city: String = "",
    val addressState: String = "",
    val country: String = "",
    val personalPhone: String = "",
    val workPhone: String = "",
    val email: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val emergencyContactRelationship: String = "",
    val bloodTypeId: String = "",
    val medicalConditions: String = "",
    val allergies: String = "",
) {
    fun value(field: ProfileEditField): String = when (field) {
        ProfileEditField.MiddleName -> middleName
        ProfileEditField.PaternalSurname -> paternalSurname
        ProfileEditField.MaternalSurname -> maternalSurname
        ProfileEditField.Gender -> genderId
        ProfileEditField.BirthDate -> birthDate
        ProfileEditField.HireDate -> hireDate
        ProfileEditField.Rfc -> rfc
        ProfileEditField.Curp -> curp
        ProfileEditField.Nss -> nss
        ProfileEditField.StreetAddress -> streetAddress
        ProfileEditField.Neighborhood -> neighborhood
        ProfileEditField.PostalCode -> postalCode
        ProfileEditField.City -> city
        ProfileEditField.AddressState -> addressState
        ProfileEditField.Country -> country
        ProfileEditField.PersonalPhone -> personalPhone
        ProfileEditField.WorkPhone -> workPhone
        ProfileEditField.Email -> email
        ProfileEditField.EmergencyContactName -> emergencyContactName
        ProfileEditField.EmergencyContactPhone -> emergencyContactPhone
        ProfileEditField.EmergencyContactRelationship -> emergencyContactRelationship
        ProfileEditField.BloodType -> bloodTypeId
        ProfileEditField.MedicalConditions -> medicalConditions
        ProfileEditField.Allergies -> allergies
    }

    fun with(field: ProfileEditField, value: String): EmployeeProfileEditState = when (field) {
        ProfileEditField.MiddleName -> copy(middleName = value)
        ProfileEditField.PaternalSurname -> copy(paternalSurname = value)
        ProfileEditField.MaternalSurname -> copy(maternalSurname = value)
        ProfileEditField.Gender -> copy(genderId = value)
        ProfileEditField.BirthDate -> copy(birthDate = value)
        ProfileEditField.HireDate -> copy(hireDate = value)
        ProfileEditField.Rfc -> copy(rfc = value)
        ProfileEditField.Curp -> copy(curp = value)
        ProfileEditField.Nss -> copy(nss = value)
        ProfileEditField.StreetAddress -> copy(streetAddress = value)
        ProfileEditField.Neighborhood -> copy(neighborhood = value)
        ProfileEditField.PostalCode -> copy(postalCode = value)
        ProfileEditField.City -> copy(city = value)
        ProfileEditField.AddressState -> copy(addressState = value)
        ProfileEditField.Country -> copy(country = value)
        ProfileEditField.PersonalPhone -> copy(personalPhone = value)
        ProfileEditField.WorkPhone -> copy(workPhone = value)
        ProfileEditField.Email -> copy(email = value)
        ProfileEditField.EmergencyContactName -> copy(emergencyContactName = value)
        ProfileEditField.EmergencyContactPhone -> copy(emergencyContactPhone = value)
        ProfileEditField.EmergencyContactRelationship -> copy(emergencyContactRelationship = value)
        ProfileEditField.BloodType -> copy(bloodTypeId = value)
        ProfileEditField.MedicalConditions -> copy(medicalConditions = value)
        ProfileEditField.Allergies -> copy(allergies = value)
    }

    companion object {
        fun fromProfile(profile: EmployeeProfileDto): EmployeeProfileEditState = EmployeeProfileEditState(
            middleName = profile.middleName.orEmpty(),
            paternalSurname = profile.paternalSurname.orEmpty(),
            maternalSurname = profile.maternalSurname.orEmpty(),
            genderId = profile.gender?.id.orEmpty(),
            birthDate = profile.birthDate.orEmpty(),
            hireDate = profile.hireDate.orEmpty(),
            rfc = profile.rfc.orEmpty(),
            curp = profile.curp.orEmpty(),
            nss = profile.nss.orEmpty(),
            streetAddress = profile.streetAddress.orEmpty(),
            neighborhood = profile.neighborhood.orEmpty(),
            postalCode = profile.postalCode.orEmpty(),
            city = profile.city.orEmpty(),
            addressState = profile.addressState.orEmpty(),
            country = profile.country.orEmpty(),
            personalPhone = profile.personalPhone.orEmpty(),
            workPhone = profile.workPhone.orEmpty(),
            email = profile.email.orEmpty(),
            emergencyContactName = profile.emergencyContactName.orEmpty(),
            emergencyContactPhone = profile.emergencyContactPhone.orEmpty(),
            emergencyContactRelationship = profile.emergencyContactRelationship.orEmpty(),
            bloodTypeId = profile.bloodType?.id.orEmpty(),
            medicalConditions = profile.medicalConditions.orEmpty(),
            allergies = profile.allergies.orEmpty(),
        )
    }
}

/** Un tipo de descuento de nómina con su nota (vacío = no aplica). */
data class DiscountRowState(
    val type: String,
    val enabled: Boolean = false,
    val note: String = "",
)

/** Filas de descuento vacías (las tres del API), para el formulario. */
fun defaultDiscountRows(): List<DiscountRowState> = hrDiscountTypes.map { DiscountRowState(type = it) }

/** Filas de descuento a partir de las que trae el expediente. */
fun discountRowsFrom(discounts: List<EmployeeDiscountDto>): List<DiscountRowState> =
    hrDiscountTypes.map { type ->
        val current = discounts.firstOrNull { it.type == type }
        DiscountRowState(type = type, enabled = current != null, note = current?.note.orEmpty())
    }

data class EmployeeProfileUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val profile: EmployeeProfileDto? = null,
    val documents: List<EmployeeDocumentDto> = emptyList(),
    val documentTypes: List<DocumentTypeDto> = emptyList(),
    val genders: List<HrCatalogRefDto> = emptyList(),
    val bloodTypes: List<HrCatalogRefDto> = emptyList(),

    // Foto: la pantalla pide `/hr/:id/photo/raw` y este contador la refresca
    // después de subir una nueva (sin depender de la URL absoluta de S3).
    val photoVersion: Int = 0,
    val photoUploading: Boolean = false,

    // Alta de documento
    val documentTypeId: String = "",
    val documentFile: PickedFile? = null,
    val uploadingDocument: Boolean = false,

    // Acciones sobre documentos
    val downloadingDocumentId: String? = null,
    val deletingDocument: EmployeeDocumentDto? = null,
    val deletingDocumentBusy: Boolean = false,

    // Avisos
    val confirmNotifyMissing: Boolean = false,
    val notifyingMissing: Boolean = false,
    val notifyingRegistration: Boolean = false,
    val confirmNotifyRegistration: Boolean = false,

    // Edición del expediente
    val editing: Boolean = false,
    val edit: EmployeeProfileEditState = EmployeeProfileEditState(),
    val editError: String? = null,
    val saving: Boolean = false,
    val discountRows: List<DiscountRowState> = defaultDiscountRows(),
) {
    /** Ruta relativa de la foto para [RemoteImage] (con versión para refrescar). */
    val photoPath: String? get() = profile?.id?.let { "/hr/$it/photo/raw?v=$photoVersion" }

    val uploadedTypeIds: Set<String> get() = documents.map { it.documentTypeId }.toSet()

    /** Tipos obligatorios del catálogo que todavía no tienen archivo. */
    val requiredMissing: List<DocumentTypeDto>
        get() = documentTypes.filter { it.required && it.id !in uploadedTypeIds }

    /** Todos los tipos activos que aún no tienen archivo. */
    val missingTypes: List<DocumentTypeDto> get() = documentTypes.filter { it.id !in uploadedTypeIds }

    val coveredTypeCount: Int get() = documentTypes.count { it.id in uploadedTypeIds }

    val hasAnyDocument: Boolean get() = documents.isNotEmpty()

    val documentTypeOptions: List<Pair<String, String>> get() = documentTypes.map { it.id to it.name }

    val genderOptions: List<Pair<String, String>>
        get() = listOf("" to "Sin dato") + genders.map { it.id to it.name }

    val bloodTypeOptions: List<Pair<String, String>>
        get() = listOf("" to "Sin dato") + bloodTypes.map { it.id to it.name }

    /** Descuentos vigentes en el formulario (los habilitados). */
    val selectedDiscounts: List<EmployeeDiscountDto>
        get() = discountRows.filter { it.enabled }.map {
            EmployeeDiscountDto(type = it.type, note = it.note.trim().ifBlank { null })
        }
}
