package com.axzydev.puertonuevoapp.core.network.schedules

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import io.ktor.http.encodeURLPathPart

/**
 * Horarios, asignaciones masivas, entradas/salidas por persona con su horario,
 * tiempo extra y prenómina.
 *
 * Espejo de `web/src/entities/schedule/api/scheduleApi.ts` y
 * `web/src/entities/overtime/api/overtimeApi.ts`. El cálculo lo hace el API:
 * la app solo manda el periodo y la dimensión de persona.
 *
 * Permisos: `schedules.view` (listados y asignaciones), `schedules.manage`
 * (CRUD y asignar/quitar), `payroll.view` (reporte semanal y export de horas
 * extra), `overtime.view` (consulta de tiempo extra), `overtime.approve`
 * (aprobar/rechazar) y `time_clock.view` / `access.log` para la fuente de
 * entradas/salidas.
 */
class SchedulesApi(private val client: ApiClient) {

    // ── Horarios ───────────────────────────────────────────────────────────

    suspend fun schedules(includeInactive: Boolean = false): List<ScheduleDto> =
        client.get(
            "/schedules" + (if (includeInactive) "?includeInactive=true" else "")
        )

    suspend fun createSchedule(input: ScheduleCreateInput): ScheduleDto =
        client.post("/schedules", input)

    suspend fun updateSchedule(id: String, input: ScheduleUpdateInput): ScheduleDto =
        client.patch("/schedules/${id.encodeURLPathPart()}", input)

    /** Baja del horario: lógica si tenía gente o estaba activo, física si ya estaba inactivo. */
    suspend fun removeSchedule(id: String): ScheduleDeleteResultDto =
        client.delete("/schedules/${id.encodeURLPathPart()}")

    // ── Asignaciones ───────────────────────────────────────────────────────

    /** Asigna un horario a N personas desde una fecha, cerrando la vigencia anterior. */
    suspend fun assign(input: ScheduleAssignmentInput): ScheduleAssignResultDto =
        client.post("/schedules/assignments", input)

    /** Cierra la vigencia de varias personas en un horario. */
    suspend fun removeAssignments(
        input: ScheduleRemoveAssignmentsInput,
    ): ScheduleRemoveAssignmentsResultDto =
        client.post("/schedules/assignments/remove", input)

    /** Tabla de asignaciones vigentes; filtros `scheduleId`, `departmentId`, `q`. */
    suspend fun assignments(request: TableRequest): TableResponse<ScheduleAssignmentRowDto> =
        client.post("/schedules/assignments/query", request)

    /** Personas con asignación vigente de un horario. */
    suspend fun assignees(scheduleId: String): List<ScheduleAssigneeDto> =
        client.get("/schedules/${scheduleId.encodeURLPathPart()}/assignees")

    // ── Asistencia y prenómina ─────────────────────────────────────────────

    /** Reporte semanal por departamento (RH / prenómina): días, extra y aprobaciones. */
    suspend fun weeklyAttendance(query: WeeklyAttendanceQuery): WeeklyAttendanceReportDto =
        client.post("/schedules/weekly-attendance", query)

    /** Entradas/salidas por persona a partir de las checadas del RELOJ. */
    suspend fun peopleAttendanceFromClock(
        query: PeopleAttendanceQuery,
    ): PeopleAttendanceResponseDto =
        client.post("/schedules/attendance/time-clock", query)

    /** Entradas/salidas por persona a partir de la bitácora de ACCESO (portería). */
    suspend fun peopleAttendanceFromAccess(
        query: PeopleAttendanceQuery,
    ): PeopleAttendanceResponseDto =
        client.post("/schedules/attendance/access", query)

    // ── Tiempo extra ───────────────────────────────────────────────────────

    /**
     * Tabla de días de tiempo extra + resumen. RH solo recibe lo aprobado,
     * sin importar el `status` pedido.
     */
    suspend fun overtime(query: OvertimeQuery): OvertimeQueryResponseDto =
        client.post("/overtime/query", query)

    /** Aprueba, rechaza o revierte a PENDIENTE días (persona + fecha). */
    suspend fun decideOvertime(input: OvertimeApprovalInput): OvertimeApprovalResultDto =
        client.post("/overtime/approvals", input)

    /**
     * Universo completo sin paginar, solo lo aprobado: es la base de los
     * totales y del export de la pantalla de tiempo extra.
     */
    suspend fun overtimeExport(query: OvertimeQuery): OvertimeExportResponseDto =
        client.post("/schedules/overtime/export", query)
}
