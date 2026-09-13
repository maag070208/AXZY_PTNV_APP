package com.axzydev.puertonuevoapp.core.network

import io.ktor.http.encodeURLParameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class CartasApi(private val client: ApiClient) {
    suspend fun list(q: String? = null): CartasListResponseDto {
        val qs = q?.takeIf { it.isNotBlank() }?.let { "?q=${it.encodeURLParameter()}" } ?: ""
        return client.get("/cartas$qs")
    }

    suspend fun get(id: String): CartaDto = client.get("/cartas/$id")

    suspend fun create(input: CartaInputDto): CartaDto = client.post("/cartas", input)

    suspend fun update(id: String, input: CartaInputDto): CartaDto = client.put("/cartas/$id", input)

    suspend fun remove(id: String) {
        client.deleteNoContent("/cartas/$id")
    }

    suspend fun peekConsecutivo(): String = client.get<ConsecutivoPeekDto>("/cartas/consecutivo/peek").siguiente

    suspend fun returnCarta(id: String, input: CartaReturnDto): CartaDto = client.post("/cartas/$id/return", input)

    suspend fun undoReturn(id: String): CartaDto = client.delete("/cartas/$id/return")
}

@Serializable
data class ConsecutivoPeekDto(
    val siguiente: String,
)

@Serializable
data class CartaPersonRefDto(
    val id: String,
    val name: String,
    val puesto: String? = null,
    val numeroEmpleado: String? = null,
    val department: DepartmentRefDto? = null,
)

@Serializable
data class CartaItemDto(
    val id: String? = null,
    val cartaId: String? = null,
    val deviceId: String? = null,
    val device: DeviceDto? = null,
    val descripcion: String? = null,
    val marca: String? = null,
    val modelo: String? = null,
    val numeroSerie: String? = null,
    val nombreEquipo: String? = null,
    val controlActivos: String? = null,
    val area: String? = null,
)

@Serializable
data class CartaDto(
    val id: String,
    @SerialName("consecutive") val consecutivo: String,
    val fecha: String,
    val numeroEmpleado: String,
    val empresa: String? = null,
    val departamento: String,
    val areaBoss: String? = null,
    val deliveryBy: String? = null,
    val creadoPorId: String? = null,
    val creadoPor: UserRefDto? = null,
    val responsableId: String? = null,
    val responsable: CartaPersonRefDto? = null,
    val encargadoId: String? = null,
    val encargado: CartaPersonRefDto? = null,
    val items: List<CartaItemDto> = emptyList(),
    val returnDate: String? = null,
    val returnedBy: String? = null,
    val returnCondition: String? = null,
    val creadoEn: String,
    val actualizadoEn: String? = null,
)

@Serializable
data class CartasListResponseDto(
    val data: List<CartaDto>,
    val total: Int,
)

@Serializable
data class CartaItemInputDto(
    val deviceId: String? = null,
    val descripcion: String? = null,
    val marca: String? = null,
    val modelo: String? = null,
    val numeroSerie: String? = null,
    val nombreEquipo: String? = null,
    val controlActivos: String? = null,
    val area: String? = null,
)

@Serializable
data class CartaInputDto(
    val consecutivo: String? = null,
    val fecha: String? = null,
    val numeroEmpleado: String,
    val empresa: String? = null,
    val departamento: String? = null,
    val areaBoss: String? = null,
    val deliveryBy: String? = null,
    val responsableId: String? = null,
    val encargadoId: String? = null,
    // Nulo en un update = "no tocar el item actual" (updateSchema.partial()
    // del backend hace opcional TODO, incluido `item`); en create siempre
    // se manda porque el backend lo exige.
    val item: CartaItemInputDto? = null,
)

@Serializable
data class CartaReturnDto(
    val returnedBy: String,
    val returnCondition: String,
)
