package com.axzydev.puertonuevoapp.core.network.devicetypes

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Adaptador al contrato REAL del API (`/inventario/tipos`), que difiere del
 * contrato documentado (`/device-types`). La app mantiene su modelo
 * ([DeviceTypeDto]) y aquí se traduce la respuesta del backend.
 *
 * El backend expone 4 flags (`useSerie/useMac/useIp/useEquipo`), no un
 * `fieldConfig`; se mapean a los campos equivalentes y el resto queda apagado.
 */
private fun ApiTipoDispositivo.toDto(): DeviceTypeDto = DeviceTypeDto(
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

private fun DeviceFieldConfigDto.toUseFlags() = UseFlags(
    useSerie = numeroSerie.enabled,
    useMac = macAddress.enabled,
    useIp = ip.enabled,
    useEquipo = nombreEquipo.enabled,
)

class DeviceTypesApi(private val client: ApiClient) {
    suspend fun list(includeInactive: Boolean = false): List<DeviceTypeDto> =
        client.get<List<ApiTipoDispositivo>>("/inventario/tipos")
            .map { it.toDto() }
            .filter { includeInactive || it.active }

    suspend fun get(id: String): DeviceTypeDto =
        client.get<List<ApiTipoDispositivo>>("/inventario/tipos")
            .firstOrNull { it.id == id }
            ?.toDto()
            ?: error("Tipo de dispositivo no encontrado")

    /** El backend no expone folios anticipados; se sintetiza el siguiente. */
    suspend fun peek(id: String): String {
        val tipo = get(id)
        return "${tipo.prefix}-${(tipo.contador + 1).toString().padStart(4, '0')}"
    }

    /** En el contrato real no existe folio de carta por tipo. */
    suspend fun peekCarta(id: String): String = ""

    suspend fun create(input: DeviceTypeCreateInput): DeviceTypeDto {
        val flags = input.fieldConfig?.toUseFlags() ?: UseFlags()
        return client.post<ApiCreateTipo, ApiTipoDispositivo>(
            "/inventario/tipos",
            ApiCreateTipo(
                code = input.code,
                name = input.name,
                folioPrefix = input.prefix,
                useSerie = flags.useSerie,
                useMac = flags.useMac,
                useIp = flags.useIp,
                useEquipo = flags.useEquipo,
            ),
        ).toDto()
    }

    suspend fun update(id: String, input: DeviceTypeUpdateInput): DeviceTypeDto {
        val flags = input.fieldConfig?.toUseFlags()
        return client.put<ApiUpdateTipo, ApiTipoDispositivo>(
            "/inventario/tipos/$id",
            ApiUpdateTipo(
                name = input.name,
                active = input.active,
                useSerie = flags?.useSerie,
                useMac = flags?.useMac,
                useIp = flags?.useIp,
                useEquipo = flags?.useEquipo,
            ),
        ).toDto()
    }

    suspend fun delete(id: String) = client.deleteNoContent("/inventario/tipos/$id")
}

private data class UseFlags(
    val useSerie: Boolean = false,
    val useMac: Boolean = false,
    val useIp: Boolean = false,
    val useEquipo: Boolean = false,
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
private data class ApiCreateTipo(
    val code: String,
    val name: String,
    val folioPrefix: String,
    val useSerie: Boolean,
    val useMac: Boolean,
    val useIp: Boolean,
    val useEquipo: Boolean,
)

@Serializable
private data class ApiUpdateTipo(
    val name: String? = null,
    val active: Boolean? = null,
    val useSerie: Boolean? = null,
    val useMac: Boolean? = null,
    val useIp: Boolean? = null,
    val useEquipo: Boolean? = null,
)
