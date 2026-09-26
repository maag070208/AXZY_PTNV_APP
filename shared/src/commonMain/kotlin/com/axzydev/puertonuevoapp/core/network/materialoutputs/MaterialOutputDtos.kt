package com.axzydev.puertonuevoapp.core.network.materialoutputs

import com.axzydev.puertonuevoapp.core.network.common.UserRefDto
import kotlinx.serialization.Serializable

@Serializable
data class MaterialOutputDeviceRefDto(
    val id: String,
    val assetTag: String,
)

@Serializable
data class MaterialOutputDto(
    val id: String,
    val date: String,
    val description: String,
    val model: String? = null,
    val brand: String? = null,
    val project: String? = null,
    val quantity: Int = 1,
    val departmentName: String,
    val userName: String,
    val notes: String? = null,
    val area: String = "Sistemas",
    val reason: String? = null,
    val deviceId: String? = null,
    val device: MaterialOutputDeviceRefDto? = null,
    val registeredById: String? = null,
    val registeredBy: UserRefDto? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class MaterialOutputsListResponseDto(
    val data: List<MaterialOutputDto>,
    val total: Int = 0,
)

@Serializable
data class MaterialOutputInput(
    val date: String? = null,
    val description: String,
    val model: String? = null,
    val brand: String? = null,
    val project: String? = null,
    val quantity: Int? = null,
    val departmentName: String,
    val userName: String,
    val notes: String? = null,
    val area: String? = null,
    val reason: String? = null,
    val deviceId: String? = null,
)

@Serializable
data class MaterialOutputSuggestionsDto(
    val departmentName: List<String> = emptyList(),
    val userName: List<String> = emptyList(),
    val project: List<String> = emptyList(),
    val brand: List<String> = emptyList(),
    val model: List<String> = emptyList(),
    val description: List<String> = emptyList(),
)

val materialOutputReasonOptions: List<Pair<String, String>> = listOf(
    "" to "Sin especificar",
    "DAMAGED" to "Dañado",
    "OBSOLETE" to "Obsoleto",
    "LOST" to "Extravío",
    "OTHER" to "Otro",
)
