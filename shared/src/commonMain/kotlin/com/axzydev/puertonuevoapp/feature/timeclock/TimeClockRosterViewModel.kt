package com.axzydev.puertonuevoapp.feature.timeclock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.timeclock.RosterRemovalInput
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockApi
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Rol de turnos: padrón del reloj, corridas de revisión y bajas pendientes.
 *
 * Espejo de `useTimeClockRoster` de la web y de `TimeClockRosterService` del
 * API (`TIME_CLOCK_SYNC.md`):
 * - `roster()` es solo lectura de la BD: nuevos en el reloj, personas que
 *   dejaron de aparecer (baja pendiente) y cuentas dadas de baja que
 *   reaparecieron. Nada se da de baja solo.
 * - `runRoster()` relee la lista de personas de cada reloj que cuenta para
 *   asistencia (solo LEE de los equipos) y recalcula el padrón; exige
 *   `time_clock.sync` y el API rechaza dos corridas simultáneas.
 * - `applyRemoval()` exige motivo (categoría del catálogo + detalle) y da de
 *   baja la cuenta con el flujo de siempre; `dismissRemoval()` descarta la baja
 *   sin motivo. El API exige `users.edit`/`hr.records` para aplicar y
 *   `time_clock.link` para descartar.
 *
 * La app solo oculta las acciones; el API vuelve a validar cada permiso.
 */
class TimeClockRosterViewModel(
    private val timeClockApi: TimeClockApi,
) : ViewModel() {

    private val _state = MutableStateFlow(TimeClockRosterUiState())
    val state: StateFlow<TimeClockRosterUiState> = _state.asStateFlow()

    init {
        load(initial = true)
    }

    /** Recarga el padrón (y el catálogo de motivos si aún no llegó). */
    fun load(initial: Boolean = false) {
        _state.update {
            it.copy(
                loading = initial || (it.roster == null && it.error == null),
                refreshing = !initial && it.roster != null,
                error = null,
            )
        }
        viewModelScope.launch {
            try {
                val roster = timeClockApi.roster()
                val categories = _state.value.categories.ifEmpty {
                    runCatching { timeClockApi.removalCategories() }.getOrDefault(emptyList())
                }
                _state.update {
                    it.copy(
                        roster = roster,
                        categories = categories,
                        loading = false,
                        refreshing = false,
                        error = null,
                    )
                }
            } catch (e: Throwable) {
                _state.update {
                    it.copy(loading = false, refreshing = false, error = networkMessage(e))
                }
            }
        }
    }

    fun retry() = load(initial = true)

    fun refresh() = load(initial = false)

    /** Corre la revisión del padrón contra los relojes (solo lectura de equipos). */
    fun runReview() {
        if (_state.value.running) return
        _state.update { it.copy(running = true) }
        viewModelScope.launch {
            try {
                val result = timeClockApi.runRoster()
                _state.update { it.copy(lastManualRun = result) }
                AppSnackbar.show(
                    if (result.suspicious) {
                        "Revisión sospechosa: un reloj bajó mucho su conteo, no se generaron bajas pendientes"
                    } else {
                        "Padrón revisado: ${result.added} nuevos, ${result.removalPending} bajas pendientes, ${result.reappeared} reapariciones"
                    },
                )
                load(initial = false)
            } catch (e: Throwable) {
                AppSnackbar.showError(networkMessage(e))
            } finally {
                _state.update { it.copy(running = false) }
            }
        }
    }

    // ── Borradores de baja ────────────────────────────────────────────────────

    fun setRemovalCategory(employeeNumber: String, categoryId: String) {
        _state.update { current ->
            val draft = current.draftOf(employeeNumber).copy(categoryId = categoryId)
            current.copy(drafts = current.drafts + (employeeNumber to draft))
        }
    }

    fun setRemovalReason(employeeNumber: String, reason: String) {
        _state.update { current ->
            val draft = current.draftOf(employeeNumber).copy(reason = reason)
            current.copy(drafts = current.drafts + (employeeNumber to draft))
        }
    }

    /** Aplica la baja pendiente con su motivo: da de baja la cuenta (flujo de siempre). */
    fun applyRemoval(employeeNumber: String) {
        val draft = _state.value.draftOf(employeeNumber)
        if (!draft.isValid || _state.value.busyNumber != null) return
        _state.update { it.copy(busyNumber = employeeNumber) }
        viewModelScope.launch {
            try {
                timeClockApi.applyRemoval(
                    employeeNumber = employeeNumber,
                    input = RosterRemovalInput(
                        categoryId = draft.categoryId,
                        reason = draft.reason.trim(),
                        // Igual que la web: la baja se aplica sin aviso adicional.
                        notifyUser = false,
                    ),
                )
                AppSnackbar.show("Se aplicó la baja del número $employeeNumber")
                _state.update { it.copy(drafts = it.drafts - employeeNumber) }
                load(initial = false)
            } catch (e: Throwable) {
                AppSnackbar.showError(networkMessage(e))
            } finally {
                _state.update { it.copy(busyNumber = null) }
            }
        }
    }

    /** Descarta la baja pendiente sin motivo. */
    fun dismissRemoval(employeeNumber: String) {
        if (_state.value.busyNumber != null) return
        _state.update { it.copy(busyNumber = employeeNumber) }
        viewModelScope.launch {
            try {
                timeClockApi.dismissRemoval(employeeNumber)
                AppSnackbar.show("Se descartó la baja pendiente del número $employeeNumber")
                _state.update { it.copy(drafts = it.drafts - employeeNumber) }
                load(initial = false)
            } catch (e: Throwable) {
                AppSnackbar.showError(networkMessage(e))
            } finally {
                _state.update { it.copy(busyNumber = null) }
            }
        }
    }
}
