package com.axzydev.puertonuevoapp.core.network.schedules

import com.axzydev.puertonuevoapp.core.network.http.TableSort
import kotlinx.serialization.Serializable

/**
 * DTOs de horarios, asignaciones masivas, tiempo extra y prenómina.
 *
 * Contrato autoritativo: `api/src/modules/schedules/models/dto/schedule.dto.ts`,
 * `api/src/modules/schedules/models/entity/schedule.entity.ts` y los modelos
 * `dto`/`entity` del módulo `overtime`. Espejo de
 * `web/src/entities/schedule/model/types.ts` y `web/src/entities/overtime/model/types.ts`.
 */

// ── Horarios (catálogo) ─────────────────────────────────────────────────────

/** Un día del horario. Las horas son "de pared" `HH:mm` o `null` si es descanso. */
@Serializable
data class ScheduleDayDto(
    /** 1 = lunes … 7 = domingo. */
    val weekday: Int = 1,
    val startTime: String? = null,
    val endTime: String? = null,
    /** Segundo tramo (jornada partida). */
    val splitStartTime: String? = null,
    val splitEndTime: String? = null,
    val restDay: Boolean = false,
)

/** Horario con sus siete días y cuántas personas tiene asignadas (`assigned`). */
@Serializable
data class ScheduleDto(
    val id: String,
    val name: String = "",
    val active: Boolean = true,
    val entryToleranceMin: Int = 0,
    val exitToleranceMin: Int = 0,
    val mealBreakMin: Int = 0,
    val minOvertimeMin: Int = 0,
    val crossesMidnight: Boolean = false,
    val days: List<ScheduleDayDto> = emptyList(),
    /** Asignaciones vigentes (`validTo = null`); lo calcula el API en el listado. */
    val assigned: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/** Alta de horario (`POST /schedules`): nombre + al menos un día. */
@Serializable
data class ScheduleCreateInput(
    val name: String,
    val entryToleranceMin: Int? = null,
    val exitToleranceMin: Int? = null,
    val mealBreakMin: Int? = null,
    val minOvertimeMin: Int? = null,
    val crossesMidnight: Boolean? = null,
    val days: List<ScheduleDayDto>,
)

/** Cambio parcial de horario (`PATCH /schedules/:id`); `days` reemplaza los días. */
@Serializable
data class ScheduleUpdateInput(
    val name: String? = null,
    val entryToleranceMin: Int? = null,
    val exitToleranceMin: Int? = null,
    val mealBreakMin: Int? = null,
    val minOvertimeMin: Int? = null,
    val crossesMidnight: Boolean? = null,
    val active: Boolean? = null,
    val days: List<ScheduleDayDto>? = null,
)

/** DELETE de horario: baja lógica si estaba activo o tenía gente, física si no. */
@Serializable
data class ScheduleDeleteResultDto(
    val soft: Boolean = false,
    val data: ScheduleDto,
)

// ── Asignación de horarios ──────────────────────────────────────────────────

/** Asignación masiva: un horario → N personas desde una fecha (cierra la vigencia previa). */
@Serializable
data class ScheduleAssignmentInput(
    val scheduleId: String,
    val userIds: List<String>,
    /** Primer día de vigencia `YYYY-MM-DD`. */
    val from: String,
)

@Serializable
data class ScheduleAssignResultDto(
    val assigned: Int = 0,
    val schedule: String = "",
    val from: String = "",
)

/** Quita la vigencia de varias personas en un horario (`POST /schedules/assignments/remove`). */
@Serializable
data class ScheduleRemoveAssignmentsInput(
    val scheduleId: String,
    val userIds: List<String>,
)

@Serializable
data class ScheduleRemoveAssignmentsResultDto(
    val removed: Int = 0,
)

/** Una fila de la tabla de asignaciones vigentes (`POST /schedules/assignments/query`). */
@Serializable
data class ScheduleAssignmentRowDto(
    val id: String,
    val userId: String = "",
    val employeeName: String = "",
    val employeeNumber: String? = null,
    val departmentId: String? = null,
    val departmentName: String? = null,
    val scheduleId: String = "",
    val scheduleName: String = "",
    /** Inicio de vigencia (ISO). */
    val from: String = "",
)

/** Persona con asignación vigente de un horario (`GET /schedules/:id/assignees`). */
@Serializable
data class ScheduleAssigneeDto(
    val userId: String = "",
    val employeeName: String = "",
    val employeeNumber: String? = null,
)

// ── Reporte semanal de asistencia (RH / prenómina) ─────────────────────────

/** `POST /schedules/weekly-attendance`: cualquier día de la semana y filtros opcionales. */
@Serializable
data class WeeklyAttendanceQuery(
    /** Día local `YYYY-MM-DD` que cae en la semana del reporte. */
    val date: String,
    val departmentId: String? = null,
    val q: String? = null,
)

@Serializable
data class WeeklyAttendanceRangeDto(
    val start: String = "",
    val end: String = "",
    val timezone: String = "",
    val days: List<String> = emptyList(),
)

/** Una sesión (entrada + salida) del día; `incident` es la anomalía de pareo si la hay. */
@Serializable
data class WeeklyAttendanceSessionDto(
    val entryAt: String? = null,
    val exitAt: String? = null,
    val workedMinutes: Int = 0,
    val incident: String? = null,
)

/**
 * Un día de la persona: `status` es WORKED, OVERTIME, ABSENCE, INCOMPLETE, REST,
 * REST_WORKED, NO_INFO o FUTURE; `approval` es APPROVED, REJECTED, PENDING o `null`.
 */
@Serializable
data class WeeklyAttendanceDayDto(
    val date: String = "",
    val status: String = "NO_INFO",
    val entryAt: String? = null,
    val exitAt: String? = null,
    val sessions: List<WeeklyAttendanceSessionDto> = emptyList(),
    val workedMin: Int = 0,
    val scheduledMin: Int = 0,
    val extraMin: Int = 0,
    val missingMin: Int = 0,
    val lateMin: Int = 0,
    val shift: String? = null,
    val scheduledStartAt: String? = null,
    val approval: String? = null,
    val approvedExtraMin: Int = 0,
)

/** Totales de minutos de una persona (o del reporte, con los conteos de gente). */
@Serializable
data class WeeklyAttendanceTotalsDto(
    val workedMin: Int = 0,
    val scheduledMin: Int = 0,
    val extraMin: Int = 0,
    val approvedExtraMin: Int = 0,
    val pendingExtraMin: Int = 0,
    val rejectedExtraMin: Int = 0,
    val missingMin: Int = 0,
    val absences: Int = 0,
    val incompleteDays: Int = 0,
)

@Serializable
data class WeeklyAttendanceRowDto(
    val userId: String = "",
    val employeeNumber: String? = null,
    /** Números que la persona tiene en los relojes. */
    val clockNumbers: List<String> = emptyList(),
    val name: String = "",
    val jobTitle: String? = null,
    val departmentId: String? = null,
    val departmentName: String? = null,
    val active: Boolean = true,
    /** `false` si su número del reloj no está vinculado a una cuenta. */
    val linked: Boolean = true,
    val scheduleName: String? = null,
    val withoutSchedule: Boolean = false,
    val days: List<WeeklyAttendanceDayDto> = emptyList(),
    val totals: WeeklyAttendanceTotalsDto = WeeklyAttendanceTotalsDto(),
)

@Serializable
data class WeeklyAttendanceSummaryDto(
    val workedMin: Int = 0,
    val scheduledMin: Int = 0,
    val extraMin: Int = 0,
    val approvedExtraMin: Int = 0,
    val pendingExtraMin: Int = 0,
    val rejectedExtraMin: Int = 0,
    val missingMin: Int = 0,
    val absences: Int = 0,
    val incompleteDays: Int = 0,
    val people: Int = 0,
    val unlinked: Int = 0,
    val withoutSchedule: Int = 0,
)

/** Reporte semanal de asistencia (`POST /schedules/weekly-attendance`). */
@Serializable
data class WeeklyAttendanceReportDto(
    val range: WeeklyAttendanceRangeDto = WeeklyAttendanceRangeDto(),
    val rows: List<WeeklyAttendanceRowDto> = emptyList(),
    val summary: WeeklyAttendanceSummaryDto = WeeklyAttendanceSummaryDto(),
)

// ── Entradas y salidas por persona (contra su horario) ─────────────────────

/**
 * Body de `POST /schedules/attendance/{time-clock,access}`: los filtros del
 * reporte de entradas/salidas + la vista rápida (`ALL`, `INCIDENTS`,
 * `ON_SITE`, `WITHOUT_RECORDS`). `period`: DAY, WEEK, FORTNIGHT o MONTH.
 */
@Serializable
data class PeopleAttendanceQuery(
    val page: Int = 1,
    val limit: Int = 20,
    val filters: PeopleAttendanceFilters? = null,
    val sort: TableSort? = null,
)

@Serializable
data class PeopleAttendanceFilters(
    val period: String,
    /** Día local de referencia `YYYY-MM-DD`. */
    val date: String,
    val departmentId: String? = null,
    val q: String? = null,
    val includeInactive: Boolean? = null,
    val view: String? = null,
)

/** Un día de la persona contra su horario: `status` es ATTENDED, LATE, ABSENCE, REST, PENDING o NO_INFO. */
@Serializable
data class PeopleAttendanceDayDto(
    val date: String = "",
    val status: String = "NO_INFO",
    val entryAt: String? = null,
    val exitAt: String? = null,
    val workedMinutes: Int = 0,
    val lateMinutes: Int = 0,
    val onSite: Boolean = false,
    val incident: String? = null,
)

@Serializable
data class PeopleAttendanceRowDto(
    val employeeId: String = "",
    val employeeName: String = "",
    val employeeNumber: String? = null,
    val jobTitle: String? = null,
    val departmentId: String? = null,
    val departmentName: String? = null,
    val active: Boolean = true,
    val linked: Boolean = true,
    /** Un elemento por día del periodo (`summary.range.days`). */
    val days: List<PeopleAttendanceDayDto> = emptyList(),
    val workedMinutes: Int = 0,
    val onSite: Boolean = false,
    val hasRecords: Boolean = false,
    val lateDays: Int = 0,
    val absences: Int = 0,
    val withoutExit: Int = 0,
    val withoutEntry: Int = 0,
)

/** Rango del reporte por persona (incluye la lista de días y el día de hoy). */
@Serializable
data class PeopleAttendanceRangeDto(
    val start: String = "",
    val end: String = "",
    val timezone: String = "",
    val period: String = "DAY",
    val days: List<String> = emptyList(),
    val today: String = "",
)

@Serializable
data class PeopleAttendanceSummaryDto(
    val people: Int = 0,
    val withRecords: Int = 0,
    val withoutRecords: Int = 0,
    val onSite: Int = 0,
    val workedMinutes: Int = 0,
    val lateDays: Int = 0,
    val absences: Int = 0,
    val withoutExit: Int = 0,
    val withoutEntry: Int = 0,
    val range: PeopleAttendanceRangeDto = PeopleAttendanceRangeDto(),
)

/** Respuesta de `POST /schedules/attendance/{time-clock,access}`. */
@Serializable
data class PeopleAttendanceResponseDto(
    val data: List<PeopleAttendanceRowDto> = emptyList(),
    val total: Int = 0,
    val summary: PeopleAttendanceSummaryDto = PeopleAttendanceSummaryDto(),
)

// ── Tiempo extra: aprobación por persona + día ─────────────────────────────

/**
 * Body de `POST /overtime/query` (tabla de días con su aprobación).
 * `period` y `date` son obligatorios; `status` es PENDING, APPROVED, REJECTED
 * o ALL (sin filtro de estatus). RH solo recibe APPROVED.
 */
@Serializable
data class OvertimeQuery(
    val page: Int = 1,
    val limit: Int = 20,
    val filters: OvertimeFilters? = null,
    val sort: TableSort? = null,
)

@Serializable
data class OvertimeFilters(
    val period: String,
    /** Día local de referencia `YYYY-MM-DD`. */
    val date: String,
    val tz: String? = null,
    val departmentId: String? = null,
    val q: String? = null,
    val includeInactive: Boolean? = null,
    val status: String? = null,
)

/** Un día de tiempo extra (persona + día) con su decisión. `status`: PENDING, APPROVED o REJECTED. */
@Serializable
data class OvertimeDayRowDto(
    val userId: String = "",
    val employeeName: String = "",
    val employeeNumber: String? = null,
    val departmentId: String? = null,
    val departmentName: String? = null,
    val active: Boolean = true,
    /** Día local `YYYY-MM-DD`. */
    val date: String = "",
    val extraMin: Int = 0,
    val workedMin: Int = 0,
    val scheduledMin: Int = 0,
    val scheduleName: String? = null,
    val restDay: Boolean = false,
    val withoutSchedule: Boolean = false,
    /** Turno programado del día ("14:00-22:00"); `null` en descanso o sin horario. */
    val shift: String? = null,
    /** Última salida del día según las sesiones del reloj. */
    val exitAt: String? = null,
    val status: String = "PENDING",
    /** Minutos contabilizados: el snapshot aprobado (0 si no está aprobado). */
    val approvedExtraMin: Int = 0,
    val note: String? = null,
    val decidedById: String? = null,
    val decidedByName: String? = null,
    val decidedAt: String? = null,
)

@Serializable
data class OvertimeRangeDto(
    val start: String = "",
    val end: String = "",
    val timezone: String = "",
    val period: String = "DAY",
)

@Serializable
data class OvertimeDepartmentMinutesDto(
    val departmentId: String? = null,
    val departmentName: String? = null,
    val minutes: Int = 0,
)

/** Resumen del filtro (no depende de la página ni de la pestaña de estatus). */
@Serializable
data class OvertimeSummaryDto(
    val totalDays: Int = 0,
    val pendingDays: Int = 0,
    val approvedDays: Int = 0,
    val rejectedDays: Int = 0,
    val pendingMinutes: Int = 0,
    val approvedMinutes: Int = 0,
    val rejectedMinutes: Int = 0,
    val peopleWithPending: Int = 0,
    val byDepartment: List<OvertimeDepartmentMinutesDto> = emptyList(),
    val range: OvertimeRangeDto = OvertimeRangeDto(),
)

/** Respuesta de `POST /overtime/query`. */
@Serializable
data class OvertimeQueryResponseDto(
    val data: List<OvertimeDayRowDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageIndex: Int = 0,
    val totalPages: Int = 0,
    val totalCount: Int = 0,
    val limit: Int = 20,
    val hasPreviousPage: Boolean = false,
    val hasNextPage: Boolean = false,
    val summary: OvertimeSummaryDto = OvertimeSummaryDto(),
)

/** Un día que se aprueba/rechaza/revierte (`persona + fecha`). */
@Serializable
data class OvertimeDecisionItem(
    val userId: String,
    val date: String,
)

/** Body de `POST /overtime/approvals`. `status`: APPROVED, REJECTED o PENDING (revertir). */
@Serializable
data class OvertimeApprovalInput(
    /** Si se manda, el API recalcula los días de ese filtro para validarlos. */
    val filters: OvertimeFilters? = null,
    val items: List<OvertimeDecisionItem>,
    val status: String,
    val note: String? = null,
)

@Serializable
data class OvertimeApprovalResultDto(
    val updated: Int = 0,
    val skipped: Int = 0,
)

// ── Tiempo extra: export (por persona, solo aprobado) ──────────────────────

/**
 * Una fila por persona del export de tiempo extra (`POST /schedules/overtime/export`).
 * No es la fila de `OvertimeDayRowDto`: el export agrega el periodo completo.
 */
@Serializable
data class OvertimePersonRowDto(
    val userId: String = "",
    val employeeName: String = "",
    val employeeNumber: String? = null,
    val departmentId: String? = null,
    val departmentName: String? = null,
    val active: Boolean = true,
    val scheduleName: String? = null,
    val scheduledMin: Int = 0,
    val workedMin: Int = 0,
    val extraMin: Int = 0,
    val missingMin: Int = 0,
    val daysWithExtra: Int = 0,
    val withoutSchedule: Boolean = false,
    val approvedMin: Int = 0,
    val pendingMin: Int = 0,
    val rejectedMin: Int = 0,
    val approvedDays: Int = 0,
    val pendingDays: Int = 0,
    val rejectedDays: Int = 0,
)

@Serializable
data class OvertimePersonSummaryDto(
    val peopleTotal: Int = 0,
    val peopleWithExtra: Int = 0,
    val totalExtraMinutes: Int = 0,
    val totalWorkedMinutes: Int = 0,
    val totalScheduledMinutes: Int = 0,
    val totalApprovedMinutes: Int = 0,
    val totalPendingMinutes: Int = 0,
    val totalRejectedMinutes: Int = 0,
    val range: OvertimeRangeDto = OvertimeRangeDto(),
)

/** Respuesta de `POST /schedules/overtime/export`: universo sin paginar, solo aprobado. */
@Serializable
data class OvertimeExportResponseDto(
    val data: List<OvertimePersonRowDto> = emptyList(),
    val total: Int = 0,
    val summary: OvertimePersonSummaryDto = OvertimePersonSummaryDto(),
)
