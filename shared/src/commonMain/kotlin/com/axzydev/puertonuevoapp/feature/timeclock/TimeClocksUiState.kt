package com.axzydev.puertonuevoapp.feature.timeclock

import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockCameraDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockConfigDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockDeviceDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockImportDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockInspectCardsDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockInspectEventsDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockInspectOverviewDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockInspectUsersDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockProgressDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockSyncHistoryDto

/**
 * Estado resumido de un reloj (la misma regla que `clockStatus` de la web):
 * pausado por credenciales > sincronizando > último intento fallido > al día >
 * sin sincronizar.
 */
enum class TimeClockCardStatus(val label: String) {
    Ok("Al día"),
    Running("Sincronizando"),
    Paused("En pausa"),
    Error("Sin conexión"),
    Pending("Sin sincronizar"),
}

/** Estado de la tarjeta de [TimeClockDeviceDto]. */
fun clockCardStatus(device: TimeClockDeviceDto): TimeClockCardStatus = when {
    device.pausedByCredentials -> TimeClockCardStatus.Paused
    device.inProgress != null -> TimeClockCardStatus.Running
    device.lastRun != null && !device.lastRun.ok -> TimeClockCardStatus.Error
    device.syncedAt != null -> TimeClockCardStatus.Ok
    else -> TimeClockCardStatus.Pending
}

/** Pestañas de la inspección en vivo del equipo (solo lectura). */
enum class TimeClockInspectTab(val label: String) {
    Overview("Resumen"),
    Users("Personas"),
    Cards("Tarjetas"),
    Events("Eventos"),
    Camera("Cámara"),
}

/** Configuración leída en vivo del reloj: cargando, leída o con error. */
data class TimeClocksConfigUi(
    val loading: Boolean = false,
    val data: TimeClockConfigDto? = null,
    val error: String? = null,
)

/** Timeline de sincronización del reloj (últimos intentos + resumen). */
data class TimeClocksHistoryUi(
    val loading: Boolean = false,
    val data: TimeClockSyncHistoryDto? = null,
    val error: String? = null,
)

/** Inspección del equipo: pestaña activa, período y lo último leído de cada una. */
data class TimeClocksInspectUi(
    val tab: TimeClockInspectTab = TimeClockInspectTab.Overview,
    val loading: Boolean = false,
    val error: String? = null,
    /** Días hacia atrás para la pestaña de eventos (1–30). */
    val days: Int = 2,
    val overview: TimeClockInspectOverviewDto? = null,
    val users: TimeClockInspectUsersDto? = null,
    val cards: TimeClockInspectCardsDto? = null,
    val events: TimeClockInspectEventsDto? = null,
    val camera: TimeClockCameraDto? = null,
)

/** Foto del rostro de una persona del reloj (`personPhoto`), cargada por bytes. */
data class TimeClocksPhotoUi(
    val employeeNumber: String,
    val loading: Boolean = true,
    val bytes: ByteArray? = null,
    val error: String? = null,
)

/**
 * Estado de la administración de relojes checadores: lista con su
 * sincronización, alta/edición/baja, detalle (configuración en vivo, timeline,
 * inspección e importación por fechas). Del equipo solo se lee.
 */
data class TimeClocksUiState(
    // Lista y sincronía
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    /** La API tiene usuario de los relojes configurado; sin él no se conecta. */
    val configured: Boolean = true,
    val devices: List<TimeClockDeviceDto> = emptyList(),
    val inProgress: TimeClockProgressDto? = null,
    val importJob: TimeClockImportDto? = null,
    /** `POST /time-clock/sync` en curso (la corrida se sigue por `inProgress`). */
    val syncing: Boolean = false,

    // Alta
    val registerOpen: Boolean = false,
    val registerUrl: String = "",
    val registerName: String = "",
    val registerCountsAttendance: Boolean = true,
    val registering: Boolean = false,
    val registerError: String? = null,

    // Edición (solo el registro del sistema; no toca el reloj)
    val editSerial: String? = null,
    val editName: String = "",
    val editCountsAttendance: Boolean = true,
    val editWriteEnabled: Boolean = false,
    val savingEdit: Boolean = false,
    val editError: String? = null,

    // Baja
    val retireSerial: String? = null,
    val retiring: Boolean = false,

    // Detalle de un reloj
    val selectedSerial: String? = null,
    val config: TimeClocksConfigUi = TimeClocksConfigUi(),
    val history: TimeClocksHistoryUi = TimeClocksHistoryUi(),
    val inspect: TimeClocksInspectUi = TimeClocksInspectUi(),
    val photo: TimeClocksPhotoUi? = null,

    // Importación manual por fechas
    val importOpen: Boolean = false,
    val importFrom: String = "",
    val importTo: String = "",
    val importing: Boolean = false,
    val importError: String? = null,
) {
    /** Reloj abierto en el detalle (null = lista). */
    val selected: TimeClockDeviceDto? get() = devices.firstOrNull { it.clockSerial == selectedSerial }

    /** Reloj que se está dando de baja (null = sin confirmación). */
    val retireTarget: TimeClockDeviceDto? get() = devices.firstOrNull { it.clockSerial == retireSerial }

    /** Hay una corrida de sincronización en curso (global o de algún reloj). */
    val syncRunning: Boolean get() = inProgress != null || devices.any { it.inProgress != null }

    /** Hay una importación manual en curso. */
    val importRunning: Boolean get() = importJob != null && importJob.finishedAt == null
}

/** `AAAA-MM-DD`, el formato de día que acepta `POST /time-clock/import`. */
fun isImportDay(value: String): Boolean = Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(value)
