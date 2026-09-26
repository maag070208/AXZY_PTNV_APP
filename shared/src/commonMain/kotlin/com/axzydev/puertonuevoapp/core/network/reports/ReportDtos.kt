package com.axzydev.puertonuevoapp.core.network.reports

import kotlinx.serialization.Serializable

@Serializable
data class AssignedDevicesResponseDto(
    val data: List<AssignedDeviceRowDto>,
    val total: Int = 0,
)

@Serializable
data class DevicesReportResponseDto(
    val data: List<DeviceReportRowDto>,
    val total: Int = 0,
)

@Serializable
data class AssignedDeviceRowDto(
    val deviceId: String,
    val assetTag: String,
    val description: String,
    val brand: String,
    val model: String,
    val type: String,
    val custodian: String,
    val employeeNumber: String? = null,
    val department: String? = null,
    val date: String? = null,
    val daysAssigned: Int? = null,
    val source: String,
    val folio: String? = null,
)

@Serializable
data class DeviceReportRowDto(
    val deviceId: String,
    val assetTag: String,
    val description: String,
    val brand: String,
    val model: String,
    val type: String,
    val serialNumber: String? = null,
    val hostname: String? = null,
    val ip: String? = null,
    val macAddress: String? = null,
    val area: String,
    val location: String? = null,
    val status: String,
    val batchId: String? = null,
    val quantity: Int = 1,
    val custodian: String? = null,
    val employeeNumber: String? = null,
    val department: String? = null,
    val date: String? = null,
    val daysAssigned: Int? = null,
    val source: String? = null,
    val folio: String? = null,
)
