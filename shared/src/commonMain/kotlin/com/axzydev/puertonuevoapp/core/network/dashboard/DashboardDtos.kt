package com.axzydev.puertonuevoapp.core.network.dashboard

import kotlinx.serialization.Serializable

/** Resumen del panel administrativo (`GET /dashboard/summary`, ADMIN/GERENTE). */
@Serializable
data class DashboardSummaryDto(
    val devices: DashboardDevicesDto = DashboardDevicesDto(),
    val tickets: DashboardTicketsDto = DashboardTicketsDto(),
    val cartas: DashboardCartasDto = DashboardCartasDto(),
    val salidas: DashboardSalidasDto = DashboardSalidasDto(),
    val departamentos: Int = 0,
    val empleados: Int = 0,
    val ticketMetricas: DashboardMetricasDto = DashboardMetricasDto(),
    val ticketEficiencia: List<DashboardEficienciaDto> = emptyList(),
    val ticketsUrgentes: List<DashboardUrgenteDto> = emptyList(),
    val recentActivity: List<DashboardActivityDto> = emptyList(),
)

@Serializable
data class DashboardDevicesDto(
    val total: Int = 0,
    val disponible: Int = 0,
    val asignado: Int = 0,
    val baja: Int = 0,
)

@Serializable
data class DashboardTicketsDto(
    val total: Int = 0,
    val abierto: Int = 0,
    val enSeguimiento: Int = 0,
    val cerrado: Int = 0,
)

@Serializable
data class DashboardCartasDto(val total: Int = 0, val activas: Int = 0)

@Serializable
data class DashboardSalidasDto(val total: Int = 0, val danadas: Int = 0)

@Serializable
data class DashboardMetricasDto(
    val tareasResueltas: Int = 0,
    val tareasPendientes: Int = 0,
    val avgResolucionDias: Double? = null,
)

@Serializable
data class DashboardUserRefDto(
    val id: String = "",
    val name: String = "",
    val puesto: String? = null,
)

@Serializable
data class DashboardEficienciaDto(
    val user: DashboardUserRefDto = DashboardUserRefDto(),
    val resueltas: Int = 0,
    val pendientes: Int = 0,
    val avgDias: Double? = null,
)

@Serializable
data class DashboardUrgenteDto(
    val id: String,
    val titulo: String = "",
    val prioridad: String = "MEDIA",
    val creadoEn: String = "",
    val diasEnEspera: Int = 0,
    val asignado: String? = null,
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
