package com.axzydev.puertonuevoapp.core.network.personal

import com.axzydev.puertonuevoapp.core.network.common.DepartmentRefDto
import com.axzydev.puertonuevoapp.core.network.users.SubareaRefDto
import kotlinx.serialization.Serializable

/** Catálogo de RH embebido (género, tipo de sangre). */
@Serializable
data class HrCatalogRefDto(
    val id: String,
    val nombre: String,
    val activo: Boolean = true,
)

/** Descuento de nómina de un empleado (INFONAVIT/IMSS/DEUDOR_ALIMENTICIO). */
@Serializable
data class EmployeeDiscountDto(
    val tipo: String,
    val nota: String? = null,
)

@Serializable
data class DocumentTypeRefDto(
    val id: String,
    val nombre: String,
)

/** Tipo de documento del expediente (catálogo RH). */
@Serializable
data class DocumentTypeDto(
    val id: String,
    val nombre: String,
    val activo: Boolean = true,
    val orden: Int = 0,
    val createdAt: String = "",
)

/** Documento subido al expediente de un empleado. */
@Serializable
data class EmployeeDocumentDto(
    val id: String,
    val tipoDocumentoId: String,
    val tipoDocumento: DocumentTypeRefDto? = null,
    val originalName: String,
    val mimeType: String,
    val sizeBytes: Long = 0,
    val url: String = "",
    val uploadedById: String,
    val createdAt: String,
)

/**
 * Expediente completo de un empleado — `GET /personal/:id`, mismo shape que el
 * DTO de la web (`personalProfileToDto`).
 */
@Serializable
data class PersonalProfileDto(
    val id: String,
    val username: String,
    val name: String,
    val email: String? = null,
    val role: String,
    val active: Boolean = true,
    val puesto: String? = null,
    val numeroEmpleado: String? = null,
    val empresa: String? = null,
    val department: DepartmentRefDto? = null,
    val subarea: SubareaRefDto? = null,

    val segundoNombre: String? = null,
    val apellidoPaterno: String? = null,
    val apellidoMaterno: String? = null,
    val fotoUrl: String? = null,

    val genero: HrCatalogRefDto? = null,
    val tipoSangre: HrCatalogRefDto? = null,
    val padecimiento: String? = null,
    val alergias: String? = null,

    val fechaNacimiento: String? = null,
    val fechaIngreso: String? = null,

    val rfc: String? = null,
    val curp: String? = null,
    val nss: String? = null,

    val calleNumero: String? = null,
    val colonia: String? = null,
    val codigoPostal: String? = null,
    val ciudad: String? = null,
    val estadoDireccion: String? = null,
    val pais: String? = null,

    val celularPersonal: String? = null,
    val celularEmpresa: String? = null,

    val contactoEmergenciaNombre: String? = null,
    val contactoEmergenciaTelefono: String? = null,
    val contactoEmergenciaParentesco: String? = null,

    val discounts: List<EmployeeDiscountDto> = emptyList(),

    val createdAt: String,
)