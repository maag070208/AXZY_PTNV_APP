package com.axzydev.puertonuevoapp.core.network

import io.ktor.http.encodeURLParameter
import kotlinx.serialization.Serializable

class SalidasApi(private val client: ApiClient) {
    suspend fun list(
        start: String? = null,
        end: String? = null,
        departamento: String? = null,
        usuario: String? = null,
        q: String? = null,
    ): SalidasListResponseDto {
        val params = buildList {
            start?.takeIf { it.isNotBlank() }?.let { add("start=${it.encodeURLParameter()}") }
            end?.takeIf { it.isNotBlank() }?.let { add("end=${it.encodeURLParameter()}") }
            departamento?.takeIf { it.isNotBlank() }?.let { add("departamento=${it.encodeURLParameter()}") }
            usuario?.takeIf { it.isNotBlank() }?.let { add("usuario=${it.encodeURLParameter()}") }
            q?.takeIf { it.isNotBlank() }?.let { add("q=${it.encodeURLParameter()}") }
        }
        val qs = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
        return client.get("/salidas$qs")
    }

    suspend fun get(id: String): SalidaDto = client.get("/salidas/$id")

    suspend fun create(input: SalidaInputDto): SalidaDto = client.post("/salidas", input)

    suspend fun update(id: String, input: SalidaInputDto): SalidaDto = client.put("/salidas/$id", input)

    suspend fun remove(id: String): SalidaDto = client.delete("/salidas/$id")
}

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
    val total: Int,
)

@Serializable
data class SalidaInputDto(
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
    val deviceId: String? = null,
)
