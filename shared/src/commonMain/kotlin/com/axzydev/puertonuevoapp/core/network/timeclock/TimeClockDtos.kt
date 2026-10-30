package com.axzydev.puertonuevoapp.core.network.timeclock

import com.axzydev.puertonuevoapp.core.network.http.TableSort
import kotlinx.serialization.Serializable

/**
 * DTOs del reloj checador (Hikvision) y de su padrón.
 *
 * Contrato autoritativo: `api/src/modules/time-clock/models/dto/time-clock.dto.ts`
 * (y los mappers de sus servicios). Espejo de los tipos del modelo de la web en
 * `web/src/entities/time-clock/model/`. La API **solo lee** de los relojes
 * para las checadas; las altas/bajas/huellas son escrituras explícitas que el
 * equipo debe permitir (`writeEnabled`).
 */

/** Cómo se identificó la persona en el reloj (derivado del `minor` ISAPI). */
@Serializable
data class TimeClockPunchDto(
    val id: String,
    val clockSerial: String = "",
    /** Nombre del reloj donde se checó; `null` si nunca se le puso nombre. */
    val clock: String? = null,
    /** Consecutivo del evento en el reloj. */
    val serialNo: Int = 0,
    val employeeNumber: String = "",
    val name: String = "",
    /** FACE, FINGERPRINT, CARD u OTHER. */
    val method: String = "OTHER",
    val minor: Int = 0,
    /** Instante de la checada según el reloj. */
    val occurredAt: String = "",
    val createdAt: String = "",
)

/** Una corrida de sincronización (último intento de cada reloj). */
@Serializable
data class TimeClockRunDto(
    val ok: Boolean = false,
    val clockSerial: String? = null,
    val startedAt: String = "",
    val finishedAt: String = "",
    val readCount: Int = 0,
    val newCount: Int = 0,
    val lastSerialNo: Int? = null,
    val error: String? = null,
)

/** Avance de la corrida en curso, en eventos del reloj (consecutivos). */
@Serializable
data class TimeClockProgressDto(
    val startedAt: String = "",
    val readCount: Int = 0,
    val newCount: Int = 0,
    /** Eventos que faltan; `null` mientras el reloj no da la cota. */
    val remaining: Int? = null,
    val total: Int? = null,
)

/** Importación manual por rango de días (`POST /time-clock/import`). */
@Serializable
data class TimeClockImportDto(
    val from: String = "",
    val to: String = "",
    val startedAt: String = "",
    /** `null` mientras sigue en curso. */
    val finishedAt: String? = null,
    val total: Int? = null,
    val readCount: Int = 0,
    val newCount: Int = 0,
    val error: String? = null,
)

/** Body de `POST /time-clock/import`: días locales inclusive y zona IANA opcional. */
@Serializable
data class TimeClockImportInput(
    val from: String,
    val to: String,
    val tz: String? = null,
)

/** Un reloj dado de alta y el estado de su sincronización. */
@Serializable
data class TimeClockDeviceDto(
    val clockSerial: String = "",
    val name: String = "",
    val url: String = "",
    /** Si sus checadas arman entradas/salidas y horas extra. */
    val countsAttendance: Boolean = false,
    /** Si el sistema puede ESCRIBIR en este reloj (altas, bajas, huellas). */
    val writeEnabled: Boolean = false,
    val model: String? = null,
    val lastSerialNo: Int = 0,
    val syncedAt: String? = null,
    val punches: Int = 0,
    val lastPunch: String? = null,
    val inProgress: TimeClockProgressDto? = null,
    val lastRun: TimeClockRunDto? = null,
    /** El reloj rechazó las credenciales: su sincronización automática se detuvo. */
    val pausedByCredentials: Boolean = false,
)

/** Estado global de la sincronización (`GET /time-clock/status`). */
@Serializable
data class TimeClockStatusDto(
    /** `false` si la API no tiene configurado el usuario del checador. */
    val configured: Boolean = false,
    val inProgress: TimeClockProgressDto? = null,
    val importJob: TimeClockImportDto? = null,
    val devices: List<TimeClockDeviceDto> = emptyList(),
)

/** Alta de un reloj (`POST /time-clock/clocks`): dirección y nombre opcional. */
@Serializable
data class TimeClockRegisterInput(
    val url: String,
    val name: String? = null,
    val countsAttendance: Boolean? = null,
)

/** Cambios del registro del reloj (`PATCH /time-clock/clocks/:serial`); no toca el equipo. */
@Serializable
data class TimeClockUpdateInput(
    val name: String? = null,
    val countsAttendance: Boolean? = null,
    val writeEnabled: Boolean? = null,
)

/** Respuesta del DELETE del reloj: la serie dada de baja. */
@Serializable
data class TimeClockRetiredDto(
    val clockSerial: String = "",
)

/** Configuración leída en vivo del reloj (`GET /clocks/:serial/settings`). Solo lectura. */
@Serializable
data class TimeClockConfigDto(
    val clockSerial: String = "",
    val readAt: String = "",
    val device: TimeClockConfigDeviceDto = TimeClockConfigDeviceDto(),
    /** `null` si el reloj no dio la hora. */
    val hour: TimeClockConfigHourDto? = null,
    /** `null` si el reloj no dio los conteos. */
    val people: TimeClockConfigPeopleDto? = null,
)

@Serializable
data class TimeClockConfigDeviceDto(
    val name: String? = null,
    val model: String? = null,
    val firmware: String? = null,
    val mac: String? = null,
)

@Serializable
data class TimeClockConfigHourDto(
    val localTime: String = "",
    /** `manual` o `NTP`. */
    val mode: String? = null,
    /** Zona POSIX del reloj. */
    val zone: String? = null,
    /** Reloj − servidor en segundos (positivo = el reloj va adelantado). */
    val driftSeconds: Int = 0,
)

@Serializable
data class TimeClockConfigPeopleDto(
    val total: Int = 0,
    val withFace: Int = 0,
    val withFingerprint: Int = 0,
    val withCard: Int = 0,
)

// ── Vinculación número del reloj ↔ usuario ───────────────────────────────────

/** Usuario del sistema al que apunta (o podría apuntar) un número del reloj. */
@Serializable
data class TimeClockUserRefDto(
    val userId: String = "",
    val name: String = "",
    val employeeNumber: String? = null,
    val active: Boolean = true,
)

/** Sugerencia de vínculo: `confidence` es HIGH (nombre y número) o MEDIUM (solo nombre). */
@Serializable
data class TimeClockSuggestionDto(
    val userId: String = "",
    val name: String = "",
    val employeeNumber: String? = null,
    val active: Boolean = true,
    val confidence: String = "HIGH",
)

/** Empleado visto en el reloj y su vínculo con una cuenta. */
@Serializable
data class TimeClockEmployeeDto(
    /** Número del empleado en el reloj (la llave del vínculo). */
    val employeeNumber: String = "",
    /** Nombre como está guardado en el reloj. */
    val name: String = "",
    val punches: Int = 0,
    val lastPunch: String = "",
    val link: TimeClockUserRefDto? = null,
    val suggestion: TimeClockSuggestionDto? = null,
)

@Serializable
data class TimeClockEmployeesSummaryDto(
    val total: Int = 0,
    val linkedCount: Int = 0,
    val withoutLink: Int = 0,
    /** Sin vincular con sugerencia HIGH (las que vincula el botón masivo). */
    val registrationSuggestions: Int = 0,
)

/** Respuesta de `POST /time-clock/employees/query`: tabla + resumen del filtro. */
@Serializable
data class TimeClockEmployeesResponseDto(
    val data: List<TimeClockEmployeeDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageIndex: Int = 0,
    val totalPages: Int = 0,
    val totalCount: Int = 0,
    val limit: Int = 20,
    val hasPreviousPage: Boolean = false,
    val hasNextPage: Boolean = false,
    val summary: TimeClockEmployeesSummaryDto = TimeClockEmployeesSummaryDto(),
)

/** Body para vincular un número del reloj a una cuenta (`PUT /time-clock/employees/:number`). */
@Serializable
data class TimeClockLinkInput(
    val userId: String,
)

/** Resultado del vínculo masivo (`POST /time-clock/employees/link-suggested`). */
@Serializable
data class TimeClockLinkSuggestedResultDto(
    val linkedCount: Int = 0,
)

/** Respuesta de los endpoints que confirman un número de empleado. */
@Serializable
data class EmployeeNumberResultDto(
    val employeeNumber: String = "",
)

/** Estado del alta de una persona del sistema en cada reloj registrado. */
@Serializable
data class EmployeeClockStatusDto(
    val employeeNumber: String? = null,
    val clocks: List<EmployeeClockStatusClockDto> = emptyList(),
)

@Serializable
data class EmployeeClockStatusClockDto(
    val serial: String = "",
    val name: String = "",
    /** ONLINE o NO_RESPONSE. */
    val connection: String = "NO_RESPONSE",
    /** SIN_ALTA, DADA_DE_ALTA o `null` si no se pudo leer. */
    val state: String? = null,
    /** El reloj acepta altas desde la app (escritura habilitada). */
    val canAlta: Boolean = false,
    /** Si sus checadas cuentan para entradas/salidas. */
    val countsAttendance: Boolean = false,
)

/** Resultado de una baja/alta en UN reloj. */
@Serializable
data class ClockSyncResultDto(
    val serial: String = "",
    val name: String = "",
    val ok: Boolean = false,
    /** TIME_CLOCK_READ_ONLY, TIME_CLOCK_NO_RESPONSE u otro código del API. */
    val code: String? = null,
)

/** Respuesta de las operaciones que recorren todos los relojes (alta/baja masiva). */
@Serializable
data class ClockSyncResultsDto(
    val results: List<ClockSyncResultDto> = emptyList(),
)

/** Resultado de dar de alta una cuenta en un reloj concreto. */
@Serializable
data class ClockAltaResultDto(
    val serial: String = "",
    val employeeNumber: String = "",
    val name: String = "",
)

// ── Entradas/salidas por persona (reporte del reloj) ────────────────────────

/**
 * Body de `POST /time-clock/report` (mismo contrato que `/access/report`).
 * `period` y `date` son obligatorios: los usa el servidor para resolver el
 * rango en la zona efectiva. `period` es DAY, WEEK, FORTNIGHT o MONTH.
 */
@Serializable
data class TimeClockReportQuery(
    val page: Int = 1,
    val limit: Int = 20,
    val filters: TimeClockReportFilters? = null,
    val sort: TableSort? = null,
)

@Serializable
data class TimeClockReportFilters(
    val period: String,
    /** Día local de referencia `YYYY-MM-DD`. */
    val date: String,
    val tz: String? = null,
    val departmentId: String? = null,
    val employeeId: String? = null,
    val q: String? = null,
    val includeInactive: Boolean? = null,
)

/** Una fila por SESIÓN (entrada + salida) del periodo. */
@Serializable
data class TimeClockReportSessionRowDto(
    val id: String,
    val employeeId: String = "",
    val employeeName: String = "",
    val employeeNumber: String? = null,
    val jobTitle: String? = null,
    val departmentId: String? = null,
    val departmentName: String? = null,
    val active: Boolean = true,
    val date: String = "",
    val entryAt: String? = null,
    val exitAt: String? = null,
    val workedMinutes: Int = 0,
    /** ENTRY_WITHOUT_EXIT, EXIT_WITHOUT_ENTRY, OPEN_ENTRY o `null`. */
    val incident: String? = null,
    val crossesMidnight: Boolean = false,
    /** `false` = número del reloj sin vincular a una cuenta (solo en este reporte). */
    val linked: Boolean = true,
)

/** Rango resuelto por el servidor en la zona horaria efectiva. */
@Serializable
data class TimeClockReportRangeDto(
    val start: String = "",
    val end: String = "",
    val timezone: String = "",
    val period: String = "DAY",
)

/** Resumen del reporte (no depende de la página). */
@Serializable
data class TimeClockReportSummaryDto(
    val peopleTotal: Int = 0,
    val peopleWithRecords: Int = 0,
    val peopleWithoutRecords: Int = 0,
    val peopleInside: Int = 0,
    val totalWorkedMinutes: Int = 0,
    val totalIncidents: Int = 0,
    val range: TimeClockReportRangeDto = TimeClockReportRangeDto(),
)

/** Respuesta paginada de `POST /time-clock/report`. */
@Serializable
data class TimeClockReportResponseDto(
    val data: List<TimeClockReportSessionRowDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageIndex: Int = 0,
    val totalPages: Int = 0,
    val totalCount: Int = 0,
    val limit: Int = 20,
    val hasPreviousPage: Boolean = false,
    val hasNextPage: Boolean = false,
    val summary: TimeClockReportSummaryDto = TimeClockReportSummaryDto(),
)

/** Respuesta de `POST /time-clock/report/export`: todas las sesiones, sin paginar. */
@Serializable
data class TimeClockReportExportDto(
    val data: List<TimeClockReportSessionRowDto> = emptyList(),
    val total: Int = 0,
    val summary: TimeClockReportSummaryDto = TimeClockReportSummaryDto(),
)

// ── Resumen del tablero del reloj ───────────────────────────────────────────

@Serializable
data class ClockAlertDto(
    /** NO_RESPONSE, CREDENTIALS, SUSPICIOUS_ROSTER, PENDING_REMOVALS, REAPPEARED, UNLINKED o SYNC_ERROR. */
    val code: String = "",
    /** critical, warning o info. */
    val severity: String = "info",
    val count: Int? = null,
)

/** Hechos del padrón para el tablero. */
@Serializable
data class RosterFactsDto(
    val suspicious: Boolean = false,
    val pendingRemovals: Int = 0,
    val reappeared: Int = 0,
    val unlinked: Int = 0,
)

@Serializable
data class SummaryClockDeviceDto(
    val deviceName: String? = null,
    val model: String? = null,
    val firmwareVersion: String? = null,
)

@Serializable
data class SummaryClockCountsDto(
    val people: Int = 0,
    val cards: Int = 0,
)

@Serializable
data class SummaryHourPersonDto(
    val employeeNumber: String = "",
    val name: String = "",
)

@Serializable
data class SummaryHourlyCountDto(
    val hour: Int = 0,
    val count: Int = 0,
    val people: List<SummaryHourPersonDto> = emptyList(),
)

@Serializable
data class SummaryRecentPunchDto(
    val occurredAt: String = "",
    val employeeNumber: String = "",
    val name: String = "",
    val method: String = "OTHER",
)

@Serializable
data class SummaryClockDto(
    val serial: String = "",
    val name: String? = null,
    val countsAttendance: Boolean = false,
    /** ONLINE, NO_RESPONSE, PAUSED_CREDENTIALS o NOT_READ. */
    val connection: String = "NOT_READ",
    val lastReadAt: String? = null,
    val lastSyncAt: String? = null,
    val lastSyncOk: Boolean? = null,
    val device: SummaryClockDeviceDto? = null,
    val counts: SummaryClockCountsDto? = null,
    val punchesToday: Int = 0,
    val hourly: List<SummaryHourlyCountDto> = emptyList(),
    val recentPunches: List<SummaryRecentPunchDto> = emptyList(),
    val alerts: List<ClockAlertDto> = emptyList(),
)

/** Resumen del tablero del reloj (`GET /time-clock/summary`). Solo lectura. */
@Serializable
data class TimeClockSummaryDto(
    val generatedAt: String = "",
    val timezone: String = "",
    val roster: RosterFactsDto = RosterFactsDto(),
    val alerts: List<ClockAlertDto> = emptyList(),
    val clocks: List<SummaryClockDto> = emptyList(),
)

// ── Inspección del reloj (solo lectura del equipo) ──────────────────────────

@Serializable
data class TimeClockInspectClockDto(
    val serialNumber: String = "",
    val name: String? = null,
    val url: String = "",
)

@Serializable
data class TimeClockInspectDeviceDto(
    val serialNumber: String = "",
    val model: String? = null,
    val deviceName: String? = null,
    val firmwareVersion: String? = null,
    val macAddress: String? = null,
)

@Serializable
data class TimeClockInspectTimeDto(
    val localTime: String = "",
    val timeMode: String? = null,
    val timeZone: String? = null,
)

@Serializable
data class TimeClockInspectCountsDto(
    val userNumber: Int = 0,
    val bindFaceUserNumber: Int = 0,
    val bindFingerprintUserNumber: Int = 0,
    val bindCardUserNumber: Int = 0,
)

/** Lo que tiene el reloj: identidad, hora y conteos (`GET /clocks/:serial/inspect/overview`). */
@Serializable
data class TimeClockInspectOverviewDto(
    val clock: TimeClockInspectClockDto = TimeClockInspectClockDto(),
    val device: TimeClockInspectDeviceDto = TimeClockInspectDeviceDto(),
    val time: TimeClockInspectTimeDto = TimeClockInspectTimeDto(),
    val counts: TimeClockInspectCountsDto = TimeClockInspectCountsDto(),
)

@Serializable
data class TimeClockInspectPersonDto(
    val employeeNumber: String = "",
    val name: String = "",
)

@Serializable
data class TimeClockInspectUsersDto(
    val total: Int = 0,
    val shown: Int = 0,
    val people: List<TimeClockInspectPersonDto> = emptyList(),
)

@Serializable
data class TimeClockInspectCardDto(
    val cardNo: String = "",
    val employeeNumber: String = "",
    val cardType: String? = null,
)

@Serializable
data class TimeClockInspectCardsDto(
    val total: Int = 0,
    val shown: Int = 0,
    val cards: List<TimeClockInspectCardDto> = emptyList(),
)

@Serializable
data class TimeClockInspectEventDto(
    val serialNo: Int = 0,
    val time: String = "",
    val minor: Int = 0,
    val employeeNumber: String? = null,
    val name: String? = null,
)

@Serializable
data class TimeClockInspectEventsDto(
    val total: Int = 0,
    val shown: Int = 0,
    val events: List<TimeClockInspectEventDto> = emptyList(),
)

/** Cámara de la terminal (`GET /clocks/:serial/inspect/camera`). Solo lectura. */
@Serializable
data class TimeClockCameraDto(
    val inputs: List<TimeClockCameraInputDto> = emptyList(),
    val streams: List<TimeClockCameraStreamDto> = emptyList(),
    val playback: TimeClockCameraPlaybackDto? = null,
)

@Serializable
data class TimeClockCameraInputDto(
    val id: String = "",
    val name: String? = null,
    val enabled: Boolean = false,
)

@Serializable
data class TimeClockCameraStreamDto(
    val id: String = "",
    val name: String? = null,
    val enabled: Boolean = false,
    val transport: String? = null,
    val width: Int? = null,
    val height: Int? = null,
)

/**
 * Cómo se puede ver el stream: `mode = "http"` con `url` reproducible en el
 * dispositivo, o `mode = "none"` cuando es RTSP y hace falta un servidor de
 * medios (`reason` y `rtspPath` lo explican).
 */
@Serializable
data class TimeClockCameraPlaybackDto(
    val mode: String = "none",
    val url: String? = null,
    val reason: String? = null,
    val rtspPath: String? = null,
)

// ── Personas y tarjetas del reloj (escriben en el equipo) ───────────────────

@Serializable
data class TimeClockPersonDto(
    val employeeNumber: String = "",
    val name: String = "",
    /** Cuenta del sistema vinculada a este número; `null` si no está vinculado. */
    val userId: String? = null,
    val cards: List<String> = emptyList(),
)

@Serializable
data class TimeClockCardDto(
    val cardNo: String = "",
    val employeeNumber: String = "",
    val cardType: String? = null,
)

/** Personas y tarjetas que tiene el reloj (`GET /clocks/:serial/people`). */
@Serializable
data class TimeClockPeopleListDto(
    val people: List<TimeClockPersonDto> = emptyList(),
    val cards: List<TimeClockCardDto> = emptyList(),
)

/** Alta de una persona en el reloj (`POST /clocks/:serial/people`). Escribe en el equipo. */
@Serializable
data class TimeClockPersonInput(
    val employeeNumber: String,
    val name: String,
    /** male, female o unknown. */
    val gender: String = "unknown",
)

/** Cambio de nombre en el reloj (`PATCH /clocks/:serial/people/:employeeNo`). */
@Serializable
data class TimeClockPersonRenameInput(
    val name: String,
)

@Serializable
data class TimeClockCardInput(
    val cardNo: String,
)

/** Respuesta de las operaciones que confirman una tarjeta. */
@Serializable
data class CardNumberResultDto(
    val cardNo: String = "",
)

@Serializable
data class TimeClockPersonFaceCountDto(
    val count: Int = 0,
)

@Serializable
data class TimeClockPersonFingerprintDto(
    val fingerPrintID: Int = 0,
    val fingerType: String? = null,
)

/** Vínculo de la persona del reloj con una cuenta, con su estado. */
@Serializable
data class TimeClockPersonLinkDto(
    /** ACTIVO o BAJA_PENDIENTE. */
    val status: String = "ACTIVO",
    val userId: String = "",
    val userName: String = "",
    val userActive: Boolean = true,
)

/** Ficha de una persona del reloj (`GET /clocks/:serial/people/:employeeNo`). */
@Serializable
data class TimeClockPersonDetailDto(
    val employeeNumber: String = "",
    val name: String = "",
    val cards: List<String> = emptyList(),
    val faces: TimeClockPersonFaceCountDto = TimeClockPersonFaceCountDto(),
    /** Metadatos de huellas, nunca las plantillas. */
    val fingerprints: List<TimeClockPersonFingerprintDto> = emptyList(),
    val link: TimeClockPersonLinkDto? = null,
    val punchesToday: Int = 0,
)

/** Conteos de rostros y huellas por número de persona (`GET /clocks/:serial/people/biometrics`). */
@Serializable
data class TimeClockBiometricsDto(
    val updatedAt: String = "",
    val people: Map<String, TimeClockBiometricCountDto> = emptyMap(),
)

@Serializable
data class TimeClockBiometricCountDto(
    val faces: Int = 0,
    val fingerprints: Int = 0,
)

@Serializable
data class UnifiedClockReadDto(
    val serial: String = "",
    val name: String = "",
    val error: String? = null,
)

/** Persona de todos los relojes, fusionada por número (`GET /time-clock/people/unified`). */
@Serializable
data class UnifiedPersonDto(
    val employeeNumber: String = "",
    val name: String = "",
    /** Series de los relojes donde está dada de alta. */
    val presentIn: List<String> = emptyList(),
)

@Serializable
data class UnifiedPeopleDto(
    val clocks: List<UnifiedClockReadDto> = emptyList(),
    val people: List<UnifiedPersonDto> = emptyList(),
)

// ── Huellas (estación de enrolamiento) ─────────────────────────────────────

/** Estado de la estación de huellas (`GET /time-clock/fingerprint-station`). */
@Serializable
data class FingerprintStationInfoDto(
    val configured: Boolean = false,
    val url: String? = null,
)

/** Resultado de probar el lector (`POST /time-clock/fingerprint-station/test`). */
@Serializable
data class FingerprintStationTestDto(
    val fingerNo: Int = 1,
    val quality: Int = 0,
    val minQuality: Int = 0,
    val acceptable: Boolean = false,
)

@Serializable
data class FingerprintStationTestInput(
    val fingerNo: Int,
)

/** Huellas de una persona en un reloj (`GET /employees/:userId/fingerprint/status`). */
@Serializable
data class FingerprintStatusDto(
    val employeeNumber: String = "",
    /** Posiciones de dedo ya registradas en el reloj (1 a 10). */
    val fingerIds: List<Int> = emptyList(),
    val fingerprints: Int = 0,
    val max: Int = 10,
)

/** Huella capturada con el sensor; la plantilla nunca viaja al dispositivo. */
@Serializable
data class FingerprintCaptureDto(
    val captureId: String = "",
    val fingerNo: Int = 0,
    val quality: Int = 0,
    val minQuality: Int = 0,
    val acceptable: Boolean = false,
)

@Serializable
data class FingerprintCaptureInput(
    val serial: String,
    val fingerNo: Int,
)

@Serializable
data class FingerprintApplyInput(
    val serial: String,
    val captureId: String,
)

/** Resultado del alta de una huella ya capturada. */
@Serializable
data class FingerprintApplyResultDto(
    val applied: Boolean = false,
    val serial: String = "",
    val fingerNo: Int = 0,
    val fingerprints: Int = 0,
)

// ── Historial de sincronización de un reloj ────────────────────────────────

/** Intento de sincronización: un renglón del timeline. `trigger`: AUTO, MANUAL, IMPORT o REGISTER. */
@Serializable
data class TimeClockSyncEventDto(
    val id: String,
    val trigger: String = "AUTO",
    val ok: Boolean = false,
    val startedAt: String = "",
    val finishedAt: String = "",
    val readCount: Int = 0,
    val newCount: Int = 0,
    val lastSerialNo: Int? = null,
    val error: String? = null,
)

@Serializable
data class TimeClockSyncHistorySummaryDto(
    val attempts: Int = 0,
    val okCount: Int = 0,
    val failCount: Int = 0,
    /** Fallos consecutivos desde el más reciente (0 = el último conectó). */
    val failuresInARow: Int = 0,
    val lastOkAt: String? = null,
    val lastFailAt: String? = null,
    val lastError: String? = null,
)

/** Timeline de un reloj (`GET /time-clock/clocks/:serial/events`). */
@Serializable
data class TimeClockSyncHistoryDto(
    val clockSerial: String = "",
    val events: List<TimeClockSyncEventDto> = emptyList(),
    val summary: TimeClockSyncHistorySummaryDto = TimeClockSyncHistorySummaryDto(),
)

// ── Padrón del reloj (roster) ──────────────────────────────────────────────

/** Persona del reloj sin vínculo a una cuenta (nueva). */
@Serializable
data class RosterPersonDto(
    val employeeNumber: String = "",
    val name: String = "",
    val clocks: List<String> = emptyList(),
)

/** Persona vinculada con baja pendiente (espera motivo). */
@Serializable
data class RosterPendingRemovalDto(
    val employeeNumber: String = "",
    val userId: String = "",
    val userName: String = "",
    val missingSince: String? = null,
)

/** Cuenta ya dada de baja que reapareció en el reloj. */
@Serializable
data class RosterReappearedDto(
    val employeeNumber: String = "",
    val userId: String = "",
    val userName: String = "",
    val reappearedAt: String? = null,
)

@Serializable
data class RosterClockResultDto(
    val serial: String = "",
    val ok: Boolean = false,
    val count: Int = 0,
    val error: String? = null,
)

/** Una corrida del padrón (la guardada, `lastRun`). */
@Serializable
data class RosterRunDto(
    val id: String,
    val trigger: String = "AUTO",
    val startedAt: String = "",
    val finishedAt: String? = null,
    val ok: Boolean = false,
    val suspicious: Boolean = false,
    val added: Int = 0,
    val removalPending: Int = 0,
    val cancelled: Int = 0,
    val reappeared: Int = 0,
    val perClock: List<RosterClockResultDto> = emptyList(),
)

/** Resultado de `POST /time-clock/roster/run`. */
@Serializable
data class RosterRunResultDto(
    val ok: Boolean = false,
    val suspicious: Boolean = false,
    val added: Int = 0,
    val removalPending: Int = 0,
    val cancelled: Int = 0,
    val reappeared: Int = 0,
    val newCount: Int = 0,
    val perClock: List<RosterClockResultDto> = emptyList(),
)

/** Padrón del reloj (`GET /time-clock/roster`): nuevos, bajas pendientes y reapariciones. */
@Serializable
data class TimeClockRosterDto(
    val lastRun: RosterRunDto? = null,
    val unlinked: List<RosterPersonDto> = emptyList(),
    val pendingRemovals: List<RosterPendingRemovalDto> = emptyList(),
    val reappeared: List<RosterReappearedDto> = emptyList(),
)

/** Motivo de baja del catálogo `removal_categories`. */
@Serializable
data class RemovalCategoryDto(
    val id: String,
    val key: String = "",
    val name: String = "",
    val active: Boolean = true,
)

@Serializable
data class RosterRemovalInput(
    val categoryId: String,
    val reason: String,
    val notifyUser: Boolean = false,
)

@Serializable
data class RemovalCategoryCreateInput(
    val key: String,
    val name: String,
)

@Serializable
data class RemovalCategoryUpdateInput(
    val name: String? = null,
    val active: Boolean? = null,
)

/** Body de `POST /employees/:userId/clock-alta`: en qué reloj se da de alta. */
@Serializable
data class ClockAltaInput(
    val serial: String,
)
