package com.axzydev.puertonuevoapp.feature.timeclock

import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import com.axzydev.puertonuevoapp.core.network.http.TableSort
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockApi
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockEmployeeDto
import com.axzydev.puertonuevoapp.core.network.users.UsersApi
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import com.axzydev.puertonuevoapp.core.ui.table.PaginatedTableViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Padrón del reloj: tabla server-side de números del reloj con su vínculo (o
 * sugerencia) a una cuenta del sistema, más las acciones de vínculo.
 *
 * Espejo de `useTimeClockEmployees` de la web y de
 * `TimeClockEmployeesService` del API:
 * - la tabla sale de `POST /time-clock/employees/query` (filtros `q`, `status`,
 *   `employeeNumber`, `name`, `link`; orden por número/nombre/checadas/última
 *   checada/vínculo) y trae el resumen completo en `summary`;
 * - vincular/cambiar y desvincular exigen `time_clock.link` (lo valida el API);
 * - "vincular sugerencias" solo enlaza las `HIGH` (nombre y número coinciden);
 * - el estado de alta en cada reloj (`employeeClockStatus`) exige
 *   `time_clocks.manage` o `time_clock.write`, así que la pantalla lo oculta
 *   cuando no aplica.
 *
 * Las mutaciones refrescan la página vigente; la app solo oculta, el API
 * vuelve a validar cada permiso.
 */
class TimeClockEmployeesViewModel(
    private val timeClockApi: TimeClockApi,
    private val usersApi: UsersApi,
    private val canLink: Boolean,
) : PaginatedTableViewModel<TimeClockEmployeeDto>() {

    override val pageSize: Int = 20
    override val searchKey: String = "q"

    private val _screen = MutableStateFlow(TimeClockEmployeesUiState())
    val screen: StateFlow<TimeClockEmployeesUiState> = _screen.asStateFlow()

    init {
        if (canLink) loadUsers()
    }

    override suspend fun fetch(
        page: Int,
        limit: Int,
        filters: Map<String, String>,
    ): TableResponse<TimeClockEmployeeDto> = fetch(page, limit, filters, null)

    override suspend fun fetch(
        page: Int,
        limit: Int,
        filters: Map<String, String>,
        sort: TableSort?,
    ): TableResponse<TimeClockEmployeeDto> {
        val response = timeClockApi.employees(
            TableRequest(page = page, limit = limit, filters = filters, sort = sort),
        )
        _screen.update { it.copy(summary = response.summary) }
        return TableResponse(data = response.data, total = response.total)
    }

    /** Mosaico/filtro de estado: `LINKED`, `UNLINKED`, `SUGGESTED` o "" (todos). */
    fun selectStatus(status: String) {
        val current = uiState.value.filters
        val next = if (status.isBlank()) {
            current - TimeClockEmployeeStatusKey
        } else {
            current + (TimeClockEmployeeStatusKey to status)
        }
        if (next == current) return
        onFiltersChange(next)
    }

    // ── Vínculo ───────────────────────────────────────────────────────────────

    /** Abre el diálogo con la cuenta actual o la sugerida ya elegida. */
    fun openLink(row: TimeClockEmployeeDto) {
        if (!canLink) return
        _screen.update {
            it.copy(
                linkingTarget = row,
                selectedUserId = row.link?.userId ?: row.suggestion?.userId ?: "",
            )
        }
    }

    fun setSelectedUserId(userId: String) {
        _screen.update { it.copy(selectedUserId = userId) }
    }

    fun dismissLink() {
        if (_screen.value.savingLink) return
        _screen.update { it.copy(linkingTarget = null, selectedUserId = "") }
    }

    /** Confirma el diálogo: vincula (o cambia) el número del reloj con la cuenta. */
    fun confirmLink() {
        val target = _screen.value.linkingTarget ?: return
        link(target, _screen.value.selectedUserId)
    }

    /** Acepta la sugerencia de la fila sin abrir el diálogo. */
    fun acceptSuggestion(row: TimeClockEmployeeDto) {
        val suggestion = row.suggestion ?: return
        link(row, suggestion.userId)
    }

    private fun link(row: TimeClockEmployeeDto, userId: String) {
        if (!canLink || userId.isBlank() || _screen.value.savingNumber != null) return
        _screen.update { it.copy(savingNumber = row.employeeNumber, savingLink = it.linkingTarget != null) }
        viewModelScope.launch {
            try {
                val linked = timeClockApi.linkEmployee(row.employeeNumber, userId)
                val name = linked.link?.name ?: "la cuenta"
                AppSnackbar.show("Se vinculó el número ${row.employeeNumber} con $name")
                _screen.update { it.copy(linkingTarget = null, selectedUserId = "") }
                refresh()
            } catch (e: Throwable) {
                AppSnackbar.showError(networkMessage(e))
            } finally {
                _screen.update { it.copy(savingNumber = null, savingLink = false) }
            }
        }
    }

    fun unlink(row: TimeClockEmployeeDto) {
        if (!canLink || _screen.value.savingNumber != null) return
        _screen.update { it.copy(savingNumber = row.employeeNumber) }
        viewModelScope.launch {
            try {
                timeClockApi.unlinkEmployee(row.employeeNumber)
                AppSnackbar.show("Se quitó el vínculo del número ${row.employeeNumber}")
                refresh()
            } catch (e: Throwable) {
                AppSnackbar.showError(networkMessage(e))
            } finally {
                _screen.update { it.copy(savingNumber = null) }
            }
        }
    }

    /** Vincula de un jalón las sugerencias seguras (`HIGH`). */
    fun linkSuggested() {
        if (!canLink || _screen.value.linkingSuggested) return
        _screen.update { it.copy(linkingSuggested = true) }
        viewModelScope.launch {
            try {
                val result = timeClockApi.linkSuggested()
                AppSnackbar.show(
                    if (result.linkedCount == 0) {
                        "No había sugerencias seguras por vincular"
                    } else {
                        "Se vincularon ${result.linkedCount} sugerencias seguras"
                    },
                )
                refresh()
            } catch (e: Throwable) {
                AppSnackbar.showError(networkMessage(e))
            } finally {
                _screen.update { it.copy(linkingSuggested = false) }
            }
        }
    }

    // ── Estado en los relojes ─────────────────────────────────────────────────

    /** Consulta si la persona vinculada está dada de alta en cada reloj. */
    fun openClockStatus(row: TimeClockEmployeeDto) {
        val userId = row.link?.userId ?: return
        _screen.update {
            it.copy(clockStatusTarget = row, clockStatus = null, loadingClockStatus = true)
        }
        viewModelScope.launch {
            try {
                val status = timeClockApi.employeeClockStatus(userId)
                _screen.update { it.copy(clockStatus = status, loadingClockStatus = false) }
            } catch (e: Throwable) {
                _screen.update { it.copy(clockStatusTarget = null, loadingClockStatus = false) }
                AppSnackbar.showError(networkMessage(e))
            }
        }
    }

    fun dismissClockStatus() {
        _screen.update {
            it.copy(clockStatusTarget = null, clockStatus = null, loadingClockStatus = false)
        }
    }

    // ── Cuentas para el selector ──────────────────────────────────────────────

    private fun loadUsers() {
        _screen.update { it.copy(loadingUsers = true) }
        viewModelScope.launch {
            try {
                val users = usersApi.employees()
                _screen.update { it.copy(users = users, loadingUsers = false) }
            } catch (e: Throwable) {
                _screen.update { it.copy(loadingUsers = false) }
                AppSnackbar.showError("No se pudieron cargar las cuentas: ${networkMessage(e)}")
            }
        }
    }
}
