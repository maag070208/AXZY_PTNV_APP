package com.axzydev.puertonuevoapp.core.network.salidas

import com.axzydev.puertonuevoapp.core.network.common.UserRefDto
import kotlinx.serialization.Serializable

@Serializable
data class SalidaDeviceRefDto(
    val id: String,
    val controlActivos: String,
)

@Serializable
data class SalidaDto(
    val id: String,
    val fecha: String,
    val descripcion: String,
    val modelo: String? = null,
    val marca: String? = null,
    val proyecto: String? = null,
    val cantidad: Int = 1,
    val departamento: String,
    val usuario: String,
    val observaciones: String? = null,
    val area: String = "Sistemas",
    val motivo: String? = null,
    val deviceId: String? = null,
    val device: SalidaDeviceRefDto? = null,
    val registradoPorId: String? = null,
    val registradoPor: UserRefDto? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class SalidasListResponseDto(
    val data: List<SalidaDto>,
    val total: Int = 0,
)

@Serializable
data class SalidaInput(
    val fecha: String? = null,
    val descripcion: String,
    val modelo: String? = null,
    val marca: String? = null,
    val proyecto: String? = null,
    val cantidad: Int? = null,
    val departamento: String,
    val usuario: String,
    val observaciones: String? = null,
    val area: String? = null,
    val motivo: String? = null,
    val deviceId: String? = null,
)

@Serializable
data class SalidaSuggestionsDto(
    val departamento: List<String> = emptyList(),
    val usuario: List<String> = emptyList(),
    val proyecto: List<String> = emptyList(),
    val marca: List<String> = emptyList(),
    val modelo: List<String> = emptyList(),
    val descripcion: List<String> = emptyList(),
)

val salidaMotivoOptions: List<Pair<String, String>> = listOf(
    "" to "Sin especificar",
    "DANADO" to "Dañado",
    "OBSOLETO" to "Obsoleto",
    "EXTRAVIO" to "Extravío",
    "OTRO" to "Otro",
)
