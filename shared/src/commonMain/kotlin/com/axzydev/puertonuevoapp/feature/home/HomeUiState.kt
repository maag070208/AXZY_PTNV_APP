package com.axzydev.puertonuevoapp.feature.home

import com.axzydev.puertonuevoapp.core.network.dashboard.AccessTodayWidgetDto
import com.axzydev.puertonuevoapp.core.network.dashboard.AttendanceTodayWidgetDto
import com.axzydev.puertonuevoapp.core.network.dashboard.DashboardSummaryDto
import com.axzydev.puertonuevoapp.core.network.dashboard.HrRecordsWidgetDto
import com.axzydev.puertonuevoapp.core.network.dashboard.MyEquipmentWidgetDto
import com.axzydev.puertonuevoapp.core.network.dashboard.OvertimeWeekWidgetDto
import com.axzydev.puertonuevoapp.core.network.dashboard.SetupGapsWidgetDto
import com.axzydev.puertonuevoapp.core.network.dashboard.SystemHealthWidgetDto
import com.axzydev.puertonuevoapp.core.network.dashboard.TasksWidgetDto
import com.axzydev.puertonuevoapp.core.network.dashboard.TicketsWidgetDto
import com.axzydev.puertonuevoapp.core.network.inventory.DeviceCategoryDto

/** Cada bloque del inicio se pide por separado (según permisos) y puede fallar solo. */
enum class HomeSection {
    Summary, Categories, Tickets, Tasks, Equipment, Attendance, Overtime, HrRecords, SetupGaps, AccessToday, SystemHealth,
}

/**
 * Inicio (clon de la web). Cada campo es `null` mientras no se haya pedido (o no haya
 * permiso); [errors] guarda el mensaje de los bloques que fallaron.
 */
data class HomeUiState(
    val loading: Boolean = true,
    val unreadNotifications: Int = 0,
    val summary: DashboardSummaryDto? = null,
    /** Dispositivos por categoría (opcional: sin permiso de inventario queda vacío). */
    val categories: List<DeviceCategoryDto> = emptyList(),
    val tickets: TicketsWidgetDto? = null,
    val tasks: TasksWidgetDto? = null,
    val equipment: MyEquipmentWidgetDto? = null,
    val attendance: AttendanceTodayWidgetDto? = null,
    val overtime: OvertimeWeekWidgetDto? = null,
    val hrRecords: HrRecordsWidgetDto? = null,
    val setupGaps: SetupGapsWidgetDto? = null,
    val accessToday: AccessTodayWidgetDto? = null,
    val systemHealth: SystemHealthWidgetDto? = null,
    val errors: Map<HomeSection, String> = emptyMap(),
)
