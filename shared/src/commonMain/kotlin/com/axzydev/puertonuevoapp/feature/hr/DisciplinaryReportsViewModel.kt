package com.axzydev.puertonuevoapp.feature.hr

import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.hr.DisciplinaryReportCreateInput
import com.axzydev.puertonuevoapp.core.network.hr.DisciplinaryReportDto
import com.axzydev.puertonuevoapp.core.network.hr.HrApi
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import com.axzydev.puertonuevoapp.core.network.http.TableSort
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.users.UserDto
import com.axzydev.puertonuevoapp.core.ui.table.PaginatedTableViewModel
import com.axzydev.puertonuevoapp.core.util.currentTimeMillis
import com.axzydev.puertonuevoapp.core.util.formatIsoUtc
import com.axzydev.puertonuevoapp.core.util.localOffsetMillis
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Espera antes de buscar personas, para no pedir una lista por letra. */
private const val SearchDebounceMs = 350L

/**
 * Actas administrativas (`hr.disciplinary_reports`): tabla server-side
 * (`POST /hr/disciplinary-reports/query`) con filtros de texto (`q`), motivo y
 * rango de fecha del incidente (`incidentDate: [desde, hasta]`), más el alta
 * (`POST /hr/disciplinary-reports`) en un formulario modal.
 */
class DisciplinaryReportsViewModel(
    private val hrApi: HrApi,
) : PaginatedTableViewModel<DisciplinaryReportDto>() {

    override val pageSize: Int = 20
    override val searchKey: String? = "q"

    private val _form = MutableStateFlow(DisciplinaryFormState())
    val formState: StateFlow<DisciplinaryFormState> = _form.asStateFlow()

    private var peopleJob: Job? = null

    override suspend fun fetch(page: Int, limit: Int, filters: Map<String, String>): TableResponse<DisciplinaryReportDto> =
        hrApi.disciplinaryReports(
            page = page,
            limit = limit,
            q = filters["q"],
            reason = filters["reason"],
            fromDay = filters["from"],
            toDay = filters["to"],
        )

    // -----------------------------------------------------------------------
    // Alta
    // -----------------------------------------------------------------------

    fun openForm() {
        _form.value = DisciplinaryFormState(visible = true, incidentDate = todayIso())
        loadPeople("")
    }

    fun closeForm() {
        peopleJob?.cancel()
        _form.value = DisciplinaryFormState()
    }

    fun setPersonQuery(value: String) {
        _form.update { it.copy(personQuery = value) }
        peopleJob?.cancel()
        peopleJob = viewModelScope.launch {
            delay(SearchDebounceMs)
            loadPeople(value)
        }
    }

    private fun loadPeople(query: String) {
        viewModelScope.launch {
            _form.update { it.copy(peopleLoading = true) }
            val filters = buildMap {
                put("active", "true")
                query.trim().takeIf { it.isNotBlank() }?.let { put("name", it) }
            }
            val result = runCatching {
                hrApi.query(
                    TableRequest(page = 1, limit = 8, filters = filters, sort = TableSort(key = "name")),
                ).data
            }
            _form.update { it.copy(people = result.getOrDefault(emptyList()), peopleLoading = false) }
        }
    }

    fun selectPerson(user: UserDto) = _form.update { it.copy(selected = user, personQuery = "", people = emptyList()) }

    fun clearPerson() = _form.update { it.copy(selected = null, personQuery = "") }

    fun setReason(reason: String) = _form.update { it.copy(reason = reason) }

    fun setIncidentDate(value: String) = _form.update {
        it.copy(incidentDate = value.filter { ch -> ch.isDigit() || ch == '-' }.take(10))
    }

    fun setDescription(value: String) = _form.update { it.copy(description = value) }

    fun setSanction(value: String) = _form.update { it.copy(sanction = value) }

    fun save() {
        val form = _form.value
        if (!form.canSave) return
        val person = form.selected ?: return
        _form.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                hrApi.createDisciplinaryReport(
                    DisciplinaryReportCreateInput(
                        userId = person.id,
                        reason = form.reason,
                        incidentDate = form.incidentDate,
                        description = form.description.trim(),
                        sanction = form.sanction.trim().ifBlank { null },
                    ),
                )
            }
            _form.update { state ->
                result.fold(
                    onSuccess = { created ->
                        state.copy(saving = false, visible = false, createdId = created.id)
                    },
                    onFailure = { error -> state.copy(saving = false, error = networkMessage(error)) },
                )
            }
            if (result.isSuccess) refresh()
        }
    }

    /** La pantalla ya navegó al detalle del acta recién creada. */
    fun consumeCreated() = _form.update { it.copy(createdId = null) }

    fun clearFormError() = _form.update { it.copy(error = null) }
}

/** Día local de hoy `YYYY-MM-DD` (el API trabaja con claves de día). */
private fun todayIso(): String {
    val now = currentTimeMillis()
    return formatIsoUtc(now + localOffsetMillis(now)).take(10)
}
