package com.axzydev.puertonuevoapp.core.network.dashboard

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Resumen del panel administrativo (`GET /dashboard/summary`, ADMIN/MANAGER). */
@Serializable
data class DashboardSummaryDto(
    val devices: DashboardDevicesDto = DashboardDevicesDto(),
    val tickets: DashboardTicketsDto = DashboardTicketsDto(),
    val custodyLetters: DashboardCustodyLettersDto = DashboardCustodyLettersDto(),
    val materialOutputs: DashboardMaterialOutputsDto = DashboardMaterialOutputsDto(),
    val departments: Int = 0,
    val employees: Int = 0,
    val ticketMetrics: DashboardMetricsDto = DashboardMetricsDto(),
    val ticketEfficiency: List<DashboardEfficiencyDto> = emptyList(),
    val urgentTickets: List<DashboardUrgentTicketDto> = emptyList(),
    val recentActivity: List<DashboardActivityDto> = emptyList(),
)

@Serializable
data class DashboardDevicesDto(
    val total: Int = 0,
    val available: Int = 0,
    val assigned: Int = 0,
    val retirement: Int = 0,
)

@Serializable
data class DashboardTicketsDto(
    val total: Int = 0,
    val open: Int = 0,
    val inProgress: Int = 0,
    val closed: Int = 0,
)

@Serializable
data class DashboardCustodyLettersDto(val total: Int = 0, val active: Int = 0)

@Serializable
data class DashboardMaterialOutputsDto(val total: Int = 0, val damaged: Int = 0)

@Serializable
data class DashboardMetricsDto(
    val resolvedTasks: Int = 0,
    val pendingTasks: Int = 0,
    val avgResolutionDays: Double? = null,
)

@Serializable
data class DashboardUserRefDto(
    val id: String = "",
    val name: String = "",
    val jobTitle: String? = null,
)

@Serializable
data class DashboardEfficiencyDto(
    val user: DashboardUserRefDto = DashboardUserRefDto(),
    val resolved: Int = 0,
    val pending: Int = 0,
    val avgDays: Double? = null,
)

@Serializable
data class DashboardUrgentTicketDto(
    val id: String,
    val title: String = "",
    val priority: String = "MEDIUM",
    val createdAt: String = "",
    val daysOnHold: Int = 0,
    val assigned: String? = null,
)

@Serializable
data class DashboardActivityDto(
    val id: String,
    val scope: String = "inventory",
    val message: String = "",
    val at: String = "",
    val targetId: String? = null,
    val deviceId: String? = null,
)

// ── Widgets de los tableros por permiso (`GET /dashboard/<widget>`) ─────────

/** `GET /dashboard/tickets` (`tickets.view`). */
@Serializable
data class TicketsWidgetDto(
    val scope: String = "ALL",
    val open: Int = 0,
    val inProgress: Int = 0,
    val closed: Int = 0,
    val unassigned: Int = 0,
    val staleDays: Int = 0,
    val stale: List<StaleTicketDto> = emptyList(),
    val openByAssignee: List<OpenByAssigneeDto> = emptyList(),
)

@Serializable
data class StaleTicketDto(
    val id: String,
    val title: String = "",
    val priority: String = "MEDIUM",
    val status: String = "",
    val createdAt: String = "",
    val assignedTo: String? = null,
)

@Serializable
data class OpenByAssigneeDto(val userId: String = "", val name: String = "", val open: Int = 0)

/** `GET /dashboard/tasks` (`tasks.view`). */
@Serializable
data class TasksWidgetDto(
    val scope: String = "ALL",
    val pending: Int = 0,
    val inProgress: Int = 0,
    val inReview: Int = 0,
    val completed: Int = 0,
    val overdue: List<TaskOverdueDto> = emptyList(),
    val pendingByUser: List<PendingByUserDto> = emptyList(),
)

@Serializable
data class TaskOverdueDto(
    val id: String,
    val title: String = "",
    val status: String = "PENDING",
    val ticketId: String = "",
    val dueDate: String? = null,
    val name: String = "",
)

@Serializable
data class PendingByUserDto(val userId: String = "", val name: String = "", val pending: Int = 0)

/** `GET /dashboard/my-equipment`: equipo en las cartas responsivas vigentes del usuario. */
@Serializable
data class MyEquipmentWidgetDto(val loans: List<MyEquipmentLoanDto> = emptyList())

@Serializable
data class MyEquipmentLoanDto(
    val id: String,
    val number: String = "",
    val date: String = "",
    val items: List<MyEquipmentItemDto> = emptyList(),
)

@Serializable
data class MyEquipmentItemDto(
    val name: String = "",
    val brand: String = "",
    val model: String = "",
    val pending: Int = 0,
    val units: List<MyEquipmentUnitDto> = emptyList(),
)

@Serializable
data class MyEquipmentUnitDto(val assetTag: String = "", val serialNumber: String? = null)

/** `GET /dashboard/attendance-today` (`attendance.view`). */
@Serializable
data class AttendanceTodayWidgetDto(
    val scope: String = "ALL",
    val counts: AttendanceCountsDto = AttendanceCountsDto(),
    val rows: List<AttendanceTodayRowDto> = emptyList(),
)

@Serializable
data class AttendanceCountsDto(
    @SerialName("PRESENT") val present: Int = 0,
    @SerialName("LATE") val late: Int = 0,
    @SerialName("NOT_ARRIVED") val notArrived: Int = 0,
    @SerialName("NOT_YET") val notYet: Int = 0,
    @SerialName("UNLINKED") val unlinked: Int = 0,
    val onSite: Int = 0,
    val people: Int = 0,
)

/** Estado: PRESENT, LATE, NOT_ARRIVED, NOT_YET, REST, NO_SCHEDULE o UNLINKED. */
@Serializable
data class AttendanceTodayRowDto(
    val userId: String = "",
    val name: String = "",
    val departmentName: String? = null,
    val status: String = "",
    val entryAt: String? = null,
    val lateMin: Int = 0,
    val shift: String? = null,
    val scheduledStartAt: String? = null,
)

/** `GET /dashboard/people` (`hr.records`). */
@Serializable
data class PeopleWidgetDto(
    val active: Int = 0,
    val hiresThisMonth: Int = 0,
    val departuresThisMonth: Int = 0,
)

/** `GET /dashboard/overtime-week` (`overtime.view`); todo en minutos. */
@Serializable
data class OvertimeWeekWidgetDto(
    val extraMin: Int = 0,
    val approvedExtraMin: Int = 0,
    val pendingExtraMin: Int = 0,
    val rejectedExtraMin: Int = 0,
    val topPending: List<OvertimePendingDto> = emptyList(),
)

@Serializable
data class OvertimePendingDto(
    val userId: String = "",
    val name: String = "",
    val departmentName: String? = null,
    val pendingExtraMin: Int = 0,
)

/** `GET /dashboard/hr-records` (`hr.records`): solo lo necesario para contar expedientes. */
@Serializable
data class HrRecordsWidgetDto(val rows: List<RecordGapsRowDto> = emptyList())

@Serializable
data class RecordGapsRowDto(
    val userId: String = "",
    val missingRequired: List<RecordGapItemDto> = emptyList(),
    val missingDocuments: List<RecordGapItemDto> = emptyList(),
    val missingFields: List<String> = emptyList(),
)

@Serializable
data class RecordGapItemDto(val id: String = "", val name: String = "")

/** `GET /dashboard/setup-gaps` (`time_clock.link`). */
@Serializable
data class SetupGapsWidgetDto(
    val unlinkedClockNumbers: Int = 0,
    val personalWithoutClock: Int = 0,
    val personalWithoutSchedule: Int = 0,
)

/** `GET /dashboard/access-today` (`access.scan`). */
@Serializable
data class AccessTodayWidgetDto(
    val entries: Int = 0,
    val exits: Int = 0,
    val registeredByMe: Int = 0,
    val onSite: List<AccessOnSiteDto> = emptyList(),
    val latest: List<AccessLatestDto> = emptyList(),
)

@Serializable
data class AccessOnSiteDto(
    val employeeId: String = "",
    val name: String? = null,
    val since: String = "",
    val site: String? = null,
)

/** `type`: ENTRY o EXIT. */
@Serializable
data class AccessLatestDto(
    val id: String = "",
    val type: String = "ENTRY",
    val at: String = "",
    val name: String? = null,
    val site: String? = null,
    val guard: String? = null,
)

/** `GET /dashboard/system-health` (`system.configure`). */
@Serializable
data class SystemHealthWidgetDto(
    val inventoryAudit: InventoryAuditNoteDto? = null,
    val clocks: List<SystemClockDto> = emptyList(),
    val failedEmailsLast7Days: Int = 0,
    val pendingEmails: Int = 0,
)

@Serializable
data class InventoryAuditNoteDto(val title: String = "", val detail: String? = null, val at: String = "")

@Serializable
data class SystemClockDto(
    val serialNumber: String = "",
    val name: String? = null,
    val syncedAt: String? = null,
)
