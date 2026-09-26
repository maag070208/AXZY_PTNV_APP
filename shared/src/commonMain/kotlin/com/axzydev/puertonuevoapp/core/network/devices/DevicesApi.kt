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
 * Adaptador al contrato REAL del API (`/inventory/devices`), que difiere del
 * contrato documentado (`/devices`, `/device-types`). La app conserva su
 * modelo ([DeviceDto]) y aquí se traduce.
 *
 * El backend separa `Device` (modelo/lote) de `DeviceUnit` (activo fijo).
 * La lista de la app trabaja a nivel modelo, con conteos por estado
 * derivados de `stock` (`?stock=true`).
 */
class DevicesApi(private val client: ApiClient) {
    suspend fun list(status: String? = null, q: String? = null, typeId: String? = null): DeviceListResponseDto {
        val params = buildList {
            add("stock=true")
            typeId?.takeIf { it.isNotBlank() }?.let { add("typeId=$it") }
            q?.takeIf { it.isNotBlank() }?.let { add("q=${it.encodeURLParameter()}") }
        }
        val qs = "?" + params.joinToString("&")
        val all = client.get<List<ApiDevice>>("/inventory/devices$qs").map { it.toDeviceDto() }
        val filtered = status?.takeIf { it.isNotBlank() }?.let { e -> all.filter { it.status == e } } ?: all
        return DeviceListResponseDto(data = filtered, total = filtered.size)
    }

    suspend fun get(id: String): DeviceDto =
        client.get<ApiDevice>("/inventory/devices/$id").toDeviceDto()

    suspend fun create(input: DeviceCreateInput): DeviceDto {
        val body = ApiCreateDevice(
            typeId = input.typeId,
            name = input.description,
            brand = input.brand,
            model = input.model,
            description = null,
            notes = null,
            initialQuantity = 1,
            units = listOf(
                ApiUnitInput(
                    serialNumber = input.serialNumber,
                    macAddress = input.macAddress,
                    ip = input.ip,
                    hostname = input.hostname,
                )
            ),
        )
        return client.post<ApiCreateDevice, ApiDevice>("/inventory/devices", body).toDeviceDto()
    }

    suspend fun update(id: String, input: DeviceUpdateInput): DeviceDto {
        val body = ApiUpdateDevice(
            name = input.description,
            brand = input.brand,
            model = input.model,
        )
        return client.put<ApiUpdateDevice, ApiDevice>("/inventory/devices/$id", body).toDeviceDto()
    }

    suspend fun remove(id: String, force: Boolean = false): DeviceRemoveResponseDto {
        val data = client.delete<ApiDevice>("/inventory/devices/$id").toDeviceDto()
        return DeviceRemoveResponseDto(soft = !force, forced = force, data = data)
    }
}

private fun ApiDevice.toDeviceDto(): DeviceDto {
    val ex = stock
    val status = when {
        (ex?.ON_LOAN ?: 0) > 0 -> "ASSIGNED"
        (ex?.AVAILABLE ?: 0) > 0 -> "AVAILABLE"
        (ex?.IN_MAINTENANCE ?: 0) > 0 -> "AVAILABLE"
        (ex?.RETIRED ?: 0) > 0 -> "RETIRED"
        else -> "AVAILABLE"
    }
    return DeviceDto(
        id = id,
        typeId = typeId,
        type = type?.toDeviceTypeDto(),
        assetTag = name,
        description = name,
        brand = brand,
        model = model,
        area = "",
        status = status,
        batchId = id,
        batchSize = ex?.total,
        batchCount = ex?.let {
            DeviceBatchCountDto(available = it.AVAILABLE, assigned = it.ON_LOAN, retirement = it.RETIRED)
        },
        createdAt = "",
        updatedAt = "",
    )
}

private fun ApiDeviceType.toDeviceTypeDto(): DeviceTypeDto = DeviceTypeDto(
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

@Serializable
private data class ApiStock(
    val total: Int = 0,
    val AVAILABLE: Int = 0,
    val ON_LOAN: Int = 0,
    val DAMAGED: Int = 0,
    val IN_MAINTENANCE: Int = 0,
    val RETIRED: Int = 0,
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
private data class ApiDevice(
    val id: String,
    val typeId: String,
    val type: ApiDeviceType? = null,
    val name: String,
    val brand: String,
    val model: String,
    val description: String? = null,
    val notes: String? = null,
    val stock: ApiStock? = null,
)

@Serializable
private data class ApiUnitInput(
    val serialNumber: String? = null,
    val macAddress: String? = null,
    val ip: String? = null,
    val hostname: String? = null,
)

@Serializable
private data class ApiCreateDevice(
    val typeId: String,
    val name: String,
    val brand: String,
    val model: String,
    val description: String? = null,
    val notes: String? = null,
    val initialQuantity: Int = 1,
    val units: List<ApiUnitInput> = emptyList(),
)

@Serializable
private data class ApiUpdateDevice(
    val name: String? = null,
    val brand: String? = null,
    val model: String? = null,
)
