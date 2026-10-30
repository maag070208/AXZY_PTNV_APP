package com.axzydev.puertonuevoapp.core.network.dashboard

import com.axzydev.puertonuevoapp.core.network.http.ApiClient

class DashboardApi(private val client: ApiClient) {
    suspend fun summary(): DashboardSummaryDto = client.get("/dashboard/summary")

    // Widgets por permiso: la API recorta los datos con el alcance (propio / área / todo).

    /** `tickets.view`. */
    suspend fun tickets(): TicketsWidgetDto = client.get("/dashboard/tickets")

    /** `tasks.view`. */
    suspend fun tasks(): TasksWidgetDto = client.get("/dashboard/tasks")

    /** Solo requiere sesión. */
    suspend fun myEquipment(): MyEquipmentWidgetDto = client.get("/dashboard/my-equipment")

    /** `attendance.view`. */
    suspend fun attendanceToday(): AttendanceTodayWidgetDto = client.get("/dashboard/attendance-today")

    /** `hr.records`. */
    suspend fun people(): PeopleWidgetDto = client.get("/dashboard/people")

    /** `overtime.view`. */
    suspend fun overtimeWeek(): OvertimeWeekWidgetDto = client.get("/dashboard/overtime-week")

    /** `hr.records`: expedientes (la app solo cuenta su estado). */
    suspend fun hrRecords(): HrRecordsWidgetDto = client.get("/dashboard/hr-records")

    /** `time_clock.link`. */
    suspend fun setupGaps(): SetupGapsWidgetDto = client.get("/dashboard/setup-gaps")

    /** `access.scan`. */
    suspend fun accessToday(): AccessTodayWidgetDto = client.get("/dashboard/access-today")

    /** `system.configure`. */
    suspend fun systemHealth(): SystemHealthWidgetDto = client.get("/dashboard/system-health")
}
