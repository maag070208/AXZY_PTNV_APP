package com.axzydev.puertonuevoapp.feature.hr

import com.axzydev.puertonuevoapp.core.network.dashboard.RecordGapsRowDto
import com.axzydev.puertonuevoapp.core.network.hr.EmployeeDocumentDto
import com.axzydev.puertonuevoapp.core.network.users.UserDto

/** Estado del expediente, igual que el tablero de la web. */
enum class RecordStatus { REQUIRED, INCOMPLETE, COMPLETE }

/** Filtro por estado del expediente. */
enum class RecordStatusFilter(val label: String) {
    All("Todos"),
    Required("Faltan obligatorios"),
    Incomplete("Incompletos"),
    Complete("Completos"),
}

/**
 * Una fila del tablero: la persona (`POST /hr/query`, datos de la cuenta) más
 * sus faltantes (`GET /dashboard/hr-records`, el mismo cálculo de la web).
 */
data class RecordBoardRow(
    val user: UserDto,
    val gaps: RecordGapsRowDto?,
) {
    val status: RecordStatus
        get() {
            val g = gaps ?: return RecordStatus.INCOMPLETE
            return when {
                g.missingRequired.isNotEmpty() -> RecordStatus.REQUIRED
                g.missingDocuments.isNotEmpty() || g.missingFields.isNotEmpty() -> RecordStatus.INCOMPLETE
                else -> RecordStatus.COMPLETE
            }
        }

    /** Tiene algo pendiente (documentos obligatorios, otros o datos personales). */
    val pending: Boolean get() = status != RecordStatus.COMPLETE

    val missingRequiredCount: Int get() = gaps?.missingRequired?.size ?: 0
    val missingOtherCount: Int get() = gaps?.missingDocuments?.size ?: 0
    val missingFieldsCount: Int get() = gaps?.missingFields?.size ?: 0
}

/**
 * Estado del tablero de expedientes: filas (persona + faltantes), filtros de
 * texto/departamento/estado, selección para el aviso masivo y el detalle de
 * documentos de la persona expandida.
 */
/** ¿El estado del expediente pasa el filtro? */
private fun RecordStatusFilter.matches(status: RecordStatus): Boolean = when (this) {
    RecordStatusFilter.All -> true
    RecordStatusFilter.Required -> status == RecordStatus.REQUIRED
    RecordStatusFilter.Incomplete -> status == RecordStatus.INCOMPLETE
    RecordStatusFilter.Complete -> status == RecordStatus.COMPLETE
}

data class EmployeeRecordsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val rows: List<RecordBoardRow> = emptyList(),
    val query: String = "",
    /** `null` = todos los departamentos. */
    val departmentId: String? = null,
    val status: RecordStatusFilter = RecordStatusFilter.All,
    /** Personas marcadas para el aviso masivo. */
    val selected: Set<String> = emptySet(),
    /** Persona a la que se le está enviando el aviso individual. */
    val notifyingId: String? = null,
    val bulkSaving: Boolean = false,
    val actionMessage: String? = null,
    val actionError: String? = null,
    /** Persona con el detalle de documentos abierto (null = ninguno). */
    val expandedUserId: String? = null,
    val documentsLoading: Boolean = false,
    val documents: List<EmployeeDocumentDto> = emptyList(),
    /** Documento que se está descargando/compartiendo. */
    val downloadingDocumentId: String? = null,
) {
    val filtered: List<RecordBoardRow>
        get() {
            val text = query.trim().lowercase()
            return rows.filter { row ->
                (status == RecordStatusFilter.All || status.matches(row.status)) &&
                    (departmentId == null || row.user.departmentId == departmentId) &&
                    (
                        text.isEmpty() ||
                            row.user.name.lowercase().contains(text) ||
                            (row.user.employeeNumber ?: "").lowercase().contains(text) ||
                            (row.user.department?.name ?: "").lowercase().contains(text)
                        )
            }
        }

    val departments: List<Pair<String, String>>
        get() = rows
            .mapNotNull { row ->
                val id = row.user.departmentId ?: return@mapNotNull null
                id to (row.user.department?.name ?: "—")
            }
            .distinctBy { it.first }
            .sortedBy { it.second }

    val total: Int get() = rows.size
    val required: Int get() = rows.count { it.status == RecordStatus.REQUIRED }
    val incomplete: Int get() = rows.count { it.status == RecordStatus.INCOMPLETE }
    val complete: Int get() = rows.count { it.status == RecordStatus.COMPLETE }

    /** Pendientes de la lista visible (a los que se puede avisar). */
    val pendingFiltered: List<RecordBoardRow> get() = filtered.filter { it.pending }

    /** Selección efectiva: solo personas con algo pendiente. */
    val selectedPending: Set<String>
        get() = selected.filterTo(mutableSetOf()) { id -> rows.any { it.user.id == id && it.pending } }
}
