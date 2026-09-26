package com.axzydev.puertonuevoapp.core.network.hr

import com.axzydev.puertonuevoapp.core.network.common.DepartmentRefDto
import com.axzydev.puertonuevoapp.core.network.users.SubareaRefDto
import kotlinx.serialization.Serializable

/** Catálogo de RH embebido (género, tipo de sangre). */
@Serializable
data class HrCatalogRefDto(
    val id: String,
    val name: String,
    val active: Boolean = true,
)

/** Descuento de nómina de un empleado (INFONAVIT/IMSS/CHILD_SUPPORT). */
@Serializable
data class EmployeeDiscountDto(
    val type: String,
    val note: String? = null,
)

@Serializable
data class DocumentTypeRefDto(
    val id: String,
    val name: String,
)

/** Tipo de documento del expediente (catálogo RH). */
@Serializable
data class DocumentTypeDto(
    val id: String,
    val name: String,
    val active: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: String = "",
)

/** Documento subido al expediente de un empleado. */
@Serializable
data class EmployeeDocumentDto(
    val id: String,
    val documentTypeId: String,
    val documentType: DocumentTypeRefDto? = null,
    val originalName: String,
    val mimeType: String,
    val sizeBytes: Long = 0,
    val url: String = "",
    val uploadedById: String,
    val createdAt: String,
)

/**
 * Expediente completo de un empleado — `GET /hr/:id`, mismo shape que el
 * DTO de la web (`personalProfileToDto`).
 */
@Serializable
data class EmployeeProfileDto(
    val id: String,
    val username: String,
    val name: String,
    val email: String? = null,
    val role: String,
    val active: Boolean = true,
    val jobTitle: String? = null,
    val employeeNumber: String? = null,
    val company: String? = null,
    val department: DepartmentRefDto? = null,
    val subarea: SubareaRefDto? = null,

    val middleName: String? = null,
    val paternalSurname: String? = null,
    val maternalSurname: String? = null,
    val photoUrl: String? = null,

    val gender: HrCatalogRefDto? = null,
    val bloodType: HrCatalogRefDto? = null,
    val medicalConditions: String? = null,
    val allergies: String? = null,

    val birthDate: String? = null,
    val hireDate: String? = null,

    val rfc: String? = null,
    val curp: String? = null,
    val nss: String? = null,

    val streetAddress: String? = null,
    val neighborhood: String? = null,
    val postalCode: String? = null,
    val city: String? = null,
    val addressState: String? = null,
    val country: String? = null,

    val personalPhone: String? = null,
    val workPhone: String? = null,

    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val emergencyContactRelationship: String? = null,

    val discounts: List<EmployeeDiscountDto> = emptyList(),

    val createdAt: String,
)