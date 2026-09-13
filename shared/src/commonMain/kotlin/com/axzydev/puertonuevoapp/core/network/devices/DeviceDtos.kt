package com.axzydev.puertonuevoapp.core.network.devices

import kotlinx.serialization.Serializable

@Serializable
data class DeviceSummaryDto(
    val total: Int,
    val disponible: Int,
    val asignado: Int,
    val baja: Int,
    val tipos: Int,
)