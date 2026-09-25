package com.axzydev.puertonuevoapp.core.network.devices

import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceFieldConfigDto
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceFieldSettingDto
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypeCountDto
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypeDto
import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Adaptador al contrato REAL del API (`/inventario`), que difiere del
 * contrato documentado (`/devices`, `/device-types`). La app conserva su
 * modelo ([DeviceDto]) y aquí se traduce.
 *
 * El backend separa `Dispositivo` (modelo/lote) de `UnidadFisica` (activo
 * fijo). La lista de la app trabaja a nivel modelo, con conteos por estado
 * derivados de `existencias` (`?existencias=true`).
 */
class DevicesApi(private val client: ApiClient) {
    suspend fun list(estado: String? = null, q: String? = null, typeId: String? = null): DeviceListResponseDto {
        val params = buildList {
            add("existencias=true")
            typeId?.takeIf { it.isNotBlank() }?.let { add("tipoId=$it") }
            q?.takeIf { it.isNotBlank() }?.let { add("q=${it.encodeURLParameter()}") }
        }
        val qs = "?" + params.joinToString("&")
        val all = client.get<List<ApiDispositivo>>("/inventario/dispositivos$qs").map { it.toDeviceDto() }
        val filtered = estado?.takeIf { it.isNotBlank() }?.let { e -> all.filter { it.estado == e } } ?: all
        return DeviceListResponseDto(data = filtered, total = filtered.size)
    }

    suspend fun get(id: String): DeviceDto =
        client.get<ApiDispositivo>("/inventario/dispositivos/$id").toDeviceDto()

    suspend fun create(input: DeviceCreateInput): DeviceDto {
        val body = ApiCreateDispositivo(
            tipoId = input.typeId,
            nombre = input.descripcion,
            marca = input.marca,
            modelo = input.modelo,
            descripcion = null,
            observaciones = null,
            cantidadInicial = 1,
            unidades = listOf(
                ApiUnidadInput(
                    numeroSerie = input.numeroSerie,
                    macAddress = input.macAddress,
                    ip = input.ip,
                    nombreEquipo = input.nombreEquipo,
                )
            ),
        )
        return client.post<ApiCreateDispositivo, ApiDispositivo>("/inventario/dispositivos", body).toDeviceDto()
    }

    suspend fun update(id: String, input: DeviceUpdateInput): DeviceDto {
        val body = ApiUpdateDispositivo(
            nombre = input.descripcion,
            marca = input.marca,
            modelo = input.modelo,
        )
        return client.put<ApiUpdateDispositivo, ApiDispositivo>("/inventario/dispositivos/$id", body).toDeviceDto()
    }

    suspend fun remove(id: String, force: Boolean = false): DeviceRemoveResponseDto {
        val data = client.delete<ApiDispositivo>("/inventario/dispositivos/$id").toDeviceDto()
        return DeviceRemoveResponseDto(soft = !force, forced = force, data = data)
    }
}

private fun ApiDispositivo.toDeviceDto(): DeviceDto {
    val ex = existencias
    val estado = when {
        (ex?.PRESTADO ?: 0) > 0 -> "ASIGNADO"
        (ex?.DISPONIBLE ?: 0) > 0 -> "DISPONIBLE"
        (ex?.MANTENIMIENTO ?: 0) > 0 -> "DISPONIBLE"
        (ex?.BAJA ?: 0) > 0 -> "BAJA"
        else -> "DISPONIBLE"
    }
    return DeviceDto(
        id = id,
        typeId = tipoId,
        type = tipo?.toDeviceTypeDto(),
        controlActivos = nombre,
        descripcion = nombre,
        marca = marca,
        modelo = modelo,
        area = "",
        estado = estado,
        loteId = id,
        loteSize = ex?.total,
        loteCount = ex?.let {
            DeviceLoteCountDto(disponible = it.DISPONIBLE, asignado = it.PRESTADO, baja = it.BAJA)
        },
        createdAt = "",
        updatedAt = "",
    )
}

private fun ApiTipoDispositivo.toDeviceTypeDto(): DeviceTypeDto = DeviceTypeDto(
    id = id,
    code = code,
    name = name,
    prefix = folioPrefix,
    contador = contador,
    active = active,
    fieldConfig = DeviceFieldConfigDto(
        numeroSerie = DeviceFieldSettingDto(enabled = useSerie),
        nombreEquipo = DeviceFieldSettingDto(enabled = useEquipo),
        ip = DeviceFieldSettingDto(enabled = useIp),
        macAddress = DeviceFieldSettingDto(enabled = useMac),
        sistemaOp = DeviceFieldSettingDto(enabled = false),
        ram = DeviceFieldSettingDto(enabled = false),
        almacenamiento = DeviceFieldSettingDto(enabled = false),
    ),
    count = count?.let { DeviceTypeCountDto(it.dispositivos) },
)

@Serializable
private data class ApiExistencias(
    val total: Int = 0,
    val DISPONIBLE: Int = 0,
    val PRESTADO: Int = 0,
    val DANADO: Int = 0,
    val MANTENIMIENTO: Int = 0,
    val BAJA: Int = 0,
)

@Serializable
private data class ApiTipoCount(val dispositivos: Int = 0)

@Serializable
private data class ApiTipoDispositivo(
    val id: String,
    val code: String,
    val name: String,
    val folioPrefix: String = "",
    val contador: Int = 0,
    val active: Boolean = true,
    val useSerie: Boolean = false,
    val useMac: Boolean = false,
    val useIp: Boolean = false,
    val useEquipo: Boolean = false,
    @SerialName("_count") val count: ApiTipoCount? = null,
)

@Serializable
private data class ApiDispositivo(
    val id: String,
    val tipoId: String,
    val tipo: ApiTipoDispositivo? = null,
    val nombre: String,
    val marca: String,
    val modelo: String,
    val descripcion: String? = null,
    val observaciones: String? = null,
    val existencias: ApiExistencias? = null,
)

@Serializable
private data class ApiUnidadInput(
    val numeroSerie: String? = null,
    val macAddress: String? = null,
    val ip: String? = null,
    val nombreEquipo: String? = null,
)

@Serializable
private data class ApiCreateDispositivo(
    val tipoId: String,
    val nombre: String,
    val marca: String,
    val modelo: String,
    val descripcion: String? = null,
    val observaciones: String? = null,
    val cantidadInicial: Int = 1,
    val unidades: List<ApiUnidadInput> = emptyList(),
)

@Serializable
private data class ApiUpdateDispositivo(
    val nombre: String? = null,
    val marca: String? = null,
    val modelo: String? = null,
)
