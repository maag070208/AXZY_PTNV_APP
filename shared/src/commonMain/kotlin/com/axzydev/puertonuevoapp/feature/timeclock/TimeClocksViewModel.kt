package com.axzydev.puertonuevoapp.feature.timeclock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockApi
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockDeviceDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockImportInput
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockRegisterInput
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockStatusDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockUpdateInput
import com.axzydev.puertonuevoapp.core.ui.AppSnackbar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Cada cuánto se refresca el estado mientras hay una sincronización o importación en curso. */
private const val PollMillis = 4_000L

/**
 * Administración de los relojes checadores (solo ADMIN, `time_clocks.manage`):
 * lista con su sincronización, alta/edición/baja del registro del sistema,
 * sincronía manual (`sync`), importación por fechas, configuración leída en
 * vivo, timeline de sincronización e inspección del equipo.
 *
 * **Del reloj solo se LEE**: dar de alta, editar o dar de baja cambia únicamente
 * cómo el sistema se conecta y usa sus checadas (TIME_CLOCK.md §1 y §4).
 */
class TimeClocksViewModel(
    private val api: TimeClockApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimeClocksUiState())
    val uiState: StateFlow<TimeClocksUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        load()
    }

    // ── Lista y sincronización ─────────────────────────────────────────────

    /** Lee `GET /time-clock/status` (relojes, corrida en curso e importación). */
    fun load(showSpinner: Boolean = true) {
        _uiState.update {
            it.copy(
                loading = showSpinner && it.devices.isEmpty(),
                refreshing = true,
                error = null,
            )
        }
        viewModelScope.launch {
            val result = runCatching { api.status() }
            result.fold(
                onSuccess = { status ->
                    applyStatus(status)
                    _uiState.update { it.copy(loading = false, refreshing = false) }
                    pollIfBusy()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(loading = false, refreshing = false, error = networkMessage(error))
                    }
                },
            )
        }
    }

    /** Relee sin vaciar la pantalla (botón de actualizar). */
    fun refresh() = load(showSpinner = false)

    /**
     * `POST /time-clock/sync`: drena de cada reloj todo lo pendiente desde su
     * cursor. Corre en segundo plano; el avance llega en `status.inProgress`.
     * 409 si todos los relojes ya están sincronizando.
     */
    fun sync() {
        if (_uiState.value.syncing) return
        _uiState.update { it.copy(syncing = true) }
        viewModelScope.launch {
            val result = runCatching { api.sync() }
            _uiState.update { it.copy(syncing = false) }
            result.fold(
                onSuccess = {
                    AppSnackbar.show("Sincronización iniciada: se está trayendo de cada reloj lo que falte.")
                    load(showSpinner = false)
                },
                onFailure = { AppSnackbar.showError(networkMessage(it)) },
            )
        }
    }

    private fun applyStatus(status: TimeClockStatusDto) {
        _uiState.update {
            it.copy(
                configured = status.configured,
                devices = status.devices,
                inProgress = status.inProgress,
                importJob = status.importJob,
                error = null,
            )
        }
    }

    /** Mientras haya corrida o importación en curso, refresca cada 4 s. */
    private fun pollIfBusy() {
        if (pollJob?.isActive == true) return
        val busy = _uiState.value.syncRunning || _uiState.value.importRunning
        if (!busy) return
        pollJob = viewModelScope.launch {
            while (true) {
                delay(PollMillis)
                val status = runCatching { api.status() }.getOrNull() ?: break
                applyStatus(status)
                val stillBusy = status.inProgress != null ||
                    status.devices.any { it.inProgress != null } ||
                    (status.importJob != null && status.importJob.finishedAt == null)
                if (!stillBusy) break
            }
        }
    }

    // ── Alta ───────────────────────────────────────────────────────────────

    fun openRegister() = _uiState.update {
        it.copy(
            registerOpen = true,
            registerUrl = "",
            registerName = "",
            registerCountsAttendance = true,
            registerError = null,
        )
    }

    fun closeRegister() {
        if (_uiState.value.registering) return
        _uiState.update { it.copy(registerOpen = false, registerError = null) }
    }

    fun setRegisterUrl(value: String) = _uiState.update { it.copy(registerUrl = value) }

    fun setRegisterName(value: String) = _uiState.update { it.copy(registerName = value) }

    fun setRegisterCountsAttendance(value: Boolean) =
        _uiState.update { it.copy(registerCountsAttendance = value) }

    /** `POST /time-clock/clocks`: la API se conecta, lee la identidad y arranca la sincronización. */
    fun register() {
        val state = _uiState.value
        val url = state.registerUrl.trim()
        if (state.registering || url.isEmpty()) return
        _uiState.update { it.copy(registering = true, registerError = null) }
        viewModelScope.launch {
            val result = runCatching {
                api.registerClock(
                    TimeClockRegisterInput(
                        url = url,
                        name = state.registerName.trim().ifBlank { null },
                        countsAttendance = state.registerCountsAttendance,
                    )
                )
            }
            _uiState.update { it.copy(registering = false) }
            result.fold(
                onSuccess = { clock ->
                    _uiState.update { it.copy(registerOpen = false) }
                    AppSnackbar.show(
                        "${clock.name.ifBlank { clock.clockSerial }} dado de alta: ya está trayendo sus checadas."
                    )
                    load(showSpinner = false)
                },
                onFailure = { error -> _uiState.update { it.copy(registerError = networkMessage(error)) } },
            )
        }
    }

    // ── Edición (solo el registro del sistema) ─────────────────────────────

    fun openEdit(device: TimeClockDeviceDto) =
        _uiState.update {
            it.copy(
                editSerial = device.clockSerial,
                editName = device.name,
                editCountsAttendance = device.countsAttendance,
                editWriteEnabled = device.writeEnabled,
                editError = null,
            )
        }

    fun closeEdit() {
        if (_uiState.value.savingEdit) return
        _uiState.update { it.copy(editSerial = null, editError = null) }
    }

    fun setEditName(value: String) = _uiState.update { it.copy(editName = value) }

    fun setEditCountsAttendance(value: Boolean) = _uiState.update { it.copy(editCountsAttendance = value) }

    fun setEditWriteEnabled(value: Boolean) = _uiState.update { it.copy(editWriteEnabled = value) }

    /** `PATCH /time-clock/clocks/:serial`: nombre, si cuenta para asistencia y si acepta escritura. */
    fun saveEdit() {
        val state = _uiState.value
        val serial = state.editSerial ?: return
        val name = state.editName.trim()
        if (state.savingEdit || name.isEmpty()) return
        _uiState.update { it.copy(savingEdit = true, editError = null) }
        viewModelScope.launch {
            val result = runCatching {
                api.updateClock(
                    serial,
                    TimeClockUpdateInput(
                        name = name,
                        countsAttendance = state.editCountsAttendance,
                        writeEnabled = state.editWriteEnabled,
                    ),
                )
            }
            _uiState.update { it.copy(savingEdit = false) }
            result.fold(
                onSuccess = { clock ->
                    _uiState.update { it.copy(editSerial = null) }
                    AppSnackbar.show("${clock.name.ifBlank { clock.clockSerial }} actualizado.")
                    load(showSpinner = false)
                },
                onFailure = { error -> _uiState.update { it.copy(editError = networkMessage(error)) } },
            )
        }
    }

    // ── Baja ───────────────────────────────────────────────────────────────

    fun askRetire(serial: String) = _uiState.update { it.copy(retireSerial = serial) }

    fun cancelRetire() {
        if (_uiState.value.retiring) return
        _uiState.update { it.copy(retireSerial = null) }
    }

    /** `DELETE /time-clock/clocks/:serial`: deja de sincronizar; sus checadas y su cursor se quedan. */
    fun confirmRetire() {
        val state = _uiState.value
        val serial = state.retireSerial ?: return
        if (state.retiring) return
        _uiState.update { it.copy(retiring = true) }
        viewModelScope.launch {
            val result = runCatching { api.retireClock(serial) }
            result.fold(
                onSuccess = { retired ->
                    _uiState.update {
                        it.copy(
                            retiring = false,
                            retireSerial = null,
                            selectedSerial = it.selectedSerial?.takeIf { selected -> selected != retired.clockSerial },
                        )
                    }
                    AppSnackbar.show("${retired.clockSerial} dado de baja.")
                    load(showSpinner = false)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(retiring = false, retireSerial = null) }
                    AppSnackbar.showError(networkMessage(error))
                },
            )
        }
    }

    // ── Detalle del reloj ──────────────────────────────────────────────────

    /** Abre el detalle: configuración en vivo, timeline e inspección del equipo. */
    fun openClock(serial: String) {
        _uiState.update {
            it.copy(
                selectedSerial = serial,
                config = TimeClocksConfigUi(loading = true),
                history = TimeClocksHistoryUi(loading = true),
                inspect = TimeClocksInspectUi(),
                photo = null,
            )
        }
        loadConfig(serial)
        loadHistory(serial)
        loadInspect()
    }

    /** Vuelve a la lista de relojes (el detalle es una sección, no otra pantalla). */
    fun closeClock() = _uiState.update {
        it.copy(
            selectedSerial = null,
            config = TimeClocksConfigUi(),
            history = TimeClocksHistoryUi(),
            inspect = TimeClocksInspectUi(),
            photo = null,
        )
    }

    /** `GET /clocks/:serial/settings`: configuración leída del reloj, sin cambiarle nada. */
    fun loadConfig(serial: String? = null) {
        val target = serial ?: _uiState.value.selectedSerial ?: return
        _uiState.update { it.copy(config = it.config.copy(loading = true, error = null)) }
        viewModelScope.launch {
            val result = runCatching { api.clockSettings(target) }
            _uiState.update { state ->
                if (state.selectedSerial != target) {
                    state
                } else {
                    state.copy(
                        config = TimeClocksConfigUi(
                            loading = false,
                            data = result.getOrNull() ?: state.config.data,
                            error = result.exceptionOrNull()?.let(::networkMessage),
                        )
                    )
                }
            }
        }
    }

    /** `GET /clocks/:serial/events`: últimos intentos de sincronización del reloj. */
    fun loadHistory(serial: String? = null) {
        val target = serial ?: _uiState.value.selectedSerial ?: return
        _uiState.update { it.copy(history = it.history.copy(loading = true, error = null)) }
        viewModelScope.launch {
            val result = runCatching { api.clockEvents(target) }
            _uiState.update { state ->
                if (state.selectedSerial != target) {
                    state
                } else {
                    state.copy(
                        history = TimeClocksHistoryUi(
                            loading = false,
                            data = result.getOrNull() ?: state.history.data,
                            error = result.exceptionOrNull()?.let(::networkMessage),
                        )
                    )
                }
            }
        }
    }

    // ── Inspección del equipo (solo lectura) ───────────────────────────────

    fun selectInspectTab(tab: TimeClockInspectTab) {
        _uiState.update { it.copy(inspect = it.inspect.copy(tab = tab, error = null)) }
        loadInspect()
    }

    fun setInspectDays(days: Int) {
        _uiState.update { it.copy(inspect = it.inspect.copy(days = days.coerceIn(1, 30))) }
        if (_uiState.value.inspect.tab == TimeClockInspectTab.Events) loadInspect()
    }

    /** Relee la pestaña de inspección activa. */
    fun refreshInspect() = loadInspect()

    /**
     * Lee del equipo lo que corresponde a la pestaña activa:
     * `inspect/overview`, `inspect/users`, `inspect/cards`, `inspect/events?days=`
     * o `inspect/camera`. Nunca escribe en el reloj.
     */
    fun loadInspect() {
        val state = _uiState.value
        val serial = state.selectedSerial ?: return
        val tab = state.inspect.tab
        val days = state.inspect.days
        _uiState.update { it.copy(inspect = it.inspect.copy(loading = true, error = null)) }
        viewModelScope.launch {
            try {
                when (tab) {
                    TimeClockInspectTab.Overview -> {
                        val data = api.inspectOverview(serial)
                        _uiState.update { it.copy(inspect = it.inspect.copy(overview = data)) }
                    }

                    TimeClockInspectTab.Users -> {
                        val data = api.inspectUsers(serial)
                        _uiState.update { it.copy(inspect = it.inspect.copy(users = data)) }
                    }

                    TimeClockInspectTab.Cards -> {
                        val data = api.inspectCards(serial)
                        _uiState.update { it.copy(inspect = it.inspect.copy(cards = data)) }
                    }

                    TimeClockInspectTab.Events -> {
                        val data = api.inspectEvents(serial, days)
                        _uiState.update { it.copy(inspect = it.inspect.copy(events = data)) }
                    }

                    TimeClockInspectTab.Camera -> {
                        val data = api.inspectCamera(serial)
                        _uiState.update { it.copy(inspect = it.inspect.copy(camera = data)) }
                    }
                }
                _uiState.update { it.copy(inspect = it.inspect.copy(loading = false, error = null)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update { it.copy(inspect = it.inspect.copy(loading = false, error = networkMessage(e))) }
            }
        }
    }

    /** `GET .../people/:employeeNo/photo`: foto del rostro que tiene el reloj (JPEG). */
    fun openPhoto(employeeNumber: String) {
        val serial = _uiState.value.selectedSerial ?: return
        _uiState.update { it.copy(photo = TimeClocksPhotoUi(employeeNumber = employeeNumber)) }
        viewModelScope.launch {
            val result = runCatching { api.personPhoto(serial, employeeNumber) }
            _uiState.update { state ->
                val photo = state.photo
                if (photo == null || photo.employeeNumber != employeeNumber) {
                    state
                } else {
                    state.copy(
                        photo = photo.copy(
                            loading = false,
                            bytes = result.getOrNull(),
                            error = result.exceptionOrNull()?.let(::networkMessage),
                        )
                    )
                }
            }
        }
    }

    fun closePhoto() = _uiState.update { it.copy(photo = null) }

    // ── Importación manual por fechas ──────────────────────────────────────

    fun openImport() = _uiState.update { it.copy(importOpen = true, importError = null) }

    fun closeImport() {
        if (_uiState.value.importing) return
        _uiState.update { it.copy(importOpen = false, importError = null) }
    }

    fun setImportFrom(value: String) = _uiState.update { it.copy(importFrom = value) }

    fun setImportTo(value: String) = _uiState.update { it.copy(importTo = value) }

    /**
     * `POST /time-clock/import`: trae de todos los relojes las checadas de esos
     * días (202; el avance sale en `status.importJob`). No mueve los cursores.
     */
    fun startImport() {
        val state = _uiState.value
        val from = state.importFrom.trim()
        val to = state.importTo.trim()
        if (state.importing || !isImportDay(from) || !isImportDay(to)) return
        _uiState.update { it.copy(importing = true, importError = null) }
        viewModelScope.launch {
            val result = runCatching { api.startImport(TimeClockImportInput(from = from, to = to)) }
            _uiState.update { it.copy(importing = false) }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(importOpen = false) }
                    AppSnackbar.show("Importación iniciada: las checadas de $from a $to se están trayendo de los relojes.")
                    load(showSpinner = false)
                },
                onFailure = { error -> _uiState.update { it.copy(importError = networkMessage(error)) } },
            )
        }
    }
}
