package com.axzydev.puertonuevoapp.core.network.cartas

import com.axzydev.puertonuevoapp.core.network.common.DepartmentRefDto
import com.axzydev.puertonuevoapp.core.network.common.UserRefDto
import com.axzydev.puertonuevoapp.core.network.devices.DeviceDto
import com.axzydev.puertonuevoapp.core.network.locations.LocationDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConsecutivoPeekDto(val siguiente: String)

@Serializable
data class ConsecutivoStateDto(
    val prefijo: String,
    val contador: Int,
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
    val ubicacionId: String? = null,
    val ubicacion: LocationDto? = null,
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
    val total: Int = 0,
)

@Serializable
data class CartaItemInput(
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
data class CartaCreateInput(
    val consecutivo: String? = null,
    val fecha: String? = null,
    val numeroEmpleado: String,
    val empresa: String? = null,
    val departamento: String? = null,
    val areaBoss: String? = null,
    val deliveryBy: String? = null,
    val responsableId: String? = null,
    val encargadoId: String? = null,
    val item: CartaItemInput,
)

/** PUT /cartas/{id} — parcial: `item` nulo = "no tocar el item actual". */
@Serializable
data class CartaUpdateInput(
    val fecha: String? = null,
    val numeroEmpleado: String? = null,
    val empresa: String? = null,
    val departamento: String? = null,
    val areaBoss: String? = null,
    val deliveryBy: String? = null,
    val responsableId: String? = null,
    val encargadoId: String? = null,
    val item: CartaItemInput? = null,
)

@Serializable
data class CartaReturnInput(
    val returnedBy: String,
    val returnCondition: String,
)

@Serializable
data class CartaGenerateInput(val typeId: String)

@Serializable
data class CartaGeneratedTipoDto(
    val code: String,
    val name: String,
    val prefix: String,
)

@Serializable
data class CartaGeneratedDto(
    val tipo: CartaGeneratedTipoDto,
    val contador: Int,
    val carta: CartaDto,
)
