package com.axzydev.puertonuevoapp.core.network.cartas

import com.axzydev.puertonuevoapp.core.network.common.DepartmentRefDto
import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.ApiException
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.Serializable

/**
 * Adaptador al contrato REAL del API. Las "cartas responsivas" del contrato
 * documentado (`/cartas`) son **préstamos** en el backend actual
 * (`/inventario/prestamos`). La app conserva su modelo [CartaDto] y aquí se
 * traduce.
 *
 * Degradaciones conocidas (no existen en el API real): folios anticipados
 * (`consecutivo/peek`), `reset` y `generate` por tipo. `undoReturn` no tiene
 * endpoint; la devolución se registra vía `/inventario/devoluciones`.
 */
class CartasApi(private val client: ApiClient) {
    suspend fun list(q: String? = null): CartasListResponseDto {
        val all = client.get<List<ApiPrestamo>>("/inventario/prestamos").map { it.toCartaDto() }
        val filtered = q?.takeIf { it.isNotBlank() }
            ?.let { query -> all.filter { it.consecutivo.contains(query, true) || it.departamento.contains(query, true) } }
            ?: all
        return CartasListResponseDto(data = filtered, total = filtered.size)
    }

    suspend fun get(id: String): CartaDto =
        client.get<ApiPrestamo>("/inventario/prestamos/$id").toCartaDto()

    suspend fun create(input: CartaCreateInput): CartaDto {
        val body = ApiCreatePrestamo(
            responsableId = input.responsableId,
            observaciones = input.deliveryBy,
            detalles = input.item.deviceId?.let { listOf(ApiPrestamoDetalleInput(it, 1)) } ?: emptyList(),
        )
        return client.post<ApiCreatePrestamo, ApiPrestamo>("/inventario/prestamos", body).toCartaDto()
    }

    suspend fun update(id: String, input: CartaUpdateInput): CartaDto {
        val body = ApiUpdatePrestamo(
            responsableId = input.responsableId,
            dispositivoId = input.item?.deviceId,
            cantidad = input.item?.deviceId?.let { 1 },
        )
        return client.put<ApiUpdatePrestamo, ApiPrestamo>("/inventario/prestamos/$id", body).toCartaDto()
    }

    suspend fun remove(id: String) = client.postNoContent("/inventario/prestamos/$id/cancelar")

    /** No existe folio global en el API real. */
    suspend fun consecutivo(): ConsecutivoStateDto = ConsecutivoStateDto(prefijo = "", contador = 0, siguiente = "")

    /** No existe folio anticipado en el API real. */
    suspend fun peekConsecutivo(): String = ""

    /** No existe reset de consecutivo en el API real. */
    suspend fun resetConsecutivo(): ConsecutivoStateDto =
        throw ApiException(501, "Reiniciar consecutivo no está disponible en el API actual")

    /** No existe generación de plantilla por tipo en el API real. */
    suspend fun generate(typeId: String): CartaGeneratedDto =
        throw ApiException(501, "Generar carta por tipo no está disponible en el API actual")

    suspend fun returnCarta(id: String, returnedBy: String, returnCondition: String): CartaDto {
        val prestamo = client.get<ApiPrestamo>("/inventario/prestamos/$id")
        val pendientes = prestamo.detalles.mapNotNull { d ->
            val pend = d.cantidad - d.devuelto
            if (pend > 0) ApiDevolucionDetalleInput(d.id, pend, returnCondition) else null
        }
        if (pendientes.isNotEmpty()) {
            client.post<ApiCreateDevolucion, ApiDevolucion>(
                "/inventario/devoluciones",
                ApiCreateDevolucion(prestamoId = id, observaciones = returnedBy, detalles = pendientes),
            )
        }
        return client.get<ApiPrestamo>("/inventario/prestamos/$id").toCartaDto()
    }

    /** No existe endpoint para deshacer la devolución en el API real. */
    suspend fun undoReturn(id: String): CartaDto =
        throw ApiException(501, "Deshacer devolución no está disponible en el API actual")
}

private fun ApiPrestamo.toCartaDto(): CartaDto = CartaDto(
    id = id,
    consecutivo = consecutivo,
    fecha = fecha,
    numeroEmpleado = responsable?.numeroEmpleado ?: "",
    departamento = departamento?.name ?: responsable?.department?.name ?: "",
    responsableId = responsableId,
    responsable = responsable?.let {
        CartaPersonRefDto(
            id = it.id,
            name = it.name,
            numeroEmpleado = it.numeroEmpleado,
            department = it.department?.let { d -> DepartmentRefDto(id = d.id, name = d.name) },
        )
    },
    items = detalles.map { d ->
        CartaItemDto(
            id = d.id,
            cartaId = id,
            deviceId = d.dispositivoId,
            descripcion = d.dispositivo?.nombre,
            marca = d.dispositivo?.marca,
            modelo = d.dispositivo?.modelo,
        )
    },
    returnDate = devoluciones.firstOrNull()?.fecha,
    creadoEn = fecha,
)

@Serializable
private data class ApiDeptRef(val id: String, val name: String)

@Serializable
private data class ApiResponsable(
    val id: String,
    val name: String,
    val username: String = "",
    val numeroEmpleado: String? = null,
    val department: ApiDeptRef? = null,
)

@Serializable
private data class ApiPrestamoDispositivo(
    val id: String,
    val nombre: String = "",
    val marca: String = "",
    val modelo: String = "",
)

@Serializable
private data class ApiPrestamoDetalle(
    val id: String,
    val dispositivoId: String = "",
    val dispositivo: ApiPrestamoDispositivo? = null,
    val cantidad: Int = 0,
    val devuelto: Int = 0,
)

@Serializable
private data class ApiDevolucionRef(val id: String, val consecutivo: String = "", val fecha: String = "")

@Serializable
private data class ApiPrestamo(
    val id: String,
    val responsableId: String? = null,
    val responsable: ApiResponsable? = null,
    val departamentoId: String? = null,
    val departamento: ApiDeptRef? = null,
    val subareaId: String? = null,
    val fecha: String = "",
    val status: String = "ACTIVO",
    val consecutivo: String = "",
    val observaciones: String? = null,
    val detalles: List<ApiPrestamoDetalle> = emptyList(),
    val devoluciones: List<ApiDevolucionRef> = emptyList(),
)

@Serializable
private data class ApiPrestamoDetalleInput(val dispositivoId: String, val cantidad: Int = 1)

@Serializable
private data class ApiCreatePrestamo(
    val responsableId: String? = null,
    val departamentoId: String? = null,
    val subareaId: String? = null,
    val observaciones: String? = null,
    val detalles: List<ApiPrestamoDetalleInput> = emptyList(),
)

@Serializable
private data class ApiUpdatePrestamo(
    val responsableId: String? = null,
    val departamentoId: String? = null,
    val subareaId: String? = null,
    val observaciones: String? = null,
    val dispositivoId: String? = null,
    val cantidad: Int? = null,
)

@Serializable
private data class ApiDevolucionDetalleInput(
    val prestamoDetalleId: String,
    val cantidad: Int,
    val condicion: String,
)

@Serializable
private data class ApiCreateDevolucion(
    val prestamoId: String,
    val responsableId: String? = null,
    val observaciones: String? = null,
    val detalles: List<ApiDevolucionDetalleInput> = emptyList(),
)

@Serializable
private data class ApiDevolucion(val id: String = "", val prestamoId: String = "")
