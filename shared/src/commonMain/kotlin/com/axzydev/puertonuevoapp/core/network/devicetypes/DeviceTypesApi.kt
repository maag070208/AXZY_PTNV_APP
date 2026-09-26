package com.axzydev.puertonuevoapp.core.network.devicetypes

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Adaptador al contrato REAL del API (`/inventory/device-types`), que difiere
 * del contrato documentado (`/device-types`). La app mantiene su modelo
 * ([DeviceTypeDto]) y aquí se traduce la respuesta del backend.
 *
 * El backend expone 4 flags (`useSerialNumber/useMac/useIp/useHostname`), no
 * un `fieldConfig`; se mapean a los campos equivalentes y el resto queda apagado.
 */
private fun ApiDeviceType.toDto(): DeviceTypeDto = DeviceTypeDto(
    id = id,
    code = code,
    name = name,
    prefix = assetTagPrefix,
    counter = counter,
    active = active,
    fieldConfig = DeviceFieldConfigDto(
        serialNumber = DeviceFieldSettingDto(enabled = useSerialNumber),
        hostname = DeviceFieldSettingDto(enabled = useHostname),
        ip = DeviceFieldSettingDto(enabled = useIp),
        macAddress = DeviceFieldSettingDto(enabled = useMac),
        operatingSystem = DeviceFieldSettingDto(enabled = false),
        ram = DeviceFieldSettingDto(enabled = false),
        storage = DeviceFieldSettingDto(enabled = false),
    ),
    count = count?.let { DeviceTypeCountDto(it.devices) },
)

private fun DeviceFieldConfigDto.toUseFlags() = UseFlags(
    useSerialNumber = serialNumber.enabled,
    useMac = macAddress.enabled,
    useIp = ip.enabled,
    useHostname = hostname.enabled,
)

class DeviceTypesApi(private val client: ApiClient) {
    suspend fun list(includeInactive: Boolean = false): List<DeviceTypeDto> =
        client.get<List<ApiDeviceType>>("/inventory/device-types")
            .map { it.toDto() }
            .filter { includeInactive || it.active }

    suspend fun get(id: String): DeviceTypeDto =
        client.get<List<ApiDeviceType>>("/inventory/device-types")
            .firstOrNull { it.id == id }
            ?.toDto()
            ?: error("Tipo de dispositivo no encontrado")

    /** El backend no expone folios anticipados; se sintetiza el siguiente. */
    suspend fun peek(id: String): String {
        val type = get(id)
        return "${type.prefix}-${(type.counter + 1).toString().padStart(4, '0')}"
    }

    /** En el contrato real no existe folio de carta por tipo. */
    suspend fun peekCustodyLetter(id: String): String = ""

    suspend fun create(input: DeviceTypeCreateInput): DeviceTypeDto {
        val flags = input.fieldConfig?.toUseFlags() ?: UseFlags()
        return client.post<ApiCreateDeviceType, ApiDeviceType>(
            "/inventory/device-types",
            ApiCreateDeviceType(
                code = input.code,
                name = input.name,
                assetTagPrefix = input.prefix,
                useSerialNumber = flags.useSerialNumber,
                useMac = flags.useMac,
                useIp = flags.useIp,
                useHostname = flags.useHostname,
            ),
        ).toDto()
    }

    suspend fun update(id: String, input: DeviceTypeUpdateInput): DeviceTypeDto {
        val flags = input.fieldConfig?.toUseFlags()
        return client.put<ApiUpdateDeviceType, ApiDeviceType>(
            "/inventory/device-types/$id",
            ApiUpdateDeviceType(
                name = input.name,
                active = input.active,
                useSerialNumber = flags?.useSerialNumber,
                useMac = flags?.useMac,
                useIp = flags?.useIp,
                useHostname = flags?.useHostname,
            ),
        ).toDto()
    }

    suspend fun delete(id: String) = client.deleteNoContent("/inventory/device-types/$id")
}

private data class UseFlags(
    val useSerialNumber: Boolean = false,
    val useMac: Boolean = false,
    val useIp: Boolean = false,
    val useHostname: Boolean = false,
)

@Serializable
private data class ApiDeviceTypeCount(val devices: Int = 0)

@Serializable
private data class ApiDeviceType(
    val id: String,
    val code: String,
    val name: String,
    val assetTagPrefix: String = "",
    val counter: Int = 0,
    val active: Boolean = true,
    val useSerialNumber: Boolean = false,
    val useMac: Boolean = false,
    val useIp: Boolean = false,
    val useHostname: Boolean = false,
    @SerialName("_count") val count: ApiDeviceTypeCount? = null,
)

@Serializable
private data class ApiCreateDeviceType(
    val code: String,
    val name: String,
    val assetTagPrefix: String,
    val useSerialNumber: Boolean,
    val useMac: Boolean,
    val useIp: Boolean,
    val useHostname: Boolean,
)

@Serializable
private data class ApiUpdateDeviceType(
    val name: String? = null,
    val active: Boolean? = null,
    val useSerialNumber: Boolean? = null,
    val useMac: Boolean? = null,
    val useIp: Boolean? = null,
    val useHostname: Boolean? = null,
)
