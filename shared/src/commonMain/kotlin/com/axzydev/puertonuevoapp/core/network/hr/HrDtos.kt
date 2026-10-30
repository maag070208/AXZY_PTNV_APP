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
    /** Obligatorio en el expediente: el alta lo exige y el tablero marca a quien le falta. */
    val required: Boolean = false,
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

// ── Estadísticas y actividad de la persona ───────────────────────────────────

/**
 * Totales de la plantilla (`GET /hr/stats`). `roles` es dinámico: las claves
 * salen de la tabla `roles` (staff), no de un enum fijo.
 */
@Serializable
data class PersonalStatsDto(
    val total: Int = 0,
    val active: Int = 0,
    val inactive: Int = 0,
    val roles: Map<String, Int> = emptyMap(),
)

/** Resumen del expediente (`GET /hr/:id/overview`). */
@Serializable
data class EmployeeOverviewSummaryDto(
    val openTickets: Int = 0,
    val pendingTasks: Int = 0,
)

@Serializable
data class EmployeeOverviewTicketDto(
    val id: String,
    val title: String = "",
    val status: String = "",
    val priority: String = "",
    val createdAt: String = "",
    /** CREATED (la persona lo creó) o ASSIGNED (se lo asignaron). */
    val relation: String = "CREATED",
)

@Serializable
data class EmployeeOverviewTaskDto(
    val id: String,
    val title: String = "",
    val status: String = "",
    val dueDate: String? = null,
    val ticketId: String = "",
    val ticketTitle: String = "",
)

@Serializable
data class EmployeeLoanItemDto(
    val device: String = "",
    val quantity: Int = 0,
    val returned: Int = 0,
)

@Serializable
data class EmployeeLoanRefDto(
    val id: String,
    val number: String = "",
    val date: String = "",
    val status: String = "",
    val items: List<EmployeeLoanItemDto> = emptyList(),
)

@Serializable
data class EmployeeKardexItemDto(
    val device: String = "",
    val quantity: Int = 0,
    val condition: String? = null,
)

/** Movimiento del kardex de la persona: `type` es LOAN o RETURN. */
@Serializable
data class EmployeeKardexEntryDto(
    val type: String,
    val date: String = "",
    val number: String = "",
    val loanNumber: String? = null,
    val items: List<EmployeeKardexItemDto> = emptyList(),
)

@Serializable
data class EmployeeInventorySummaryDto(
    val activeLoans: Int = 0,
    val borrowedUnits: Int = 0,
)

/**
 * Equipo prestado y kardex de la persona. `null` cuando la sesión no tiene
 * `loans.view` (el API recorta el bloque, no lo deja vacío).
 */
@Serializable
data class EmployeeInventoryOverviewDto(
    val summary: EmployeeInventorySummaryDto = EmployeeInventorySummaryDto(),
    val loans: List<EmployeeLoanRefDto> = emptyList(),
    val kardex: List<EmployeeKardexEntryDto> = emptyList(),
)

/** Actividad de una persona en el sistema (`GET /hr/:id/overview`). */
@Serializable
data class EmployeeOverviewDto(
    val summary: EmployeeOverviewSummaryDto = EmployeeOverviewSummaryDto(),
    val inventory: EmployeeInventoryOverviewDto? = null,
    val tickets: List<EmployeeOverviewTicketDto> = emptyList(),
    val tasks: List<EmployeeOverviewTaskDto> = emptyList(),
)

// ── Edición del expediente ───────────────────────────────────────────────────

/**
 * Campos que un `PATCH /hr/:id/profile` puede **limpiar**.
 *
 * El `ApiClient` serializa con `explicitNulls = false`: un campo nulo del DTO
 * se omite del JSON (semántica de parche parcial), así que "mandar null para
 * borrar" no es posible con un DTO tipado. Para los campos donde borrar es una
 * acción real (género, tipo de sangre, domicilio, fecha) el llamador indica la
 * clave en [EmployeeProfileUpdateInput.clearFields] y el API recibe un `null`
 * explícito. Los campos de texto se limpian mandando `""`.
 */
enum class EmployeeProfileField(val key: String) {
    MIDDLE_NAME("middleName"),
    PATERNAL_SURNAME("paternalSurname"),
    MATERNAL_SURNAME("maternalSurname"),
    EMAIL("email"),
    EMPLOYEE_NUMBER("employeeNumber"),
    JOB_TITLE("jobTitle"),
    GENDER("genderId"),
    BLOOD_TYPE("bloodTypeId"),
    MEDICAL_CONDITIONS("medicalConditions"),
    ALLERGIES("allergies"),
    BIRTH_DATE("birthDate"),
    HIRE_DATE("hireDate"),
    RFC("rfc"),
    CURP("curp"),
    NSS("nss"),
    STREET_ADDRESS("streetAddress"),
    NEIGHBORHOOD("neighborhood"),
    POSTAL_CODE("postalCode"),
    CITY("city"),
    ADDRESS_STATE("addressState"),
    COUNTRY("country"),
    PERSONAL_PHONE("personalPhone"),
    WORK_PHONE("workPhone"),
    DEPARTMENT("departmentId"),
    SUBAREA("subareaId"),
    EMERGENCY_CONTACT_NAME("emergencyContactName"),
    EMERGENCY_CONTACT_PHONE("emergencyContactPhone"),
    EMERGENCY_CONTACT_RELATIONSHIP("emergencyContactRelationship"),
}

/**
 * `PATCH /hr/:id/profile`: todos los campos son opcionales y el que no viaja
 * no se toca (el expediente es el dueño de los datos de la persona).
 */
@Serializable
data class EmployeeProfileUpdateInput(
    val name: String? = null,
    val middleName: String? = null,
    val paternalSurname: String? = null,
    val maternalSurname: String? = null,
    val email: String? = null,
    val employeeNumber: String? = null,
    val jobTitle: String? = null,
    val genderId: String? = null,
    val bloodTypeId: String? = null,
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
    val departmentId: String? = null,
    val subareaId: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val emergencyContactRelationship: String? = null,
    /** No viaja al API: pide mandar `null` explícito en esas claves. */
    @kotlinx.serialization.Transient
    val clearFields: Set<EmployeeProfileField> = emptySet(),
)

/** `PUT /hr/:id/discounts`: reemplaza la lista completa (INFONAVIT/IMSS/CHILD_SUPPORT). */
@Serializable
data class EmployeeDiscountsInput(
    val discounts: List<EmployeeDiscountDto> = emptyList(),
)

@Serializable
data class EmployeeAssignmentResultDto(
    val updated: Int = 0,
)

/** Resultado de subir la foto (`POST /hr/:id/photo`). */
@Serializable
data class EmployeePhotoDto(
    val photoUrl: String = "",
)

/** Aviso individual de faltantes (`POST /hr/:id/notify-missing-records`). */
@Serializable
data class NotifyMissingResultDto(
    val notified: Boolean = false,
    val emailed: Boolean = false,
)

/** Aviso masivo de faltantes (`POST /hr/notify-missing-records`). */
@Serializable
data class NotifyMissingBulkInput(
    val userIds: List<String>,
)

@Serializable
data class NotifyMissingBulkResultDto(
    val notified: Int = 0,
    val emailed: Int = 0,
    val skipped: Int = 0,
)

/** Correo de "Alta de personal" con los documentos adjuntos (`POST /hr/:id/notify-registration`). */
@Serializable
data class NotifyRegistrationResultDto(
    val sent: Boolean = false,
    val attachments: Int = 0,
)

/** Respuesta liviana de los DELETE que solo confirman el id borrado. */
@Serializable
data class IdResultDto(
    val id: String,
)

// ── Actas administrativas ────────────────────────────────────────────────────

@Serializable
data class DisciplinaryReportPersonRefDto(
    val id: String,
    val name: String,
    val employeeNumber: String? = null,
    val jobTitle: String? = null,
    val department: DepartmentRefDto? = null,
    val subarea: SubareaRefDto? = null,
)

/** Persona que firmó/registró el acta. */
@Serializable
data class NamedUserRefDto(
    val id: String,
    val name: String,
)

/** `reason`: ABSENCE, TARDINESS, INTOXICATION, MISCONDUCT, NONCOMPLIANCE u OTHER. */
@Serializable
data class DisciplinaryReportDto(
    val id: String,
    val reason: String,
    val incidentDate: String,
    val description: String = "",
    val sanction: String? = null,
    val createdAt: String = "",
    val user: DisciplinaryReportPersonRefDto,
    val createdBy: NamedUserRefDto,
)

@Serializable
data class DisciplinaryReportCreateInput(
    val userId: String,
    val reason: String,
    /** Día local `YYYY-MM-DD` del incidente. */
    val incidentDate: String,
    val description: String,
    val sanction: String? = null,
)

// ── Catálogos de RH (tipos de documento, géneros, tipos de sangre) ───────────

/** Alta de catálogo simple (género / tipo de sangre): solo el nombre. */
@Serializable
data class HrCatalogCreateInput(
    val name: String,
)

/** Cambio de catálogo simple: renombrar y/o activar/desactivar. */
@Serializable
data class HrCatalogUpdateInput(
    val name: String? = null,
    val active: Boolean? = null,
)

/** Alta de tipo de documento (además del nombre, orden y si es obligatorio). */
@Serializable
data class DocumentTypeCreateInput(
    val name: String,
    val sortOrder: Int? = null,
    val required: Boolean? = null,
)

@Serializable
data class DocumentTypeUpdateInput(
    val name: String? = null,
    val active: Boolean? = null,
    val sortOrder: Int? = null,
    val required: Boolean? = null,
)

/**
 * DELETE de un catálogo: la API hace baja lógica la primera vez (`soft = true`)
 * y física cuando ya estaba inactivo.
 */
@Serializable
data class HrCatalogDeleteResultDto(
    val soft: Boolean = false,
    val data: HrCatalogRefDto,
)

@Serializable
data class DocumentTypeDeleteResultDto(
    val soft: Boolean = false,
    val data: DocumentTypeDto,
)