package com.axzydev.puertonuevoapp.feature.hr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.dashboard.DashboardApi
import com.axzydev.puertonuevoapp.core.network.dashboard.RecordGapsRowDto
import com.axzydev.puertonuevoapp.core.network.hr.EmployeeDocumentDto
import com.axzydev.puertonuevoapp.core.network.hr.HrApi
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableSort
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.users.UserDto
import com.axzydev.puertonuevoapp.core.platform.shareFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Tope de páginas al recorrer la plantilla (100 × 10 = 1000 personas). */
private const val MaxStaffPages = 10
private const val StaffPageSize = 100

/**
 * Tablero de expedientes de RH (`hr.records`).
 *
 * Las personas vienen de `POST /hr/query` (misma tabla de personal, solo
 * activas) y los faltantes de `GET /dashboard/hr-records`, que es el cálculo
 * que usa la web (documentos obligatorios, otros documentos y datos personales
 * vacíos). No hay endpoint tabular de expedientes: el tablero cruza ambas
 * fuentes por `userId` y filtra en el cliente, igual que la web.
 *
 * Acciones: abrir el expediente (navegación), avisar faltantes de una persona
 * (`POST /hr/:id/notify-missing-records`), aviso masivo de la selección
 * (`POST /hr/notify-missing-records`) y descargar/compartir los documentos ya
 * subidos (`GET /hr/:id/documents/:docId/download` → `shareFile`).
 */
class EmployeeRecordsViewModel(
    private val hrApi: HrApi,
    private val dashboardApi: DashboardApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmployeeRecordsUiState())
    val uiState: StateFlow<EmployeeRecordsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val users = runCatching { fetchAllStaff() }
            val gaps = runCatching { dashboardApi.hrRecords().rows.associateBy { it.userId } }
            _uiState.update { state ->
                val failure = users.exceptionOrNull() ?: gaps.exceptionOrNull()
                if (failure != null) {
                    state.copy(loading = false, error = networkMessage(failure))
                } else {
                    val byId = gaps.getOrDefault(emptyMap())
                    state.copy(
                        loading = false,
                        error = null,
                        rows = users.getOrDefault(emptyList()).map { user -> RecordBoardRow(user, byId[user.id]) },
                    )
                }
            }
        }
    }

    /** Recorre la tabla de personal activo por páginas (el tope evita bucles). */
    private suspend fun fetchAllStaff(): List<UserDto> {
        val all = mutableListOf<UserDto>()
        var page = 1
        var total = Int.MAX_VALUE
        while (all.size < total && page <= MaxStaffPages) {
            val response = hrApi.query(
                TableRequest(
                    page = page,
                    limit = StaffPageSize,
                    filters = mapOf("active" to "true"),
                    sort = TableSort(key = "name"),
                ),
            )
            all += response.data
            total = response.total
            if (response.data.isEmpty()) break
            page += 1
        }
        return all
    }

    // -----------------------------------------------------------------------
    // Filtros
    // -----------------------------------------------------------------------

    fun setQuery(value: String) = _uiState.update { it.copy(query = value) }

    fun setDepartment(departmentId: String?) = _uiState.update { it.copy(departmentId = departmentId) }

    fun setStatus(status: RecordStatusFilter) = _uiState.update { it.copy(status = status) }

    // -----------------------------------------------------------------------
    // Avisos de faltantes
    // -----------------------------------------------------------------------

    fun toggleSelected(userId: String) = _uiState.update { state ->
        val row = state.rows.firstOrNull { it.user.id == userId }
        if (row == null || !row.pending) return@update state
        val next = if (userId in state.selected) state.selected - userId else state.selected + userId
        state.copy(selected = next, actionError = null, actionMessage = null)
    }

    /** Marca/desmarca a todos los pendientes de la lista visible. */
    fun toggleSelectAll() = _uiState.update { state ->
        val visible = state.pendingFiltered.map { it.user.id }.toSet()
        val next = if (state.selected.containsAll(visible) && visible.isNotEmpty()) {
            state.selected - visible
        } else {
            state.selected + visible
        }
        state.copy(selected = next)
    }

    fun clearSelection() = _uiState.update { it.copy(selected = emptySet()) }

    /** Aviso individual: notificación en la app + correo si la persona tiene uno. */
    fun notifyOne(row: RecordBoardRow) {
        if (!row.pending || _uiState.value.notifyingId != null) return
        _uiState.update { it.copy(notifyingId = row.user.id, actionError = null, actionMessage = null) }
        viewModelScope.launch {
            val result = runCatching { hrApi.notifyMissing(row.user.id) }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { sent ->
                        state.copy(
                            notifyingId = null,
                            actionMessage = if (sent.emailed) {
                                "Se avisó a ${row.user.name} y se le envió correo."
                            } else {
                                "Se avisó a ${row.user.name} en la app (sin correo registrado)."
                            },
                        )
                    },
                    onFailure = { error -> state.copy(notifyingId = null, actionError = networkMessage(error)) },
                )
            }
        }
    }

    /** Aviso masivo a la selección; el API omite a quien ya está completo. */
    fun notifySelected() {
        val ids = _uiState.value.selectedPending.toList()
        if (ids.isEmpty() || _uiState.value.bulkSaving) return
        _uiState.update { it.copy(bulkSaving = true, actionError = null, actionMessage = null) }
        viewModelScope.launch {
            val result = runCatching { hrApi.notifyMissingBulk(ids) }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { sent ->
                        state.copy(
                            bulkSaving = false,
                            selected = emptySet(),
                            actionMessage = "Aviso enviado a ${sent.notified} persona(s)" +
                                (if (sent.emailed > 0) " (${sent.emailed} con correo)" else "") +
                                (if (sent.skipped > 0) "; ${sent.skipped} ya estaban completas" else "") + ".",
                        )
                    },
                    onFailure = { error -> state.copy(bulkSaving = false, actionError = networkMessage(error)) },
                )
            }
            if (result.isSuccess) load()
        }
    }

    // -----------------------------------------------------------------------
    // Documentos del expediente (ver/descargar)
    // -----------------------------------------------------------------------

    fun toggleDocuments(userId: String) {
        val state = _uiState.value
        if (state.expandedUserId == userId) {
            _uiState.update { it.copy(expandedUserId = null, documents = emptyList(), documentsLoading = false) }
            return
        }
        _uiState.update { it.copy(expandedUserId = userId, documents = emptyList(), documentsLoading = true) }
        viewModelScope.launch {
            val result = runCatching { hrApi.documents(userId) }
            _uiState.update { current ->
                if (current.expandedUserId != userId) return@update current
                result.fold(
                    onSuccess = { documents -> current.copy(documents = documents, documentsLoading = false) },
                    onFailure = { error -> current.copy(documentsLoading = false, actionError = networkMessage(error)) },
                )
            }
        }
    }

    /** Descarga el archivo del API y lo comparte con la hoja del sistema. */
    fun downloadDocument(userId: String, document: EmployeeDocumentDto) {
        if (_uiState.value.downloadingDocumentId != null) return
        _uiState.update { it.copy(downloadingDocumentId = document.id, actionError = null, actionMessage = null) }
        viewModelScope.launch {
            val result = runCatching {
                val bytes = hrApi.downloadDocument(userId, document.id)
                shareFile(document.originalName, document.mimeType, bytes)
            }
            _uiState.update { state ->
                result.fold(
                    onSuccess = {
                        state.copy(
                            downloadingDocumentId = null,
                            actionMessage = "Documento «${document.originalName}» compartido.",
                        )
                    },
                    onFailure = { error -> state.copy(downloadingDocumentId = null, actionError = networkMessage(error)) },
                )
            }
        }
    }

    fun clearFeedback() = _uiState.update { it.copy(actionError = null, actionMessage = null) }
}
