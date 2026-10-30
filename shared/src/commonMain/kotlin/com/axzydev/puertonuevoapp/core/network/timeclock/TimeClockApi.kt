package com.axzydev.puertonuevoapp.core.network.timeclock

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import io.ktor.http.encodeURLPathPart

/**
 * Reloj checador (Hikvision) y su padrón.
 *
 * Espejo de `web/src/entities/time-clock/api/timeClockApi.ts`. Permisos del
 * API: `time_clock.view` (checadas y reportes), `time_clock.sync`
 * (importar/sincronizar/padrón), `time_clock.link` (vincular números),
 * `time_clocks.manage` (alta/baja de relojes) y `time_clock.write`
 * (altas/bajas/huellas EN el reloj).
 *
 * Los GET de inspección leen el equipo; los métodos marcados como ESCRITURA
 * modifican el reloj y solo funcionan si su `writeEnabled` está encendido
 * (`TIME_CLOCK_READ_ONLY` si no).
 */
class TimeClockApi(private val client: ApiClient) {

    // ── Checadas, estado y sincronización ──────────────────────────────────

    /** Tabla server-side de checadas (`time_clock.view`); filtros `q`, `employeeNumber`, `method`, `from`, `to`, `tz`. */
    suspend fun punches(request: TableRequest): TableResponse<TimeClockPunchDto> =
        client.post("/time-clock/query", request)

    suspend fun status(): TimeClockStatusDto =
        client.get("/time-clock/status")

    /** Tablero del reloj: conexión, checadas de hoy por hora y alertas. */
    suspend fun summary(): TimeClockSummaryDto =
        client.get("/time-clock/summary")

    /**
     * Importa del reloj las checadas de un rango de días (solo lee el equipo).
     * Responde de inmediato (202) y el avance sale en [status] (`importJob`).
     */
    suspend fun startImport(input: TimeClockImportInput): TimeClockImportDto =
        client.post("/time-clock/import", input)

    /** Drena del reloj todo lo pendiente desde su cursor (202; avance en [status]). */
    suspend fun sync(): TimeClockProgressDto =
        client.postNoBody("/time-clock/sync")

    // ── Entradas/salidas por persona ───────────────────────────────────────

    /** Reporte paginado de entradas/salidas (mismo contrato que `/access/report`). */
    suspend fun report(query: TimeClockReportQuery): TimeClockReportResponseDto =
        client.post("/time-clock/report", query)

    /** Misma cuenta con TODAS las sesiones del periodo (sin paginar). */
    suspend fun reportExport(query: TimeClockReportQuery): TimeClockReportExportDto =
        client.post("/time-clock/report/export", query)

    // ── Padrón: números del reloj ↔ cuentas ────────────────────────────────

    /** Empleados vistos en el reloj con su vínculo o sugerencia; filtros `q` y `estado` (`LINKED`/`UNLINKED`/`SUGGESTED`). */
    suspend fun employees(request: TableRequest): TimeClockEmployeesResponseDto =
        client.post("/time-clock/employees/query", request)

    suspend fun linkEmployee(employeeNumber: String, userId: String): TimeClockEmployeeDto =
        client.put(
            "/time-clock/employees/${employeeNumber.encodeURLPathPart()}",
            TimeClockLinkInput(userId),
        )

    suspend fun unlinkEmployee(employeeNumber: String): EmployeeNumberResultDto =
        client.delete("/time-clock/employees/${employeeNumber.encodeURLPathPart()}")

    /** Vincula de un jalón las sugerencias HIGH (nombre y número coinciden). */
    suspend fun linkSuggested(): TimeClockLinkSuggestedResultDto =
        client.postNoBody("/time-clock/employees/link-suggested")

    /** Estado del alta de la persona en cada reloj registrado. */
    suspend fun employeeClockStatus(userId: String): EmployeeClockStatusDto =
        client.get("/time-clock/employees/${userId.encodeURLPathPart()}/clock-status")

    // ── Relojes (registro del sistema; del equipo solo se LEE) ─────────────

    /** Da de alta un reloj: se conecta, lee su identidad y arranca su sincronización. */
    suspend fun registerClock(input: TimeClockRegisterInput): TimeClockDeviceDto =
        client.post("/time-clock/clocks", input)

    /** Cambia nombre / si cuenta para asistencia / si acepta escritura. No toca el equipo. */
    suspend fun updateClock(serial: String, input: TimeClockUpdateInput): TimeClockDeviceDto =
        client.patch("/time-clock/clocks/${serial.encodeURLPathPart()}", input)

    /** Deja de sincronizar el reloj; sus checadas se conservan. */
    suspend fun retireClock(serial: String): TimeClockRetiredDto =
        client.delete("/time-clock/clocks/${serial.encodeURLPathPart()}")

    /** Configuración leída en vivo (identidad, hora y personas). Solo lectura. */
    suspend fun clockSettings(serial: String): TimeClockConfigDto =
        client.get("/time-clock/clocks/${serial.encodeURLPathPart()}/settings")

    /** Timeline de sincronización del reloj (últimos intentos + resumen). */
    suspend fun clockEvents(serial: String, limit: Int = 50): TimeClockSyncHistoryDto =
        client.get("/time-clock/clocks/${serial.encodeURLPathPart()}/events?limit=$limit")

    // ── Inspección del equipo (solo lectura) ───────────────────────────────

    suspend fun inspectOverview(serial: String): TimeClockInspectOverviewDto =
        client.get("/time-clock/clocks/${serial.encodeURLPathPart()}/inspect/overview")

    suspend fun inspectUsers(serial: String): TimeClockInspectUsersDto =
        client.get("/time-clock/clocks/${serial.encodeURLPathPart()}/inspect/users")

    suspend fun inspectCards(serial: String): TimeClockInspectCardsDto =
        client.get("/time-clock/clocks/${serial.encodeURLPathPart()}/inspect/cards")

    /** Eventos leídos del equipo (checadas y de puerta) de los últimos [days] días (1–30). */
    suspend fun inspectEvents(serial: String, days: Int = 2): TimeClockInspectEventsDto =
        client.get("/time-clock/clocks/${serial.encodeURLPathPart()}/inspect/events?days=$days")

    suspend fun inspectCamera(serial: String): TimeClockCameraDto =
        client.get("/time-clock/clocks/${serial.encodeURLPathPart()}/inspect/camera")

    // ── Personas y tarjetas del reloj (ESCRITURA en el equipo) ─────────────

    /** Personas de todos los relojes, fusionadas por número. */
    suspend fun peopleUnified(): UnifiedPeopleDto =
        client.get("/time-clock/people/unified")

    suspend fun people(serial: String): TimeClockPeopleListDto =
        client.get("/time-clock/clocks/${serial.encodeURLPathPart()}/people")

    suspend fun personDetail(serial: String, employeeNumber: String): TimeClockPersonDetailDto =
        client.get(
            "/time-clock/clocks/${serial.encodeURLPathPart()}/people/${employeeNumber.encodeURLPathPart()}"
        )

    /** Foto del rostro de una persona del reloj (JPEG). */
    suspend fun personPhoto(serial: String, employeeNumber: String): ByteArray =
        client.getBytes(
            "/time-clock/clocks/${serial.encodeURLPathPart()}/people/${employeeNumber.encodeURLPathPart()}/photo"
        )

    /** Conteos de rostros/huellas por persona (solo lectura del equipo, con caché). */
    suspend fun biometrics(serial: String, refresh: Boolean = false): TimeClockBiometricsDto =
        client.get(
            "/time-clock/clocks/${serial.encodeURLPathPart()}/people/biometrics" +
                (if (refresh) "?refresh=true" else "")
        )

    /** ESCRITURA: da de alta una persona en el reloj. */
    suspend fun createPerson(serial: String, input: TimeClockPersonInput): EmployeeNumberResultDto =
        client.post("/time-clock/clocks/${serial.encodeURLPathPart()}/people", input)

    /** ESCRITURA: cambia el nombre de la persona en el reloj. */
    suspend fun renamePerson(
        serial: String,
        employeeNumber: String,
        name: String,
    ): EmployeeNumberResultDto =
        client.patch(
            "/time-clock/clocks/${serial.encodeURLPathPart()}/people/${employeeNumber.encodeURLPathPart()}",
            TimeClockPersonRenameInput(name),
        )

    /** ESCRITURA: da de baja a la persona del reloj (quita antes sus tarjetas). */
    suspend fun removePerson(serial: String, employeeNumber: String): EmployeeNumberResultDto =
        client.delete(
            "/time-clock/clocks/${serial.encodeURLPathPart()}/people/${employeeNumber.encodeURLPathPart()}"
        )

    /** ESCRITURA: registra una tarjeta a la persona. */
    suspend fun addCard(
        serial: String,
        employeeNumber: String,
        cardNo: String,
    ): CardNumberResultDto =
        client.post(
            "/time-clock/clocks/${serial.encodeURLPathPart()}/people/${employeeNumber.encodeURLPathPart()}/cards",
            TimeClockCardInput(cardNo),
        )

    /** ESCRITURA: quita una tarjeta del reloj. */
    suspend fun removeCard(serial: String, cardNo: String): CardNumberResultDto =
        client.delete(
            "/time-clock/clocks/${serial.encodeURLPathPart()}/cards/${cardNo.encodeURLPathPart()}"
        )

    /** ESCRITURA: da de alta a la persona del sistema en UN reloj. */
    suspend fun clockAlta(userId: String, serial: String): ClockAltaResultDto =
        client.post(
            "/time-clock/employees/${userId.encodeURLPathPart()}/clock-alta",
            ClockAltaInput(serial),
        )

    /** ESCRITURA: da de alta a la persona en todos los relojes que lo acepten. */
    suspend fun clockAltaAll(userId: String): ClockSyncResultsDto =
        client.postNoBody("/time-clock/employees/${userId.encodeURLPathPart()}/clock-alta-all")

    /** ESCRITURA: da de baja a la persona de todos los relojes. */
    suspend fun clockBajaAll(userId: String): ClockSyncResultsDto =
        client.postNoBody("/time-clock/employees/${userId.encodeURLPathPart()}/clock-baja-all")

    // ── Huellas ────────────────────────────────────────────────────────────

    suspend fun fingerprintStation(): FingerprintStationInfoDto =
        client.get("/time-clock/fingerprint-station")

    suspend fun fingerprintStationTest(fingerNo: Int): FingerprintStationTestDto =
        client.post("/time-clock/fingerprint-station/test", FingerprintStationTestInput(fingerNo))

    suspend fun fingerprintStatus(userId: String, serial: String): FingerprintStatusDto =
        client.get(
            "/time-clock/employees/${userId.encodeURLPathPart()}/fingerprint/status" +
                "?serial=${serial.encodeURLPathPart()}"
        )

    /** ESCRITURA: captura una huella con el sensor del reloj (no guarda nada aún). */
    suspend fun fingerprintCapture(
        userId: String,
        serial: String,
        fingerNo: Int,
    ): FingerprintCaptureDto =
        client.post(
            "/time-clock/employees/${userId.encodeURLPathPart()}/fingerprint/capture",
            FingerprintCaptureInput(serial = serial, fingerNo = fingerNo),
        )

    /** ESCRITURA: aplica al reloj una huella ya capturada. */
    suspend fun fingerprintApply(
        userId: String,
        serial: String,
        captureId: String,
    ): FingerprintApplyResultDto =
        client.post(
            "/time-clock/employees/${userId.encodeURLPathPart()}/fingerprint/apply",
            FingerprintApplyInput(serial = serial, captureId = captureId),
        )

    // ── Padrón del reloj (roster) ──────────────────────────────────────────

    /** Nuevos en el reloj, bajas pendientes y reapariciones (solo lee la BD). */
    suspend fun roster(): TimeClockRosterDto =
        client.get("/time-clock/roster")

    /** Revisa el padrón contra los relojes que cuentan (solo LEE de los equipos). */
    suspend fun runRoster(): RosterRunResultDto =
        client.postNoBody("/time-clock/roster/run")

    /** Aplica la baja pendiente con su motivo: da de baja a la cuenta. */
    suspend fun applyRemoval(employeeNumber: String, input: RosterRemovalInput): EmployeeNumberResultDto =
        client.post(
            "/time-clock/roster/employees/${employeeNumber.encodeURLPathPart()}/removal",
            input,
        )

    /** Descarta la baja pendiente sin motivo. */
    suspend fun dismissRemoval(employeeNumber: String): EmployeeNumberResultDto =
        client.postNoBody(
            "/time-clock/roster/employees/${employeeNumber.encodeURLPathPart()}/dismiss"
        )

    suspend fun removalCategories(includeInactive: Boolean = false): List<RemovalCategoryDto> =
        client.get(
            "/time-clock/removal-categories" +
                (if (includeInactive) "?includeInactive=true" else "")
        )

    suspend fun createRemovalCategory(input: RemovalCategoryCreateInput): RemovalCategoryDto =
        client.post("/time-clock/removal-categories", input)

    suspend fun updateRemovalCategory(
        id: String,
        input: RemovalCategoryUpdateInput,
    ): RemovalCategoryDto =
        client.patch("/time-clock/removal-categories/${id.encodeURLPathPart()}", input)
}
