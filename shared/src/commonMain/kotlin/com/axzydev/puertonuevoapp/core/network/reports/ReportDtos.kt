package com.axzydev.puertonuevoapp.core.network.reports

import kotlinx.serialization.Serializable

@Serializable
data class AsignadosResponseDto(
    val data: List<AsignadoRowDto>,
    val total: Int = 0,
)

@Serializable
data class DevicesReportResponseDto(
    val data: List<DeviceReportRowDto>,
    val total: Int = 0,
)

@Serializable
data class AsignadoRowDto(
    val deviceId: String,
    val controlActivos: String,
    val descripcion: String,
    val marca: String,
    val modelo: String,
    val tipo: String,
    val responsable: String,
    val numeroEmpleado: String? = null,
    val departamento: String? = null,
    val fecha: String? = null,
    val diasAsignado: Int? = null,
    val origen: String,
    val folio: String? = null,
)

@Serializable
data class DeviceReportRowDto(
    val deviceId: String,
    val controlActivos: String,
    val descripcion: String,
    val marca: String,
    val modelo: String,
    val tipo: String,
    val numeroSerie: String? = null,
    val nombreEquipo: String? = null,
    val ip: String? = null,
    val macAddress: String? = null,
    val area: String,
    val location: String? = null,
    val estado: String,
    val loteId: String? = null,
    val cantidad: Int = 1,
    val responsable: String? = null,
    val numeroEmpleado: String? = null,
    val departamento: String? = null,
    val fecha: String? = null,
    val diasAsignado: Int? = null,
    val origen: String? = null,
    val folio: String? = null,
)
