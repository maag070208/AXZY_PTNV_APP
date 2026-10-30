package com.axzydev.puertonuevoapp.core.network.hr

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.MultipartPart
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import com.axzydev.puertonuevoapp.core.network.users.UserDto
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Expedientes de personal (RH). Endpoints `hr.records` y, para los catálogos,
 * `catalogs.manage`; las actas usan `hr.disciplinary_reports`.
 *
 * Espejo de `web/src/entities/hr/api/hrApi.ts`. La tabla usa `POST /hr/query`
 * con el contrato server-side de `TableRequest` (filtros `name`,
 * `departmentId`, `active`, `role`). Los listados de catálogo aceptan
 * `includeInactive` como en la web.
 *
 * El expediente es el dueño de los datos de la persona: `updateProfile` manda
 * solo lo que se tocó y `clearFields` cubre los borrados que necesitan un
 * `null` explícito (ver [EmployeeProfileField]).
 */
class HrApi(private val client: ApiClient) {

    // ── Plantilla y expediente ──────────────────────────────────────────────

    suspend fun query(request: TableRequest): TableResponse<UserDto> =
        client.post("/hr/query", request)

    /** Totales de la plantilla (`hr.records`); `roles` es dinámico. */
    suspend fun stats(): PersonalStatsDto =
        client.get("/hr/stats")

    suspend fun get(id: String): EmployeeProfileDto =
        client.get("/hr/${id.encodeURLPathPart()}")

    /** Actividad de la persona: resumen, equipo prestado (si tiene `loans.view`), tickets y tareas. */
    suspend fun overview(id: String): EmployeeOverviewDto =
        client.get("/hr/${id.encodeURLPathPart()}/overview")

    /**
     * Edita el expediente. Solo viajan los campos no nulos; los de
     * [EmployeeProfileUpdateInput.clearFields] se mandan como `null` explícito.
     */
    suspend fun updateProfile(id: String, input: EmployeeProfileUpdateInput): EmployeeProfileDto =
        client.patch("/hr/${id.encodeURLPathPart()}/profile", input.toJsonBody())

    /** Reemplaza la lista de descuentos de nómina. */
    suspend fun setDiscounts(id: String, discounts: List<EmployeeDiscountDto>): EmployeeProfileDto =
        client.put("/hr/${id.encodeURLPathPart()}/discounts", EmployeeDiscountsInput(discounts))

    /** Sube/reemplaza la foto (`multipart`, campo `file`); devuelve su URL. */
    suspend fun uploadPhoto(id: String, file: MultipartPart): EmployeePhotoDto =
        client.postMultipart("/hr/${id.encodeURLPathPart()}/photo", parts = listOf(file))

    // ── Documentos del expediente ───────────────────────────────────────────

    suspend fun documents(id: String): List<EmployeeDocumentDto> =
        client.get("/hr/${id.encodeURLPathPart()}/documents")

    /**
     * Sube un documento del expediente (`multipart`: `file` + `documentTypeId`).
     * El tipo debe existir en el catálogo (`documentTypes`).
     */
    suspend fun uploadDocument(
        id: String,
        documentTypeId: String,
        file: MultipartPart,
    ): EmployeeDocumentDto =
        client.postMultipart(
            path = "/hr/${id.encodeURLPathPart()}/documents",
            parts = listOf(file),
            fields = mapOf("documentTypeId" to documentTypeId),
        )

    suspend fun removeDocument(id: String, documentId: String): IdResultDto =
        client.delete("/hr/${id.encodeURLPathPart()}/documents/${documentId.encodeURLPathPart()}")

    /** Bytes del documento (para abrirlo o compartirlo). */
    suspend fun downloadDocument(id: String, documentId: String): ByteArray =
        client.getBytes(
            "/hr/${id.encodeURLPathPart()}/documents/${documentId.encodeURLPathPart()}/download"
        )

    // ── Avisos ──────────────────────────────────────────────────────────────

    /** Avisa a la persona qué le falta (notificación + correo). 409 si ya está completo. */
    suspend fun notifyMissing(id: String): NotifyMissingResultDto =
        client.postNoBody("/hr/${id.encodeURLPathPart()}/notify-missing-records")

    /** Aviso masivo ("Avisar a pendientes"); omite a los completos. */
    suspend fun notifyMissingBulk(userIds: List<String>): NotifyMissingBulkResultDto =
        client.post("/hr/notify-missing-records", NotifyMissingBulkInput(userIds))

    /** Correo de "Alta de personal" con los documentos adjuntos. */
    suspend fun notifyRegistration(id: String): NotifyRegistrationResultDto =
        client.postNoBody("/hr/${id.encodeURLPathPart()}/notify-registration")

    // ── Asignación en lote de departamento/subárea ──────────────────────────

    /**
     * `POST /hr/assignment`. `departmentId` y `subareaId` son obligatorios en el
     * contrato (aceptan `null` = sin departamento), así que el cuerpo se arma a
     * mano para que el `null` viaje explícito y no se omita.
     */
    suspend fun assignDepartments(
        userIds: List<String>,
        departmentId: String?,
        subareaId: String? = null,
    ): EmployeeAssignmentResultDto =
        client.post(
            path = "/hr/assignment",
            body = buildJsonObject {
                putJsonArray("userIds") { userIds.forEach { add(JsonPrimitive(it)) } }
                put("departmentId", departmentId?.let(::JsonPrimitive) ?: JsonNull)
                put("subareaId", subareaId?.let(::JsonPrimitive) ?: JsonNull)
            },
        )

    // ── Actas administrativas ───────────────────────────────────────────────

    /** Tabla server-side de actas (`hr.disciplinary_reports`). */
    suspend fun disciplinaryReports(request: TableRequest): TableResponse<DisciplinaryReportDto> =
        client.post("/hr/disciplinary-reports/query", request)

    /**
     * Tabla de actas con rango de fechas: el API lee
     * `filters.incidentDate = [desde, hasta]` (arreglo de dos días), que
     * [TableRequest.filters] no puede expresar (`Map<String, String>`), así que
     * el cuerpo JSON se arma aquí. `fromDay`/`toDay` son días locales
     * `YYYY-MM-DD`; si ambos vienen vacíos el filtro no viaja.
     */
    suspend fun disciplinaryReports(
        page: Int,
        limit: Int,
        q: String? = null,
        reason: String? = null,
        fromDay: String? = null,
        toDay: String? = null,
    ): TableResponse<DisciplinaryReportDto> =
        client.post<JsonObject, TableResponse<DisciplinaryReportDto>>(
            "/hr/disciplinary-reports/query",
            buildJsonObject {
                put("page", JsonPrimitive(page))
                put("limit", JsonPrimitive(limit))
                put(
                    "filters",
                    buildJsonObject {
                        q?.takeIf { it.isNotBlank() }?.let { put("q", JsonPrimitive(it)) }
                        reason?.takeIf { it.isNotBlank() }?.let { put("reason", JsonPrimitive(it)) }
                        if (!fromDay.isNullOrBlank() || !toDay.isNullOrBlank()) {
                            putJsonArray("incidentDate") {
                                add(fromDay?.takeIf { it.isNotBlank() }?.let(::JsonPrimitive) ?: JsonNull)
                                add(toDay?.takeIf { it.isNotBlank() }?.let(::JsonPrimitive) ?: JsonNull)
                            }
                        }
                    },
                )
            },
        )

    suspend fun disciplinaryReportsByEmployee(id: String): List<DisciplinaryReportDto> =
        client.get("/hr/disciplinary-reports/employee/${id.encodeURLPathPart()}")

    suspend fun disciplinaryReport(id: String): DisciplinaryReportDto =
        client.get("/hr/disciplinary-reports/${id.encodeURLPathPart()}")

    suspend fun createDisciplinaryReport(input: DisciplinaryReportCreateInput): DisciplinaryReportDto =
        client.post("/hr/disciplinary-reports", input)

    suspend fun removeDisciplinaryReport(id: String): IdResultDto =
        client.delete("/hr/disciplinary-reports/${id.encodeURLPathPart()}")

    // ── Catálogos de RH ─────────────────────────────────────────────────────

    suspend fun documentTypes(includeInactive: Boolean = false): List<DocumentTypeDto> =
        client.get("/hr/catalogs/document-types${includeInactiveParam(includeInactive)}")

    suspend fun createDocumentType(input: DocumentTypeCreateInput): DocumentTypeDto =
        client.post("/hr/catalogs/document-types", input)

    suspend fun updateDocumentType(id: String, input: DocumentTypeUpdateInput): DocumentTypeDto =
        client.patch("/hr/catalogs/document-types/${id.encodeURLPathPart()}", input)

    suspend fun removeDocumentType(id: String): DocumentTypeDeleteResultDto =
        client.delete("/hr/catalogs/document-types/${id.encodeURLPathPart()}")

    suspend fun genders(includeInactive: Boolean = false): List<HrCatalogRefDto> =
        client.get("/hr/catalogs/genders${includeInactiveParam(includeInactive)}")

    suspend fun createGender(name: String): HrCatalogRefDto =
        client.post("/hr/catalogs/genders", HrCatalogCreateInput(name))

    suspend fun updateGender(id: String, input: HrCatalogUpdateInput): HrCatalogRefDto =
        client.patch("/hr/catalogs/genders/${id.encodeURLPathPart()}", input)

    suspend fun removeGender(id: String): HrCatalogDeleteResultDto =
        client.delete("/hr/catalogs/genders/${id.encodeURLPathPart()}")

    suspend fun bloodTypes(includeInactive: Boolean = false): List<HrCatalogRefDto> =
        client.get("/hr/catalogs/blood-types${includeInactiveParam(includeInactive)}")

    suspend fun createBloodType(name: String): HrCatalogRefDto =
        client.post("/hr/catalogs/blood-types", HrCatalogCreateInput(name))

    suspend fun updateBloodType(id: String, input: HrCatalogUpdateInput): HrCatalogRefDto =
        client.patch("/hr/catalogs/blood-types/${id.encodeURLPathPart()}", input)

    suspend fun removeBloodType(id: String): HrCatalogDeleteResultDto =
        client.delete("/hr/catalogs/blood-types/${id.encodeURLPathPart()}")
}

private fun includeInactiveParam(includeInactive: Boolean): String =
    if (includeInactive) "?includeInactive=true" else ""

/**
 * Cuerpo del `PATCH /hr/:id/profile`: los no nulos viajan, y las claves de
 * `clearFields` viajan como `null` explícito (el `ApiClient` omite los nulos de
 * un DTO, así que el cuerpo se arma como `JsonObject`).
 */
private fun EmployeeProfileUpdateInput.toJsonBody(): JsonObject = buildJsonObject {
    name?.let { put("name", it) }
    middleName?.let { put("middleName", it) }
    paternalSurname?.let { put("paternalSurname", it) }
    maternalSurname?.let { put("maternalSurname", it) }
    email?.let { put("email", it) }
    employeeNumber?.let { put("employeeNumber", it) }
    jobTitle?.let { put("jobTitle", it) }
    genderId?.let { put("genderId", it) }
    bloodTypeId?.let { put("bloodTypeId", it) }
    medicalConditions?.let { put("medicalConditions", it) }
    allergies?.let { put("allergies", it) }
    birthDate?.let { put("birthDate", it) }
    hireDate?.let { put("hireDate", it) }
    rfc?.let { put("rfc", it) }
    curp?.let { put("curp", it) }
    nss?.let { put("nss", it) }
    streetAddress?.let { put("streetAddress", it) }
    neighborhood?.let { put("neighborhood", it) }
    postalCode?.let { put("postalCode", it) }
    city?.let { put("city", it) }
    addressState?.let { put("addressState", it) }
    country?.let { put("country", it) }
    personalPhone?.let { put("personalPhone", it) }
    workPhone?.let { put("workPhone", it) }
    departmentId?.let { put("departmentId", it) }
    subareaId?.let { put("subareaId", it) }
    emergencyContactName?.let { put("emergencyContactName", it) }
    emergencyContactPhone?.let { put("emergencyContactPhone", it) }
    emergencyContactRelationship?.let { put("emergencyContactRelationship", it) }
    clearFields.forEach { field -> put(field.key, JsonNull) }
}
