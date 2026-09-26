package com.axzydev.puertonuevoapp.core.network.dashboard

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
